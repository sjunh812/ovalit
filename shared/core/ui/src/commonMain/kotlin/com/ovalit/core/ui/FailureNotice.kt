package com.ovalit.core.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import com.ovalit.core.designsystem.component.LocalOvalitToast
import com.ovalit.core.model.OvalitError
import com.ovalit.core.model.ovalitError
import com.ovalit.core.ui.resources.Res
import com.ovalit.core.ui.resources.error_offline
import com.ovalit.core.ui.resources.error_riot_busy
import com.ovalit.core.ui.resources.error_riot_down
import com.ovalit.core.ui.resources.error_session_expired
import com.ovalit.core.ui.resources.failed_accept_friend
import com.ovalit.core.ui.resources.failed_decline_friend
import com.ovalit.core.ui.resources.failed_delete_data
import com.ovalit.core.ui.resources.failed_friend_request
import com.ovalit.core.ui.resources.failed_friends_refresh
import com.ovalit.core.ui.resources.failed_ping_already_active
import com.ovalit.core.ui.resources.failed_ping_cancel
import com.ovalit.core.ui.resources.failed_ping_full
import com.ovalit.core.ui.resources.failed_ping_invite
import com.ovalit.core.ui.resources.failed_ping_reply
import com.ovalit.core.ui.resources.failed_ping_send
import com.ovalit.core.ui.resources.failed_ping_time
import com.ovalit.core.ui.resources.failed_refresh
import com.ovalit.core.ui.resources.failed_rival
import com.ovalit.core.ui.resources.failed_setting
import com.ovalit.core.ui.resources.failed_unfriend
import com.ovalit.core.ui.resources.failed_unlink
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.getString

/** 사용자가 한 일 중 무엇이 실패했는지입니다. 까닭을 모를 때 이것으로 "수락하지 못했어요"처럼 적습니다. */
enum class FailedAction {
    REFRESH,
    FRIENDS_REFRESH,
    ACCEPT_FRIEND,
    DECLINE_FRIEND,
    FRIEND_REQUEST,
    UNFRIEND,
    RIVAL,
    PING_SEND,
    PING_REPLY,
    PING_TIME,
    PING_INVITE,
    PING_CANCEL,

    /** 다른 기기에서 그사이 초대를 보내 하나 더 보낼 수 없었습니다. */
    PING_ALREADY_ACTIVE,

    /** 다른 기기에서 그사이 더 불러 자리가 찼습니다. */
    PING_FULL,
    SETTING,
    DELETE_DATA,
    UNLINK,
}

/**
 * 화면 아래에 잠깐 띄울 실패 안내입니다. [error]가 [OvalitError.Unknown]이 아니면 까닭을 적고("인터넷에 연결되어 있지
 * 않아요"), 모르면 무엇을 못 했는지 적습니다("수락하지 못했어요").
 */
data class FailureNotice(val action: FailedAction, val error: OvalitError = OvalitError.Unknown)

/**
 * ViewModel이 실패를 화면으로 넘기는 통로입니다. 화면이 잠깐 없을 때 생긴 안내는 몇 개까지 쌓아 두었다가 다시 붙으면
 * 띄웁니다. 화면은 [FailureNoticesEffect]로 받습니다.
 */
class FailureNotices {
    private val channel = Channel<FailureNotice>(capacity = 4, onBufferOverflow = BufferOverflow.DROP_OLDEST)

    val flow: Flow<FailureNotice> = channel.receiveAsFlow()

    fun send(notice: FailureNotice) {
        channel.trySend(notice)
    }
}

/**
 * [block]을 띄우고, 실패하면 삼키지 않고 [notices]로 알립니다. 취소는 그대로 던집니다. 실패해도 앱이 죽지 않습니다.
 * `viewModelScope`에는 예외 처리기가 없어서 그냥 `launch`에서 던지면 앱이 죽습니다.
 */
fun CoroutineScope.launchNotifying(
    notices: FailureNotices,
    action: FailedAction,
    onFailure: () -> Unit = {},
    block: suspend CoroutineScope.() -> Unit,
): Job = launch {
    try {
        block()
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        onFailure()
        notices.send(FailureNotice(action, e.ovalitError))
    }
}

/** [notices]가 보낸 안내를 앱 맨 위의 토스트로 띄웁니다. 토스트가 없으면(프리뷰, UI 테스트) 아무것도 하지 않습니다. */
@Composable
fun FailureNoticesEffect(notices: Flow<FailureNotice>) {
    val toast = LocalOvalitToast.current ?: return
    LaunchedEffect(notices, toast) {
        notices.collect { toast.show(getString(it.text)) }
    }
}

private val FailureNotice.text: StringResource
    get() = when (error) {
        OvalitError.Offline -> Res.string.error_offline
        OvalitError.SessionExpired -> Res.string.error_session_expired
        OvalitError.RiotBusy -> Res.string.error_riot_busy
        OvalitError.RiotDown -> Res.string.error_riot_down
        OvalitError.Unknown -> when (action) {
            FailedAction.REFRESH -> Res.string.failed_refresh
            FailedAction.FRIENDS_REFRESH -> Res.string.failed_friends_refresh
            FailedAction.ACCEPT_FRIEND -> Res.string.failed_accept_friend
            FailedAction.DECLINE_FRIEND -> Res.string.failed_decline_friend
            FailedAction.FRIEND_REQUEST -> Res.string.failed_friend_request
            FailedAction.UNFRIEND -> Res.string.failed_unfriend
            FailedAction.RIVAL -> Res.string.failed_rival
            FailedAction.PING_SEND -> Res.string.failed_ping_send
            FailedAction.PING_REPLY -> Res.string.failed_ping_reply
            FailedAction.PING_TIME -> Res.string.failed_ping_time
            FailedAction.PING_INVITE -> Res.string.failed_ping_invite
            FailedAction.PING_CANCEL -> Res.string.failed_ping_cancel
            FailedAction.PING_ALREADY_ACTIVE -> Res.string.failed_ping_already_active
            FailedAction.PING_FULL -> Res.string.failed_ping_full
            FailedAction.SETTING -> Res.string.failed_setting
            FailedAction.DELETE_DATA -> Res.string.failed_delete_data
            FailedAction.UNLINK -> Res.string.failed_unlink
        }
    }
