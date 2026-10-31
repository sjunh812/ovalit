package com.ovalit.ads

import android.content.Context
import android.os.SystemClock
import androidx.activity.ComponentActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdLoader
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.VideoOptions
import com.google.android.gms.ads.nativead.NativeAd
import com.google.android.gms.ads.nativead.NativeAdOptions
import com.ovalit.BuildConfig
import com.ovalit.core.data.Analytics
import com.ovalit.core.data.AnalyticsEvents
import com.ovalit.core.data.UserPreferencesRepository
import com.ovalit.core.designsystem.theme.OvalitSpacing
import com.ovalit.core.ui.AdPlacement
import com.ovalit.core.ui.AdRenderer
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Instant
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.transformLatest
import kotlinx.coroutines.launch

/**
 * AdMob 네이티브 광고를 받아 광고 자리에 그립니다.
 * 광고 단위 ID가 없으면 [enabled]가 `false`라 앱이 이 렌더러를 깔지 않습니다.
 *
 * 받은 광고는 자리 키마다 하나씩 [MAX_ADS]개까지 들고 있다가 같은 키가 다시 보이면 그대로 써서 목록을 오르내려도 새로 요청하지 않습니다.
 * AdMob이 받은 광고를 한 시간 넘게 두지 말라고 해서 한 시간이 지나면 버리고 새로 받습니다.
 * 앱이 [REFRESH_AFTER_AWAY] 넘게 가려졌다 돌아오면 보이는 자리의 광고를 새로 받습니다.
 * 새 광고가 올 때까지 옛 광고를 그대로 둬서 자리가 비었다 차지 않습니다.
 * 못 받은 키는 [RETRY_AFTER]가 지난 뒤 그 자리가 다시 그려질 때 한 번 더 요청합니다.
 * 바로 다시 요청하면 채울 광고가 없을 때 요청만 쌓입니다.
 *
 * 보상형 광고를 끝까지 보면 24시간 동안 광고 자리를 비웁니다([offerAdFree]).
 * 끝나는 시각은 설정에 적어 앱을 다시 켜도 이어집니다.
 */
internal class AdMobRenderer(
    private val activity: ComponentActivity,
    private val preferences: UserPreferencesRepository,
    private val scope: CoroutineScope,
    private val analytics: Analytics,
) : AdRenderer {

    private val ads = mutableStateMapOf<String, LoadedAd>()
    private val loading = mutableSetOf<String>()

    // 못 받은 키와 그 시각이다
    private val failedAt = mutableMapOf<String, Long>()

    // 앱이 오래 가려졌다 돌아올 때마다 하나씩 올린다.
    // 보이는 자리는 이 값이 바뀌면 받은 지 오래된 광고를 새로 받는다.
    private var generation by mutableIntStateOf(0)
    private var stoppedAt: Long? = null

    // 앱이 다시 보일 때마다 하나씩 올리고, 그때마다 광고 없이 보기가 끝났는지 벽시계로 다시 본다.
    // delay는 기기가 잠든 동안 멈춰서 밤새 화면을 잠가 두면 끝나는 시각이 그만큼 밀린다.
    private val starts = MutableStateFlow(0)

    // destroy() 뒤에 늦게 온 광고는 담지 않고 바로 버린다.
    // 담으면 끝내 destroy()되지 않고 액티비티를 붙잡는다.
    private var destroyed = false

    init {
        activity.lifecycle.addObserver(
            LifecycleEventObserver { _, event ->
                when (event) {
                    Lifecycle.Event.ON_STOP -> stoppedAt = SystemClock.elapsedRealtime()
                    Lifecycle.Event.ON_START -> {
                        val away = stoppedAt?.let { SystemClock.elapsedRealtime() - it }
                        stoppedAt = null
                        if (away != null && away >= REFRESH_AFTER_AWAY.inWholeMilliseconds) generation++
                        starts.value++
                    }
                    else -> Unit
                }
            },
        )
    }

    // 설정을 읽기 전에는 숨길지 모르니 `null`이고 광고를 그리지 않는다.
    // 그리다가 숨기면 광고가 한 번 번쩍인다.
    // 광고 없이 보기가 끝나는 순간 `false`로 바꾼다.
    // 그릴 때만 시각을 견주면 홈처럼 광고 자리가 계속 화면에 남는 곳은 끝나도 다른 화면을 오가기 전까지 광고가 돌아오지 않는다.
    @OptIn(ExperimentalCoroutinesApi::class)
    private val hidden: StateFlow<Boolean?> = preferences.preferences
        .map { it.adFreeUntil?.toEpochMilliseconds() }
        .distinctUntilChanged()
        .combine(starts) { until, _ -> until }
        .transformLatest { until ->
            val left = until?.let { it - System.currentTimeMillis() } ?: 0L
            if (left > 0) {
                emit(true)
                delay(left)
            }
            emit(false)
        }
        .distinctUntilChanged()
        .stateIn(scope, SharingStarted.Eagerly, null)

    init {
        // 광고 없이 보는 24시간 동안 받아 둔 광고를 들고 있지 않는다.
        // 끝나면 자리마다 새로 받는다.
        scope.launch {
            hidden.collect { if (it == true) dropAll() }
        }
    }

    // ×로 닫은 광고 자리다.
    // 다시 스크롤해 와도 렌더러가 사는 동안은 비워 둔다.
    private val closed = mutableStateListOf<String>()

    // 띄울 시트다.
    // closeKey가 있으면 광고 줄의 ×에서 연 것이다.
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
        val hidden by hidden.collectAsState()
        if (hidden != false || key in closed) return
        val loaded = ads[key]
        LaunchedEffect(key, generation) {
            val current = ads[key]
            val age = current?.let { SystemClock.elapsedRealtime() - it.loadedAt }
            when {
                current == null -> load(key, placement)
                age != null && age > AD_LIFETIME.inWholeMilliseconds -> {
                    ads.remove(key)
                    current.ad.destroy()
                    load(key, placement)
                }
                // 오래 가려졌다 돌아왔으면 옛 광고를 둔 채 새로 받아 바꾼다
                current.generation < generation -> load(key, placement)
            }
        }
        // 한 시간이 지난 광고는 새로 받는 동안에도 그리지 않는다(AdMob).
        // 카드에 담는 자리는 카드 안쪽 여백이 있어서 줄 여백을 빼야 다른 카드와 높이가 맞는다.
        if (loaded != null && SystemClock.elapsedRealtime() - loaded.loadedAt <= AD_LIFETIME.inWholeMilliseconds) {
            val verticalPadding = if (placement.inCard) 0.dp else 16.dp
            // 화면에 바로 놓인 목록에서는 위아래 경기 줄과 같은 화면 여백을 쓴다
            val horizontalPadding = if (placement.inCard) OvalitSpacing.gutter else OvalitSpacing.screen
            frame {
                if (placement.media) {
                    NativeAdCard(loaded.ad, verticalPadding, horizontalPadding, onClose = { onClose(key) })
                } else {
                    NativeAdRow(loaded.ad, verticalPadding, horizontalPadding, onClose = { onClose(key) })
                }
            }
        }
    }

    /** 앱 맨 위에 깔아 두는 시트 자리입니다. [offerAdFree]를 부르거나 광고 줄의 ×를 누르면 뜹니다. */
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
        destroyed = true
        dropAll()
    }

    private fun dropAll() {
        ads.values.forEach { it.ad.destroy() }
        ads.clear()
    }

    private fun load(key: String, placement: AdPlacement) {
        val now = SystemClock.elapsedRealtime()
        if (key in loading || failedAt[key]?.let { now - it < RETRY_AFTER.inWholeMilliseconds } == true) return
        loading += key
        val requested = generation
        AdLoader.Builder(activity, BuildConfig.ADMOB_NATIVE_UNIT_ID)
            .forNativeAd { ad ->
                loading -= key
                failedAt -= key
                // 닫은 자리나 화면이 사라진 뒤에 늦게 온 광고는 쓰지 않는다
                if (destroyed || key in closed || hidden.value == true) {
                    ad.destroy()
                    return@forNativeAd
                }
                ads.put(key, LoadedAd(ad, SystemClock.elapsedRealtime(), requested))?.ad?.destroy()
                trim()
            }
            .withAdListener(
                object : AdListener() {
                    override fun onAdFailedToLoad(error: LoadAdError) {
                        loading -= key
                        failedAt[key] = SystemClock.elapsedRealtime()
                    }
                },
            )
            .withNativeAdOptions(nativeAdOptions(placement))
            .build()
            .loadAd(AdRequest.Builder().build())
    }

    // 오른쪽 끝에 우리 ×가 있어서 AdChoices 표시는 왼쪽 위에 둔다.
    // 한 줄 광고는 경기 줄과 같은 높이라 큰 그림을 받지 않는다.
    // 카드 광고는 정사각 그림을 먼저 달라고 하고 영상은 소리 없이 시작한다.
    // 소리를 내면 사용자가 듣던 음악이 끊긴다.
    private fun nativeAdOptions(placement: AdPlacement): NativeAdOptions = NativeAdOptions.Builder()
        .setAdChoicesPlacement(NativeAdOptions.ADCHOICES_TOP_LEFT)
        .apply {
            if (placement.media) {
                setMediaAspectRatio(NativeAdOptions.NATIVE_MEDIA_ASPECT_RATIO_SQUARE)
                setVideoOptions(VideoOptions.Builder().setStartMuted(true).build())
            } else {
                setReturnUrlsForImageAssets(false)
            }
        }
        .build()

    // 오래된 것부터 버린다.
    // 지금 보이는 광고는 가장 최근에 받은 것들이라 남는다.
    private fun trim() {
        while (ads.size > MAX_ADS) {
            val oldest = ads.entries.minBy { it.value.loadedAt }
            ads.remove(oldest.key)
            oldest.value.ad.destroy()
        }
    }

    private class LoadedAd(val ad: NativeAd, val loadedAt: Long, val generation: Int)

    private data class AdSheet(val closeKey: String?)

    companion object {
        val enabled: Boolean get() = BuildConfig.ADMOB_NATIVE_UNIT_ID.isNotBlank()

        /** SDK를 띄웁니다. 메인 스레드를 막지 않게 뒤에서 합니다. 광고가 꺼져 있으면 아무것도 하지 않습니다. */
        fun start(context: Context, scope: CoroutineScope) {
            if (!enabled) return
            scope.launch(Dispatchers.IO) { MobileAds.initialize(context) }
        }

        // 경기 상세와 친구 프로필은 경기와 친구마다 자리가 따로다.
        // 여러 화면을 오가다 돌아와도 받아 둔 광고가 남아 있을 만큼 든다.
        private const val MAX_ADS = 10
        private val AD_LIFETIME = 1.hours

        // 새 경기를 다시 확인하는 간격(NewMatchesWatcher)과 같다.
        // 시작 기준선이다.
        private val REFRESH_AFTER_AWAY = 10.minutes
        private val RETRY_AFTER = 1.minutes

        // 하루 몇 번 짧게 여는 앱이라 이보다 짧으면 다음에 열 때쯤 끝나 있다.
        // 다시 보려면 끝날 때까지 기다려야 해서 하루 한 번이 저절로 지켜진다.
        private val AD_FREE = 24.hours

    }
}
