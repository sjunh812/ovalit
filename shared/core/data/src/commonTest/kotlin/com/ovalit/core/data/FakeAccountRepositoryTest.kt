package com.ovalit.core.data

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Duration
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest

class FakeAccountRepositoryTest {

    // CLAUDE.md: 연동을 해제하면 저장된 경기와 리포트를 모두 지운다
    @Test
    fun `연동을 해제하면 저장한 경기도 모두 지운다`() = runTest {
        val matches = FakeMatchRepository()
        val account = FakeAccountRepository(matches)

        account.unlink()

        assertNull(account.account.first())
        assertEquals(emptyList(), matches.observeMatches().first())
    }

    // 경기는 연동하자마자가 아니라 S0-4 첫 수집에서 채운다
    @Test
    fun `다시 연동하고 첫 수집을 마치면 경기가 다시 찬다`() = runTest {
        val matches = FakeMatchRepository(importDelay = Duration.ZERO)
        val account = FakeAccountRepository(matches)
        account.unlink()

        account.link()
        assertEquals(emptyList(), matches.observeMatches().first())
        matches.importRecent()

        assertNotNull(account.account.first())
        assertTrue(matches.observeMatches().first().isNotEmpty())
        assertTrue(matches.importProgress.first()!!.isDone)
    }

    // 이전 계정 경기가 남아 있으면 새로 받은 경기와 섞인다
    @Test
    fun `연동하면 전에 받아 둔 경기를 비우고 첫 수집을 기다린다`() = runTest {
        val matches = FakeMatchRepository()
        val account = FakeAccountRepository(matches)

        account.link()

        assertEquals(emptyList(), matches.observeMatches().first())
    }

    @Test
    fun `저장된 데이터만 지우면 연동은 그대로다`() = runTest {
        val matches = FakeMatchRepository()
        val account = FakeAccountRepository(matches)

        matches.deleteAll()

        assertNotNull(account.account.first())
        assertEquals(emptyList(), matches.observeMatches().first())
    }
}
