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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role as SemanticsRole
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
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
import com.ovalit.core.model.FixedMetric
import com.ovalit.core.model.Movement
import com.ovalit.core.model.WeaponCategory
import com.ovalit.core.model.WeaponHighlight
import com.ovalit.core.model.WeaponMetric
import com.ovalit.core.model.WeaponReport
import com.ovalit.core.model.WeaponStats
import com.ovalit.core.ui.MetricFormat
import com.ovalit.core.ui.format
import com.ovalit.core.ui.label
import com.ovalit.core.ui.periodLabel
import com.ovalit.core.ui.valueText
import com.ovalit.core.ui.SeparatedRow
import com.ovalit.core.ui.rememberFittingStyle
import com.ovalit.core.ui.rememberWidestWidth
import com.ovalit.core.ui.shrinkToFit
import com.ovalit.feature.profile.resources.Res
import com.ovalit.feature.profile.resources.act_matches
import com.ovalit.feature.profile.resources.column_damage
import com.ovalit.feature.profile.resources.column_headshot
import com.ovalit.feature.profile.resources.column_kda
import com.ovalit.feature.profile.resources.no_matches
import com.ovalit.feature.profile.resources.not_enough_sample
import com.ovalit.feature.profile.resources.weapons_act_basis
import com.ovalit.feature.profile.resources.weapons_act_kills
import com.ovalit.feature.profile.resources.weapons_act_value
import com.ovalit.feature.profile.resources.weapons_by_category
import com.ovalit.feature.profile.resources.weapons_compared
import com.ovalit.feature.profile.resources.weapons_category_unknown
import com.ovalit.feature.profile.resources.weapons_collapse
import com.ovalit.feature.profile.resources.weapons_expand
import com.ovalit.feature.profile.resources.weapons_kda
import com.ovalit.feature.profile.resources.weapons_kda_with_counts
import com.ovalit.feature.profile.resources.weapons_kills
import com.ovalit.feature.profile.resources.weapons_moved_down
import com.ovalit.feature.profile.resources.weapons_moved_up
import com.ovalit.feature.profile.resources.weapons_sample_kills
import com.ovalit.feature.profile.resources.weapons_single_round_note
import com.ovalit.feature.profile.resources.weapons_title
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

            Spacer(Modifier.height(OvalitSpacing.lg))
            Highlights(report, uiState.catalog)
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

private val HighlightColumn = 54.dp
private val HighlightThumbWidth = 48.dp

private val WeaponMetric.fixed: FixedMetric
    get() = when (this) {
        WeaponMetric.KD -> FixedMetric.KD
        WeaponMetric.DAMAGE_PER_ROUND -> FixedMetric.DAMAGE
        WeaponMetric.HEADSHOT_RATE -> FixedMetric.HEADSHOT_RATE
    }

/**
 * 위쪽 세 무기 표의 한 칸입니다.
 *
 * @property change 이번 기간을 띄운 줄에만 있고, 비교할 값이 없으면 빈 글자입니다. 줄 안의 칸 높이를 맞추려고 비워 둡니다.
 * @property rise 움직였다고 판단한 변화의 방향입니다. 오르면 1, 내리면 -1, 평소 범위 안이거나 모르면 0입니다.
 */
private class HighlightCell(val value: String, val change: String?, val rise: Int)

/**
 * 위쪽 세 무기의 표입니다. 홈 리포트와 같은 기간의 K/D, 라운드당 피해량, 헤드샷을 그 앞 4주 평균과 견줍니다. 기간 표본이
 * 모자란 무기는 그 줄만 이번 액트 값을 띄우고 그렇다고 적습니다. 무기끼리 같은 지표를 위아래로 견줄 수 있게 열을
 * 맞추고, 열 제목은 맨 위에 한 번만 둡니다.
 */
@Composable
private fun Highlights(report: WeaponReport, catalog: ContentCatalog) {
    val colors = OvalitTheme.colors
    val typography = OvalitTheme.typography
    val period = report.period?.takeIf { report.highlights.any { it.current != null } }
    val baselineWeeks = report.highlights.firstOrNull { highlight ->
        highlight.current != null && WeaponMetric.entries.any { highlight.baseline?.value(it) != null }
    }?.baselineWeeks

    val labels = WeaponMetric.entries.map { stringResource(it.fixed.label) }
    val rows = report.highlights.map { highlight ->
        HighlightLine(
            highlight = highlight,
            name = catalog.weaponName(highlight.act.weapon),
            cells = highlightCells(highlight),
            comparesPeriod = period != null,
        )
    }
    val moveUp = stringResource(Res.string.weapons_moved_up)
    val moveDown = stringResource(Res.string.weapons_moved_down)
    val actLabel = stringResource(Res.string.weapons_act_value)

    BoxWithConstraints(modifier = Modifier.fillMaxWidth().padding(horizontal = OvalitSpacing.gutter)) {
        // 칸마다 따로 줄이면 자릿수가 많은 칸만 작아진다. 표 전체가 같은 크기를 쓰고, 옆 칸과 붙지 않게 폭을 조금 남긴다.
        val cellWidth = HighlightColumn - HighlightCellGap
        val valueStyle = rememberFittingStyle(
            rows.flatMap { row -> row.cells.orEmpty().map { it.value } },
            typography.metricM.copy(fontSize = typography.titleM.fontSize, lineHeight = typography.titleM.lineHeight),
            cellWidth,
        )
        val changeStyle = rememberFittingStyle(
            rows.flatMap { row -> row.cells.orEmpty().mapNotNull { it.change } },
            typography.metricS.copy(fontWeight = FontWeight.SemiBold),
            cellWidth,
        )
        val labelStyle = rememberFittingStyle(labels, typography.caption, cellWidth)

        // 이름 칸의 글자가 한 줄에 안 들어가면 세 줄 모두 숫자를 이름 밑으로 내린다. 한 줄만 내리면 열이 어긋난다.
        // "요즘 잘 / 맞아요"처럼 문구가 가운데서 갈리는 것도 이렇게 막는다.
        val nameWords = rows.flatMap { it.name.split(' ') }
        val captions = rows.flatMap { row ->
            listOfNotNull(
                row.tag?.let { if (it > 0) moveUp else moveDown },
                killsText(row),
            )
        }
        val besideCells = maxWidth - HighlightThumbWidth - OvalitSpacing.md - HighlightColumn * WeaponMetric.entries.size
        val needed = maxOf(rememberWidestWidth(nameWords, typography.bodyStrong), rememberWidestWidth(captions, typography.caption))
        val stacked = needed > besideCells
        val nameWidth = if (stacked) maxWidth - HighlightThumbWidth - OvalitSpacing.md else besideCells
        // 이름은 어절 단위로 꺾이게 두고, 가장 긴 어절이 한 줄에 들어가는 크기로 셋을 같이 줄인다
        val styles = HighlightStyles(
            name = rememberFittingStyle(nameWords, typography.bodyStrong, nameWidth, min = 11.sp),
            value = valueStyle,
            change = changeStyle,
        )

        Column(verticalArrangement = Arrangement.spacedBy(OvalitSpacing.lg)) {
            Row(verticalAlignment = Alignment.Bottom) {
                val caption = typography.caption
                SeparatedRow(
                    items = listOfNotNull(
                        { OvalitText(text = period?.let { periodLabel(it) } ?: actLabel, style = caption, color = colors.t3) },
                        baselineWeeks?.takeIf { period != null }?.let { weeks ->
                            { OvalitText(text = stringResource(Res.string.weapons_compared, weeks), style = caption, color = colors.t3) }
                        },
                    ),
                    separator = { OvalitText(text = " · ", style = caption, color = colors.t3) },
                    modifier = Modifier.weight(1f),
                )
                labels.forEach { label ->
                    OvalitText(
                        text = label,
                        modifier = Modifier.width(HighlightColumn),
                        style = labelStyle,
                        color = colors.t3,
                        textAlign = TextAlign.End,
                        maxLines = 1,
                    )
                }
            }
            rows.forEach { HighlightRow(it, styles, stacked) }
        }
    }
}

private val HighlightCellGap = 6.dp

private class HighlightStyles(val name: TextStyle, val value: TextStyle, val change: TextStyle)

private class HighlightLine(
    val highlight: WeaponHighlight,
    val name: String,
    val cells: List<HighlightCell>?,
    val comparesPeriod: Boolean,
) {
    /** 표는 이번 기간을 보는데 이 줄만 표본이 모자라 이번 액트 값을 띄웁니다. */
    val fallsBackToAct: Boolean get() = comparesPeriod && highlight.current == null

    // "요즘 잘 맞아요"는 맞히는 얘기라 헤드샷이 움직였을 때만 붙인다
    val tag: Int? get() = cells?.get(WeaponMetric.HEADSHOT_RATE.ordinal)?.rise?.takeIf { it != 0 }
}

// 순서를 정한 이번 액트 킬이다. 표가 이번 기간을 볼 때 이번 주 킬과 섞어 적으면 순서가 틀려 보인다.
@Composable
private fun killsText(line: HighlightLine): String {
    val kills = line.highlight.act.kills.withThousands()
    return if (line.comparesPeriod) {
        stringResource(Res.string.weapons_act_kills, kills)
    } else {
        stringResource(Res.string.weapons_sample_kills, kills)
    }
}

// 이번 기간을 띄울 수 없으면 이번 액트 값을 띄운다. 이번 액트로도 세 칸이 다 비면 null이고 줄에 "표본 부족"을 적는다.
@Composable
private fun highlightCells(highlight: WeaponHighlight): List<HighlightCell>? {
    val current = highlight.current
    val shown = current ?: highlight.act
    if (current == null && WeaponMetric.entries.all { shown.value(it) == null }) return null

    return WeaponMetric.entries.map { metric ->
        val format = metric.fixed.format
        val now = shown.value(metric)
        val usual = highlight.baseline?.value(metric)?.takeIf { current != null }
        HighlightCell(
            value = now?.let { format.valueText(it) } ?: NO_VALUE,
            // 변화량은 보이는 자릿수로 반올림한 값끼리 뺀다
            change = when {
                current == null -> null
                now != null && usual != null -> format.formatChange(now, usual)
                else -> ""
            },
            // 동적 칸과 같은 규칙이다. 평소 흔들림 안의 변화는 칠하지도, 문구를 붙이지도 않는다.
            rise = if (now != null && usual != null && highlight.movement(metric) == Movement.MOVED) format.direction(now, usual) else 0,
        )
    }
}

/** [stacked]면 숫자 세 칸을 이름 밑 오른쪽에 둡니다. 열 위치는 그대로라 머리의 열 제목과 맞습니다. */
@Composable
private fun HighlightRow(line: HighlightLine, styles: HighlightStyles, stacked: Boolean) {
    val weapon = line.highlight.act.weapon
    if (stacked) {
        Column(modifier = Modifier.semantics(mergeDescendants = true) {}, verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                WeaponThumb(weapon, line.name, width = HighlightThumbWidth, height = 28.dp)
                Spacer(Modifier.width(OvalitSpacing.md))
                HighlightName(line, styles.name, Modifier.weight(1f))
            }
            HighlightCells(line, styles, Modifier.align(Alignment.End))
        }
    } else {
        Row(modifier = Modifier.semantics(mergeDescendants = true) {}, verticalAlignment = Alignment.CenterVertically) {
            WeaponThumb(weapon, line.name, width = HighlightThumbWidth, height = 28.dp)
            Spacer(Modifier.width(OvalitSpacing.md))
            HighlightName(line, styles.name, Modifier.weight(1f))
            HighlightCells(line, styles)
        }
    }
}

@Composable
private fun HighlightName(line: HighlightLine, nameStyle: TextStyle, modifier: Modifier) {
    val colors = OvalitTheme.colors
    val caption = OvalitTheme.typography.caption
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(2.dp)) {
        SeparatedRow(
            items = listOfNotNull(
                { OvalitText(text = line.name, style = nameStyle) },
                line.tag?.let { rise ->
                    {
                        OvalitText(
                            text = stringResource(if (rise > 0) Res.string.weapons_moved_up else Res.string.weapons_moved_down),
                            style = caption,
                            color = riseColor(rise),
                        )
                    }
                },
            ),
            separator = { Spacer(Modifier.width(7.dp)) },
            alignBaseline = true,
        )
        OvalitText(text = killsText(line), style = caption, color = colors.t3)
    }
}

@Composable
private fun HighlightCells(line: HighlightLine, styles: HighlightStyles, modifier: Modifier = Modifier) {
    val caption = OvalitTheme.typography.caption
    val cells = line.cells
    if (cells == null) {
        OvalitText(
            text = stringResource(Res.string.not_enough_sample),
            modifier = modifier.width(HighlightColumn * WeaponMetric.entries.size),
            style = caption,
            color = OvalitTheme.colors.t3,
            textAlign = TextAlign.End,
        )
        return
    }
    Column(modifier = modifier, horizontalAlignment = Alignment.End) {
        Row {
            cells.forEach { cell ->
                Column(modifier = Modifier.width(HighlightColumn), horizontalAlignment = Alignment.End) {
                    OvalitText(text = cell.value, style = styles.value, maxLines = 1)
                    cell.change?.let { OvalitText(text = it, style = styles.change, color = riseColor(cell.rise), maxLines = 1) }
                }
            }
        }
        // 변화량 자리에 이 줄의 숫자가 이번 액트 것임을 적는다. 머리에는 이번 기간이라고 적혀 있다.
        if (line.fallsBackToAct) {
            OvalitText(text = stringResource(Res.string.weapons_act_basis), style = caption, color = OvalitTheme.colors.t3, maxLines = 1)
        }
    }
}

@Composable
private fun riseColor(rise: Int): Color = when {
    rise > 0 -> OvalitTheme.colors.pos
    rise < 0 -> OvalitTheme.colors.neg
    else -> OvalitTheme.colors.t3
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
                // 줄마다 KDA 줄을 따로 줄이면 킬이 많은 총만 작아진다. 계열 안의 줄이 같은 크기를 쓴다.
                val caption = OvalitTheme.typography.caption
                val lines = weapons.map { kdaLine(it) }
                val nameWidth = maxWidth - WeaponThumbWidth - OvalitSpacing.md
                val beside = rememberFittingStyle(lines.map { it.text }, caption, nameWidth - KdColumn - DamageColumn - HeadshotColumn)
                val below = rememberFittingStyle(lines.map { it.text }, caption, nameWidth)
                // 숫자 세 칸 옆에서 KDA 줄을 한참 줄여야 들어가면 계열 안의 모든 줄에서 세 칸을 이름 밑으로 내린다
                val stacked = beside.fontSize.value < caption.fontSize.value * MIN_KDA_SCALE
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    WeaponColumns()
                    weapons.forEachIndexed { index, weapon ->
                        WeaponRow(weapon, catalog, lines[index], if (stacked) below else beside, stacked)
                    }
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

private val KdColumn = 48.dp
private val DamageColumn = 52.dp
private val HeadshotColumn = 56.dp
private val WeaponThumbWidth = 44.dp

private const val MIN_KDA_SCALE = 0.8f

/**
 * "1.87 (369/267/131)"처럼 KDA 뒤에 K/D/A 합계를 붙인 줄입니다. KDA만 한 단계 밝고 굵게 둡니다. 들고 시작한 라운드가
 * 모자라 데스와 어시가 믿을 만하지 않으면 KDA를 빼고 합계만 둡니다.
 */
@Composable
private fun kdaLine(weapon: WeaponStats): AnnotatedString {
    val counts = stringResource(
        Res.string.weapons_kda,
        weapon.kills.withThousands(),
        weapon.deaths.withThousands(),
        weapon.assists.withThousands(),
    )
    val kda = weapon.kda?.takeIf { weapon.isCarriedMeasurable } ?: return AnnotatedString(counts)
    val ratio = MetricFormat.TWO_DECIMALS.format(kda)
    val text = stringResource(Res.string.weapons_kda_with_counts, ratio, counts)
    val colors = OvalitTheme.colors
    return buildAnnotatedString {
        append(text)
        addStyle(SpanStyle(color = colors.t2, fontWeight = FontWeight.SemiBold), 0, ratio.length)
    }
}

// 펼친 계열 맨 위에 두는 열 제목이다. 숫자만 있으면 무엇인지 모른다. KDA는 이름 밑 줄의 제목이라 이름 쪽에 두고,
// K/D는 위쪽 세 무기 표처럼 오른쪽 첫 열에 둔다.
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
        listOf(
            stringResource(FixedMetric.KD.label) to KdColumn,
            stringResource(Res.string.column_damage) to DamageColumn,
            stringResource(Res.string.column_headshot) to HeadshotColumn,
        )
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
 * 무기 한 줄입니다. 이름 밑에 KDA와 K/D/A 합계를 두고 오른쪽에 K/D, 라운드당 피해량, 헤드샷을 둡니다. K/D와 피해량은
 * 들고 시작한 라운드, 헤드샷은 한 무기만 쓴 라운드가 표본입니다. 둘 다 모자라면 칸을 합쳐 "표본 부족"이라고 한 번만
 * 적고, 하나만 모자라면 그 칸만 비웁니다. [stacked]면 세 칸을 이름 밑 오른쪽에 둡니다.
 */
@Composable
private fun WeaponRow(weapon: WeaponStats, catalog: ContentCatalog, line: AnnotatedString, lineStyle: TextStyle, stacked: Boolean) {
    val name = catalog.weaponName(weapon.weapon)
    val nameAndKda: @Composable (Modifier) -> Unit = { modifier ->
        Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(2.dp)) {
            OvalitText(text = name, style = OvalitTheme.typography.body, maxLines = 1, overflow = TextOverflow.Ellipsis)
            OvalitText(
                text = line,
                style = lineStyle,
                color = OvalitTheme.colors.t3,
                maxLines = 1,
                autoSize = shrinkToFit(lineStyle.fontSize, min = 7.sp),
            )
        }
    }
    if (stacked) {
        Column(modifier = Modifier.semantics(mergeDescendants = true) {}, verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                WeaponThumb(weapon.weapon, name, width = WeaponThumbWidth, height = 24.dp)
                Spacer(Modifier.width(OvalitSpacing.md))
                nameAndKda(Modifier.weight(1f))
            }
            WeaponCells(weapon, Modifier.align(Alignment.End))
        }
    } else {
        Row(modifier = Modifier.semantics(mergeDescendants = true) {}, verticalAlignment = Alignment.CenterVertically) {
            WeaponThumb(weapon.weapon, name, width = WeaponThumbWidth, height = 24.dp)
            Spacer(Modifier.width(OvalitSpacing.md))
            nameAndKda(Modifier.weight(1f))
            WeaponCells(weapon)
        }
    }
}

@Composable
private fun WeaponCells(weapon: WeaponStats, modifier: Modifier = Modifier) {
    val colors = OvalitTheme.colors
    val metric = OvalitTheme.typography.metricS
    if (!weapon.isCarriedMeasurable && !weapon.isMeasurable) {
        OvalitText(
            text = stringResource(Res.string.not_enough_sample),
            modifier = modifier.width(KdColumn + DamageColumn + HeadshotColumn),
            style = OvalitTheme.typography.caption,
            color = colors.t3,
            textAlign = TextAlign.End,
        )
        return
    }
    Row(modifier = modifier) {
        OvalitText(
            text = weapon.value(WeaponMetric.KD)?.let { MetricFormat.TWO_DECIMALS.format(it) } ?: NO_VALUE,
            modifier = Modifier.width(KdColumn),
            style = metric,
            color = colors.t2,
            textAlign = TextAlign.End,
            maxLines = 1,
        )
        OvalitText(
            text = weapon.damagePerRound?.takeIf { weapon.isCarriedMeasurable }?.let { MetricFormat.INTEGER.format(it) } ?: NO_VALUE,
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

