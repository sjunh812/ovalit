package com.ovalit.core.designsystem.haptic

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback

/**
 * 무언가를 확정하는 동작에만 짧게 울립니다. 친구 수락·거절, 요청 보내기, 라이벌 지정, 설정 토글이 그렇습니다.
 * 스크롤, 탭 이동, 칩에는 쓰지 않습니다. 누를 때마다 울리면 알림처럼 느껴집니다.
 */
@Stable
class OvalitHaptics internal constructor(private val feedback: HapticFeedback) {
    fun confirm() = feedback.performHapticFeedback(HapticFeedbackType.Confirm)

    fun reject() = feedback.performHapticFeedback(HapticFeedbackType.Reject)

    fun toggle(on: Boolean) = feedback.performHapticFeedback(if (on) HapticFeedbackType.ToggleOn else HapticFeedbackType.ToggleOff)
}

@Composable
fun rememberOvalitHaptics(): OvalitHaptics {
    val feedback = LocalHapticFeedback.current
    return remember(feedback) { OvalitHaptics(feedback) }
}
