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
import androidx.compose.foundation.shape.CircleShape
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
import com.ovalit.core.designsystem.component.OvalitText
import com.ovalit.core.designsystem.icon.OvalitIcon
import com.ovalit.core.designsystem.icon.OvalitIcons
import com.ovalit.core.designsystem.theme.OvalitSpacing
import com.ovalit.core.designsystem.theme.OvalitTheme
import com.ovalit.core.model.Account
import com.ovalit.core.model.QueueFilter
import com.ovalit.core.model.ThemePreference
import com.ovalit.feature.settings.resources.Res
import com.ovalit.feature.settings.resources.account_linked
import com.ovalit.feature.settings.resources.account_unlinked
import com.ovalit.feature.settings.resources.default_queue
import com.ovalit.feature.settings.resources.delete_data
import com.ovalit.feature.settings.resources.notify_analysis_done
import com.ovalit.feature.settings.resources.notify_weekly_report
import com.ovalit.feature.settings.resources.notify_weekly_report_time
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
import kotlinx.datetime.number
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

private val RowMinHeight = 56.dp
private val AvatarSize = 44.dp

/**
 * S4 설정입니다.
 *
 * @param appVersion 앱 모듈만 버전을 알아서 밖에서 받습니다.
 * @param onUnlinked 연동을 해제하고 데이터를 다 지운 뒤에 불립니다. 인트로로 돌려보냅니다.
 */
@Composable
fun SettingsRoute(
    appVersion: String,
    onUnlinked: () -> Unit,
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
            onThemeChange = viewModel::setTheme,
            onDefaultQueueChange = viewModel::setDefaultQueue,
            onDeleteData = viewModel::deleteData,
            onUnlink = { viewModel.unlink(onUnlinked) },
        ),
        modifier = modifier,
    )
}

internal class SettingsActions(
    val onStatsPublicChange: (Boolean) -> Unit = {},
    val onNotifyAnalysisDoneChange: (Boolean) -> Unit = {},
    val onNotifyWeeklyReportChange: (Boolean) -> Unit = {},
    val onThemeChange: (ThemePreference) -> Unit = {},
    val onDefaultQueueChange: (QueueFilter) -> Unit = {},
    val onDeleteData: () -> Unit = {},
    val onUnlink: () -> Unit = {},
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
        if (uiState !is SettingsUiState.Success) return@Box
        val preferences = uiState.preferences

        Column(
            modifier = Modifier
                .fillMaxSize()
                .safeDrawingPadding()
                .verticalScroll(rememberScrollState()),
        ) {
            Spacer(Modifier.height(OvalitSpacing.gutter))
            OvalitText(
                text = stringResource(Res.string.settings_title),
                modifier = Modifier.padding(horizontal = OvalitSpacing.gutter),
                style = OvalitTheme.typography.titleL,
            )
            Spacer(Modifier.height(OvalitSpacing.lg))
            AccountHeader(uiState.account)

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
            ToggleRow(
                title = stringResource(Res.string.notify_weekly_report),
                trailingLabel = stringResource(Res.string.notify_weekly_report_time),
                checked = preferences.notifyWeeklyReport,
                onCheckedChange = actions.onNotifyWeeklyReportChange,
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

@Composable
private fun AccountHeader(account: Account?) {
    val colors = OvalitTheme.colors

    Row(
        modifier = Modifier.padding(horizontal = OvalitSpacing.gutter),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // 플레이어 카드는 콘텐츠 카탈로그가 붙은 뒤에 넣는다. 그전까지는 이름 첫 글자로 자리를 잡는다.
        Box(
            modifier = Modifier.size(AvatarSize).background(colors.fill, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            OvalitText(
                text = account?.riotId?.take(1).orEmpty(),
                style = OvalitTheme.typography.bodyStrong,
                color = colors.t2,
            )
        }
        Spacer(Modifier.width(OvalitSpacing.md))
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
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
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = RowMinHeight)
            .toggleable(value = checked, role = Role.Switch, onValueChange = onCheckedChange)
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
