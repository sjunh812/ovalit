package com.ovalit.core.testing

import com.ovalit.core.data.UserPreferencesRepository
import com.ovalit.core.model.Focus
import com.ovalit.core.model.QueueFilter
import com.ovalit.core.model.ThemePreference
import com.ovalit.core.model.UserPreferences
import kotlin.time.Instant
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/** 메모리에만 두는 기기 설정입니다. 바꾸면 [preferences]에 바로 들어가서 테스트가 저장된 값을 읽어 볼 수 있습니다. */
class TestUserPreferencesRepository(initial: UserPreferences = UserPreferences.Default) : UserPreferencesRepository {

    private val state = MutableStateFlow(initial)

    override val preferences: StateFlow<UserPreferences> = state.asStateFlow()

    override suspend fun setTheme(theme: ThemePreference) = state.update { it.copy(theme = theme) }

    override suspend fun setDefaultQueue(queue: QueueFilter) = state.update { it.copy(defaultQueue = queue) }

    override suspend fun setNotifyAnalysisDone(enabled: Boolean) = state.update { it.copy(notifyAnalysisDone = enabled) }

    override suspend fun setNotifyWeeklyReport(enabled: Boolean) = state.update { it.copy(notifyWeeklyReport = enabled) }

    override suspend fun setNotifyPing(enabled: Boolean) = state.update { it.copy(notifyPing = enabled) }

    override suspend fun setFocus(focus: Focus) = state.update { it.copy(focus = focus) }

    override suspend fun setSeenProfileHint() = state.update { it.copy(seenProfileHint = true) }

    override suspend fun setSeenNotificationPrimer() = state.update { it.copy(seenNotificationPrimer = true) }

    override suspend fun setAskedNotificationPermission() = state.update { it.copy(askedNotificationPermission = true) }

    override suspend fun setAdFreeUntil(until: Instant) = state.update { it.copy(adFreeUntil = until) }
}
