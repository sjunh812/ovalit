package com.ovalit.core.designsystem.component

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.staticCompositionLocalOf
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow

/**
 * 지금 탭을 탭바에서 한 번 더 누를 때마다 흐릅니다. 화면 전환을 정하는 `composeApp`이 탭 화면마다 채웁니다. 탭 밖에서는 흐르지
 * 않습니다.
 */
val LocalTabReselects = staticCompositionLocalOf<Flow<Unit>> { emptyFlow() }

/** iOS와 인스타그램, 토스처럼 지금 탭을 한 번 더 누르면 맨 위로 올립니다. 탭 화면의 세로 스크롤에 답니다. */
@Composable
fun ScrollToTopOnReselect(state: ScrollState) {
    val reselects = LocalTabReselects.current
    LaunchedEffect(reselects, state) { reselects.collect { state.animateScrollTo(0) } }
}

/** [ScrollToTopOnReselect]의 목록판입니다. 멀리 내려가 있으면 굴러 올라가는 데 오래 걸려서 가까이 뛴 뒤 굴립니다. */
@Composable
fun ScrollToTopOnReselect(state: LazyListState) {
    val reselects = LocalTabReselects.current
    LaunchedEffect(reselects, state) {
        reselects.collect {
            if (state.firstVisibleItemIndex > JumpFrom) state.scrollToItem(JumpFrom)
            state.animateScrollToItem(0)
        }
    }
}

private const val JumpFrom = 6
