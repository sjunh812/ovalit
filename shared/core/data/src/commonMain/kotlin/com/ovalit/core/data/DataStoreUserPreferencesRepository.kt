package com.ovalit.core.data

import androidx.datastore.core.DataStore
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import com.ovalit.core.model.Focus
import com.ovalit.core.model.QueueFilter
import com.ovalit.core.model.ThemePreference
import com.ovalit.core.model.UserPreferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import okio.IOException
import okio.Path.Companion.toPath

/**
 * 파일이 깨졌으면 비우고 기본값으로 다시 시작합니다. 설정 몇 개를 잃는 게 앱이 켜지지 않는 것보다 낫습니다.
 *
 * @param path `.preferences_pb`로 끝나야 합니다. 다른 확장자면 DataStore가 예외를 냅니다.
 */
fun createPreferencesDataStore(path: String): DataStore<Preferences> =
    PreferenceDataStoreFactory.createWithPath(
        corruptionHandler = ReplaceFileCorruptionHandler { emptyPreferences() },
        produceFile = { path.toPath() },
    )

class DataStoreUserPreferencesRepository(
    private val dataStore: DataStore<Preferences>,
) : UserPreferencesRepository {

    // 읽다가 입출력 오류가 나면 기본값으로 연다. 그대로 던지면 테마를 읽는 MainActivity부터 앱이 죽는다.
    override val preferences: Flow<UserPreferences> = dataStore.data
        .catch { error -> if (error is IOException) emit(emptyPreferences()) else throw error }
        .map { stored ->
            val default = UserPreferences.Default
            UserPreferences(
                theme = stored[Keys.theme].toEnumOr(default.theme),
                defaultQueue = stored[Keys.defaultQueue].toEnumOr(default.defaultQueue),
                statsPublic = stored[Keys.statsPublic] ?: default.statsPublic,
                notifyAnalysisDone = stored[Keys.notifyAnalysisDone] ?: default.notifyAnalysisDone,
                notifyWeeklyReport = stored[Keys.notifyWeeklyReport] ?: default.notifyWeeklyReport,
                notifyPing = stored[Keys.notifyPing] ?: default.notifyPing,
                focus = stored[Keys.focus].toEnumOr(default.focus),
            )
        }

    override suspend fun setTheme(theme: ThemePreference) = set(Keys.theme, theme.name)

    override suspend fun setDefaultQueue(queue: QueueFilter) = set(Keys.defaultQueue, queue.name)

    override suspend fun setStatsPublic(public: Boolean) = set(Keys.statsPublic, public)

    override suspend fun setNotifyAnalysisDone(enabled: Boolean) = set(Keys.notifyAnalysisDone, enabled)

    override suspend fun setNotifyWeeklyReport(enabled: Boolean) = set(Keys.notifyWeeklyReport, enabled)

    override suspend fun setNotifyPing(enabled: Boolean) = set(Keys.notifyPing, enabled)

    override suspend fun setFocus(focus: Focus) = set(Keys.focus, focus.name)

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
        val notifyPing = booleanPreferencesKey("notify_ping")
        val focus = stringPreferencesKey("focus")
    }
}

private inline fun <reified T : Enum<T>> String?.toEnumOr(default: T): T =
    enumValues<T>().firstOrNull { it.name == this } ?: default
