package com.ovalit.core.data

import com.ovalit.core.model.Focus
import com.ovalit.core.model.PingReminder
import com.ovalit.core.model.QueueFilter
import com.ovalit.core.model.ThemePreference
import com.ovalit.core.model.UserPreferences
import kotlin.time.Instant
import kotlinx.coroutines.flow.Flow

interface UserPreferencesRepository {

    val preferences: Flow<UserPreferences>

    suspend fun setTheme(theme: ThemePreference)

    suspend fun setDefaultQueue(queue: QueueFilter)

    suspend fun setStatsPublic(public: Boolean)

    suspend fun setNotifyAnalysisDone(enabled: Boolean)

    suspend fun setNotifyWeeklyReport(enabled: Boolean)

    suspend fun setNotifyPing(enabled: Boolean)

    suspend fun setPingReminder(reminder: PingReminder)


    suspend fun setFocus(focus: Focus)

    /** 홈의 내 프로필 안내를 띄웠다고 적습니다. 다시 띄우지 않습니다. */
    suspend fun setSeenProfileHint()

    /** 알림을 켜 달라는 시트를 띄웠다고 적습니다. 다시 띄우지 않습니다. */
    suspend fun setSeenNotificationPrimer()

    /** 시스템 알림 권한 창을 띄웠다고 적습니다. */
    suspend fun setAskedNotificationPermission()

    /** 보상형 광고를 끝까지 봐서 [until]까지 광고를 숨깁니다. */
    suspend fun setAdFreeUntil(until: Instant)
}
