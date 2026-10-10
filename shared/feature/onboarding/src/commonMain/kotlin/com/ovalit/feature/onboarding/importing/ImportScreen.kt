package com.ovalit.feature.onboarding.importing

import androidx.compose.animation.animateColorAsState
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
import com.ovalit.core.designsystem.component.OvalitPressOutset
import com.ovalit.core.designsystem.component.OvalitText
import com.ovalit.core.designsystem.component.pressIndication
import com.ovalit.core.designsystem.icon.OvalitIcon
import com.ovalit.core.designsystem.icon.OvalitIcons
import com.ovalit.core.designsystem.theme.OvalitSpacing
import com.ovalit.core.designsystem.theme.OvalitTheme
import com.ovalit.core.model.Focus
import com.ovalit.core.model.ImportProgress
import com.ovalit.core.model.OvalitError
import com.ovalit.core.ui.NotificationPermission
import com.ovalit.core.ui.description
import com.ovalit.core.ui.label
import com.ovalit.core.ui.resultColor
import com.ovalit.feature.onboarding.resources.Res
import com.ovalit.feature.onboarding.resources.import_background
import com.ovalit.feature.onboarding.resources.import_background_silent
import com.ovalit.feature.onboarding.resources.import_count
import com.ovalit.feature.onboarding.resources.import_done
import com.ovalit.feature.onboarding.resources.import_done_none
import com.ovalit.feature.onboarding.resources.import_loading
import com.ovalit.feature.onboarding.resources.import_notify_when_done
import com.ovalit.feature.onboarding.resources.import_open_report
import com.ovalit.feature.onboarding.resources.import_progress_description
import com.ovalit.feature.onboarding.resources.import_retry
import com.ovalit.feature.onboarding.resources.import_stopped
import com.ovalit.feature.onboarding.resources.import_stopped_later
import com.ovalit.feature.onboarding.resources.import_stopped_offline
import com.ovalit.feature.onboarding.resources.import_subtitle
import com.ovalit.feature.onboarding.resources.import_title
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

// 목업의 선택지 순서다. 기본값인 "특별히 없어요"는 아무것도 안 고른 것과 같아서 맨 아래에 둔다.
private val FocusOrder = listOf(Focus.AIM, Focus.ROUND_PLAY, Focus.CONSISTENCY, Focus.NONE)

/**
 * S0-4 불러오는 중입니다. 수집이 끝나야 리포트로 넘어갈 수 있습니다.
 *
 * @param notifications 다 불러오면 보낼 알림의 권한입니다. 들어올 때 묻지 않고, 권한이 없으면 사용자가 직접 켜는 버튼을 둡니다.
 */
@Composable
fun ImportRoute(
    onOpenReport: () -> Unit,
    modifier: Modifier = Modifier,
    notifications: NotificationPermission = NotificationPermission.NotNeeded,
    viewModel: ImportViewModel = koinViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    ImportScreen(
        uiState,
        onSelectFocus = viewModel::selectFocus,
        onOpenReport = onOpenReport,
        onRetry = viewModel::retry,
        modifier = modifier,
        notifications = notifications,
    )
}

@Composable
internal fun ImportScreen(
    uiState: ImportUiState,
    onSelectFocus: (Focus) -> Unit,
    onOpenReport: () -> Unit,
    modifier: Modifier = Modifier,
    onRetry: () -> Unit = {},
    notifications: NotificationPermission = NotificationPermission.NotNeeded,
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
                    style = OvalitTheme.typography.display,
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
            Progress(uiState.progress, onOpenReport, onRetry) {
                BackgroundNote(notifyWhenDone = uiState.notifyWhenDone, notifications = notifications)
            }
        }
    }
}

@Composable
private fun FocusOption(focus: Focus, selected: Boolean, onClick: () -> Unit) {
    val colors = OvalitTheme.colors
    val shape = RoundedCornerShape(12.dp)
    // 목업처럼 고른 칸만 액센트 테두리와 체크로 표시한다
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

/** @param loading 받는 동안 진행 막대 밑에 두는 안내입니다. */
@Composable
private fun Progress(progress: ImportProgress?, onOpenReport: () -> Unit, onRetry: () -> Unit, loading: @Composable () -> Unit) {
    val colors = OvalitTheme.colors
    val loaded = progress?.loaded ?: 0
    val total = progress?.total ?: 0
    val done = progress?.isDone == true
    val stoppedBy = progress?.stoppedBy?.takeIf { !done }
    val description = stringResource(Res.string.import_progress_description, loaded, total)

    Column(modifier = Modifier.padding(start = OvalitSpacing.xl, end = OvalitSpacing.xl, bottom = OvalitSpacing.xl)) {
        OvalitDivider()
        Spacer(Modifier.height(18.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            OvalitText(
                text = when {
                    done && total == 0 -> stringResource(Res.string.import_done_none)
                    done -> stringResource(Res.string.import_done, total)
                    stoppedBy != null -> stringResource(Res.string.import_stopped)
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
        when {
            done -> OvalitPrimaryButton(text = stringResource(Res.string.import_open_report), onClick = onOpenReport)
            // 멈춰도 받은 칸은 그대로 두고 남은 것만 이어 받는다. 기다리면 저절로 다시 시도한다.
            stoppedBy != null -> {
                OvalitText(
                    text = stringResource(if (stoppedBy == OvalitError.Offline) Res.string.import_stopped_offline else Res.string.import_stopped_later),
                    style = OvalitTheme.typography.caption,
                    color = colors.t3,
                )
                Spacer(Modifier.height(12.dp))
                OvalitPrimaryButton(text = stringResource(Res.string.import_retry), onClick = onRetry)
            }
            else -> loading()
        }
    }
}

// 알림을 보낼 수 없으면 보내겠다고 적지 않는다. 권한이 없으면 그 밑에서 직접 켜게 한다. 들어오자마자 시스템 창으로 물으면
// 무엇을 알려 주는지 모른 채 거절하기 쉽고, 한 번 거절하면 다시 묻기 어렵다.
@Composable
private fun BackgroundNote(notifyWhenDone: Boolean, notifications: NotificationPermission) {
    val colors = OvalitTheme.colors
    val willNotify = notifyWhenDone && !notifications.missing
    OvalitText(
        text = stringResource(if (willNotify) Res.string.import_background else Res.string.import_background_silent),
        style = OvalitTheme.typography.caption,
        color = colors.t3,
    )
    // 설정에서 분석 완료 알림을 껐으면 사용자가 고른 것이라 켜자고 하지 않는다
    if (notifyWhenDone && notifications.missing) {
        Row(
            modifier = Modifier
                .heightIn(min = TouchHeight)
                .clickable(
                    interactionSource = null,
                    indication = pressIndication(horizontalOutset = OvalitPressOutset),
                    role = Role.Button,
                    onClick = notifications.request,
                ),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            OvalitText(
                text = stringResource(Res.string.import_notify_when_done),
                style = OvalitTheme.typography.label.copy(fontWeight = FontWeight.SemiBold),
                color = colors.t1,
            )
            Spacer(Modifier.width(2.dp))
            OvalitIcon(OvalitIcons.ChevronRight, contentDescription = null, tint = colors.t3, size = 14.dp)
        }
    }
}

private val TouchHeight = 44.dp
