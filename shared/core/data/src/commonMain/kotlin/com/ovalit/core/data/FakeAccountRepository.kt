package com.ovalit.core.data

import com.ovalit.core.model.Account
import kotlin.time.Clock
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn

/** RSO가 붙기 전까지 쓰는 가짜 계정입니다. S0-2에서 계속하면 연동된 것으로 칩니다. */
class FakeAccountRepository(
    private val matchRepository: FakeMatchRepository,
    private val clock: Clock = Clock.System,
    private val friendRepository: FakeFriendRepository? = null,
) : AccountRepository {

    private val linked = MutableStateFlow<Account?>(fakeAccount())

    override val account: Flow<Account?> = linked

    /** 경기는 비워 두고 첫 수집([MatchRepository.importRecent])이 채웁니다. */
    suspend fun link() {
        linked.value = fakeAccount()
        matchRepository.deleteAll()
        friendRepository?.refill()
    }

    override suspend fun unlink() {
        linked.value = null
        matchRepository.deleteAll()
        friendRepository?.clear()
    }

    private fun fakeAccount() = Account(
        riotId = "오발러#KR1",
        linkedOn = clock.todayIn(TimeZone.currentSystemDefault()),
    )
}
