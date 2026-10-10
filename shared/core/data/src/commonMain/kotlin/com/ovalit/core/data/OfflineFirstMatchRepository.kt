package com.ovalit.core.data

import com.ovalit.core.model.FIRST_IMPORT_WEEKS
import com.ovalit.core.model.ImportProgress
import com.ovalit.core.model.Match
import com.ovalit.core.model.MatchId
import com.ovalit.core.model.NewMatchesProgress
import com.ovalit.core.model.OvalitException
import com.ovalit.core.model.forFirstImport
import com.ovalit.core.model.ovalitError
import kotlin.coroutines.cancellation.CancellationException
import kotlin.time.Clock
import kotlin.time.Duration.Companion.days
import kotlin.time.Instant
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * 내 경기를 서버에서 받아 기기에 저장합니다. CLAUDE.md "경기 받기"에서 저장소가 지킬 규칙은 모두 여기 있습니다. 서버
 * ([MatchRemoteSource])와 기기 저장([MatchStore])은 인터페이스로만 불러서, 가짜 서버와 메모리를 붙이든 실제 서버와 로컬 DB를
 * 붙이든 규칙은 그대로입니다.
 *
 * - 첫 수집은 최근 50경기를 받되 8주를 넘는 경기와 커스텀 게임은 받지 않습니다. 둘이 함께 부르면 뒤에 온 쪽은 앞의 것이 끝날 때까지
 *   기다리고, 이미 끝났으면 다시 받지 않습니다.
 * - 새 경기는 첫 수집을 마친 뒤에만 받습니다. 저장한 경기와 첫 수집이 잘라낸 경기는 다시 받지 않습니다. 두 화면이 같이 당겨도 한
 *   번만 받고, 부른 화면이 사라져도 [scope]에서 끝까지 받습니다. 멈추는 건 [deleteAll]뿐입니다.
 * - 끝난 경기는 바뀌지 않아 한 판씩 받는 대로 저장합니다. 받다 실패하면 받은 경기는 그대로 두고 까닭을 적은 뒤
 *   [OvalitException]으로 던집니다. 남은 것은 다음에 이어 받습니다.
 *
 * @param scope 새 경기를 받는 곳입니다. 앱이 사는 동안 도는 스코프를 넘깁니다.
 */
class OfflineFirstMatchRepository(
    private val remote: MatchRemoteSource,
    private val store: MatchStore,
    private val clock: Clock,
    private val scope: CoroutineScope,
) : MatchRepository {

    override fun observeMatches(): Flow<List<Match>> = store.matches

    override val importProgress: Flow<ImportProgress?> = store.importProgress

    override val checkedAt: Flow<Instant?> = store.checkedAt

    // 받다 멈춘 경기는 받는 중이 아니다. 진행 줄이 남으면 홈이 새 경기를 기다리며 숫자를 내보내지 않고 당겨도 무시한다.
    override val newMatchesProgress: Flow<NewMatchesProgress?> = store.newMatches
        .map { batch -> batch?.takeUnless { it.stopped }?.let { NewMatchesProgress(total = it.total, received = it.total - it.left.size) } }
        .distinctUntilChanged()

    // 저장소에 쓰는 일은 모두 이 잠금 안에서 한다. 지우기와 저장이 엇갈리면 지운 뒤에 받던 경기가 들어온다.
    private val lock = Mutex()

    // 같은 첫 수집이 둘이 함께 돌지 않게 막는다. 다 받을 때까지 쥐고 있어서 [lock]과 따로 둔다.
    private val importing = Mutex()

    // 지울 때마다 늘린다. 받던 쪽은 저장하기 전에 이 값을 보고, 그사이 지웠으면 멈춘다. 첫 수집은 부른 쪽(WorkManager)에서
    // 돌아서 지우기가 멈출 수 없다.
    private var generation = 0

    private var inFlight: Deferred<Result<Int>>? = null

    init {
        scope.launch { resumeInterrupted() }
    }

    override suspend fun importRecent() {
        importing.withLock {
            val started = lock.withLock {
                val progress = store.importProgress.first()
                if (progress?.isDone == true) return
                // 다시 받기 시작하면 멈춘 까닭을 지운다. 목록부터 받지 못했으면 몇 판을 받을지 모르니 진행도를 비운다.
                store.setImportProgress(progress?.takeIf { it.total > 0 }?.copy(stoppedBy = null))
                generation
            }
            try {
                for (id in pickForImport(started) ?: return) {
                    val match = remote.match(id)
                    lock.withLock {
                        if (generation != started) return
                        val progress = store.importProgress.first() ?: return
                        // 상세를 보고서야 커스텀 게임인 걸 알았으면 받을 판 수에서 뺀다. 그대로 두면 막대가 끝까지 차지 않는다.
                        if (match == null) {
                            store.setImportProgress(progress.copy(total = progress.total - 1))
                        } else {
                            store.save(match)
                            store.setImportProgress(progress.copy(results = progress.results + match.myTeamWon))
                        }
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                lock.withLock {
                    if (generation != started) return
                    val progress = store.importProgress.first() ?: ImportProgress(total = 0, results = emptyList())
                    store.setImportProgress(progress.copy(stoppedBy = e.ovalitError))
                }
                throw e.asOvalitException()
            }
        }
    }

    // 최근 50경기를 고르고 진행도를 새로 적는다. 중간에 끊겼다 다시 돌면 이미 받은 경기는 건너뛴다. 그사이 지웠으면 `null`이다.
    private suspend fun pickForImport(started: Int): List<MatchId>? {
        val listed = remote.matchList()
        val now = clock.now()
        val picked = listed.filter { it.queue != null }.forFirstImport(now) { it.startedAt }
        return lock.withLock {
            if (generation != started) return null
            val saved = store.matches.first().associateBy { it.id }
            store.setCheckedAt(now)
            store.setImportProgress(ImportProgress(total = picked.size, results = picked.mapNotNull { saved[it.id] }.map { it.myTeamWon }))
            picked.map { it.id }.filterNot { it in saved }
        }
    }

    override suspend fun refresh(): Int {
        // 홈과 경기 탭이 같이 당기면 한 번만 받는다. 뒤에 온 쪽은 앞의 것이 끝날 때까지 기다리고 새로 받지 않는다.
        val (receiving, joined) = lock.withLock {
            // 지운 뒤에는 진행도가 비어 다시 첫 수집을 기다린다
            if (store.importProgress.first()?.isDone != true) return 0
            inFlight?.takeIf { it.isActive }?.let { it to true } ?: (receive() to false)
        }
        val received = receiving.await().getOrThrow()
        return if (joined) 0 else received
    }

    // [lock] 안에서 부른다. 실패는 던지지 않고 결과에 담는다. 기다리는 쪽이 없어도 실패가 [scope]로 번지지 않고, 같이 기다리던
    // 쪽은 모두 같은 실패를 받는다.
    private fun receive(): Deferred<Result<Int>> {
        val started = generation
        return scope.async {
            try {
                Result.success(receiveNew(started))
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Result.failure(e.asOvalitException())
            }
        }.also { inFlight = it }
    }

    // 최신 경기부터 한 판씩 받아 바로 저장한다
    private suspend fun receiveNew(started: Int): Int {
        var batch = leftovers(started) ?: listNew(started) ?: return 0
        var received = 0
        try {
            while (batch.left.isNotEmpty()) {
                val match = remote.match(batch.left.first())
                batch = batch.copy(left = batch.left.drop(1))
                lock.withLock {
                    if (generation != started) return received
                    if (match != null) {
                        store.save(match)
                        received++
                    }
                    store.setNewMatches(batch)
                }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            // 받은 경기는 그대로 두고 남은 것만 다음에 받는다. Riot 레이트 리밋은 앱 전체에 걸려서 받은 것을 다시 받지 않는다.
            lock.withLock { if (generation == started) store.setNewMatches(batch.copy(stopped = true)) }
            throw e
        }
        lock.withLock { if (generation == started) store.setNewMatches(null) }
        return received
    }

    // 받다 멈췄거나 받던 중에 앱이 꺼져 남은 경기다. 남았으면 목록을 다시 받지 않고 그것부터 받는다. 저장하고 남은 목록에서
    // 빼기 전에 꺼졌으면 저장한 경기가 남아 있어서 뺀다.
    private suspend fun leftovers(started: Int): NewMatchesBatch? = lock.withLock {
        if (generation != started) return null
        val batch = store.newMatches.first() ?: return null
        val saved = store.matches.first().mapTo(HashSet()) { it.id }
        batch.copy(left = batch.left.filterNot { it in saved }, stopped = false).also { store.setNewMatches(it) }
    }

    // 경기 ID 목록을 받아 저장한 것과 견주고 새로 끝난 경기만 고른다. 받을 게 없으면 `null`이다.
    private suspend fun listNew(started: Int): NewMatchesBatch? {
        val listed = remote.matchList()
        val now = clock.now()
        return lock.withLock {
            if (generation != started) return null
            val stored = store.matches.first()
            val saved = stored.mapTo(HashSet()) { it.id }
            // 첫 수집이 50경기에서 잘라낸 경기는 새 경기가 아니다. 저장한 가장 오래된 경기보다 앞선 경기는 받지 않는다.
            val oldest = stored.minOfOrNull { it.startedAt }
            val fresh = listed
                .filter { it.queue != null && it.id !in saved }
                .filter { now - it.startedAt <= IMPORT_WINDOW && (oldest == null || it.startedAt > oldest) }
                .sortedByDescending { it.startedAt }
                .map { it.id }
            store.setCheckedAt(now)
            NewMatchesBatch(total = fresh.size, left = fresh).takeIf { fresh.isNotEmpty() }?.also { store.setNewMatches(it) }
        }
    }

    // 받던 중에 앱이 꺼졌으면 다시 켜자마자 남은 것을 이어 받는다. 부른 화면이 사라져도 받기는 끝까지 간다(CLAUDE.md 경기 받기).
    // 실패해 멈춘 것은 다음에 refresh를 부를 때 받는다.
    private suspend fun resumeInterrupted() {
        lock.withLock {
            val batch = store.newMatches.first() ?: return
            if (batch.stopped || inFlight?.isActive == true || store.importProgress.first()?.isDone != true) return
            receive()
        }
    }

    // 진행도도 같이 비운다. 남겨 두면 다시 연동했을 때 S0-4가 새 수집 전에 지난 수집의 "리포트 보기"를 띄운다. 받던 새 경기는
    // 멈출 때까지 기다린 뒤에 지운다. 기다리지 않으면 막 받은 한 판이 지운 뒤에 들어온다.
    override suspend fun deleteAll() {
        lock.withLock {
            inFlight?.cancelAndJoin()
            inFlight = null
            generation++
            store.clear()
        }
    }
}

private val IMPORT_WINDOW = (FIRST_IMPORT_WEEKS * 7).days

private fun Exception.asOvalitException(): OvalitException = this as? OvalitException ?: OvalitException(ovalitError, this)
