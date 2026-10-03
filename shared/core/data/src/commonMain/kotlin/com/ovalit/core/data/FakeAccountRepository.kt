package com.ovalit.core.data

import com.ovalit.core.model.Account
import kotlin.time.Clock
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn

/** RSO가 붙기 전까지 쓰는 가짜 계정입니다. 처음부터 연동된 채로 시작하고, S0-2에서 계속하면 경기를 비우고 다시 연동합니다. */
class FakeAccountRepository(
    private val matchRepository: FakeMatchRepository,
    private val clock: Clock = Clock.System,
    private val friendRepository: FakeFriendRepository? = null,
    private val pingRepository: FakePingRepository? = null,
) : AccountRepository {

    private val linked = MutableStateFlow<Account?>(fakeAccount())

    override val account: Flow<Account?> = linked

    /** 경기는 비워 두고 첫 수집([MatchRepository.importRecent])이 채웁니다. */
    suspend fun link() {
        linked.value = fakeAccount()
        matchRepository.deleteAll()
        friendRepository?.refill()
        pingRepository?.refill()
    }

    override suspend fun unlink() {
        linked.value = null
        matchRepository.deleteAll()
        friendRepository?.clear()
        pingRepository?.clear()
    }

    private fun fakeAccount() = Account(
        id = Me,
        riotId = MY_RIOT_ID,
        linkedOn = clock.todayIn(TimeZone.currentSystemDefault()),
    )
}
