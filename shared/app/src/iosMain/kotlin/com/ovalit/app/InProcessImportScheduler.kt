package com.ovalit.app

import com.ovalit.core.data.AccountRepository
import com.ovalit.core.data.ImportScheduler
import com.ovalit.core.data.MatchRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * 앱이 떠 있는 동안 첫 수집을 앱 안에서 받습니다. iOS에는 WorkManager가 없고 앱을 닫은 뒤 이어 받기와 끝났다는 알림은 범위 밖이라,
 * 새 경기를 넘겨받는 일([continueNewMatches])은 하지 않습니다. 앱이 살아 있으면 받던 것이 끝까지 갑니다.
 */
internal class InProcessImportScheduler(
    private val scope: CoroutineScope,
    private val account: AccountRepository,
    private val matches: MatchRepository,
) : ImportScheduler {
    private var job: Job? = null

    override fun start() {
        if (job?.isActive != true) job = launchImport()
    }

    override fun retry() {
        job?.cancel()
        job = launchImport()
    }

    override fun continueNewMatches(total: Int) {}

    override fun cancel() {
        job?.cancel()
        job = null
    }

    // 연동을 해제한 뒤에는 전적을 요청하지 않는다(CLAUDE.md 지켜야 할 선)
    private fun launchImport(): Job = scope.launch {
        if (account.account.first() == null) return@launch
        matches.importRecent()
    }
}
