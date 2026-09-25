package com.ovalit.core.data

import com.ovalit.core.model.Account
import kotlin.time.Clock
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn

/** RSO가 붙기 전까지 쓰는 가짜 계정입니다. 인트로에서 시작하면 연동된 것으로 칩니다. */
class FakeAccountRepository(
    private val matchRepository: FakeMatchRepository,
    private val clock: Clock = Clock.System,
    private val friendRepository: FakeFriendRepository? = null,
) : AccountRepository {

    private val linked = MutableStateFlow<Account?>(fakeAccount())

    override val account: Flow<Account?> = linked

    fun link() {
        linked.value = fakeAccount()
        matchRepository.refill()
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
