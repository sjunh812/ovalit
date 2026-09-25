package com.ovalit.feature.profile

import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role as SemanticsRole
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ovalit.core.designsystem.component.OvalitDivider
import com.ovalit.core.designsystem.component.OvalitText
import com.ovalit.core.designsystem.icon.OvalitIcon
import com.ovalit.core.designsystem.icon.OvalitIcons
import com.ovalit.core.designsystem.theme.OvalitSpacing
import com.ovalit.core.designsystem.theme.OvalitTheme
import com.ovalit.core.model.ContentCatalog
import com.ovalit.core.model.Movement
import com.ovalit.core.model.WeaponCategory
import com.ovalit.core.model.WeaponHighlight
import com.ovalit.core.model.WeaponReport
import com.ovalit.core.model.WeaponStats
import com.ovalit.feature.profile.resources.Res
import com.ovalit.feature.profile.resources.act_matches
import com.ovalit.feature.profile.resources.no_matches
import com.ovalit.feature.profile.resources.not_enough_sample
import com.ovalit.feature.profile.resources.weapons_act_value
import com.ovalit.feature.profile.resources.weapons_baseline
import com.ovalit.feature.profile.resources.weapons_by_category
import com.ovalit.feature.profile.resources.weapons_category_unknown
import com.ovalit.feature.profile.resources.weapons_collapse
import com.ovalit.feature.profile.resources.weapons_expand
import com.ovalit.feature.profile.resources.weapons_kills
import com.ovalit.feature.profile.resources.weapons_moved_down
import com.ovalit.feature.profile.resources.weapons_moved_up
import com.ovalit.feature.profile.resources.weapons_sample
import com.ovalit.feature.profile.resources.weapons_single_round_note
import com.ovalit.feature.profile.resources.weapons_title
import kotlin.math.abs
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

/** S6 무기 화면입니다. */
@Composable
fun WeaponsRoute(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ProfileViewModel = koinViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    WeaponsScreen(uiState, onBack, modifier)
}

@Composable
internal fun WeaponsScreen(uiState: ProfileUiState, onBack: () -> Unit, modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxSize().background(OvalitTheme.colors.bg)) {
        if (uiState !is ProfileUiState.Success) return@Box
        val report = uiState.weapons

        Column(modifier = Modifier.fillMaxSize().safeDrawingPadding().verticalScroll(rememberScrollState())) {
            SubScreenTopBar(
                title = stringResource(Res.string.weapons_title),
                caption = stringResource(Res.string.act_matches, report.matches),
                onBack = onBack,
            )
            if (report.weapons.isEmpty()) {
                OvalitText(
                    text = stringResource(Res.string.no_matches),
                    modifier = Modifier.padding(OvalitSpacing.gutter),
                    color = OvalitTheme.colors.t2,
                )
                return@Column
            }

            Spacer(Modifier.height(OvalitSpacing.xl))
            Column(
                modifier = Modifier.padding(horizontal = OvalitSpacing.gutter),
                verticalArrangement = Arrangement.spacedBy(OvalitSpacing.lg),
            ) {
                report.highlights.forEach { Highlight(it, uiState.catalog) }
            }
            Spacer(Modifier.height(10.dp))
            OvalitText(
                text = stringResource(Res.string.weapons_single_round_note),
                modifier = Modifier.padding(horizontal = OvalitSpacing.gutter),
                style = OvalitTheme.typography.caption,
                color = OvalitTheme.colors.t3,
            )
            Spacer(Modifier.height(OvalitSpacing.xl))
            OvalitDivider(Modifier.padding(horizontal = OvalitSpacing.gutter))
            Spacer(Modifier.height(18.dp))
            Categories(report, uiState.catalog)
            Spacer(Modifier.height(OvalitSpacing.xxl))
        }
    }
}

@Composable
private fun Highlight(highlight: WeaponHighlight, catalog: ContentCatalog) {
    val colors = OvalitTheme.colors
    val name = catalog.weaponName(highlight.act.weapon)
    val current = highlight.current?.takeIf { it.isMeasurable }?.headshotRate
    val usual = highlight.baseline?.takeIf { it.isMeasurable }?.headshotRate
    // 동적 칸과 같은 규칙이다. 평소 흔들림 안의 변화는 칠하지도, 문구를 붙이지도 않는다.
    val direction = if (current != null && usual != null) current.percentSteps().compareTo(usual.percentSteps()) else 0
    val moved = highlight.movement == Movement.MOVED && direction != 0
    val changeColor = when {
        !moved -> colors.t3
        direction > 0 -> colors.pos
        else -> colors.neg
    }

    Row(modifier = Modifier.semantics(mergeDescendants = true) {}, verticalAlignment = Alignment.CenterVertically) {
        Thumbnail(name = name, width = 40.dp, height = 24.dp)
        Spacer(Modifier.width(OvalitSpacing.md))
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Row(verticalAlignment = Alignment.Bottom) {
                OvalitText(text = name, modifier = Modifier.alignByBaseline(), style = OvalitTheme.typography.bodyStrong)
                if (moved) {
                    Spacer(Modifier.width(7.dp))
                    OvalitText(
                        text = stringResource(if (direction > 0) Res.string.weapons_moved_up else Res.string.weapons_moved_down),
                        modifier = Modifier.alignByBaseline(),
                        style = OvalitTheme.typography.caption,
                        color = changeColor,
                    )
                }
            }
            OvalitText(
                text = stringResource(Res.string.weapons_sample, highlight.act.kills.withThousands(), highlight.act.singleWeaponRounds.withThousands()),
                style = OvalitTheme.typography.caption,
                color = colors.t3,
            )
        }
        Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(3.dp)) {
            // 이번 기간 표본이 모자라면 이번 액트 값을 대신 띄우고 그렇다고 적는다
            val shown = current ?: highlight.act.takeIf { it.isMeasurable }?.headshotRate
            Row(verticalAlignment = Alignment.Bottom) {
                OvalitText(
                    text = percentText(shown),
                    modifier = Modifier.alignByBaseline(),
                    style = OvalitTheme.typography.metricM.copy(fontSize = OvalitTheme.typography.titleM.fontSize),
                )
                if (current != null && usual != null) {
                    Spacer(Modifier.width(5.dp))
                    OvalitText(
                        text = changeText(current, usual),
                        modifier = Modifier.alignByBaseline(),
                        style = OvalitTheme.typography.metricS.copy(fontWeight = FontWeight.SemiBold),
                        color = changeColor,
                    )
                }
            }
            OvalitText(
                text = when {
                    current != null && usual != null ->
                        stringResource(Res.string.weapons_baseline, highlight.baselineWeeks, percentText(usual))
                    shown != null -> stringResource(Res.string.weapons_act_value)
                    else -> stringResource(Res.string.not_enough_sample)
                },
                style = OvalitTheme.typography.caption,
                color = colors.t3,
            )
        }
    }
}

// 변화량은 화면에 보이는 자릿수로 반올림한 값끼리 뺀다
private fun changeText(current: Double, usual: Double): String {
    val change = current.percentSteps() - usual.percentSteps()
    val sign = when {
        change > 0 -> "+"
        change < 0 -> "−"
        else -> ""
    }
    return sign + abs(change)
}

@Composable
private fun Categories(report: WeaponReport, catalog: ContentCatalog) {
    val byCategory = report.weapons
        .groupBy { catalog.weapons[it.weapon]?.category }
        .entries
        .sortedByDescending { (_, weapons) -> weapons.sumOf { it.kills } }
    val top = byCategory.firstOrNull()?.key
    var expanded by rememberSaveable { mutableStateOf(top?.name) }

    Row(modifier = Modifier.fillMaxWidth().padding(horizontal = OvalitSpacing.gutter), verticalAlignment = Alignment.Bottom) {
        OvalitText(
            text = stringResource(Res.string.weapons_by_category),
            modifier = Modifier.weight(1f),
            style = OvalitTheme.typography.bodyStrong,
        )
        OvalitText(
            text = stringResource(Res.string.weapons_kills, report.kills.withThousands()),
            style = OvalitTheme.typography.caption,
            color = OvalitTheme.colors.t3,
        )
    }
    Spacer(Modifier.height(OvalitSpacing.sm))
    byCategory.forEachIndexed { index, (category, weapons) ->
        if (index > 0) OvalitDivider(Modifier.padding(start = OvalitSpacing.gutter), color = OvalitTheme.colors.lineWeak)
        val key = category?.name
        CategoryRow(
            category = category,
            kills = weapons.sumOf { it.kills },
            share = weapons.sumOf { it.kills }.toFloat() / report.kills.coerceAtLeast(1),
            highlighted = category == top,
            expanded = expanded == key,
            onToggle = { expanded = if (expanded == key) null else key },
        )
        if (expanded == key) {
            Column(
                modifier = Modifier.padding(start = OvalitSpacing.gutter + OvalitSpacing.md, end = OvalitSpacing.gutter, bottom = 10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                weapons.forEach { WeaponRow(it, catalog) }
            }
        }
    }
}

@Composable
private fun CategoryRow(
    category: WeaponCategory?,
    kills: Int,
    share: Float,
    highlighted: Boolean,
    expanded: Boolean,
    onToggle: () -> Unit,
) {
    val name = category?.let { stringResource(it.label) } ?: stringResource(Res.string.weapons_category_unknown)
    val action = stringResource(if (expanded) Res.string.weapons_collapse else Res.string.weapons_expand, name)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .clickable(onClickLabel = action, role = SemanticsRole.Button, onClick = onToggle)
            .padding(horizontal = OvalitSpacing.gutter),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        OvalitText(
            text = name,
            modifier = Modifier.width(64.dp),
            style = if (highlighted) OvalitTheme.typography.bodyStrong else OvalitTheme.typography.body,
            maxLines = 1,
        )
        ShareBar(fraction = share, highlighted = highlighted, modifier = Modifier.weight(1f))
        OvalitText(
            text = stringResource(Res.string.weapons_kills, kills.withThousands()),
            modifier = Modifier.width(56.dp),
            style = OvalitTheme.typography.metricS,
            color = if (highlighted) OvalitTheme.colors.t1 else OvalitTheme.colors.t2,
            textAlign = TextAlign.End,
        )
        Spacer(Modifier.width(OvalitSpacing.sm))
        OvalitIcon(
            imageVector = if (expanded) OvalitIcons.ChevronDown else OvalitIcons.ChevronRight,
            contentDescription = null,
            tint = OvalitTheme.colors.t3,
            size = 14.dp,
        )
    }
}

@Composable
private fun WeaponRow(weapon: WeaponStats, catalog: ContentCatalog) {
    val name = catalog.weaponName(weapon.weapon)
    Row(modifier = Modifier.semantics(mergeDescendants = true) {}, verticalAlignment = Alignment.CenterVertically) {
        Thumbnail(name = name, width = 32.dp, height = 19.dp)
        Spacer(Modifier.width(OvalitSpacing.md))
        OvalitText(text = name, modifier = Modifier.weight(1f), style = OvalitTheme.typography.body)
        OvalitText(
            text = stringResource(Res.string.weapons_kills, weapon.kills.withThousands()),
            modifier = Modifier.width(56.dp),
            style = OvalitTheme.typography.metricS,
            color = OvalitTheme.colors.t2,
            textAlign = TextAlign.End,
        )
        OvalitText(
            text = if (weapon.isMeasurable) percentText(weapon.headshotRate) else stringResource(Res.string.not_enough_sample),
            modifier = Modifier.width(64.dp),
            style = if (weapon.isMeasurable) OvalitTheme.typography.metricS else OvalitTheme.typography.caption,
            color = if (weapon.isMeasurable) OvalitTheme.colors.t1 else OvalitTheme.colors.t3,
            textAlign = TextAlign.End,
        )
    }
}
