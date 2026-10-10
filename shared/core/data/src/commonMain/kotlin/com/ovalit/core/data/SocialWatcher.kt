package com.ovalit.core.data

import kotlin.coroutines.cancellation.CancellationException
import kotlin.time.Clock
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Instant
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * 앱 화면이 다시 보이면 ㅇㅂㅇ과 친구를 다시 받습니다. 푸시가 늦거나 오지 않으면 다시 열어도 옛 초대와 친구 목록이 남고, 상대가
 * 내 친구 요청을 수락해도 친구 탭을 당기기 전까지 홈 친구 비교에 나오지 않습니다.
 *
 * 우리 서버만 불러 Riot 몫을 쓰지 않지만, 다른 앱을 잠깐 오갈 때마다 받지 않게 [RECHECK_AFTER] 안에서는 한 번만 받습니다.
 * 저절로 한 일이라 받지 못해도 알리지 않고 다음에 다시 받습니다(CLAUDE.md 실패 안내).
 */
class SocialWatcher(
    private val pings: PingRepository,
    private val friends: FriendRepository,
    private val account: AccountRepository,
    private val scope: CoroutineScope,
    private val clock: Clock = Clock.System,
) {
    private val lock = Mutex()
    private var checkedAt: Instant? = null

    /** 앱 화면이 하나라도 보이기 시작할 때 부릅니다. */
    fun onAppVisible() {
        scope.launch {
            if (account.account.first() == null) return@launch
            val due = lock.withLock {
                val now = clock.now()
                val last = checkedAt
                (last == null || now - last >= RECHECK_AFTER).also { if (it) checkedAt = now }
            }
            if (!due) return@launch
            try {
                launch { pings.refresh() }
                friends.refresh()
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                lock.withLock { checkedAt = null }
            }
        }
    }

    companion object {
        /** 다시 받기까지 기다리는 시간입니다. 시작 기준선입니다. */
        val RECHECK_AFTER = 1.minutes
    }
}
