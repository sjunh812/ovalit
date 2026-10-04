package com.ovalit.core.data

import com.ovalit.core.model.Focus
import com.ovalit.core.model.QueueFilter
import com.ovalit.core.model.ThemePreference
import com.ovalit.core.model.UserPreferences
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Instant
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import okio.FileSystem
import okio.Path

class DataStoreUserPreferencesRepositoryTest {

    private fun repository(file: Path = newFile()) =
        DataStoreUserPreferencesRepository(createPreferencesDataStore(file.toString()))

    private fun newFile() = FileSystem.SYSTEM_TEMPORARY_DIRECTORY / "prefs-${Random.nextLong()}.preferences_pb"

    @Test
    fun `저장한 적이 없으면 기본값이다`() = runTest {
        assertEquals(UserPreferences.Default, repository().preferences.first())
    }

    // CLAUDE.md: 전적 공개 토글은 기본으로 켜져 있다
    @Test
    fun `전적 공개는 기본으로 켜져 있다`() = runTest {
        assertEquals(true, repository().preferences.first().statsPublic)
    }

    // 깨진 파일을 읽다가 예외가 나면 테마를 읽는 MainActivity부터 앱이 켜지자마자 죽는다.
    @Test
    fun `저장 파일이 깨졌으면 기본값으로 다시 시작한다`() = runTest {
        val file = newFile()
        FileSystem.SYSTEM.write(file) { writeUtf8("깨진 파일") }
        val repository = repository(file)

        assertEquals(UserPreferences.Default, repository.preferences.first())
        repository.setTheme(ThemePreference.DARK)
        assertEquals(ThemePreference.DARK, repository.preferences.first().theme)
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
        repository.setSeenProfileHint()
        repository.setAdFreeUntil(Instant.parse("2026-10-05T08:20:00Z"))

        assertEquals(
            UserPreferences(
                theme = ThemePreference.DARK,
                defaultQueue = QueueFilter.COMPETITIVE,
                statsPublic = false,
                notifyAnalysisDone = false,
                notifyWeeklyReport = false,
                focus = Focus.ROUND_PLAY,
                seenProfileHint = true,
                adFreeUntil = Instant.parse("2026-10-05T08:20:00Z"),
            ),
            repository.preferences.first(),
        )
    }
}
