package com.ovalit.core.designsystem.component

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.ovalit.core.designsystem.theme.OvalitTheme

/**
 * [content]가 세로로 스크롤되어야 당김을 받습니다.
 * 동그라미를 액센트로 칠하지 않는 건 액센트 버튼과 헷갈려서입니다.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OvalitPullToRefresh(
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val state = rememberPullToRefreshState()
    val colors = OvalitTheme.colors
    PullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = onRefresh,
        modifier = modifier,
        state = state,
        indicator = {
            PullToRefreshDefaults.Indicator(
                state = state,
                isRefreshing = isRefreshing,
                modifier = Modifier.align(Alignment.TopCenter),
                containerColor = colors.raised,
                color = colors.t1,
            )
        },
    ) {
        content()
    }
}
