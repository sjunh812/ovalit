package com.ovalit.core.data

import com.ovalit.core.model.NEW_MATCHES_IN_BACKGROUND_FROM
import kotlin.coroutines.cancellation.CancellationException
import kotlin.time.Clock
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Instant
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.runningFold
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * 앱 화면이 다시 보이면 새로 끝난 경기를 저절로 받고, 받던 중에 화면이 가려지면 남은 경기를 [ImportScheduler]에 넘깁니다.
 *
 * 마지막으로 확인한 지 [RECHECK_AFTER]가 지나지 않았으면 다시 확인하지 않습니다. 다른 앱을 잠깐 오갈 때마다 경기 ID 목록을
 * 받으면 앱 전체의 Riot 몫을 씁니다. 첫 수집을 막 마친 것도 확인한 것으로 칩니다.
 *
 * @param scope 앱이 사는 동안 도는 곳입니다. 화면이 사라져도 받기가 이어집니다.
 */
class NewMatchesWatcher(
    private val matches: MatchRepository,
    private val account: AccountRepository,
    private val scheduler: ImportScheduler,
    private val scope: CoroutineScope,
    private val clock: Clock = Clock.System,
    private val analytics: Analytics = NoAnalytics,
) {
    private val lock = Mutex()
    private var checkedAt: Instant? = null

    init {
        // 받는 중이던 첫 수집이 끝났을 때만 확인한 것으로 친다. 앱을 켤 때 이미 끝나 있던 첫 수집은 저장해 둔 경기뿐이라 켜자마자 확인한다.
        scope.launch {
            matches.importProgress
                .map { it?.isDone }
                .distinctUntilChanged()
                .runningFold(Pair<Boolean?, Boolean?>(null, null)) { (_, previous), done -> previous to done }
                .filter { (previous, done) -> previous == false && done == true }
                .collect { lock.withLock { checkedAt = clock.now() } }
        }
    }

    /** 앱 화면이 하나라도 보이기 시작할 때 부릅니다. */
    fun onAppVisible() {
        scope.launch {
            // RSO 세션 없이 전적을 요청하지 않는다(CLAUDE.md 지켜야 할 선). 첫 수집 전이나 받는 중이면 S0-4가 받는다.
            if (account.account.first() == null) return@launch
            if (matches.importProgress.first()?.isDone != true) return@launch
            val stale = lock.withLock {
                val now = clock.now()
                val last = checkedAt
                (last == null || now - last >= RECHECK_AFTER).also { if (it) checkedAt = now }
            }
            if (!stale) return@launch
            try {
                logRefresh(analytics, source = "app_open") { matches.refresh() }
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                // 받지 못하면 다음에 앱이 보일 때 다시 확인한다. 저장해 둔 경기는 그대로다.
                lock.withLock { checkedAt = null }
            }
        }
    }

    /** 앱 화면이 모두 가려질 때 부릅니다. 받을 경기가 많이 남았으면 앱을 닫아도 이어 받게 맡깁니다. */
    fun onAppHidden() {
        scope.launch {
            val progress = matches.newMatchesProgress.first() ?: return@launch
            if (progress.remaining >= NEW_MATCHES_IN_BACKGROUND_FROM) scheduler.continueNewMatches(progress.total)
        }
    }

    companion object {
        /** 다시 확인하기까지 기다리는 시간입니다. 시작 기준선입니다. */
        val RECHECK_AFTER = 10.minutes
    }
}
