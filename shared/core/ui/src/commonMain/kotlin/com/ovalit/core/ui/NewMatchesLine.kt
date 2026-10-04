package com.ovalit.core.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.ovalit.core.designsystem.component.OvalitText
import com.ovalit.core.designsystem.theme.OvalitSpacing
import com.ovalit.core.designsystem.theme.OvalitTheme
import com.ovalit.core.model.NEW_MATCHES_IN_BACKGROUND_FROM
import com.ovalit.core.model.NewMatchesProgress
import com.ovalit.core.ui.resources.Res
import com.ovalit.core.ui.resources.new_matches_count
import com.ovalit.core.ui.resources.new_matches_description
import com.ovalit.core.ui.resources.new_matches_in_background
import com.ovalit.core.ui.resources.new_matches_receiving
import org.jetbrains.compose.resources.stringResource

/**
 * 오래 쉬었다 와서 새 경기를 여러 판 받는 동안 홈과 경기 탭 맨 위에 두는 한 줄입니다. 몇 판 중 몇 판을 받았는지 적고 가는
 * 막대를 채웁니다. 다 받으면 접혀 사라집니다. [progress]가 `null`이면 그리지 않습니다.
 *
 * 받는 동안 홈 숫자는 그대로라 왜 안 바뀌는지 여기서 알 수 있습니다. 앱을 닫아도 이어 받을 만큼 많으면 그렇다고 한 줄 더 적습니다.
 *
 * @param bottomSpacing 줄이 보일 때만 밑에 두는 간격입니다. 사라질 때 같이 접혀 아래 내용이 한 번에 올라옵니다.
 */
@Composable
fun NewMatchesLine(progress: NewMatchesProgress?, modifier: Modifier = Modifier, bottomSpacing: Dp = 0.dp) {
    // 사라지는 동안에도 마지막 숫자를 그대로 둔다. null로 바로 바꾸면 접히는 중에 글자가 비어 보인다.
    val shown = remember { LastShown<NewMatchesProgress>() }.update(progress)
    AnimatedVisibility(
        visible = progress != null,
        modifier = modifier,
        enter = expandVertically(expandFrom = Alignment.Top) + fadeIn(),
        exit = shrinkVertically(shrinkTowards = Alignment.Top) + fadeOut(),
    ) {
        if (shown != null) {
            Column(modifier = Modifier.padding(bottom = bottomSpacing)) {
                Line(shown)
            }
        }
    }
}

@Composable
private fun Line(progress: NewMatchesProgress) {
    val colors = OvalitTheme.colors
    val description = stringResource(Res.string.new_matches_description, progress.received, progress.total)
    val fraction by animateFloatAsState(if (progress.total > 0) progress.received.toFloat() / progress.total else 0f)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = OvalitSpacing.gutter)
            // 한 판 받을 때마다 낭독기가 읽으면 시끄럽다. 진행도로만 알려 사용자가 짚을 때 읽게 한다.
            .clearAndSetSemantics {
                contentDescription = description
                progressBarRangeInfo = ProgressBarRangeInfo(progress.received.toFloat(), 0f..progress.total.toFloat())
            },
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            OvalitText(
                text = stringResource(Res.string.new_matches_receiving),
                modifier = Modifier.weight(1f),
                style = OvalitTheme.typography.label,
                color = colors.t2,
                maxLines = 1,
            )
            OvalitText(
                text = stringResource(Res.string.new_matches_count, progress.received, progress.total),
                style = OvalitTheme.typography.metricS.copy(fontWeight = FontWeight.Medium),
                color = colors.t2,
                maxLines = 1,
            )
        }
        Spacer(Modifier.height(OvalitSpacing.sm))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(BarHeight)
                .clip(RoundedCornerShape(BarHeight))
                .background(colors.bar),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(fraction)
                    .height(BarHeight)
                    .clip(RoundedCornerShape(BarHeight))
                    .background(colors.t2),
            )
        }
        if (progress.total >= NEW_MATCHES_IN_BACKGROUND_FROM) {
            Spacer(Modifier.height(6.dp))
            OvalitText(
                text = stringResource(Res.string.new_matches_in_background),
                style = OvalitTheme.typography.caption,
                color = colors.t3,
            )
        }
    }
}

private val BarHeight = 3.dp

/** 마지막으로 받은 `null`이 아닌 값을 들고 있습니다. */
private class LastShown<T : Any> {
    private var last: T? = null

    fun update(value: T?): T? {
        if (value != null) last = value
        return last
    }
}
