package com.ovalit.core.data

import com.ovalit.core.model.QueueFilter
import com.ovalit.core.model.ThemePreference
import com.ovalit.core.model.UserPreferences
import kotlinx.coroutines.flow.Flow

interface UserPreferencesRepository {

    val preferences: Flow<UserPreferences>

    suspend fun setTheme(theme: ThemePreference)

    suspend fun setDefaultQueue(queue: QueueFilter)

    suspend fun setStatsPublic(public: Boolean)

    suspend fun setNotifyAnalysisDone(enabled: Boolean)

    suspend fun setNotifyWeeklyReport(enabled: Boolean)
}
