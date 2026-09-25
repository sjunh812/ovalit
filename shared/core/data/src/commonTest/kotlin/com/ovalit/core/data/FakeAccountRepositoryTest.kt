package com.ovalit.core.data

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
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

    @Test
    fun `다시 연동하면 경기를 다시 채운다`() = runTest {
        val matches = FakeMatchRepository()
        val account = FakeAccountRepository(matches)
        account.unlink()

        account.link()

        assertNotNull(account.account.first())
        assertTrue(matches.observeMatches().first().isNotEmpty())
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
