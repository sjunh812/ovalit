package com.ovalit.feature.match

import com.ovalit.core.data.FakeContentRepository
import com.ovalit.core.data.FakeFriendRepository
import com.ovalit.core.data.FakeMatchRepository
import com.ovalit.core.model.Match
import com.ovalit.core.model.MatchId
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.datetime.TimeZone

@OptIn(ExperimentalCoroutinesApi::class)
class MatchDetailViewModelTest {

    private val matches = FakeMatchRepository()
    private val friends = FakeFriendRepository()

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `스코어보드는 팀마다 전투 점수 순이다`() = runTest {
        val state = collect(viewModel(anyMatch()))

        assertEquals(state.myTeam.map { it.line.combatScore }.sortedDescending(), state.myTeam.map { it.line.combatScore })
        assertEquals(state.enemyTeam.map { it.line.combatScore }.sortedDescending(), state.enemyTeam.map { it.line.combatScore })
        assertEquals(1, state.myTeam.count { it.relation == PlayerRelation.ME })
    }

    @Test
    fun `친구와 앱을 쓰는 사람과 안 쓰는 사람을 가른다`() = runTest {
        val friendIds = friends.friends.first().map { it.id }.toSet()
        val match = matches.observeMatches().first().first { match ->
            match.players.any { it.player in friendIds } && friends.appUsersAmong(match.players.map { it.player }).size > 1
        }

        val rows = collect(viewModel(match)).let { it.myTeam + it.enemyTeam }
        val appUsers = friends.appUsersAmong(match.players.map { it.player })

        rows.forEach { row ->
            val expected = when {
                row.line.player == match.me -> PlayerRelation.ME
                row.line.player in friendIds -> PlayerRelation.FRIEND
                row.line.player in appUsers -> PlayerRelation.APP_USER
                else -> PlayerRelation.NOT_APP_USER
            }
            assertEquals(expected, row.relation, row.line.riotId)
        }
    }

    @Test
    fun `요청을 보내면 그 사람은 보낸 요청으로 바뀐다`() = runTest {
        val match = matches.observeMatches().first().first { match ->
            val users = friends.appUsersAmong(match.players.map { it.player })
            val friendIds = friends.friends.first().map { it.id }.toSet()
            users.any { it !in friendIds }
        }
        val viewModel = viewModel(match)
        val target = collect(viewModel).let { it.myTeam + it.enemyTeam }.first { it.relation == PlayerRelation.APP_USER }

        viewModel.sendRequest(target.line.player)

        val after = assertIs<MatchDetailUiState.Success>(viewModel.uiState.value).let { it.myTeam + it.enemyTeam }
        assertEquals(PlayerRelation.REQUEST_SENT, after.first { it.line.player == target.line.player }.relation)
    }

    @Test
    fun `저장한 경기를 지우면 화면을 닫는다`() = runTest {
        val viewModel = viewModel(anyMatch())
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect() }

        matches.deleteAll()

        assertEquals(MatchDetailUiState.Gone, viewModel.uiState.value)
    }

    @Test
    fun `라운드 목록은 스코어의 라운드 수와 같다`() = runTest {
        val match = anyMatch()
        val state = collect(viewModel(match))

        assertEquals(match.score.myTeam + match.score.enemyTeam, state.rounds.size)
        assertTrue(state.buys.isNotEmpty())
    }

    private suspend fun anyMatch(): Match = matches.observeMatches().first().first()

    private fun viewModel(match: Match) =
        MatchDetailViewModel(MatchId(match.id.value), friends, matches, FakeContentRepository(), TimeZone.of("Asia/Seoul"))

    private suspend fun TestScope.collect(viewModel: MatchDetailViewModel): MatchDetailUiState.Success {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect() }
        return assertIs(viewModel.uiState.value)
    }
}
