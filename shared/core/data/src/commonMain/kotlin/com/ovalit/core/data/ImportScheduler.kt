package com.ovalit.core.data

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/**
 * 첫 수집을 시작합니다. 안드로이드는 앱을 닫아도 이어 받도록 WorkManager에 맡기고, 다 받으면 알림을
 * 보냅니다. 주기적으로 다시 받는 동기화는 두지 않습니다.
 */
fun interface ImportScheduler {
    fun start()
}

/** WorkManager가 없는 곳(iOS, 테스트)에서 쓰는 구현입니다. 앱이 떠 있는 동안만 받습니다. */
class CoroutineImportScheduler(
    private val scope: CoroutineScope,
    private val matchRepository: MatchRepository,
) : ImportScheduler {
    private var running: Job? = null

    override fun start() {
        if (running?.isActive == true) return
        running = scope.launch { matchRepository.importRecent() }
    }
}
