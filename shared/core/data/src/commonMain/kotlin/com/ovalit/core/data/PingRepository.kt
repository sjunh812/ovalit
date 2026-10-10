package com.ovalit.core.data

import com.ovalit.core.model.Ping
import com.ovalit.core.model.PingAnswer
import com.ovalit.core.model.PingId
import com.ovalit.core.model.PlayerId
import kotlin.time.Clock
import kotlin.time.Instant
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.transformLatest

/**
 * 친구에게 보내는 ㅇㅂㅇ(오발있?)입니다. 서버의 `/pings`를 그대로 옮깁니다. 받은 친구에게는 서버가 FCM으로 알리고, 앱은 알림을
 * 받거나 화면을 열 때 [refresh]로 다시 받습니다.
 */
interface PingRepository {

    /**
     * 아직 끝나지 않은 ㅇㅂㅇ입니다. 내가 보낸 것과 받은 것이 섞여 있고 최근 것이 앞에 옵니다. 취소한 것은 빠집니다. 끝날 시각이
     * 지나면 목록이 바뀌지 않아도 빼서 다시 내보냅니다([whileActive]).
     */
    val pings: Flow<List<Ping>>

    /** 서로 수락한 친구 [friends](1~4명)에게 [startsAt]에 하자고 보냅니다. */
    suspend fun send(friends: List<PlayerId>, startsAt: Instant): PingSendResult

    /** 받은 ㅇㅂㅇ에 답합니다. [PingAnswer.OTHER_TIME]이면 [proposedAt]을 같이 넘깁니다. */
    suspend fun reply(id: PingId, answer: PingAnswer, proposedAt: Instant? = null)

    /** 보낸 ㅇㅂㅇ의 시각을 옮깁니다. 그 시각을 낸 친구 말고는 모두에게 다시 묻습니다. */
    suspend fun moveTo(id: PingId, startsAt: Instant)

    /**
     * 보낸 ㅇㅂㅇ에 친구를 더 부릅니다. 누가 못 간다고 했거나 깜빡 빠뜨린 친구를 더할 때 씁니다. 못 간다고 한 친구를 뺀 인원이
     * [com.ovalit.core.model.MAX_PING_FRIENDS]를 넘으면 부르지 않습니다.
     */
    suspend fun invite(id: PingId, friends: List<PlayerId>): PingInviteResult

    suspend fun cancel(id: PingId)

    suspend fun refresh()
}

/**
 * 끝날 시각([Ping.expiresAt])이 지난 ㅇㅂㅇ을 빼고 내보냅니다. 남은 것 가운데 가장 먼저 끝나는 시각에 다시 걸러 내보내서, 목록이
 * 바뀌지 않아도 끝난 초대가 홈과 친구 탭에 남지 않습니다. 받아 온 목록만 거르면 앱을 켜 둔 채 한 시간이 지나도 지난 초대가 남는다.
 */
@OptIn(ExperimentalCoroutinesApi::class)
fun Flow<List<Ping>>.whileActive(clock: Clock): Flow<List<Ping>> = transformLatest { pings ->
    while (true) {
        val now = clock.now()
        val active = pings.filter { it.isActive(now) }
        emit(active)
        val next = active.minOfOrNull { it.expiresAt } ?: break
        delay(next - now)
    }
}.distinctUntilChanged()

enum class PingInviteResult {
    INVITED,

    /** 자리가 모자랍니다. 그사이 다른 기기에서 더 불렀으면 이렇게 옵니다. */
    FULL,
}

enum class PingSendResult {
    SENT,

    /** 보낸 ㅇㅂㅇ이 아직 끝나지 않았습니다. 한 번에 하나만 보냅니다. */
    ALREADY_ACTIVE,

    /** 하루에 보낼 수 있는 만큼 보냈습니다. 서버가 하루 10번까지 받습니다. */
    TOO_MANY,
}
