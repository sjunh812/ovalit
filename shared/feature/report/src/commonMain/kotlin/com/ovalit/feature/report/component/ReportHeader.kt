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
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.unit.dp
import com.ovalit.core.designsystem.component.OvalitExpandable
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
import kotlin.math.roundToInt
import kotlinx.datetime.number
import org.jetbrains.compose.resources.stringResource

// 로고 말풍선 높이가 오른쪽 위 아바타(30dp)와 비슷해지는 폭
private val TopBarLogoWidth = 38.dp
private val AvatarSize = 30.dp
private val TierEmblemSize = 16.dp
private val ProfileTouchSize = 48.dp
private val ProfileEntryPadding = 7.dp

// 안내 화살표 끝과 아바타 사이
private val ProfileHintGap = 2.dp

/**
 * 홈 맨 위 줄입니다. 왼쪽은 ㅇㅂㅇ 로고, 오른쪽은 티어와 아바타이고 누르면 내 프로필입니다.
 *
 * 안내("내 프로필은 여기서 볼 수 있어요")는 이 줄 밑에 자리를 만들어 넣습니다.
 * 칩 위에 띄우면 칩을 가려서 그 칩을 누르면 칩 대신 안내가 닫혔습니다.
 * 닫으면 접히고 밑의 칩과 카드가 따라 올라옵니다.
 *
 * @param profileHint 안내를 둘지입니다.
 */
@Composable
internal fun ReportTopBar(
    badge: PlayerBadge?,
    onOpenProfile: () -> Unit,
    modifier: Modifier = Modifier,
    profileHint: Boolean = false,
    onDismissProfileHint: () -> Unit = {},
) {
    val appName = stringResource(DesignSystemRes.string.app_name)

    Layout(
        modifier = modifier,
        content = {
            OvalitTabHeader(
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
                                .padding(horizontal = ProfileEntryPadding),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            TierLabel(
                                badge = badge,
                                style = OvalitTheme.typography.caption,
                                color = OvalitTheme.colors.t2,
                                emblemSize = TierEmblemSize,
                                modifier = Modifier.padding(end = 10.dp),
                            )
                            PlayerAvatar(riotId = badge.riotId, size = AvatarSize)
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
            OvalitExpandable(visible = profileHint && badge != null) {
                // 아바타 오른쪽 끝은 OvalitTabHeader의 끝 여백과 누름 영역 여백만큼 들어와 있다
                ProfileHint(
                    avatarEnd = OvalitSpacing.sm + ProfileEntryPadding,
                    avatarSize = AvatarSize,
                    onDismiss = onDismissProfileHint,
                )
            }
        },
    ) { measurables, constraints ->
        val header = measurables[0].measure(constraints)
        val hint = measurables.getOrNull(1)?.measure(constraints.copy(minHeight = 0))
        // 아바타는 OvalitTabHeader 위 여백을 뺀 자리의 세로 가운데에 있다.
        // 안내를 그 밑 빈 곳으로 끌어올려 화살표가 아바타 바로 밑에서 시작하게 한다.
        // 접히는 동안 안내 높이가 끌어올린 만큼보다 작아지면 머리 줄 높이만 남는다.
        val belowAvatar = (header.height - OvalitSpacing.sm.toPx() - AvatarSize.toPx()) / 2
        val lift = (belowAvatar - ProfileHintGap.toPx()).roundToInt().coerceIn(0, hint?.height ?: 0)
        layout(header.width, header.height + (hint?.height ?: 0) - lift) {
            header.place(0, 0)
            hint?.place(0, header.height - lift)
        }
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
