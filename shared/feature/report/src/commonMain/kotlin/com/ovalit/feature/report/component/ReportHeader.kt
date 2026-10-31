package com.ovalit.feature.report.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.unit.dp
import com.ovalit.core.designsystem.component.OvalitLogo
import com.ovalit.core.designsystem.component.OvalitTabHeader
import com.ovalit.core.designsystem.component.OvalitText
import com.ovalit.core.designsystem.resources.Res as DesignSystemRes
import com.ovalit.core.designsystem.resources.app_name
import com.ovalit.core.designsystem.theme.OvalitSpacing
import com.ovalit.core.designsystem.theme.OvalitTheme
import com.ovalit.core.model.WeeklyReport
import com.ovalit.core.ui.PlayerAvatar
import com.ovalit.core.ui.PlayerBadge
import com.ovalit.core.ui.TierLabel
import com.ovalit.core.ui.WRAPPING_SEPARATOR
import com.ovalit.core.ui.keepTogether
import com.ovalit.core.ui.label
import com.ovalit.core.ui.mainRoleText
import com.ovalit.core.ui.periodLabel
import com.ovalit.feature.report.label
import com.ovalit.feature.report.resources.Res
import com.ovalit.feature.report.resources.open_profile
import com.ovalit.feature.report.resources.period_date_range
import com.ovalit.feature.report.resources.period_matches
import com.ovalit.feature.report.resources.period_no_matches_this_week
import kotlinx.datetime.number
import org.jetbrains.compose.resources.stringResource

// 로고 말풍선 높이가 오른쪽 위 아바타(30dp)와 비슷해지는 폭
private val TopBarLogoWidth = 38.dp
private val AvatarSize = 30.dp
private val TierEmblemSize = 16.dp
private val ProfileTouchSize = 48.dp

@Composable
internal fun ReportTopBar(
    badge: PlayerBadge?,
    onOpenProfile: () -> Unit,
    modifier: Modifier = Modifier,
    onAvatarPositioned: ((LayoutCoordinates) -> Unit)? = null,
) {
    val appName = stringResource(DesignSystemRes.string.app_name)

    OvalitTabHeader(
        modifier = modifier,
        actions = {
            if (badge != null) {
                Row(
                    modifier = Modifier
                        .heightIn(min = ProfileTouchSize)
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
                    PlayerAvatar(
                        riotId = badge.riotId,
                        size = AvatarSize,
                        modifier = if (onAvatarPositioned != null) Modifier.onGloballyPositioned(onAvatarPositioned) else Modifier,
                    )
                }
            }
        },
    ) {
        OvalitLogo(
            modifier = Modifier
                .width(TopBarLogoWidth)
                .semantics {
                    contentDescription = appName
                    role = Role.Image
                },
        )
    }
}

@Composable
internal fun PeriodHeader(report: WeeklyReport.Ready, modifier: Modifier = Modifier) {
    val period = report.period

    Column(modifier = modifier.padding(horizontal = OvalitSpacing.gutter)) {
        TitleWithCaption(
            title = periodLabel(period),
            // 홈 첫 카드의 제목이라 다른 카드 제목(cardTitle)보다 한 단계 크다
            titleStyle = OvalitTheme.typography.titleM,
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
    // 바로 밑 승패 막대가 "6승 2패"로 경기 수를 이미 말해서 막대가 없을 때만 경기 수를 적는다
    val matches = if (report.wins + report.losses == 0) stringResource(Res.string.period_matches, report.metrics.matches) else null
    val roleText = report.mainRole?.let { mainRoleText(it, report.mainRoleShare) }

    // 카드 머리 오른쪽 글은 모두 보통 굵기 --t3다(docs/screens.md).
    // 좁으면 항목 사이에서만 줄이 바뀐다.
    return buildAnnotatedString {
        if (roleText != null) {
            append(roleText.keepTogether())
            append(WRAPPING_SEPARATOR)
        }
        append(range.keepTogether())
        if (matches != null) {
            append(WRAPPING_SEPARATOR)
            append(matches.keepTogether())
        }
    }
}
