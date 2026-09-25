package com.ovalit.feature.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role as SemanticsRole
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ovalit.core.designsystem.component.OvalitDivider
import com.ovalit.core.designsystem.component.OvalitText
import com.ovalit.core.designsystem.icon.OvalitIcon
import com.ovalit.core.designsystem.icon.OvalitIcons
import com.ovalit.core.designsystem.theme.OvalitSpacing
import com.ovalit.core.designsystem.theme.OvalitTheme
import com.ovalit.feature.profile.resources.Res
import com.ovalit.feature.profile.resources.act_matches
import com.ovalit.feature.profile.resources.main_role
import com.ovalit.feature.profile.resources.no_matches
import com.ovalit.feature.profile.resources.profile_agents
import com.ovalit.feature.profile.resources.profile_agents_value
import com.ovalit.feature.profile.resources.profile_weapons
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

private val BannerHeight = 112.dp
private val AvatarSize = 64.dp
private val RowMinHeight = 56.dp

/** 내 프로필입니다. 홈 오른쪽 위 아바타에서 들어옵니다. */
@Composable
fun ProfileRoute(
    onBack: () -> Unit,
    onOpenAgents: () -> Unit,
    onOpenWeapons: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ProfileViewModel = koinViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    ProfileScreen(uiState, onBack, onOpenAgents, onOpenWeapons, modifier)
}

@Composable
internal fun ProfileScreen(
    uiState: ProfileUiState,
    onBack: () -> Unit,
    onOpenAgents: () -> Unit,
    onOpenWeapons: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = OvalitTheme.colors

    Box(modifier = modifier.fillMaxSize().background(colors.bg)) {
        if (uiState !is ProfileUiState.Success) return@Box
        val agents = uiState.agents
        val riotId = uiState.account?.riotId.orEmpty()

        Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
            // 목업의 배너 자리다. 플레이어 카드가 붙기 전까지 배경색만 칠하고 그라데이션은 쓰지 않는다.
            Box(modifier = Modifier.fillMaxWidth()) {
                Box(modifier = Modifier.fillMaxWidth().height(BannerHeight).background(colors.raised)) {
                    Box(Modifier.safeDrawingPadding()) {
                        SubScreenTopBar(title = null, caption = null, onBack = onBack)
                    }
                }
                Box(
                    modifier = Modifier
                        .padding(start = OvalitSpacing.gutter, top = BannerHeight - AvatarSize / 2)
                        .size(AvatarSize)
                        .background(colors.fill, CircleShape)
                        .border(3.dp, colors.bg, CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    OvalitText(text = riotId.take(1), style = OvalitTheme.typography.titleM, color = colors.t2)
                }
            }
            Spacer(Modifier.height(10.dp))
            Column(
                modifier = Modifier.padding(horizontal = OvalitSpacing.gutter),
                verticalArrangement = Arrangement.spacedBy(5.dp),
            ) {
                OvalitText(text = riotId, style = OvalitTheme.typography.titleL)
                OvalitText(
                    text = buildAnnotatedString {
                        agents.mainRole?.let { role ->
                            val name = stringResource(role.label)
                            val text = stringResource(Res.string.main_role, name)
                            val start = text.indexOf(name)
                            append(text)
                            if (start >= 0) {
                                addStyle(SpanStyle(color = colors.t2, fontWeight = FontWeight.SemiBold), start, start + name.length)
                            }
                            append(" · ")
                        }
                        append(stringResource(Res.string.act_matches, agents.matches))
                    },
                    style = OvalitTheme.typography.caption,
                    color = colors.t3,
                )
            }
            Spacer(Modifier.height(OvalitSpacing.xl))

            if (agents.matches == 0) {
                OvalitText(
                    text = stringResource(Res.string.no_matches),
                    modifier = Modifier.padding(horizontal = OvalitSpacing.gutter),
                    style = OvalitTheme.typography.body,
                    color = colors.t2,
                )
                return@Column
            }

            OvalitDivider(Modifier.padding(horizontal = OvalitSpacing.gutter))
            Spacer(Modifier.height(OvalitSpacing.xs))
            SummaryRow(
                title = stringResource(Res.string.profile_agents),
                value = agentsSummary(uiState),
                onClick = onOpenAgents,
            )
            OvalitDivider(Modifier.padding(start = OvalitSpacing.gutter), color = colors.lineWeak)
            SummaryRow(
                title = stringResource(Res.string.profile_weapons),
                value = uiState.weapons.highlights
                    .map { uiState.catalog.weaponName(it.act.weapon) }
                    .joinToString(" · "),
                onClick = onOpenWeapons,
            )
            Spacer(Modifier.height(OvalitSpacing.xxl))
        }
    }
}

@Composable
private fun agentsSummary(uiState: ProfileUiState.Success): String {
    val agents = uiState.agents
    val main = agents.roles.firstOrNull() ?: return ""
    val share = main.rounds.toDouble() / agents.roles.sumOf { it.rounds }
    val names = agents.agents
        .filter { it.role == main.role }
        .take(2)
        .map { uiState.catalog.agentName(it.agent) }
        .joinToString(", ")
    return stringResource(Res.string.profile_agents_value, stringResource(main.role.label), percentText(share), names)
}

@Composable
private fun SummaryRow(title: String, value: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = RowMinHeight)
            .clickable(role = SemanticsRole.Button, onClick = onClick)
            .padding(horizontal = OvalitSpacing.gutter, vertical = OvalitSpacing.md),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        OvalitText(text = title, style = OvalitTheme.typography.body)
        Spacer(Modifier.width(OvalitSpacing.md))
        OvalitText(
            text = value,
            modifier = Modifier.weight(1f),
            style = OvalitTheme.typography.body,
            color = OvalitTheme.colors.t2,
            textAlign = TextAlign.End,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(Modifier.width(OvalitSpacing.xs))
        OvalitIcon(OvalitIcons.ChevronRight, contentDescription = null, tint = OvalitTheme.colors.t4, size = 16.dp)
    }
}
