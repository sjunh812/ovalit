package com.ovalit.core.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import com.ovalit.core.designsystem.component.LocalOvalitToast
import com.ovalit.core.ui.resources.Res
import com.ovalit.core.ui.resources.refresh_none
import com.ovalit.core.ui.resources.refresh_received
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import org.jetbrains.compose.resources.getString

/**
 * 홈과 경기 탭을 당겨 새 경기를 받은 결과를 화면으로 넘기는 통로입니다.
 * 화면은 [RefreshResultsEffect]로 받아 "새 경기 3판을 불러왔어요"를 한 줄 띄웁니다.
 * 앱이 다시 보일 때처럼 저절로 받은 것은 보내지 않습니다.
 *
 * [FailureNotices]와 달리 받는 화면이 없을 때 생긴 결과는 버립니다.
 * 다른 탭에 갔다 돌아온 뒤에 띄우면 언제 당긴 결과인지 모릅니다.
 */
class RefreshResults {
    private val results = MutableSharedFlow<Int>(extraBufferCapacity = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)

    /** 당길 때마다 새로 받은 경기 수를 내보냅니다. 없었으면 0입니다. */
    val flow: Flow<Int> = results

    fun send(received: Int) {
        results.tryEmit(received)
    }
}

/** [results]를 앱 맨 위의 토스트로 띄웁니다. 토스트가 없으면(프리뷰, UI 테스트) 아무것도 하지 않습니다. */
@Composable
fun RefreshResultsEffect(results: Flow<Int>) {
    val toast = LocalOvalitToast.current ?: return
    LaunchedEffect(results, toast) {
        results.collect { received ->
            toast.show(if (received > 0) getString(Res.string.refresh_received, received) else getString(Res.string.refresh_none))
        }
    }
}
