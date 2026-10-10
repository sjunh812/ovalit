package com.ovalit.feature.match

import com.ovalit.core.data.FakeContentRepository
import com.ovalit.core.data.FakeFriendRepository
import com.ovalit.core.data.FakeMatchRepository
import com.ovalit.core.data.FriendRepository
import com.ovalit.core.model.Match
import com.ovalit.core.model.MatchId
import com.ovalit.core.model.PlayerId
import com.ovalit.core.model.Queue
import com.ovalit.core.testing.SameThread
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
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
        val (myTeam, enemyTeam) = state.groups

        assertEquals(listOf(ScoreboardSide.MY_TEAM, ScoreboardSide.ENEMY_TEAM), state.groups.map { it.side })
        assertEquals(myTeam.rows.map { it.line.combatScore }.sortedDescending(), myTeam.rows.map { it.line.combatScore })
        assertEquals(enemyTeam.rows.map { it.line.combatScore }.sortedDescending(), enemyTeam.rows.map { it.line.combatScore })
        assertEquals(1, myTeam.rows.count { it.relation == PlayerRelation.ME })
    }

    // 데스매치는 팀이 없어 우리 팀과 상대 팀으로 나누면 열세 명이 모두 "상대 팀"이 된다
    @Test
    fun `데스매치 스코어보드는 모두를 등수 순으로 한 묶음에 둔다`() {
        val groups = MatchPreviewData.deathmatch.scoreboardGroups { PlayerRelation.NOT_APP_USER }
        val ranks = groups.single().rows.map { assertNotNull(it.placement).rank }

        assertEquals(ScoreboardSide.EVERYONE, groups.single().side)
        assertEquals(14, ranks.size)
        assertEquals(ranks.sorted(), ranks)
        assertEquals(3, groups.single().rows.indexOfFirst { it.line.player == MatchPreviewData.deathmatch.me } + 1)
    }

    @Test
    fun `건틀릿 스코어보드는 두 명씩 팀마다 등수 순으로 묶는다`() {
        val groups = MatchPreviewData.gauntlet.scoreboardGroups { PlayerRelation.NOT_APP_USER }

        assertEquals((1..8).toList(), groups.map { it.rank })
        assertTrue(groups.all { it.rows.size == 2 })
        assertEquals(listOf(2), groups.filter { it.side == ScoreboardSide.MY_TEAM }.map { it.rank })
        assertTrue(groups.flatMap { it.rows }.all { it.placement == null && it.stats == null })
    }

    @Test
    fun `친구와 앱을 쓰는 사람과 안 쓰는 사람을 가른다`() = runTest {
        val friendIds = friends.friends.first().map { it.id }.toSet()
        val match = matches.observeMatches().first().first { match ->
            match.players.any { it.player in friendIds } && friends.appUsersAmong(match.players.map { it.player }).size > 1
        }

        val rows = collect(viewModel(match)).rows
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
        val target = collect(viewModel).rows.first { it.relation == PlayerRelation.APP_USER }

        viewModel.sendRequest(target.line.player)

        val after = assertIs<MatchDetailUiState.Success>(viewModel.uiState.value).rows
        assertEquals(PlayerRelation.REQUEST_SENT, after.first { it.line.player == target.line.player }.relation)
    }

    // 앱 모듈이 이때 알림을 켜 달라고 묻는다
    @Test
    fun `스코어보드에서 친구 요청을 수락하면 다 끝난 뒤에 알린다`() = runTest {
        val viewModel = viewModel(anyMatch())
        val request = friends.requests.first().first()
        var accepted = 0

        viewModel.accept(request.id, onAccepted = { accepted++ })

        assertEquals(1, accepted)
        assertTrue(friends.friends.first().any { it.id == request.id })
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

    // 스코어보드는 기기에 있는 경기라 서버에 묻지 못해도 그린다
    @Test
    fun `앱을 쓰는지 묻지 못해도 경기를 보여주고 앱을 안 쓴다고 하지 않는다`() = runTest {
        val broken = object : FriendRepository by friends {
            override suspend fun appUsersAmong(players: Collection<PlayerId>): Set<PlayerId> = error("서버에 닿지 않음")
        }
        val viewModel = MatchDetailViewModel(MatchId(anyMatch().id.value), broken, matches, FakeContentRepository(), TimeZone.of("Asia/Seoul"), computation = SameThread)

        val state = collect(viewModel)

        val relations = state.rows.map { it.relation }
        assertTrue(PlayerRelation.UNKNOWN in relations)
        assertTrue(relations.none { it == PlayerRelation.APP_USER || it == PlayerRelation.NOT_APP_USER })
    }

    // 가짜 경기에는 데스매치처럼 라운드가 없는 모드도 섞여 있어 경쟁전을 고른다
    private suspend fun anyMatch(): Match = matches.observeMatches().first().first { it.queue == Queue.COMPETITIVE }

    private fun viewModel(match: Match) =
        MatchDetailViewModel(MatchId(match.id.value), friends, matches, FakeContentRepository(), TimeZone.of("Asia/Seoul"), computation = SameThread)

    private suspend fun TestScope.collect(viewModel: MatchDetailViewModel): MatchDetailUiState.Success {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect() }
        return assertIs(viewModel.uiState.value)
    }
}
