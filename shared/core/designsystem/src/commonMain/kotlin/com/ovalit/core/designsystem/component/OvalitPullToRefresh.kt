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
 * 당겨서 새로고침입니다. 안에는 세로로 스크롤되는 것을 둡니다. 동그라미는 `--raised` 위에 `--t1`로 돌아서
 * 금색 버튼과 겹쳐 보이지 않습니다.
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
