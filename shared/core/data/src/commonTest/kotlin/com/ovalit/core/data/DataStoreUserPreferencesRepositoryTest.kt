package com.ovalit.core.data

import com.ovalit.core.model.Focus
import com.ovalit.core.model.QueueFilter
import com.ovalit.core.model.ThemePreference
import com.ovalit.core.model.UserPreferences
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import okio.FileSystem

class DataStoreUserPreferencesRepositoryTest {

    private fun repository(): DataStoreUserPreferencesRepository {
        val file = FileSystem.SYSTEM_TEMPORARY_DIRECTORY / "prefs-${Random.nextLong()}.preferences_pb"
        return DataStoreUserPreferencesRepository(createPreferencesDataStore(file.toString()))
    }

    @Test
    fun `저장한 적이 없으면 기본값이다`() = runTest {
        assertEquals(UserPreferences.Default, repository().preferences.first())
    }

    // CLAUDE.md: 전적 공개 토글은 기본으로 켜져 있다
    @Test
    fun `전적 공개는 기본으로 켜져 있다`() = runTest {
        assertEquals(true, repository().preferences.first().statsPublic)
    }

    @Test
    fun `바꾼 값을 다시 읽으면 그대로 나온다`() = runTest {
        val repository = repository()

        repository.setTheme(ThemePreference.DARK)
        repository.setDefaultQueue(QueueFilter.COMPETITIVE)
        repository.setStatsPublic(false)
        repository.setNotifyAnalysisDone(false)
        repository.setNotifyWeeklyReport(false)
        repository.setFocus(Focus.ROUND_PLAY)

        assertEquals(
            UserPreferences(
                theme = ThemePreference.DARK,
                defaultQueue = QueueFilter.COMPETITIVE,
                statsPublic = false,
                notifyAnalysisDone = false,
                notifyWeeklyReport = false,
                focus = Focus.ROUND_PLAY,
            ),
            repository.preferences.first(),
        )
    }
}
