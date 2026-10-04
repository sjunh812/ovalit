package com.ovalit.feature.settings

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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ovalit.core.designsystem.component.OvalitDisclaimer
import com.ovalit.core.designsystem.component.OvalitDivider
import com.ovalit.core.designsystem.component.OvalitOutlinedButton
import com.ovalit.core.designsystem.component.OvalitSwitch
import com.ovalit.core.designsystem.component.OvalitTabHeader
import com.ovalit.core.designsystem.component.OvalitText
import com.ovalit.core.designsystem.haptic.rememberOvalitHaptics
import com.ovalit.core.designsystem.icon.OvalitIcon
import com.ovalit.core.designsystem.icon.OvalitIcons
import com.ovalit.core.designsystem.theme.OvalitSpacing
import com.ovalit.core.designsystem.theme.OvalitTheme
import com.ovalit.core.model.Account
import com.ovalit.core.model.Focus
import com.ovalit.core.model.QueueFilter
import com.ovalit.core.model.ThemePreference
import com.ovalit.core.ui.PlayerAvatar
import com.ovalit.core.ui.label
import com.ovalit.feature.settings.resources.Res
import com.ovalit.feature.settings.resources.account_linked
import com.ovalit.feature.settings.resources.account_unlinked
import com.ovalit.feature.settings.resources.default_queue
import com.ovalit.feature.settings.resources.delete_data
import com.ovalit.feature.settings.resources.focus
import com.ovalit.feature.settings.resources.notify_analysis_done
import com.ovalit.feature.settings.resources.notify_ping
import com.ovalit.feature.settings.resources.notify_ping_description
import com.ovalit.feature.settings.resources.notify_weekly_report
import com.ovalit.feature.settings.resources.notify_weekly_report_time
import com.ovalit.feature.settings.resources.open_profile
import com.ovalit.feature.settings.resources.section_data
import com.ovalit.feature.settings.resources.section_display
import com.ovalit.feature.settings.resources.section_notifications
import com.ovalit.feature.settings.resources.section_public
import com.ovalit.feature.settings.resources.settings_title
import com.ovalit.feature.settings.resources.stats_public
import com.ovalit.feature.settings.resources.stats_public_description
import com.ovalit.feature.settings.resources.stored_matches
import com.ovalit.feature.settings.resources.stored_matches_count
import com.ovalit.feature.settings.resources.theme
import com.ovalit.feature.settings.resources.unlink
import com.ovalit.feature.settings.resources.unlink_note
import com.ovalit.feature.settings.resources.version
import com.ovalit.feature.settings.resources.view_profile
import kotlinx.datetime.number
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

private val RowMinHeight = 56.dp
private val AvatarSize = 44.dp

/**
 * S4 설정입니다.
 *
 * @param appVersion 앱 모듈만 버전을 알아서 밖에서 받습니다.
 * @param onUnlinked 연동을 해제하고 데이터를 다 지운 뒤에 불립니다. 앱 모듈이 여기서 인트로로 돌려보냅니다.
 * @param onOpenProfile 맨 위 계정 줄을 누르면 부릅니다. 홈 오른쪽 위 말고도 내 프로필로 가는 길입니다(사용자 요청, 2026-10-03).
 */
@Composable
fun SettingsRoute(
    appVersion: String,
    onUnlinked: () -> Unit,
    onOpenProfile: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = koinViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    SettingsScreen(
        uiState = uiState,
        appVersion = appVersion,
        actions = SettingsActions(
            onStatsPublicChange = viewModel::setStatsPublic,
            onNotifyAnalysisDoneChange = viewModel::setNotifyAnalysisDone,
            onNotifyWeeklyReportChange = viewModel::setNotifyWeeklyReport,
            onNotifyPingChange = viewModel::setNotifyPing,
            onThemeChange = viewModel::setTheme,
            onDefaultQueueChange = viewModel::setDefaultQueue,
            onFocusChange = viewModel::setFocus,
            onDeleteData = viewModel::deleteData,
            onUnlink = { viewModel.unlink(onUnlinked) },
            onOpenProfile = onOpenProfile,
        ),
        modifier = modifier,
    )
}

internal class SettingsActions(
    val onStatsPublicChange: (Boolean) -> Unit = {},
    val onNotifyAnalysisDoneChange: (Boolean) -> Unit = {},
    val onNotifyWeeklyReportChange: (Boolean) -> Unit = {},
    val onNotifyPingChange: (Boolean) -> Unit = {},
    val onThemeChange: (ThemePreference) -> Unit = {},
    val onDefaultQueueChange: (QueueFilter) -> Unit = {},
    val onFocusChange: (Focus) -> Unit = {},
    val onDeleteData: () -> Unit = {},
    val onUnlink: () -> Unit = {},
    val onOpenProfile: () -> Unit = {},
)

@Composable
internal fun SettingsScreen(
    uiState: SettingsUiState,
    appVersion: String,
    actions: SettingsActions,
    modifier: Modifier = Modifier,
) {
    var openSheet by rememberSaveable { mutableStateOf<SettingsSheet?>(null) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(OvalitTheme.colors.bg),
    ) {
        // 처음 열 때 머리 줄까지 비었다가 한꺼번에 뜨지 않게 머리는 바로 그린다. 설정은 기기에 있어 곧 뜬다.
        if (uiState !is SettingsUiState.Success) {
            OvalitTabHeader(title = stringResource(Res.string.settings_title), modifier = Modifier.safeDrawingPadding())
            return@Box
        }
        val preferences = uiState.preferences

        Column(
            modifier = Modifier
                .fillMaxSize()
                .safeDrawingPadding()
                .verticalScroll(rememberScrollState()),
        ) {
            OvalitTabHeader(title = stringResource(Res.string.settings_title))
            AccountHeader(uiState.account, onOpenProfile = actions.onOpenProfile)

            SectionHeader(stringResource(Res.string.section_public))
            ToggleRow(
                title = stringResource(Res.string.stats_public),
                description = stringResource(Res.string.stats_public_description),
                checked = preferences.statsPublic,
                onCheckedChange = actions.onStatsPublicChange,
            )

            SectionHeader(stringResource(Res.string.section_notifications))
            ToggleRow(
                title = stringResource(Res.string.notify_analysis_done),
                checked = preferences.notifyAnalysisDone,
                onCheckedChange = actions.onNotifyAnalysisDoneChange,
            )
            RowDivider()
            // 서버가 월요일 9시에 FCM 토픽으로 한 번 보낸다. 끄면 기기가 토픽 구독을 푼다.
            ToggleRow(
                title = stringResource(Res.string.notify_weekly_report),
                trailingLabel = stringResource(Res.string.notify_weekly_report_time),
                checked = preferences.notifyWeeklyReport,
                onCheckedChange = actions.onNotifyWeeklyReportChange,
            )
            RowDivider()
            ToggleRow(
                title = stringResource(Res.string.notify_ping),
                description = stringResource(Res.string.notify_ping_description),
                checked = preferences.notifyPing,
                onCheckedChange = actions.onNotifyPingChange,
            )

            SectionHeader(stringResource(Res.string.section_display))
            ValueRow(
                title = stringResource(Res.string.theme),
                value = stringResource(preferences.theme.label),
                onClick = { openSheet = SettingsSheet.THEME },
            )
            RowDivider()
            ValueRow(
                title = stringResource(Res.string.default_queue),
                value = stringResource(preferences.defaultQueue.label),
                onClick = { openSheet = SettingsSheet.DEFAULT_QUEUE },
            )
            RowDivider()
            ValueRow(
                title = stringResource(Res.string.focus),
                value = stringResource(preferences.focus.label),
                onClick = { openSheet = SettingsSheet.FOCUS },
            )

            SectionHeader(stringResource(Res.string.section_data))
            ValueRow(
                title = stringResource(Res.string.stored_matches),
                value = stringResource(Res.string.stored_matches_count, uiState.storedMatches),
            )
            RowDivider()
            ValueRow(
                title = stringResource(Res.string.delete_data),
                onClick = { openSheet = SettingsSheet.DELETE_DATA },
            )

            Spacer(Modifier.height(OvalitSpacing.xl))
            OvalitDivider(Modifier.padding(horizontal = OvalitSpacing.gutter))
            Spacer(Modifier.height(OvalitSpacing.xl))
            UnlinkSection(onClick = { openSheet = SettingsSheet.UNLINK })

            Spacer(Modifier.height(OvalitSpacing.xxl))
            Column(
                modifier = Modifier.padding(horizontal = OvalitSpacing.gutter),
                verticalArrangement = Arrangement.spacedBy(OvalitSpacing.sm),
            ) {
                OvalitDisclaimer(textAlign = TextAlign.Start)
                OvalitText(
                    text = stringResource(Res.string.version, appVersion),
                    style = OvalitTheme.typography.caption,
                    color = OvalitTheme.colors.t3,
                )
            }
            Spacer(Modifier.height(OvalitSpacing.xl))
        }

        openSheet?.let { sheet ->
            SettingsSheetContent(
                sheet = sheet,
                uiState = uiState,
                actions = actions,
                onDismiss = { openSheet = null },
            )
        }
    }
}

// 연동한 계정이면 줄 전체가 눌려 내 프로필로 간다. 목업의 계정 줄에 "프로필 보기"와 화살표만 더했다.
@Composable
private fun AccountHeader(account: Account?, onOpenProfile: () -> Unit) {
    val colors = OvalitTheme.colors

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (account != null) {
                    Modifier.clickable(onClickLabel = stringResource(Res.string.open_profile), role = Role.Button, onClick = onOpenProfile)
                } else {
                    Modifier
                },
            )
            .padding(horizontal = OvalitSpacing.gutter, vertical = OvalitSpacing.xs),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        PlayerAvatar(riotId = account?.riotId.orEmpty(), size = AvatarSize)
        Spacer(Modifier.width(OvalitSpacing.md))
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            if (account != null) {
                OvalitText(text = account.riotId, style = OvalitTheme.typography.bodyStrong)
                OvalitText(
                    text = stringResource(Res.string.account_linked, account.linkedOn.month.number, account.linkedOn.day),
                    style = OvalitTheme.typography.caption,
                    color = colors.t3,
                )
            } else {
                OvalitText(
                    text = stringResource(Res.string.account_unlinked),
                    style = OvalitTheme.typography.body,
                    color = colors.t2,
                )
            }
        }
        if (account != null) {
            Spacer(Modifier.width(OvalitSpacing.sm))
            OvalitText(text = stringResource(Res.string.view_profile), style = OvalitTheme.typography.caption, color = colors.t3)
            Spacer(Modifier.width(2.dp))
            OvalitIcon(OvalitIcons.ChevronRight, contentDescription = null, tint = colors.t4, size = 16.dp)
        }
    }
}

@Composable
private fun SectionHeader(text: String) {
    OvalitText(
        text = text,
        modifier = Modifier.padding(
            start = OvalitSpacing.gutter,
            end = OvalitSpacing.gutter,
            top = OvalitSpacing.xl,
            bottom = OvalitSpacing.xs,
        ),
        style = OvalitTheme.typography.label,
        color = OvalitTheme.colors.t3,
    )
}

@Composable
private fun ToggleRow(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    description: String? = null,
    trailingLabel: String? = null,
) {
    val haptics = rememberOvalitHaptics()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = RowMinHeight)
            .toggleable(
                value = checked,
                role = Role.Switch,
                onValueChange = { on ->
                    haptics.toggle(on)
                    onCheckedChange(on)
                },
            )
            .padding(horizontal = OvalitSpacing.gutter, vertical = OvalitSpacing.md),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(OvalitSpacing.xs),
        ) {
            OvalitText(text = title, style = OvalitTheme.typography.body)
            if (description != null) {
                OvalitText(text = description, style = OvalitTheme.typography.caption, color = OvalitTheme.colors.t3)
            }
        }
        if (trailingLabel != null) {
            OvalitText(text = trailingLabel, style = OvalitTheme.typography.label, color = OvalitTheme.colors.t3)
            Spacer(Modifier.width(OvalitSpacing.sm))
        } else {
            Spacer(Modifier.width(OvalitSpacing.lg))
        }
        OvalitSwitch(checked = checked)
    }
}

// 누를 곳이 없는 화살표는 두지 않는다. [onClick]이 있을 때만 그린다.
@Composable
private fun ValueRow(
    title: String,
    value: String? = null,
    onClick: (() -> Unit)? = null,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = RowMinHeight)
            .then(if (onClick != null) Modifier.clickable(role = Role.Button, onClick = onClick) else Modifier)
            .padding(horizontal = OvalitSpacing.gutter, vertical = OvalitSpacing.md),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        OvalitText(text = title, modifier = Modifier.weight(1f), style = OvalitTheme.typography.body)
        if (value != null) {
            Spacer(Modifier.width(OvalitSpacing.md))
            OvalitText(text = value, style = OvalitTheme.typography.body, color = OvalitTheme.colors.t2)
        }
        if (onClick != null) {
            Spacer(Modifier.width(OvalitSpacing.xs))
            OvalitIcon(OvalitIcons.ChevronRight, contentDescription = null, tint = OvalitTheme.colors.t4, size = 16.dp)
        }
    }
}

@Composable
private fun RowDivider() {
    OvalitDivider(
        modifier = Modifier.padding(start = OvalitSpacing.gutter),
        color = OvalitTheme.colors.lineWeak,
    )
}

@Composable
private fun UnlinkSection(onClick: () -> Unit) {
    Column(
        modifier = Modifier.padding(horizontal = OvalitSpacing.gutter),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        OvalitOutlinedButton(
            text = stringResource(Res.string.unlink),
            onClick = onClick,
            contentColor = OvalitTheme.colors.neg,
        )
        Spacer(Modifier.height(OvalitSpacing.sm))
        OvalitText(
            text = stringResource(Res.string.unlink_note),
            style = OvalitTheme.typography.caption,
            color = OvalitTheme.colors.t3,
            textAlign = TextAlign.Center,
        )
    }
}
