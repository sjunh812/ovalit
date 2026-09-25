package com.ovalit.feature.report.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ovalit.core.designsystem.component.OvalitChip
import com.ovalit.core.designsystem.component.OvalitLogo
import com.ovalit.core.designsystem.component.OvalitText
import com.ovalit.core.designsystem.resources.Res as DesignSystemRes
import com.ovalit.core.designsystem.resources.app_name
import com.ovalit.core.designsystem.theme.OvalitSpacing
import com.ovalit.core.designsystem.theme.OvalitTheme
import com.ovalit.core.model.QueueFilter
import com.ovalit.core.model.WeeklyReport
import com.ovalit.core.ui.PlayerAvatar
import com.ovalit.core.ui.PlayerBadge
import com.ovalit.core.ui.TierLabel
import com.ovalit.core.ui.label
import com.ovalit.core.ui.periodLabel
import com.ovalit.core.ui.resources.Res as CoreUiRes
import com.ovalit.core.ui.resources.main_role
import com.ovalit.feature.report.label
import com.ovalit.feature.report.resources.Res
import com.ovalit.feature.report.resources.open_profile
import com.ovalit.feature.report.resources.period_date_range
import com.ovalit.feature.report.resources.period_matches
import com.ovalit.feature.report.resources.period_no_matches_this_week
import kotlinx.datetime.number
import org.jetbrains.compose.resources.stringResource

// 목업의 글자 로고(18px)와 높이가 비슷해지는 폭
private val TopBarLogoWidth = 46.dp
private val AvatarSize = 30.dp
private val TierEmblemSize = 16.dp
private val ProfileTouchSize = 44.dp

@Composable
internal fun ReportTopBar(badge: PlayerBadge?, onOpenProfile: () -> Unit, modifier: Modifier = Modifier) {
    val appName = stringResource(DesignSystemRes.string.app_name)

    Row(
        modifier = modifier.fillMaxWidth().padding(start = OvalitSpacing.gutter, end = OvalitSpacing.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        OvalitLogo(
            modifier = Modifier
                .width(TopBarLogoWidth)
                .semantics {
                    contentDescription = appName
                    role = Role.Image
                },
        )
        Spacer(Modifier.weight(1f))
        if (badge != null) {
            Row(
                modifier = Modifier
                    .heightIn(min = ProfileTouchSize)
                    .clip(RoundedCornerShape(ProfileTouchSize / 2))
                    .clickable(
                        onClickLabel = stringResource(Res.string.open_profile),
                        role = Role.Button,
                        onClick = onOpenProfile,
                    )
                    .padding(horizontal = 7.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TierLabel(
                    badge = badge,
                    style = OvalitTheme.typography.caption,
                    color = OvalitTheme.colors.t2,
                    emblemSize = TierEmblemSize,
                    modifier = Modifier.padding(end = 10.dp),
                )
                PlayerAvatar(agent = badge.agent, riotId = badge.riotId, modifier = Modifier.size(AvatarSize))
            }
        }
    }
}

@Composable
internal fun QueueChips(
    selected: QueueFilter,
    onSelect: (QueueFilter) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = OvalitSpacing.gutter),
        horizontalArrangement = Arrangement.spacedBy(OvalitSpacing.xs),
    ) {
        QueueFilter.entries.forEach { filter ->
            OvalitChip(
                text = stringResource(filter.label),
                selected = filter == selected,
                onClick = { onSelect(filter) },
            )
        }
    }
}

@Composable
internal fun PeriodHeader(report: WeeklyReport.Ready, modifier: Modifier = Modifier) {
    val period = report.period

    Column(modifier = modifier.padding(horizontal = OvalitSpacing.gutter)) {
        TitleWithCaption(
            title = periodLabel(period),
            titleStyle = OvalitTheme.typography.titleL,
            caption = periodCaption(report),
        )
        if (!period.includesThisWeek) {
            Spacer(Modifier.height(2.dp))
            OvalitText(
                text = stringResource(Res.string.period_no_matches_this_week),
                style = OvalitTheme.typography.caption,
                color = OvalitTheme.colors.t2,
            )
        }
    }
}

@Composable
private fun periodCaption(report: WeeklyReport.Ready): AnnotatedString {
    val first = report.period.firstDay
    val last = report.period.lastDay
    val range = stringResource(Res.string.period_date_range, first.month.number, first.day, last.month.number, last.day)
    val matches = stringResource(Res.string.period_matches, report.metrics.matches)
    val role = report.mainRole?.let { stringResource(it.label) }
    val roleText = role?.let { stringResource(CoreUiRes.string.main_role, it) }
    val emphasis = SpanStyle(color = OvalitTheme.colors.t2, fontWeight = FontWeight.SemiBold)

    // 역할 이름만 한 단계 밝고 굵게 둔다. 홈에서 강조는 이 한 곳뿐이다(CLAUDE.md 화면).
    return buildAnnotatedString {
        if (role != null && roleText != null) {
            // 그 역할만 했다는 뜻으로 읽히지 않게 "주로"를 붙인다. 가장 많은 라운드를 뛴 역할이다.
            val start = roleText.indexOf(role)
            append(roleText)
            if (start >= 0) addStyle(emphasis, start, start + role.length)
            append(SEPARATOR)
        }
        append(range)
        append(SEPARATOR)
        append(matches)
    }
}
