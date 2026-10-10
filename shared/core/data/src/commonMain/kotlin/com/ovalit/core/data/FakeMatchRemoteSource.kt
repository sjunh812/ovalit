package com.ovalit.core.data

import com.ovalit.core.model.Match
import com.ovalit.core.model.MatchId
import com.ovalit.core.model.MatchListEntry
import com.ovalit.core.model.Queue
import com.ovalit.core.model.forFirstImport
import kotlin.time.Clock
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * 프로덕션 키가 나오기 전까지 서버 대신 가짜 경기([fakeMatches])를 줍니다.
 *
 * 처음 목록을 줄 때 그 시각으로 지난 70일 경기를 만들어 둡니다.
 * 그 뒤로는 목록을 줄 때마다 지난번 목록을 준 뒤로 [FAKE_MATCH_EVERY]마다 한 판이 끝난 것으로 쳐서 붙입니다.
 * 당기면 늘 한 판은 받고, 앱을 10분 넘게 떠났다 오면 스무 판이 넘게 쌓여 진행 줄과 WorkManager로 이어 받기를 볼 수 있습니다.
 *
 * @param importDelay 지난 경기 한 판을 주는 시간입니다. S0-4 막대가 차오르는 걸 볼 수 있게 틈을 둡니다.
 * @param downloadDelay 새로 끝난 경기 한 판을 주는 시간입니다. 서버가 경기 상세를 1분에 120번까지 받으니 실제와 비슷하게 둡니다.
 */
class FakeMatchRemoteSource(
    private val clock: Clock = Clock.System,
    private val importDelay: Duration = 70.milliseconds,
    private val downloadDelay: Duration = 500.milliseconds,
) : MatchRemoteSource {

    private val lock = Mutex()

    // 서버에 있는 내 경기다. 처음 목록을 줄 때 만들고, 그 뒤로 끝난 경기를 붙인다.
    private var history: List<Match> = emptyList()

    // 마지막으로 목록을 준 시각이다. 그 뒤로 지난 시간만큼 새 경기가 끝난 것으로 친다. `null`이면 아직 목록을 준 적이 없다.
    private var listedAt: Instant? = null

    // 경기마다 주는 데 걸리는 시간이다. 없으면 [importDelay]다.
    private val waits = mutableMapOf<MatchId, Duration>()
    private var finishedBatches = 0

    /**
     * 첫 수집을 마친 채로 시작하려고 지금 목록을 한 번 준 것으로 칩니다.
     * 첫 수집이 받았을 경기를 돌려줍니다.
     * 만들자마자 다른 곳에 넘기기 전에만 부릅니다.
     */
    internal fun startImported(): List<Match> {
        val now = clock.now()
        history = fakeMatches(now)
        listedAt = now
        return history.forFirstImport(now) { it.startedAt }
    }

    /**
     * 경기와 목록을 준 시각을 잊습니다.
     * 저장된 경기를 지운 뒤 다시 받는 첫 수집은 그때 시각으로 만든 지난 경기만 받고, 앞서 끝난 새 경기는 섞이지 않습니다.
     * 홈에 맞춰 둔 가짜 지표(움직인 칸, 짚을 점, 개선 포인트)는 그 50경기로 맞췄습니다.
     */
    suspend fun forget() {
        lock.withLock {
            history = emptyList()
            listedAt = null
            waits.clear()
        }
    }

    override suspend fun matchList(): List<MatchListEntry> {
        // 지난 경기만 주는 첫 목록은 바로 준다. 새 경기를 확인할 때만 서버를 다녀오는 시간을 둔다.
        if (lock.withLock { listedAt != null }) delay(LIST_DELAY)
        return lock.withLock {
            val now = clock.now()
            history = listedAt?.let { history + finishedSince(it, now) } ?: fakeMatches(now)
            listedAt = now
            history.map { MatchListEntry(it.id, it.startedAt, it.queue) }
        }
    }

    override suspend fun match(id: MatchId): Match? {
        val (match, wait) = lock.withLock { history.firstOrNull { it.id == id } to (waits[id] ?: importDelay) }
        delay(wait)
        return match
    }

    // 경쟁전으로 고른다. 일반전을 받으면 홈 칩이 경쟁일 때 숫자가 그대로라 새로고침한 티가 안 난다.
    private fun finishedSince(since: Instant, now: Instant): List<Match> {
        finishedBatches++
        val count = ((now - since) / FAKE_MATCH_EVERY).toInt().coerceIn(1, MAX_FAKE_NEW_MATCHES)
        val picked = fakeMatches(now, seed = SEED + finishedBatches, withFriends = false)
            .filter { it.queue == Queue.COMPETITIVE }
            .sortedByDescending { it.startedAt }
            .take(count)
        // 맨 앞 경기는 방금 끝났고, 그 앞 경기는 한 시간씩 일찍 시작했다. 한 판이 한 시간을 넘지 않아 시작 순서가 뒤집히지 않는다.
        val latestStart = now - JUST_FINISHED - picked.first().lengthMillis.milliseconds
        // 당겨서 한 판만 받을 때는 당김 표시가 오래 돌지 않게 바로 준다
        val wait = if (picked.size > 1) downloadDelay else Duration.ZERO
        return picked.mapIndexed { index, match ->
            match.copy(id = MatchId("fresh-$finishedBatches-$index"), startedAt = latestStart - FAKE_SESSION_GAP * index)
                .also { waits[it.id] = wait }
        }
    }
}

private val LIST_DELAY = 700.milliseconds
private val JUST_FINISHED = 3.minutes
private val FAKE_MATCH_EVERY = 30.seconds
private val FAKE_SESSION_GAP = 1.hours
private const val MAX_FAKE_NEW_MATCHES = 40
