package com.ovalit.feature.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
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
import androidx.compose.runtime.produceState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ovalit.core.designsystem.component.OvalitCard
import com.ovalit.core.designsystem.component.OvalitCardGap
import com.ovalit.core.designsystem.component.OvalitDangerButton
import com.ovalit.core.designsystem.component.OvalitDisclaimer
import com.ovalit.core.designsystem.component.OvalitSwitch
import com.ovalit.core.designsystem.component.OvalitTabHeader
import com.ovalit.core.designsystem.component.OvalitText
import com.ovalit.core.designsystem.component.ScrollToTopOnReselect
import com.ovalit.core.designsystem.haptic.rememberOvalitHaptics
import com.ovalit.core.designsystem.icon.OvalitIcon
import com.ovalit.core.designsystem.icon.OvalitIcons
import com.ovalit.core.designsystem.theme.OvalitSpacing
import com.ovalit.core.designsystem.theme.OvalitTheme
import com.ovalit.core.model.Account
import com.ovalit.core.model.Focus
import com.ovalit.core.model.PingReminder
import com.ovalit.core.model.QueueFilter
import com.ovalit.core.model.ThemePreference
import com.ovalit.core.ui.FailureNoticesEffect
import com.ovalit.core.ui.LocalAdRenderer
import com.ovalit.core.ui.PlayerAvatar
import com.ovalit.core.ui.clockText
import com.ovalit.core.ui.label
import com.ovalit.feature.settings.resources.Res
import com.ovalit.feature.settings.resources.account_linked
import com.ovalit.feature.settings.resources.account_unlinked
import com.ovalit.feature.settings.resources.ad_free
import com.ovalit.feature.settings.resources.ad_free_description
import com.ovalit.feature.settings.resources.ad_free_until_today
import com.ovalit.feature.settings.resources.ad_free_until_tomorrow
import com.ovalit.feature.settings.resources.default_queue
import com.ovalit.feature.settings.resources.delete_data
import com.ovalit.feature.settings.resources.focus
import com.ovalit.feature.settings.resources.notify_analysis_done
import com.ovalit.feature.settings.resources.notify_ping
import com.ovalit.feature.settings.resources.notify_ping_description
import com.ovalit.feature.settings.resources.notify_ping_invites
import com.ovalit.feature.settings.resources.notify_weekly_report
import com.ovalit.feature.settings.resources.notify_weekly_report_time
import com.ovalit.feature.settings.resources.open_profile
import com.ovalit.feature.settings.resources.ping_reminder
import com.ovalit.feature.settings.resources.ping_reminder_description
import com.ovalit.feature.settings.resources.section_data
import com.ovalit.feature.settings.resources.section_display
import com.ovalit.feature.settings.resources.section_notifications
import com.ovalit.feature.settings.resources.section_public
import com.ovalit.feature.settings.resources.section_support
import com.ovalit.feature.settings.resources.send_feedback
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
import kotlin.time.Clock
import kotlin.time.Instant
import kotlinx.coroutines.delay
import kotlinx.datetime.TimeZone
import kotlinx.datetime.number
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

private val RowMinHeight = 56.dp
private val AvatarSize = 44.dp

/**
 * S4 설정입니다.
 *
 * @param appVersion 앱 모듈만 버전을 알아서 밖에서 받습니다.
 * @param onUnlinked 연동을 해제하고 데이터를 다 지운 뒤에 불립니다. 앱 모듈이 여기서 인트로로 돌려보냅니다.
 * @param onOpenProfile 맨 위 계정 줄을 누르면 내 프로필을 엽니다.
 * @param onSendFeedback 메일 앱을 엽니다. 받을 주소가 정해지지 않았으면 `null`이고 그 줄을 두지 않습니다.
 */
@Composable
fun SettingsRoute(
    appVersion: String,
    onUnlinked: () -> Unit,
    onOpenProfile: () -> Unit,
    modifier: Modifier = Modifier,
    onSendFeedback: (() -> Unit)? = null,
    viewModel: SettingsViewModel = koinViewModel(),
) {
    FailureNoticesEffect(viewModel.notices)
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    SettingsScreen(
        uiState = uiState,
        appVersion = appVersion,
        actions = SettingsActions(
            onStatsPublicChange = viewModel::setStatsPublic,
            onNotifyAnalysisDoneChange = viewModel::setNotifyAnalysisDone,
            onNotifyWeeklyReportChange = viewModel::setNotifyWeeklyReport,
            onNotifyPingChange = viewModel::setNotifyPing,
            onPingReminderChange = viewModel::setPingReminder,
            onThemeChange = viewModel::setTheme,
            onDefaultQueueChange = viewModel::setDefaultQueue,
            onFocusChange = viewModel::setFocus,
            onDeleteData = viewModel::deleteData,
            onUnlink = { viewModel.unlink(onUnlinked) },
            onOpenProfile = onOpenProfile,
            onSendFeedback = onSendFeedback,
        ),
        modifier = modifier,
    )
}

internal class SettingsActions(
    val onStatsPublicChange: (Boolean) -> Unit = {},
    val onNotifyAnalysisDoneChange: (Boolean) -> Unit = {},
    val onNotifyWeeklyReportChange: (Boolean) -> Unit = {},
    val onNotifyPingChange: (Boolean) -> Unit = {},
    val onPingReminderChange: (PingReminder) -> Unit = {},
    val onThemeChange: (ThemePreference) -> Unit = {},
    val onDefaultQueueChange: (QueueFilter) -> Unit = {},
    val onFocusChange: (Focus) -> Unit = {},
    val onDeleteData: () -> Unit = {},
    val onUnlink: () -> Unit = {},
    val onOpenProfile: () -> Unit = {},
    val onSendFeedback: (() -> Unit)? = null,
)

@Composable
internal fun SettingsScreen(
    uiState: SettingsUiState,
    appVersion: String,
    actions: SettingsActions,
    modifier: Modifier = Modifier,
    now: Instant = Clock.System.now(),
    timeZone: TimeZone = TimeZone.currentSystemDefault(),
) {
    var openSheet by rememberSaveable { mutableStateOf<SettingsSheet?>(null) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(OvalitTheme.colors.canvas),
    ) {
        // 처음 열 때 머리 줄까지 비었다가 한꺼번에 뜨지 않게 머리는 바로 그린다. 설정은 기기에 있어 곧 뜬다.
        if (uiState !is SettingsUiState.Success) {
            OvalitTabHeader(title = stringResource(Res.string.settings_title), modifier = Modifier.safeDrawingPadding())
            return@Box
        }
        val preferences = uiState.preferences

        val scroll = rememberScrollState()
        ScrollToTopOnReselect(scroll)
        Column(
            modifier = Modifier
                .fillMaxSize()
                .safeDrawingPadding()
                .verticalScroll(scroll),
        ) {
            OvalitTabHeader(title = stringResource(Res.string.settings_title))
            Spacer(Modifier.height(OvalitSpacing.xs))
            // 홈, 친구 탭처럼 묶음마다 카드 하나다. 묶음이 많아 선으로만 나누면 지금 어느 묶음인지 한눈에 안 들어온다.
            Column(verticalArrangement = Arrangement.spacedBy(OvalitCardGap)) {
                AccountCard(uiState.account, onOpenProfile = actions.onOpenProfile)

                SettingsCard(stringResource(Res.string.section_public)) {
                    ToggleRow(
                        title = stringResource(Res.string.stats_public),
                        description = stringResource(Res.string.stats_public_description),
                        checked = preferences.statsPublic,
                        onCheckedChange = actions.onStatsPublicChange,
                    )
                }

                SettingsCard(stringResource(Res.string.section_notifications)) {
                    ToggleRow(
                        title = stringResource(Res.string.notify_analysis_done),
                        checked = preferences.notifyAnalysisDone,
                        onCheckedChange = actions.onNotifyAnalysisDoneChange,
                    )
                    // 서버가 월요일 9시에 FCM 토픽으로 한 번 보낸다. 끄면 기기가 토픽 구독을 푼다.
                    ToggleRow(
                        title = stringResource(Res.string.notify_weekly_report),
                        trailingLabel = stringResource(Res.string.notify_weekly_report_time),
                        checked = preferences.notifyWeeklyReport,
                        onCheckedChange = actions.onNotifyWeeklyReportChange,
                    )
                }

                // 오발있? 알림과 거기 딸린 시작 전 알림을 한 카드에 둔다. 스위치를 끄면 시작 전 알림도 오지 않아 그 줄을 흐리게 남긴다.
                // 숨기면 화면이 튀고 그런 설정이 있다는 것도 안 보인다.
                SettingsCard(stringResource(Res.string.notify_ping)) {
                    ToggleRow(
                        title = stringResource(Res.string.notify_ping_invites),
                        description = stringResource(Res.string.notify_ping_description),
                        checked = preferences.notifyPing,
                        onCheckedChange = actions.onNotifyPingChange,
                    )
                    ValueRow(
                        title = stringResource(Res.string.ping_reminder),
                        description = stringResource(Res.string.ping_reminder_description),
                        value = stringResource(preferences.pingReminder.label),
                        enabled = preferences.notifyPing,
                        onClick = { openSheet = SettingsSheet.PING_REMINDER },
                    )
                }

                SettingsCard(stringResource(Res.string.section_display)) {
                    ValueRow(
                        title = stringResource(Res.string.theme),
                        value = stringResource(preferences.theme.label),
                        onClick = { openSheet = SettingsSheet.THEME },
                    )
                    ValueRow(
                        title = stringResource(Res.string.default_queue),
                        value = stringResource(preferences.defaultQueue.label),
                        onClick = { openSheet = SettingsSheet.DEFAULT_QUEUE },
                    )
                    ValueRow(
                        title = stringResource(Res.string.focus),
                        value = stringResource(preferences.focus.label),
                        onClick = { openSheet = SettingsSheet.FOCUS },
                    )
                    // 보상형 광고로 24시간 광고를 숨긴다. 광고 줄의 ×로도 같은 광고를 본다. 숨기는 동안에는 언제까지인지만 적는다.
                    val ads = LocalAdRenderer.current
                    if (ads != null && ads.canOfferAdFree) {
                        val until = rememberStillAhead(preferences.adFreeUntil, now)
                        ValueRow(
                            title = stringResource(Res.string.ad_free),
                            description = stringResource(Res.string.ad_free_description),
                            value = until?.let { adFreeUntilText(it, now, timeZone) },
                            onClick = if (until == null) ads::offerAdFree else null,
                        )
                    }
                }

                SettingsCard(stringResource(Res.string.section_data)) {
                    ValueRow(
                        title = stringResource(Res.string.stored_matches),
                        value = stringResource(Res.string.stored_matches_count, uiState.storedMatches),
                    )
                    ValueRow(
                        title = stringResource(Res.string.delete_data),
                        onClick = { openSheet = SettingsSheet.DELETE_DATA },
                    )
                }

                actions.onSendFeedback?.let { sendFeedback ->
                    SettingsCard(stringResource(Res.string.section_support)) {
                        ValueRow(title = stringResource(Res.string.send_feedback), onClick = sendFeedback)
                    }
                }
            }

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

// 연동한 계정이면 카드 전체가 눌려 내 프로필로 간다. 목업의 계정 줄에 "프로필 보기"와 화살표만 더했다.
@Composable
private fun AccountCard(account: Account?, onOpenProfile: () -> Unit) {
    val colors = OvalitTheme.colors

    OvalitCard(
        onClick = if (account != null) onOpenProfile else null,
        onClickLabel = stringResource(Res.string.open_profile),
    ) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = OvalitSpacing.gutter),
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
}

// 묶음 하나를 담는 카드다. 묶음 이름은 친구 탭 카드처럼 카드 안 맨 위에 굵게 둔다.
@Composable
private fun SettingsCard(title: String, content: @Composable ColumnScope.() -> Unit) {
    // 줄마다 위아래 여백이 있어 카드 아래 여백은 줄인다
    OvalitCard(bottomPadding = OvalitSpacing.sm) {
        OvalitText(
            text = title,
            modifier = Modifier.padding(start = OvalitSpacing.gutter, end = OvalitSpacing.gutter, bottom = OvalitSpacing.xs),
            style = OvalitTheme.typography.bodyStrong,
        )
        content()
    }
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
    description: String? = null,
    enabled: Boolean = true,
    onClick: (() -> Unit)? = null,
) {
    val colors = OvalitTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = RowMinHeight)
            .then(if (onClick != null) Modifier.clickable(enabled = enabled, role = Role.Button, onClick = onClick) else Modifier)
            .padding(horizontal = OvalitSpacing.gutter, vertical = OvalitSpacing.md),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(OvalitSpacing.xs),
        ) {
            OvalitText(text = title, style = OvalitTheme.typography.body, color = if (enabled) colors.t1 else colors.t4)
            if (description != null) {
                OvalitText(text = description, style = OvalitTheme.typography.caption, color = if (enabled) colors.t3 else colors.t4)
            }
        }
        if (value != null) {
            Spacer(Modifier.width(OvalitSpacing.md))
            OvalitText(text = value, style = OvalitTheme.typography.body, color = if (enabled) colors.t2 else colors.t4)
        }
        if (onClick != null) {
            Spacer(Modifier.width(OvalitSpacing.xs))
            OvalitIcon(OvalitIcons.ChevronRight, contentDescription = null, tint = if (enabled) colors.t4 else colors.t5, size = 16.dp)
        }
    }
}

// 끝나는 시각이 오늘이면 "오늘 23:00까지", 내일이면 "내일 16:20까지"다
/**
 * [until]이 [now]보다 뒤면 그대로, 지났으면 `null`입니다. 지나는 순간 `null`로 바꿔 다시 그립니다. 그릴 때만 견주면 광고 없이
 * 보기가 끝난 뒤에도 "오늘 16:20까지"에 멈춰 다시 누를 수 없습니다.
 */
@Composable
private fun rememberStillAhead(until: Instant?, now: Instant): Instant? {
    val ahead by produceState(until?.takeIf { it > now }, until, now) {
        val left = until?.let { it - now }
        if (left != null && left.isPositive()) {
            value = until
            delay(left)
        }
        value = null
    }
    return ahead
}

@Composable
private fun adFreeUntilText(until: Instant, now: Instant, timeZone: TimeZone): String {
    val end = until.toLocalDateTime(timeZone)
    val time = clockText(end)
    val today = now.toLocalDateTime(timeZone).date
    return stringResource(if (end.date == today) Res.string.ad_free_until_today else Res.string.ad_free_until_tomorrow, time)
}

@Composable
private fun UnlinkSection(onClick: () -> Unit) {
    Column(
        modifier = Modifier.padding(horizontal = OvalitSpacing.gutter),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        OvalitDangerButton(text = stringResource(Res.string.unlink), onClick = onClick)
        Spacer(Modifier.height(OvalitSpacing.sm))
        OvalitText(
            text = stringResource(Res.string.unlink_note),
            style = OvalitTheme.typography.caption,
            color = OvalitTheme.colors.t3,
            textAlign = TextAlign.Center,
        )
    }
}
