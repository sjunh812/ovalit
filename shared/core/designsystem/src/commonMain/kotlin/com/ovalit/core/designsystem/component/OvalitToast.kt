package com.ovalit.core.designsystem.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.ovalit.core.designsystem.theme.OvalitSpacing
import com.ovalit.core.designsystem.theme.OvalitTheme
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * 화면 아래에 잠깐 띄우는 한 줄 안내입니다. 사용자가 한 일이 실패했을 때와 홈·경기 탭을 당겨 새 경기를 받은 결과에 씁니다.
 * 저절로 일어난 일(백그라운드 새 경기 확인, 광고)은 조용히 넘깁니다. [show]를 겹쳐 부르면 앞의 안내가 사라진 뒤에 차례로 띄웁니다.
 */
@Stable
class OvalitToastState {
    var message: String? by mutableStateOf(null)
        private set

    private val queue = Mutex()

    suspend fun show(text: String, duration: Duration = 3.seconds) {
        queue.withLock {
            message = text
            try {
                delay(duration)
            } finally {
                message = null
            }
            // 사라지는 모습이 끝난 뒤에 다음 안내를 띄운다
            delay(ExitPause)
        }
    }
}

private val ExitPause = 0.2.seconds

/** 앱 맨 위에서 채웁니다. `null`이면(프리뷰, UI 테스트) 안내를 띄우지 않습니다. */
val LocalOvalitToast = staticCompositionLocalOf<OvalitToastState?> { null }

@Composable
fun rememberOvalitToastState(): OvalitToastState = remember { OvalitToastState() }

/** [state]의 안내를 그립니다. 놓을 자리(탭바 위 등)는 부르는 쪽이 [modifier]로 정합니다. */
@Composable
fun OvalitToastHost(state: OvalitToastState, modifier: Modifier = Modifier) {
    val colors = OvalitTheme.colors
    // 사라지는 동안에도 마지막 글자를 그대로 둔다
    val last = remember { arrayOf("") }
    state.message?.let { last[0] = it }
    AnimatedVisibility(
        visible = state.message != null,
        modifier = modifier,
        enter = slideInVertically { it / 2 } + fadeIn(),
        exit = slideOutVertically { it / 2 } + fadeOut(),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = OvalitSpacing.gutter)
                .clip(RoundedCornerShape(12.dp))
                .background(colors.t1)
                .semantics { liveRegion = LiveRegionMode.Polite }
                .padding(horizontal = 16.dp, vertical = 13.dp),
        ) {
            OvalitText(text = last[0], style = OvalitTheme.typography.body, color = colors.bg)
        }
    }
}
