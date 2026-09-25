package com.ovalit.feature.onboarding.importing

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ovalit.core.designsystem.component.OvalitDivider
import com.ovalit.core.designsystem.component.OvalitPrimaryButton
import com.ovalit.core.designsystem.component.OvalitText
import com.ovalit.core.designsystem.component.pressIndication
import com.ovalit.core.designsystem.icon.OvalitIcon
import com.ovalit.core.designsystem.icon.OvalitIcons
import com.ovalit.core.designsystem.theme.OvalitSpacing
import com.ovalit.core.designsystem.theme.OvalitTheme
import com.ovalit.core.model.Focus
import com.ovalit.core.model.ImportProgress
import com.ovalit.core.ui.description
import com.ovalit.core.ui.label
import com.ovalit.core.ui.resultColor
import com.ovalit.feature.onboarding.resources.Res
import com.ovalit.feature.onboarding.resources.import_background
import com.ovalit.feature.onboarding.resources.import_count
import com.ovalit.feature.onboarding.resources.import_done
import com.ovalit.feature.onboarding.resources.import_done_none
import com.ovalit.feature.onboarding.resources.import_loading
import com.ovalit.feature.onboarding.resources.import_open_report
import com.ovalit.feature.onboarding.resources.import_progress_description
import com.ovalit.feature.onboarding.resources.import_subtitle
import com.ovalit.feature.onboarding.resources.import_title
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

// 목업의 선택지 순서다. "특별히 없어요"가 기본값이라 맨 아래에 선택된 채로 둔다.
private val FocusOrder = listOf(Focus.AIM, Focus.ROUND_PLAY, Focus.CONSISTENCY, Focus.NONE)

/** S0-4 불러오는 중입니다. 수집이 끝나야 리포트로 넘어갈 수 있습니다. */
@Composable
fun ImportRoute(
    onOpenReport: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ImportViewModel = koinViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    ImportScreen(uiState, onSelectFocus = viewModel::selectFocus, onOpenReport = onOpenReport, modifier = modifier)
}

@Composable
internal fun ImportScreen(
    uiState: ImportUiState,
    onSelectFocus: (Focus) -> Unit,
    onOpenReport: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = OvalitTheme.colors
    Box(modifier = modifier.fillMaxSize().background(colors.bg)) {
        if (uiState !is ImportUiState.Success) return@Box

        Column(modifier = Modifier.fillMaxSize().safeDrawingPadding()) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = OvalitSpacing.xl),
            ) {
                Spacer(Modifier.height(56.dp))
                OvalitText(
                    text = stringResource(Res.string.import_title),
                    modifier = Modifier.semantics { heading() },
                    style = OvalitTheme.typography.titleL,
                )
                Spacer(Modifier.height(14.dp))
                OvalitText(text = stringResource(Res.string.import_subtitle), style = OvalitTheme.typography.label, color = colors.t3)
                Spacer(Modifier.height(28.dp))
                Column(modifier = Modifier.selectableGroup(), verticalArrangement = Arrangement.spacedBy(9.dp)) {
                    FocusOrder.forEach { focus ->
                        FocusOption(focus, selected = focus == uiState.focus, onClick = { onSelectFocus(focus) })
                    }
                }
                Spacer(Modifier.height(OvalitSpacing.xl))
            }
            Progress(uiState.progress, onOpenReport)
        }
    }
}

@Composable
private fun FocusOption(focus: Focus, selected: Boolean, onClick: () -> Unit) {
    val colors = OvalitTheme.colors
    val shape = RoundedCornerShape(12.dp)
    // 목업처럼 고른 칸만 금색 테두리와 체크로 표시한다
    val border by animateColorAsState(if (selected) colors.accent else colors.line)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            // 누르면 칸 면과 테두리까지 같이 줄어야 해서 칠하기 전에 단다
            .selectable(
                selected = selected,
                interactionSource = null,
                indication = pressIndication(shape),
                role = Role.RadioButton,
                onClick = onClick,
            )
            .clip(shape)
            .background(if (selected) colors.fill else colors.bg)
            .border(1.dp, border, shape)
            .padding(horizontal = OvalitSpacing.lg, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            OvalitText(text = stringResource(focus.label), style = OvalitTheme.typography.bodyStrong)
            OvalitText(
                text = stringResource(focus.description),
                style = OvalitTheme.typography.caption,
                color = if (selected) colors.t2 else colors.t3,
            )
        }
        Spacer(Modifier.size(OvalitSpacing.md))
        Box(
            modifier = Modifier
                .size(20.dp)
                .clip(CircleShape)
                .then(if (selected) Modifier.background(colors.accent) else Modifier.border(1.5.dp, colors.t4, CircleShape)),
            contentAlignment = Alignment.Center,
        ) {
            if (selected) OvalitIcon(OvalitIcons.Check, contentDescription = null, tint = colors.onAccent, size = 12.dp)
        }
    }
}

@Composable
private fun Progress(progress: ImportProgress?, onOpenReport: () -> Unit) {
    val colors = OvalitTheme.colors
    val loaded = progress?.loaded ?: 0
    val total = progress?.total ?: 0
    val done = progress?.isDone == true
    val description = stringResource(Res.string.import_progress_description, loaded, total)

    Column(modifier = Modifier.padding(start = OvalitSpacing.xl, end = OvalitSpacing.xl, bottom = OvalitSpacing.xl)) {
        OvalitDivider()
        Spacer(Modifier.height(18.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            OvalitText(
                text = when {
                    done && total == 0 -> stringResource(Res.string.import_done_none)
                    done -> stringResource(Res.string.import_done, total)
                    else -> stringResource(Res.string.import_loading)
                },
                modifier = Modifier.weight(1f),
                style = OvalitTheme.typography.label.copy(fontWeight = FontWeight.SemiBold),
            )
            if (total > 0) {
                OvalitText(
                    text = stringResource(Res.string.import_count, loaded, total),
                    style = OvalitTheme.typography.metricS.copy(fontWeight = FontWeight.Bold),
                    color = colors.t2,
                )
            }
        }
        if (total > 0) {
            Spacer(Modifier.height(11.dp))
            // 받은 경기마다 한 칸씩 이긴 판은 초록, 진 판은 빨강으로 채운다. 아직 안 받은 칸은 흐리게 둔다.
            Row(
                modifier = Modifier.fillMaxWidth().height(14.dp).semantics { contentDescription = description },
                horizontalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                repeat(total) { index ->
                    val result = progress?.results?.getOrNull(index)
                    val received = index < loaded
                    Box(
                        Modifier
                            .weight(1f)
                            .height(14.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(if (received) resultColor(result) else colors.lineWeak),
                    )
                }
            }
        }
        Spacer(Modifier.height(12.dp))
        if (done) {
            OvalitPrimaryButton(text = stringResource(Res.string.import_open_report), onClick = onOpenReport)
        } else {
            OvalitText(text = stringResource(Res.string.import_background), style = OvalitTheme.typography.caption, color = colors.t3)
        }
    }
}
