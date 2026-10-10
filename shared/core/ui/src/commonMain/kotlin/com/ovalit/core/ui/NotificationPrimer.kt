package com.ovalit.core.ui

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.ovalit.core.designsystem.component.OvalitBottomSheet
import com.ovalit.core.designsystem.component.OvalitPrimaryButton
import com.ovalit.core.designsystem.component.OvalitTextButton
import com.ovalit.core.designsystem.theme.OvalitSpacing
import com.ovalit.core.ui.resources.Res
import com.ovalit.core.ui.resources.notification_primer_allow
import com.ovalit.core.ui.resources.notification_primer_body
import com.ovalit.core.ui.resources.notification_primer_later
import com.ovalit.core.ui.resources.notification_primer_title
import org.jetbrains.compose.resources.stringResource

/**
 * 이 앱이 알림을 보낼 수 있는지와, 못 보내면 켜 달라고 묻는 길입니다.
 * 플랫폼마다 달라서 앱 모듈이 만들어 넘깁니다.
 *
 * @property missing 알림 권한이 없거나 휴대폰 설정에서 이 앱 알림을 꺼 두었는지입니다.
 * @property request 시스템 권한 창을 띄웁니다.
 *   시스템이 더는 창을 띄우지 않으면 이 앱의 알림 설정 화면을 엽니다.
 */
class NotificationPermission(val missing: Boolean, val request: () -> Unit) {
    companion object {
        /** 물을 게 없는 상태입니다. 알림을 보내지 않는 iOS와 프리뷰, UI 테스트가 씁니다. */
        val NotNeeded = NotificationPermission(missing = false, request = {})
    }
}

/**
 * 알림을 꺼 둔 사람에게 알림이 쓸모 있어지는 순간 "친구가 부르면 알려드릴까요?"를 한 번만 묻습니다.
 * 친구 요청을 수락했을 때와 파티를 모집했을 때 [offer]를 부릅니다.
 * 앱을 처음 열 때 묻지 않는 건 무엇을 알려 주는지 모른 채 거절하기 쉬워서입니다.
 * 시트는 [NotificationPrimerHost]가 그립니다.
 */
@Stable
class NotificationPrimer internal constructor(
    private val permission: () -> NotificationPermission,
    private val seen: () -> Boolean,
    private val onSeen: () -> Unit,
) {
    var isShown by mutableStateOf(false)
        private set

    // 띄웠다고 적은 값이 돌아오기 전에 또 불려도 한 번만 띄운다
    private var offered = false

    /** 알림이 꺼져 있고 아직 물은 적이 없으면 시트를 띄웁니다. 띄우자마자 물었다고 적습니다. */
    fun offer() {
        if (offered || seen() || !permission().missing) return
        offered = true
        isShown = true
        onSeen()
    }

    internal fun allow() {
        isShown = false
        permission().request()
    }

    internal fun later() {
        isShown = false
    }
}

/**
 * @param seen 전에 물은 적이 있는지입니다.
 *   기기에 적어 둔 값을 읽기 전에는 `true`로 넘겨 묻지 않습니다.
 * @param onSeen 시트를 띄웠을 때 부릅니다.
 *   기기에 적어 다시 묻지 않게 합니다.
 */
@Composable
fun rememberNotificationPrimer(permission: NotificationPermission, seen: Boolean, onSeen: () -> Unit): NotificationPrimer {
    val currentPermission by rememberUpdatedState(permission)
    val currentSeen by rememberUpdatedState(seen)
    val currentOnSeen by rememberUpdatedState(onSeen)
    return remember { NotificationPrimer({ currentPermission }, { currentSeen }, { currentOnSeen() }) }
}

/** [primer]가 띄우면 알림을 켜 달라는 시트를 그립니다. 앱 맨 위에 하나만 둡니다. */
@Composable
fun NotificationPrimerHost(primer: NotificationPrimer) {
    if (primer.isShown) NotificationPrimerSheet(onAllow = primer::allow, onLater = primer::later)
}

@Composable
internal fun NotificationPrimerSheet(onAllow: () -> Unit, onLater: () -> Unit) {
    OvalitBottomSheet(
        title = stringResource(Res.string.notification_primer_title),
        body = stringResource(Res.string.notification_primer_body),
        onDismiss = onLater,
    ) {
        OvalitPrimaryButton(text = stringResource(Res.string.notification_primer_allow), onClick = onAllow)
        Spacer(Modifier.height(OvalitSpacing.xs))
        OvalitTextButton(text = stringResource(Res.string.notification_primer_later), onClick = onLater, modifier = Modifier.fillMaxWidth())
    }
}
