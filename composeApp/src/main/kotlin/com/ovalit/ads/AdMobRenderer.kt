package com.ovalit.ads

import android.app.Activity
import android.content.Context
import android.os.SystemClock
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.dp
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdLoader
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.nativead.NativeAd
import com.google.android.gms.ads.nativead.NativeAdOptions
import com.ovalit.BuildConfig
import com.ovalit.core.data.Analytics
import com.ovalit.core.data.AnalyticsEvents
import com.ovalit.core.data.UserPreferencesRepository
import com.ovalit.core.ui.AdPlacement
import com.ovalit.core.ui.AdRenderer
import kotlin.time.Duration.Companion.hours
import kotlin.time.Instant
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * AdMob 네이티브 광고를 받아 광고 자리에 그립니다. 광고 단위 ID가 없으면 [enabled]가 `false`라 앱이 이 렌더러를 깔지 않고,
 * 광고 자리는 비어 있습니다.
 *
 * 받은 광고는 자리 키마다 하나를 들고 있다가 같은 키가 다시 보이면 그대로 씁니다. 경기 목록을 올렸다 내릴 때마다 새로 요청하지
 * 않으려는 것입니다. 들고 있는 건 [MAX_ADS]개까지이고, 한 시간이 지난 광고는 버리고 새로 받습니다. AdMob은 받은 광고를 한 시간
 * 넘게 두지 말라고 합니다. 못 받은 키는 이 화면이 사는 동안 다시 요청하지 않습니다.
 *
 * 보상형 광고를 끝까지 보면 24시간 동안 광고 자리를 비웁니다([offerAdFree]). 끝나는 시각은 설정에 적어 앱을 다시 켜도 이어집니다.
 */
internal class AdMobRenderer(
    private val activity: Activity,
    private val preferences: UserPreferencesRepository,
    private val scope: CoroutineScope,
    private val analytics: Analytics,
) : AdRenderer {

    private val ads = mutableStateMapOf<String, LoadedAd>()
    private val loading = mutableSetOf<String>()
    private val failed = mutableSetOf<String>()

    // 설정을 읽기 전에는 숨길지 모르니 광고를 그리지 않는다. 그리다가 숨기면 광고가 한 번 번쩍인다.
    private val adFreeUntilMillis = preferences.preferences
        .map { it.adFreeUntil?.toEpochMilliseconds() ?: NOT_HIDDEN }
        .stateIn(scope, SharingStarted.Eagerly, UNKNOWN)

    // 이 화면이 사는 동안 ×로 닫은 광고 자리다. 다시 스크롤해 와도 그 자리는 비워 둔다.
    private val closed = mutableStateListOf<String>()

    // 띄울 시트다. closeKey가 있으면 광고 줄의 ×에서 연 것이다.
    private var sheet by mutableStateOf<AdSheet?>(null)

    override val canOfferAdFree: Boolean get() = BuildConfig.ADMOB_REWARDED_UNIT_ID.isNotBlank()

    override fun offerAdFree() {
        if (canOfferAdFree) sheet = AdSheet(closeKey = null)
    }

    // 보상형 광고가 없으면 물을 게 없어 바로 닫는다
    private fun onClose(key: String) {
        if (canOfferAdFree) sheet = AdSheet(closeKey = key) else close(key)
    }

    private fun close(key: String) {
        closed += key
        ads.remove(key)?.ad?.destroy()
    }

    @Composable
    override fun Render(placement: AdPlacement, key: String, frame: @Composable (content: @Composable () -> Unit) -> Unit) {
        val hiddenUntil by adFreeUntilMillis.collectAsState()
        if (hiddenUntil == UNKNOWN || System.currentTimeMillis() < hiddenUntil || key in closed) return
        val loaded = ads[key]
        LaunchedEffect(key) {
            val current = ads[key]
            if (current != null && SystemClock.elapsedRealtime() - current.loadedAt > AD_LIFETIME.inWholeMilliseconds) {
                ads.remove(key)
                current.ad.destroy()
            }
            if (ads[key] == null) load(key)
        }
        // 카드에 담는 자리는 카드 안쪽 여백이 있어서 줄 여백을 빼야 다른 카드와 높이가 맞는다
        if (loaded != null) {
            frame {
                NativeAdRow(
                    ad = loaded.ad,
                    verticalPadding = if (placement.inCard) 0.dp else 16.dp,
                    onClose = { onClose(key) },
                )
            }
        }
    }

    /** 앱 맨 위에 까는 시트입니다. [offerAdFree]를 부르면 뜹니다. */
    @Composable
    fun Sheets() {
        val shown = sheet ?: return
        AdFreeSheet(
            activity = activity,
            unitId = BuildConfig.ADMOB_REWARDED_UNIT_ID,
            onEarned = {
                analytics.log(AnalyticsEvents.AD_FREE_START, mapOf("entry" to if (shown.closeKey != null) "ad_row" else "settings"))
                val until = Instant.fromEpochMilliseconds(System.currentTimeMillis() + AD_FREE.inWholeMilliseconds)
                scope.launch { preferences.setAdFreeUntil(until) }
            },
            onDismiss = { sheet = null },
            onCloseOne = shown.closeKey?.let { key -> { close(key) } },
        )
    }

    fun destroy() {
        ads.values.forEach { it.ad.destroy() }
        ads.clear()
    }

    private fun load(key: String) {
        if (key in loading || key in failed) return
        loading += key
        AdLoader.Builder(activity, BuildConfig.ADMOB_NATIVE_UNIT_ID)
            .forNativeAd { ad ->
                loading -= key
                ads[key] = LoadedAd(ad, SystemClock.elapsedRealtime())
                trim()
            }
            .withAdListener(
                object : AdListener() {
                    override fun onAdFailedToLoad(error: LoadAdError) {
                        loading -= key
                        failed += key
                    }
                },
            )
            // 큰 그림이나 영상은 쓰지 않는다. 경기 줄과 같은 높이의 한 줄이라 아이콘, 제목, 버튼만 둔다.
            .withNativeAdOptions(
                NativeAdOptions.Builder()
                    // 오른쪽 끝에 우리 ×가 있어서 AdChoices 표시는 왼쪽 위 여백에 둔다. 겹치면 무엇을 눌렀는지 헷갈린다.
                    .setAdChoicesPlacement(NativeAdOptions.ADCHOICES_TOP_LEFT)
                    .setReturnUrlsForImageAssets(false)
                    .build(),
            )
            .build()
            .loadAd(AdRequest.Builder().build())
    }

    // 오래된 것부터 버린다. 지금 보이는 광고는 가장 최근에 받은 것들이라 남는다.
    private fun trim() {
        while (ads.size > MAX_ADS) {
            val oldest = ads.entries.minBy { it.value.loadedAt }
            ads.remove(oldest.key)
            oldest.value.ad.destroy()
        }
    }

    private class LoadedAd(val ad: NativeAd, val loadedAt: Long)

    private data class AdSheet(val closeKey: String?)

    companion object {
        val enabled: Boolean get() = BuildConfig.ADMOB_NATIVE_UNIT_ID.isNotBlank()

        /** SDK를 띄웁니다. 메인 스레드를 막지 않게 뒤에서 합니다. 광고가 꺼져 있으면 아무것도 하지 않습니다. */
        fun start(context: Context, scope: CoroutineScope) {
            if (!enabled) return
            scope.launch(Dispatchers.IO) { MobileAds.initialize(context) }
        }

        private const val MAX_ADS = 6
        private val AD_LIFETIME = 1.hours

        // 보상형 광고 한 편으로 광고를 숨기는 시간이다(사용자 결정, 2026-10-04). 하루 몇 번 짧게 여는 앱이라 짧으면 남는 게 없다.
        // 다시 보려면 끝날 때까지 기다려야 해서 하루 한 번이 저절로 지켜진다.
        private val AD_FREE = 24.hours

        private const val UNKNOWN = -1L
        private const val NOT_HIDDEN = 0L
    }
}
