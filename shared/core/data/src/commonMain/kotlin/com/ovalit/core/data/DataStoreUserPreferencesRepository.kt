package com.ovalit.core.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.ovalit.core.model.QueueFilter
import com.ovalit.core.model.ThemePreference
import com.ovalit.core.model.UserPreferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import okio.Path.Companion.toPath

/** @param path `.preferences_pb`로 끝나야 합니다. 다른 확장자면 DataStore가 예외를 냅니다. */
fun createPreferencesDataStore(path: String): DataStore<Preferences> =
    PreferenceDataStoreFactory.createWithPath(produceFile = { path.toPath() })

class DataStoreUserPreferencesRepository(
    private val dataStore: DataStore<Preferences>,
) : UserPreferencesRepository {

    override val preferences: Flow<UserPreferences> = dataStore.data.map { stored ->
        val default = UserPreferences.Default
        UserPreferences(
            theme = stored[Keys.theme].toEnumOr(default.theme),
            defaultQueue = stored[Keys.defaultQueue].toEnumOr(default.defaultQueue),
            statsPublic = stored[Keys.statsPublic] ?: default.statsPublic,
            notifyAnalysisDone = stored[Keys.notifyAnalysisDone] ?: default.notifyAnalysisDone,
            notifyWeeklyReport = stored[Keys.notifyWeeklyReport] ?: default.notifyWeeklyReport,
        )
    }

    override suspend fun setTheme(theme: ThemePreference) = set(Keys.theme, theme.name)

    override suspend fun setDefaultQueue(queue: QueueFilter) = set(Keys.defaultQueue, queue.name)

    override suspend fun setStatsPublic(public: Boolean) = set(Keys.statsPublic, public)

    override suspend fun setNotifyAnalysisDone(enabled: Boolean) = set(Keys.notifyAnalysisDone, enabled)

    override suspend fun setNotifyWeeklyReport(enabled: Boolean) = set(Keys.notifyWeeklyReport, enabled)

    private suspend fun <T> set(key: Preferences.Key<T>, value: T) {
        dataStore.edit { it[key] = value }
    }

    // enum을 이름으로 저장한다. 이름을 바꾸면 전에 저장한 값을 못 읽고 기본값으로 돌아간다.
    private object Keys {
        val theme = stringPreferencesKey("theme")
        val defaultQueue = stringPreferencesKey("default_queue")
        val statsPublic = booleanPreferencesKey("stats_public")
        val notifyAnalysisDone = booleanPreferencesKey("notify_analysis_done")
        val notifyWeeklyReport = booleanPreferencesKey("notify_weekly_report")
    }
}

private inline fun <reified T : Enum<T>> String?.toEnumOr(default: T): T =
    enumValues<T>().firstOrNull { it.name == this } ?: default
