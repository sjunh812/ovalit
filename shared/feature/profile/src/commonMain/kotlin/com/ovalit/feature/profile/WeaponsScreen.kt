package com.ovalit.feature.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ovalit.core.designsystem.component.OvalitBackTopBar
import com.ovalit.core.designsystem.component.OvalitDisclosureIcon
import com.ovalit.core.designsystem.component.OvalitDivider
import com.ovalit.core.designsystem.component.OvalitExpandable
import com.ovalit.core.designsystem.component.OvalitText
import com.ovalit.core.designsystem.component.OvalitTopBarCaption
import com.ovalit.core.designsystem.theme.OvalitSpacing
import com.ovalit.core.designsystem.theme.OvalitTheme
import com.ovalit.core.model.ContentCatalog
import com.ovalit.core.model.Movement
import com.ovalit.core.model.WeaponCategory
import com.ovalit.core.model.WeaponHighlight
import com.ovalit.core.model.WeaponReport
import com.ovalit.core.model.WeaponStats
import com.ovalit.core.ui.MetricFormat
import com.ovalit.core.ui.SeparatedRow
import com.ovalit.core.ui.rememberFittingStyle
import com.ovalit.core.ui.shrinkToFit
import com.ovalit.feature.profile.resources.Res
import com.ovalit.feature.profile.resources.act_matches
import com.ovalit.feature.profile.resources.column_damage
import com.ovalit.feature.profile.resources.column_headshot
import com.ovalit.feature.profile.resources.column_kda
import com.ovalit.feature.profile.resources.no_matches
import com.ovalit.feature.profile.resources.not_enough_sample
import com.ovalit.feature.profile.resources.weapons_act_value
import com.ovalit.feature.profile.resources.weapons_baseline
import com.ovalit.feature.profile.resources.weapons_by_category
import com.ovalit.feature.profile.resources.weapons_category_unknown
import com.ovalit.feature.profile.resources.weapons_collapse
import com.ovalit.feature.profile.resources.weapons_expand
import com.ovalit.feature.profile.resources.weapons_kda
import com.ovalit.feature.profile.resources.weapons_kills
import com.ovalit.feature.profile.resources.weapons_moved_down
import com.ovalit.feature.profile.resources.weapons_moved_up
import com.ovalit.feature.profile.resources.weapons_sample_kills
import com.ovalit.feature.profile.resources.weapons_sample_rounds
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
            OvalitBackTopBar(onBack = onBack, title = stringResource(Res.string.weapons_title)) {
                OvalitTopBarCaption(stringResource(Res.string.act_matches, report.matches))
            }
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
            Spacer(Modifier.height(OvalitSpacing.xl))
            OvalitDivider(Modifier.padding(horizontal = OvalitSpacing.gutter))
            Spacer(Modifier.height(18.dp))
            Categories(report, uiState.catalog)
            Spacer(Modifier.height(OvalitSpacing.lg))
            // 숫자마다 무기 몫을 가르는 기준이 달라서 표 밑에 한 번에 적는다
            OvalitText(
                text = stringResource(Res.string.weapons_single_round_note),
                modifier = Modifier.padding(horizontal = OvalitSpacing.gutter),
                style = OvalitTheme.typography.caption,
                color = OvalitTheme.colors.t3,
            )
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
        WeaponThumb(highlight.act.weapon, name, width = 52.dp, height = 30.dp)
        Spacer(Modifier.width(OvalitSpacing.md))
        // 좁은 화면에서 글자를 키우면 한 줄에 다 안 들어간다. 문구를 통째로 다음 줄에 내려서 "요즘 잘 / 맞아요"처럼
        // 가운데서 갈리거나 점이 줄 끝에 남지 않게 한다.
        val caption = OvalitTheme.typography.caption
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            SeparatedRow(
                items = listOfNotNull(
                    { OvalitText(text = name, style = OvalitTheme.typography.bodyStrong) },
                    if (moved) {
                        {
                            OvalitText(
                                text = stringResource(if (direction > 0) Res.string.weapons_moved_up else Res.string.weapons_moved_down),
                                style = caption,
                                color = changeColor,
                            )
                        }
                    } else {
                        null
                    },
                ),
                separator = { Spacer(Modifier.width(7.dp)) },
                alignBaseline = true,
            )
            SeparatedRow(
                items = listOf(
                    { OvalitText(stringResource(Res.string.weapons_sample_kills, highlight.act.kills.withThousands()), style = caption, color = colors.t3) },
                    {
                        OvalitText(
                            text = stringResource(Res.string.weapons_sample_rounds, highlight.act.singleWeaponRounds.withThousands()),
                            style = caption,
                            color = colors.t3,
                        )
                    },
                ),
                separator = { OvalitText(text = " · ", style = caption, color = colors.t3) },
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
        OvalitExpandable(visible = expanded == key) {
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = OvalitSpacing.gutter + OvalitSpacing.md, end = OvalitSpacing.gutter, bottom = 10.dp),
            ) {
                // 줄마다 K/D/A를 따로 줄이면 킬이 많은 총만 작아진다. 계열 안의 줄이 같은 크기를 쓴다.
                val kdaWidth = maxWidth - WeaponThumbWidth - OvalitSpacing.md - DamageColumn - HeadshotColumn
                val kdaStyle = rememberFittingStyle(weapons.map { kdaText(it) }, OvalitTheme.typography.caption, kdaWidth)
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    WeaponColumns()
                    weapons.forEach { WeaponRow(it, catalog, kdaStyle) }
                }
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
        OvalitDisclosureIcon(expanded = expanded)
    }
}

private val DamageColumn = 56.dp
private val HeadshotColumn = 60.dp
private val WeaponThumbWidth = 44.dp

@Composable
private fun kdaText(weapon: WeaponStats): String = stringResource(
    Res.string.weapons_kda,
    weapon.kills.withThousands(),
    weapon.deaths.withThousands(),
    weapon.assists.withThousands(),
)

// 펼친 계열 맨 위에 두는 열 제목이다. 숫자만 있으면 무엇인지 모른다. K/D/A는 이름 밑 줄의 제목이라 이름 쪽에 둔다.
@Composable
private fun WeaponColumns() {
    val style = OvalitTheme.typography.caption
    Row(modifier = Modifier.fillMaxWidth()) {
        Spacer(Modifier.width(WeaponThumbWidth + OvalitSpacing.md))
        OvalitText(
            text = stringResource(Res.string.column_kda),
            modifier = Modifier.weight(1f),
            style = style,
            color = OvalitTheme.colors.t3,
            maxLines = 1,
        )
        listOf(stringResource(Res.string.column_damage) to DamageColumn, stringResource(Res.string.column_headshot) to HeadshotColumn)
            .forEach { (title, width) ->
                OvalitText(
                    text = title,
                    modifier = Modifier.width(width),
                    style = style,
                    color = OvalitTheme.colors.t3,
                    textAlign = TextAlign.End,
                    maxLines = 1,
                    autoSize = shrinkToFit(style.fontSize, min = 7.sp),
                )
            }
    }
}

/**
 * 무기 한 줄입니다. 이름 밑에 K/D/A 합계를 두고 오른쪽에 라운드당 피해량과 헤드샷을 둡니다. 둘 다 표본이 모자라면 칸을 합쳐
 * "표본 부족"이라고 한 번만 적고, 하나만 모자라면 그 칸만 비웁니다.
 */
@Composable
private fun WeaponRow(weapon: WeaponStats, catalog: ContentCatalog, kdaStyle: TextStyle) {
    val colors = OvalitTheme.colors
    val name = catalog.weaponName(weapon.weapon)
    val metric = OvalitTheme.typography.metricS
    Row(modifier = Modifier.semantics(mergeDescendants = true) {}, verticalAlignment = Alignment.CenterVertically) {
        WeaponThumb(weapon.weapon, name, width = WeaponThumbWidth, height = 24.dp)
        Spacer(Modifier.width(OvalitSpacing.md))
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            OvalitText(text = name, style = OvalitTheme.typography.body, maxLines = 1, overflow = TextOverflow.Ellipsis)
            OvalitText(
                text = kdaText(weapon),
                style = kdaStyle,
                color = colors.t3,
                maxLines = 1,
                autoSize = shrinkToFit(kdaStyle.fontSize, min = 7.sp),
            )
        }
        if (!weapon.isDamageMeasurable && !weapon.isMeasurable) {
            OvalitText(
                text = stringResource(Res.string.not_enough_sample),
                modifier = Modifier.width(DamageColumn + HeadshotColumn),
                style = OvalitTheme.typography.caption,
                color = colors.t3,
                textAlign = TextAlign.End,
            )
            return@Row
        }
        OvalitText(
            text = weapon.damagePerRound?.takeIf { weapon.isDamageMeasurable }?.let { MetricFormat.INTEGER.format(it) } ?: NO_VALUE,
            modifier = Modifier.width(DamageColumn),
            style = metric,
            color = colors.t2,
            textAlign = TextAlign.End,
            maxLines = 1,
        )
        OvalitText(
            text = weapon.headshotRate?.takeIf { weapon.isMeasurable }?.let { percentText(it) } ?: NO_VALUE,
            modifier = Modifier.width(HeadshotColumn),
            style = metric,
            color = colors.t1,
            textAlign = TextAlign.End,
            maxLines = 1,
        )
    }
}
