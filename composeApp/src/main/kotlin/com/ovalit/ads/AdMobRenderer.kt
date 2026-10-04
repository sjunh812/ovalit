package com.ovalit.ads

import android.content.Context
import android.os.SystemClock
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.ui.unit.dp
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdLoader
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.nativead.NativeAd
import com.google.android.gms.ads.nativead.NativeAdOptions
import com.ovalit.BuildConfig
import com.ovalit.core.ui.AdPlacement
import com.ovalit.core.ui.AdRenderer
import kotlin.time.Duration.Companion.hours
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * AdMob 네이티브 광고를 받아 광고 자리에 그립니다. 광고 단위 ID가 없으면 [enabled]가 `false`라 앱이 이 렌더러를 깔지 않고,
 * 광고 자리는 비어 있습니다.
 *
 * 받은 광고는 자리 키마다 하나를 들고 있다가 같은 키가 다시 보이면 그대로 씁니다. 경기 목록을 올렸다 내릴 때마다 새로 요청하지
 * 않으려는 것입니다. 들고 있는 건 [MAX_ADS]개까지이고, 한 시간이 지난 광고는 버리고 새로 받습니다. AdMob은 받은 광고를 한 시간
 * 넘게 두지 말라고 합니다. 못 받은 키는 이 화면이 사는 동안 다시 요청하지 않습니다.
 */
internal class AdMobRenderer(private val context: Context) : AdRenderer {

    private val ads = mutableStateMapOf<String, LoadedAd>()
    private val loading = mutableSetOf<String>()
    private val failed = mutableSetOf<String>()

    @Composable
    override fun Render(placement: AdPlacement, key: String, frame: @Composable (content: @Composable () -> Unit) -> Unit) {
        val loaded = ads[key]
        LaunchedEffect(key) {
            val current = ads[key]
            if (current != null && SystemClock.elapsedRealtime() - current.loadedAt > AD_LIFETIME.inWholeMilliseconds) {
                ads.remove(key)
                current.ad.destroy()
            }
            if (ads[key] == null) load(key)
        }
        // 홈은 카드 안쪽 여백이 있어서 줄 여백을 빼야 다른 카드와 높이가 맞는다
        if (loaded != null) frame { NativeAdRow(loaded.ad, verticalPadding = if (placement == AdPlacement.HOME) 0.dp else 13.dp) }
    }

    fun destroy() {
        ads.values.forEach { it.ad.destroy() }
        ads.clear()
    }

    private fun load(key: String) {
        if (key in loading || key in failed) return
        loading += key
        AdLoader.Builder(context, BuildConfig.ADMOB_NATIVE_UNIT_ID)
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
                    .setAdChoicesPlacement(NativeAdOptions.ADCHOICES_TOP_RIGHT)
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

    companion object {
        val enabled: Boolean get() = BuildConfig.ADMOB_NATIVE_UNIT_ID.isNotBlank()

        /** SDK를 띄웁니다. 메인 스레드를 막지 않게 뒤에서 합니다. 광고가 꺼져 있으면 아무것도 하지 않습니다. */
        fun start(context: Context, scope: CoroutineScope) {
            if (!enabled) return
            scope.launch(Dispatchers.IO) { MobileAds.initialize(context) }
        }

        private const val MAX_ADS = 6
        private val AD_LIFETIME = 1.hours
    }
}
