package com.ovalit.feature.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.ovalit.core.designsystem.component.OvalitBottomSheet
import com.ovalit.core.designsystem.component.OvalitPrimaryButton
import com.ovalit.core.designsystem.component.OvalitSheetOption
import com.ovalit.core.designsystem.component.OvalitTextButton
import com.ovalit.core.designsystem.theme.OvalitSpacing
import com.ovalit.core.model.Focus
import com.ovalit.core.model.PingReminder
import com.ovalit.core.model.QueueFilter
import com.ovalit.core.model.ThemePreference
import com.ovalit.core.ui.description
import com.ovalit.core.ui.label
import com.ovalit.feature.settings.resources.Res
import com.ovalit.feature.settings.resources.cancel
import com.ovalit.feature.settings.resources.default_queue
import com.ovalit.feature.settings.resources.default_queue_description
import com.ovalit.feature.settings.resources.delete_body
import com.ovalit.feature.settings.resources.delete_confirm
import com.ovalit.feature.settings.resources.delete_title
import com.ovalit.feature.settings.resources.focus
import com.ovalit.feature.settings.resources.focus_description
import com.ovalit.feature.settings.resources.ping_reminder
import com.ovalit.feature.settings.resources.ping_reminder_10
import com.ovalit.feature.settings.resources.ping_reminder_30
import com.ovalit.feature.settings.resources.ping_reminder_60
import com.ovalit.feature.settings.resources.ping_reminder_description
import com.ovalit.feature.settings.resources.ping_reminder_off
import com.ovalit.feature.settings.resources.theme
import com.ovalit.feature.settings.resources.theme_dark
import com.ovalit.feature.settings.resources.theme_light
import com.ovalit.feature.settings.resources.theme_system
import com.ovalit.feature.settings.resources.unlink_body
import com.ovalit.feature.settings.resources.unlink_confirm
import com.ovalit.feature.settings.resources.unlink_title
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

internal enum class SettingsSheet { THEME, DEFAULT_QUEUE, FOCUS, PING_REMINDER, DELETE_DATA, UNLINK }

internal val PingReminder.label: StringResource
    get() = when (this) {
        PingReminder.TEN_MINUTES -> Res.string.ping_reminder_10
        PingReminder.THIRTY_MINUTES -> Res.string.ping_reminder_30
        PingReminder.ONE_HOUR -> Res.string.ping_reminder_60
        PingReminder.OFF -> Res.string.ping_reminder_off
    }

internal val ThemePreference.label: StringResource
    get() = when (this) {
        ThemePreference.SYSTEM -> Res.string.theme_system
        ThemePreference.DARK -> Res.string.theme_dark
        ThemePreference.LIGHT -> Res.string.theme_light
    }

@Composable
internal fun SettingsSheetContent(
    sheet: SettingsSheet,
    uiState: SettingsUiState.Success,
    actions: SettingsActions,
    onDismiss: () -> Unit,
) {
    when (sheet) {
        SettingsSheet.THEME -> OptionSheet(
            title = stringResource(Res.string.theme),
            options = ThemePreference.entries,
            selected = uiState.preferences.theme,
            label = { it.label },
            onSelect = actions.onThemeChange,
            onDismiss = onDismiss,
        )
        SettingsSheet.DEFAULT_QUEUE -> OptionSheet(
            title = stringResource(Res.string.default_queue),
            body = stringResource(Res.string.default_queue_description),
            options = QueueFilter.entries,
            selected = uiState.preferences.defaultQueue,
            label = { it.label },
            onSelect = actions.onDefaultQueueChange,
            onDismiss = onDismiss,
        )
        SettingsSheet.FOCUS -> OptionSheet(
            title = stringResource(Res.string.focus),
            body = stringResource(Res.string.focus_description),
            options = Focus.entries,
            selected = uiState.preferences.focus,
            label = { it.label },
            onSelect = actions.onFocusChange,
            onDismiss = onDismiss,
            // S0-4처럼 무엇을 먼저 보는지 밑에 적는다. 이름만으로는 어떤 지표가 올라오는지 모른다.
            caption = { it.description },
        )
        SettingsSheet.PING_REMINDER -> OptionSheet(
            title = stringResource(Res.string.ping_reminder),
            body = stringResource(Res.string.ping_reminder_description),
            options = PingReminder.entries,
            selected = uiState.accountSettings.pingReminder,
            label = { it.label },
            onSelect = actions.onPingReminderChange,
            onDismiss = onDismiss,
        )
        SettingsSheet.DELETE_DATA -> ConfirmSheet(
            title = stringResource(Res.string.delete_title),
            body = stringResource(Res.string.delete_body, uiState.storedMatches),
            confirm = stringResource(Res.string.delete_confirm),
            onConfirm = actions.onDeleteData,
            onDismiss = onDismiss,
        )
        SettingsSheet.UNLINK -> ConfirmSheet(
            title = stringResource(Res.string.unlink_title),
            body = stringResource(Res.string.unlink_body),
            confirm = stringResource(Res.string.unlink_confirm),
            onConfirm = actions.onUnlink,
            onDismiss = onDismiss,
        )
    }
}

@Composable
private fun <T> OptionSheet(
    title: String,
    options: List<T>,
    selected: T,
    label: (T) -> StringResource,
    onSelect: (T) -> Unit,
    onDismiss: () -> Unit,
    body: String? = null,
    caption: ((T) -> StringResource)? = null,
) {
    OvalitBottomSheet(title = title, body = body, onDismiss = onDismiss) {
        Column(modifier = Modifier.selectableGroup()) {
            options.forEach { option ->
                OvalitSheetOption(
                    text = stringResource(label(option)),
                    caption = caption?.let { stringResource(it(option)) },
                    selected = option == selected,
                    onClick = {
                        onSelect(option)
                        onDismiss()
                    },
                )
            }
        }
    }
}

@Composable
private fun ConfirmSheet(
    title: String,
    body: String,
    confirm: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    OvalitBottomSheet(title = title, body = body, onDismiss = onDismiss) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(OvalitSpacing.xs),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            OvalitPrimaryButton(
                text = confirm,
                onClick = {
                    onConfirm()
                    onDismiss()
                },
            )
            OvalitTextButton(text = stringResource(Res.string.cancel), onClick = onDismiss)
        }
    }
}
