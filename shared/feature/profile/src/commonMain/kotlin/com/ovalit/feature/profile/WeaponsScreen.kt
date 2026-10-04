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
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role as SemanticsRole
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ovalit.core.designsystem.component.OvalitDisclosureIcon
import com.ovalit.core.designsystem.component.OvalitDivider
import com.ovalit.core.designsystem.component.OvalitExpandable
import com.ovalit.core.designsystem.component.OvalitStaged
import com.ovalit.core.designsystem.component.OvalitText
import com.ovalit.core.designsystem.component.currentMaxWidth
import com.ovalit.core.designsystem.component.rememberContentShown
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
import com.ovalit.core.ui.NO_VALUE
import com.ovalit.core.ui.SeparatedRow
import com.ovalit.core.ui.SeparatorDot
import com.ovalit.core.ui.WeaponThumb
import com.ovalit.core.ui.annotated
import com.ovalit.core.ui.format
import com.ovalit.core.ui.kdaText
import com.ovalit.core.ui.columnLabel
import com.ovalit.core.ui.label
import com.ovalit.core.ui.percentText
import com.ovalit.core.ui.periodLabel
import com.ovalit.core.ui.rememberFittingStyle
import com.ovalit.core.ui.rememberWidestWidth
import com.ovalit.core.ui.shrinkToFit
import com.ovalit.core.ui.valueText
import com.ovalit.core.ui.weaponName
import com.ovalit.core.ui.withThousands
import com.ovalit.feature.profile.resources.Res
import com.ovalit.feature.profile.resources.column_damage
import com.ovalit.feature.profile.resources.column_headshot
import com.ovalit.feature.profile.resources.column_kda
import com.ovalit.feature.profile.resources.no_matches
import com.ovalit.feature.profile.resources.weapons_act_value
import com.ovalit.feature.profile.resources.weapons_by_category
import com.ovalit.feature.profile.resources.weapons_category_unknown
import com.ovalit.feature.profile.resources.weapons_collapse
import com.ovalit.feature.profile.resources.weapons_compared
import com.ovalit.feature.profile.resources.weapons_expand
import com.ovalit.feature.profile.resources.weapons_kills
import com.ovalit.feature.profile.resources.weapons_moved_down
import com.ovalit.feature.profile.resources.weapons_moved_up
import com.ovalit.feature.profile.resources.weapons_owner_suffix
import com.ovalit.feature.profile.resources.weapons_sample_kills
import com.ovalit.feature.profile.resources.weapons_single_round_note
import com.ovalit.feature.profile.resources.weapons_title
import com.ovalit.feature.profile.resources.weapons_top_order
import com.ovalit.feature.profile.resources.weapons_top_title
import com.ovalit.feature.profile.resources.weapons_unused
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

/** S6 무기 화면입니다. 홈, 내 프로필, 친구 프로필(S5)에서 열립니다. */
@Composable
fun WeaponsRoute(
    owner: RecordsOwner,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: RecordsViewModel = koinViewModel { parametersOf(owner) },
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    WeaponsScreen(uiState, onBack, modifier, loadingTitle = Res.string.weapons_title.takeIf { owner == RecordsOwner.Me })
}

@Composable
internal fun WeaponsScreen(
    uiState: RecordsUiState,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    loadingTitle: StringResource? = Res.string.weapons_title,
) {
    // 뒤에서 센다. 밀려 들어오는 중에 도착하면 다 들어올 때까지 자리 틀을 두고 서서히 바꾼다.
    val loaded = uiState != RecordsUiState.Loading
    val shown = rememberContentShown(loaded = loaded)
    OvalitStaged(
        ready = loaded && shown,
        placeholder = { WeaponsSkeleton(loadingTitle, onBack) },
        modifier = modifier.fillMaxSize().background(OvalitTheme.colors.bg),
    ) {
        WeaponsContent(uiState, onBack)
    }
}

@Composable
private fun WeaponsContent(uiState: RecordsUiState, onBack: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize()) {
        when (uiState) {
            RecordsUiState.Loading -> return@Box
            RecordsUiState.Hidden -> {
                RecordsHidden(Res.string.weapons_title, onBack)
                return@Box
            }
            is RecordsUiState.Success -> Unit
        }
        val report = uiState.weapons

        Column(modifier = Modifier.fillMaxSize().safeDrawingPadding().verticalScroll(rememberScrollState())) {
            RecordsTopBar(
                title = Res.string.weapons_title,
                ownerSuffix = Res.string.weapons_owner_suffix,
                ownerName = uiState.ownerName,
                matches = report.matches,
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

// 위쪽 세 무기 표의 한 칸이다. change는 이번 액트 값을 띄운 표면 null이고, 비교할 값이 없으면 빈 글자다. 빈 글자로
// 두어야 줄 안의 칸 높이가 맞는다. rise는 움직였다고 판단한 변화의 방향이다. 오르면 1, 내리면 -1, 평소 범위 안이거나
// 모르면 0이다.
private class HighlightCell(val value: String, val change: String?, val rise: Int)

// 위쪽 세 무기의 표다. 이번 액트에 킬을 많이 낸 세 무기를 두고, 숫자는 홈 리포트와 같은 기간의 K/D, 라운드당 피해량,
// 헤드샷을 바로 앞 최대 4주 평균과 견준다. 어떤 세 무기인지는 제목 줄이, 숫자가 언제 것인지는 열 제목 줄이 말한다(사용자
// 요청, 2026-10-03). 표본이 적어도 그 기간 숫자를 그대로 띄우고 변화량은 표본을 넘긴 칸에만 적는다. 그 기간에 안 쓴 무기는
// 숫자 대신 안 썼다고 적는다. 리포트를 만들 만큼 경기가 없으면 표 전체가 이번 액트 값이다.
@Composable
private fun Highlights(report: WeaponReport, catalog: ContentCatalog) {
    val colors = OvalitTheme.colors
    val typography = OvalitTheme.typography
    val period = report.period
    val baselineWeeks = report.highlights.firstOrNull { highlight ->
        WeaponMetric.entries.any { highlight.current?.value(it) != null && highlight.baseline?.value(it) != null }
    }?.baselineWeeks

    val labels = WeaponMetric.entries.map { stringResource(it.fixed.columnLabel) }
    val periodText = period?.let { periodLabel(it) }
    val unusedText = periodText?.let { stringResource(Res.string.weapons_unused, it) }
    val rows = report.highlights.map { highlight ->
        HighlightLine(
            highlight = highlight,
            name = catalog.weaponName(highlight.act.weapon),
            cells = highlightCells(highlight, comparesPeriod = period != null),
        )
    }
    val moveUp = stringResource(Res.string.weapons_moved_up)
    val moveDown = stringResource(Res.string.weapons_moved_down)
    val actLabel = stringResource(Res.string.weapons_act_value)

    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = OvalitSpacing.gutter)) {
        Row(verticalAlignment = Alignment.Bottom) {
            OvalitText(
                text = stringResource(Res.string.weapons_top_title),
                modifier = Modifier.weight(1f),
                style = typography.bodyStrong,
            )
            OvalitText(text = stringResource(Res.string.weapons_top_order), style = typography.caption, color = colors.t3)
        }
        Spacer(Modifier.height(OvalitSpacing.md))
        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
            // 칸마다 따로 줄이면 자릿수가 많은 칸만 작아진다. 표 전체가 같은 크기를 쓰고, 옆 칸과 붙지 않게 폭을 조금 남긴다.
            // 숫자는 무기 이름과 같은 15sp, 변화량은 홈 칸처럼 한 단계 작은 11sp다. 17sp로 두니 아홉 칸 숫자와 색 칠한 변화량이
            // 이름보다 커서 표가 무거웠다(사용자 요청, 2026-10-03).
            val cellWidth = HighlightColumn - HighlightCellGap
            val valueStyle = rememberFittingStyle(
                rows.flatMap { row -> row.cells.orEmpty().map { it.value } },
                typography.metricM.copy(fontSize = typography.bodyStrong.fontSize, lineHeight = typography.bodyStrong.lineHeight),
                cellWidth,
            )
            val changeStyle = rememberFittingStyle(
                rows.flatMap { row -> row.cells.orEmpty().mapNotNull { it.change } },
                typography.metricS.copy(fontSize = 11.sp, lineHeight = 16.sp),
                cellWidth,
            )
            val labelStyle = rememberFittingStyle(labels, typography.caption, cellWidth)

            // 이름의 가장 긴 어절이나 "요즘 잘 맞아요", 킬 수가 숫자 옆 폭에 안 들어가면 세 줄 모두 숫자를 이름 밑으로
            // 내린다. 한 줄만 내리면 열이 어긋난다. 문구를 통째로 재서 "요즘 잘 / 맞아요"처럼 가운데서 갈리는 것도 막는다.
            val nameWords = rows.flatMap { it.name.split(' ') }
            val captions = rows.flatMap { row ->
                listOfNotNull(
                    row.tag?.let { if (it > 0) moveUp else moveDown },
                    killsText(row),
                )
            }
            val besideCells = currentMaxWidth - HighlightThumbWidth - OvalitSpacing.md - HighlightColumn * WeaponMetric.entries.size
            val needed = maxOf(rememberWidestWidth(nameWords, typography.bodyStrong), rememberWidestWidth(captions, typography.caption))
            val stacked = needed > besideCells
            val nameWidth = if (stacked) currentMaxWidth - HighlightThumbWidth - OvalitSpacing.md else besideCells
            // 이름은 어절 단위로 꺾이게 두고, 가장 긴 어절이 한 줄에 들어가는 크기로 셋을 같이 줄인다
            val styles = HighlightStyles(
                name = rememberFittingStyle(nameWords, typography.bodyStrong, nameWidth, min = 11.sp),
                value = valueStyle,
                change = changeStyle,
            )

            Column(verticalArrangement = Arrangement.spacedBy(OvalitSpacing.lg)) {
                Row(verticalAlignment = Alignment.Bottom) {
                    val caption = typography.caption
                    // 숫자가 언제 것인지가 이 표에서 가장 헷갈리는 자리라 기간만 한 단계 밝고 굵게 둔다
                    SeparatedRow(
                        items = listOfNotNull(
                            {
                                OvalitText(
                                    text = periodText ?: actLabel,
                                    style = caption.copy(fontWeight = FontWeight.SemiBold),
                                    color = colors.t2,
                                )
                            },
                            baselineWeeks?.let { weeks ->
                                { OvalitText(text = stringResource(Res.string.weapons_compared, weeks), style = caption, color = colors.t3) }
                            },
                        ),
                        separator = { SeparatorDot(caption, colors.t3) },
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
                rows.forEach { HighlightRow(it, styles, stacked, unusedText) }
            }
        }
    }
}

private val HighlightCellGap = 6.dp

private class HighlightStyles(val name: TextStyle, val value: TextStyle, val change: TextStyle)

private class HighlightLine(
    val highlight: WeaponHighlight,
    val name: String,
    val cells: List<HighlightCell>?,
) {
    // "요즘 잘 맞아요"는 맞히는 얘기라 헤드샷이 움직였을 때만 붙인다
    val tag: Int? get() = cells?.get(WeaponMetric.HEADSHOT_RATE.ordinal)?.rise?.takeIf { it != 0 }
}

// 순서를 정한 이번 액트 킬이다. 기간 킬을 적으면 킬이 적은 무기가 위에 있는 것처럼 보인다. 이번 액트라는 건 제목 줄이 말한다.
@Composable
private fun killsText(line: HighlightLine): String = stringResource(Res.string.weapons_sample_kills, line.highlight.act.kills.withThousands())

// 기간을 보는 표인데 그 기간에 이 무기를 안 썼으면 null이고 줄에 안 썼다고 적는다
@Composable
private fun highlightCells(highlight: WeaponHighlight, comparesPeriod: Boolean): List<HighlightCell>? {
    val shown = if (comparesPeriod) highlight.current ?: return null else highlight.act

    return WeaponMetric.entries.map { metric ->
        val format = metric.fixed.format
        // 변화량은 두 기간 모두 표본을 넘긴 값끼리, 보이는 자릿수로 반올림해 뺀다
        val now = highlight.current?.value(metric)
        val usual = highlight.baseline?.value(metric)
        HighlightCell(
            value = shown.recorded(metric)?.let { format.valueText(it) } ?: NO_VALUE,
            change = when {
                !comparesPeriod -> null
                now != null && usual != null -> format.formatChange(now, usual)
                else -> ""
            },
            // 동적 칸과 같은 규칙이다. 평소 흔들림 안의 변화는 칠하지도, 문구를 붙이지도 않는다.
            rise = if (now != null && usual != null && highlight.movement(metric) == Movement.MOVED) format.direction(now, usual) else 0,
        )
    }
}

// stacked면 숫자 세 칸을 이름 밑 오른쪽에 둔다. 열 위치는 그대로라 머리의 열 제목과 맞는다.
@Composable
private fun HighlightRow(line: HighlightLine, styles: HighlightStyles, stacked: Boolean, unusedText: String?) {
    val weapon = line.highlight.act.weapon
    if (stacked) {
        Column(modifier = Modifier.semantics(mergeDescendants = true) {}, verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                WeaponThumb(weapon, line.name, width = HighlightThumbWidth, height = 28.dp)
                Spacer(Modifier.width(OvalitSpacing.md))
                HighlightName(line, styles.name, Modifier.weight(1f))
            }
            HighlightCells(line, styles, unusedText, Modifier.align(Alignment.End))
        }
    } else {
        Row(modifier = Modifier.semantics(mergeDescendants = true) {}, verticalAlignment = Alignment.CenterVertically) {
            WeaponThumb(weapon, line.name, width = HighlightThumbWidth, height = 28.dp)
            Spacer(Modifier.width(OvalitSpacing.md))
            HighlightName(line, styles.name, Modifier.weight(1f))
            HighlightCells(line, styles, unusedText)
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
private fun HighlightCells(line: HighlightLine, styles: HighlightStyles, unusedText: String?, modifier: Modifier = Modifier) {
    val cells = line.cells
    if (cells == null) {
        OvalitText(
            text = unusedText.orEmpty(),
            modifier = modifier.width(HighlightColumn * WeaponMetric.entries.size),
            style = OvalitTheme.typography.caption,
            color = OvalitTheme.colors.t3,
            textAlign = TextAlign.End,
        )
        return
    }
    Row(modifier = modifier) {
        cells.forEach { cell ->
            Column(modifier = Modifier.width(HighlightColumn), horizontalAlignment = Alignment.End) {
                OvalitText(text = cell.value, style = styles.value, maxLines = 1)
                cell.change?.let { OvalitText(text = it, style = styles.change, color = riseColor(cell.rise), maxLines = 1) }
            }
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
    // 카탈로그에 없는 무기는 계열이 null이다. 펼친 계열이 없다는 뜻도 null이라 따로 키를 두지 않으면 "그 밖의 무기"가
    // 늘 펼쳐진 것으로 읽혀 접히지 않는다.
    fun WeaponCategory?.key(): String = this?.name ?: UNKNOWN_CATEGORY
    var expanded by rememberSaveable { mutableStateOf(byCategory.firstOrNull()?.key?.key()) }
    // 계열 이름과 킬 수 칸은 가장 긴 글자에 맞춘다. 폭을 박아 두면 "그 밖의 무기"가 기본 글자 크기에서도 잘렸다.
    val names = byCategory.map { (category, _) -> category?.let { stringResource(it.label) } ?: stringResource(Res.string.weapons_category_unknown) }
    val killTexts = byCategory.map { (_, weapons) -> stringResource(Res.string.weapons_kills, weapons.sumOf { it.kills }.withThousands()) }
    val nameWidth = rememberWidestWidth(names, OvalitTheme.typography.bodyStrong).coerceAtMost(MaxCategoryNameWidth)
    val killsWidth = rememberWidestWidth(killTexts, OvalitTheme.typography.metricS).coerceAtMost(MaxCategoryKillsWidth)

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
    byCategory.forEachIndexed { index, (category, weapons) -> key(category) {
        if (index > 0) OvalitDivider(Modifier.padding(start = OvalitSpacing.gutter), color = OvalitTheme.colors.lineWeak)
        val key = category.key()
        CategoryRow(
            name = names[index],
            kills = killTexts[index],
            share = weapons.sumOf { it.kills }.toFloat() / report.kills.coerceAtLeast(1),
            highlighted = category == top,
            expanded = expanded == key,
            onToggle = { expanded = if (expanded == key) null else key },
            nameWidth = nameWidth,
            killsWidth = killsWidth,
        )
        OvalitExpandable(visible = expanded == key) {
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxWidth()
                    // 계열 이름과 같은 자리에서 시작한다. 한 단계 들여 쓰니 무기 그림이 왼쪽에서 떨어져 떠 보였다(사용자 요청,
                    // 2026-10-03).
                    .padding(start = OvalitSpacing.gutter, end = OvalitSpacing.gutter, bottom = 10.dp),
            ) {
                // 줄마다 KDA 줄을 따로 줄이면 킬이 많은 총만 작아진다. 계열 안의 줄이 같은 크기를 쓴다.
                val caption = OvalitTheme.typography.caption
                val lines = weapons.map { weapon -> kdaText(weapon.kda, weapon.kills, weapon.deaths, weapon.assists).annotated() }
                val nameWidth = currentMaxWidth - WeaponThumbWidth - OvalitSpacing.md
                val beside = rememberFittingStyle(lines.map { it.text }, caption, nameWidth - KdColumn - DamageColumn - HeadshotColumn)
                val below = rememberFittingStyle(lines.map { it.text }, caption, nameWidth)
                // 세 칸 옆에 맞춘 KDA 줄 크기가 원래의 80%보다 작으면 계열 안의 모든 줄에서 세 칸을 이름 밑으로 내린다
                val stacked = beside.fontSize.value < caption.fontSize.value * MIN_KDA_SCALE
                // 이름과 KDA 두 줄짜리 줄이라 10dp로는 붙어 보였다(사용자 요청, 2026-10-03)
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    WeaponColumns()
                    weapons.forEachIndexed { index, weapon ->
                        WeaponRow(weapon, catalog, lines[index], if (stacked) below else beside, stacked)
                    }
                }
            }
        }
    } }
}

private const val UNKNOWN_CATEGORY = "UNKNOWN"
private val MaxCategoryNameWidth = 120.dp
private val MaxCategoryKillsWidth = 88.dp

@Composable
private fun CategoryRow(
    name: String,
    kills: String,
    share: Float,
    highlighted: Boolean,
    expanded: Boolean,
    onToggle: () -> Unit,
    nameWidth: Dp,
    killsWidth: Dp,
) {
    val action = stringResource(if (expanded) Res.string.weapons_collapse else Res.string.weapons_expand, name)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .clickable(onClickLabel = action, role = SemanticsRole.Button, onClick = onToggle)
            .padding(horizontal = OvalitSpacing.gutter),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val nameStyle = if (highlighted) OvalitTheme.typography.bodyStrong else OvalitTheme.typography.body
        OvalitText(
            text = name,
            modifier = Modifier.width(nameWidth),
            style = nameStyle,
            maxLines = 1,
            autoSize = shrinkToFit(nameStyle.fontSize),
        )
        Spacer(Modifier.width(OvalitSpacing.sm))
        ShareBar(fraction = share, highlighted = highlighted, modifier = Modifier.weight(1f))
        Spacer(Modifier.width(OvalitSpacing.sm))
        OvalitText(
            text = kills,
            modifier = Modifier.width(killsWidth),
            style = OvalitTheme.typography.metricS,
            color = if (highlighted) OvalitTheme.colors.t1 else OvalitTheme.colors.t2,
            textAlign = TextAlign.End,
            maxLines = 1,
            autoSize = shrinkToFit(OvalitTheme.typography.metricS.fontSize),
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
                    autoSize = shrinkToFit(style.fontSize),
                )
            }
    }
}

// stacked면 세 칸을 이름 밑 오른쪽에 둔다. 열 위치는 그대로라 계열 맨 위의 열 제목과 맞는다.
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
                autoSize = shrinkToFit(lineStyle.fontSize),
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

// K/D와 피해량은 들고 시작한 라운드, 헤드샷은 한 무기만 쓴 라운드로 센다. 표본이 적어도 내 기록이라 그대로 적는다(사용자
// 요청, 2026-10-03). 계산할 라운드가 하나도 없을 때만 비운다.
@Composable
private fun WeaponCells(weapon: WeaponStats, modifier: Modifier = Modifier) {
    val colors = OvalitTheme.colors
    val metric = OvalitTheme.typography.metricS
    Row(modifier = modifier) {
        OvalitText(
            text = weapon.kd?.let { MetricFormat.TWO_DECIMALS.format(it) } ?: NO_VALUE,
            modifier = Modifier.width(KdColumn),
            style = metric,
            color = colors.t2,
            textAlign = TextAlign.End,
            maxLines = 1,
        )
        OvalitText(
            text = weapon.damagePerRound?.let { MetricFormat.INTEGER.format(it) } ?: NO_VALUE,
            modifier = Modifier.width(DamageColumn),
            style = metric,
            color = colors.t2,
            textAlign = TextAlign.End,
            maxLines = 1,
        )
        OvalitText(
            text = weapon.headshotRate?.let { percentText(it) } ?: NO_VALUE,
            modifier = Modifier.width(HeadshotColumn),
            style = metric,
            color = colors.t1,
            textAlign = TextAlign.End,
            maxLines = 1,
        )
    }
}

