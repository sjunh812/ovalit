package com.ovalit.feature.profile

import com.ovalit.core.data.FakeAccountRepository
import com.ovalit.core.data.FakeContentRepository
import com.ovalit.core.data.FakeMatchRepository
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.time.Clock
import kotlinx.datetime.TimeZone

@OptIn(ExperimentalCoroutinesApi::class)
class ProfileViewModelTest {

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `요원과 무기 집계에 콘텐츠 카탈로그 이름표를 붙여 내놓는다`() = runTest {
        val matches = FakeMatchRepository()
        val viewModel = ProfileViewModel(
            FakeAccountRepository(matches),
            matches,
            FakeContentRepository(),
            Clock.System,
            TimeZone.of("Asia/Seoul"),
        )
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect() }

        val state = assertIs<ProfileUiState.Success>(viewModel.uiState.value)
        assertTrue(state.agents.matches > 0)
        assertEquals(state.agents.matches, state.weapons.matches)
        assertTrue(state.weapons.weapons.all { it.weapon in state.catalog.weapons })
        assertTrue(state.agents.agents.all { it.agent in state.catalog.agents })
    }
}
