package com.ovalit.core.testing

import com.ovalit.core.data.PingInviteResult
import com.ovalit.core.data.PingRepository
import com.ovalit.core.data.PingSendResult
import com.ovalit.core.model.Ping
import com.ovalit.core.model.PingAnswer
import com.ovalit.core.model.PingId
import com.ovalit.core.model.PlayerId
import kotlin.time.Instant
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

/**
 * [refresh]는 부른 횟수만 세고 다시 받지는 않는 [PingRepository]입니다. 나머지는 [delegate]에 맡기고, 넘기지 않으면
 * ㅇㅂㅇ이 하나도 없습니다([NoPings]).
 */
class TestPingRepository(delegate: PingRepository = NoPings) : PingRepository by delegate {

    var refreshed = 0
        private set

    override suspend fun refresh() {
        refreshed++
    }
}

/** ㅇㅂㅇ이 하나도 없는 [PingRepository]입니다. 보내거나 답해도 목록은 빈 채로 둡니다. */
object NoPings : PingRepository {
    override val pings: Flow<List<Ping>> = flowOf(emptyList())

    override suspend fun send(friends: List<PlayerId>, startsAt: Instant) = PingSendResult.SENT

    override suspend fun reply(id: PingId, answer: PingAnswer, proposedAt: Instant?) = Unit

    override suspend fun moveTo(id: PingId, startsAt: Instant) = Unit

    override suspend fun invite(id: PingId, friends: List<PlayerId>) = PingInviteResult.INVITED

    override suspend fun cancel(id: PingId) = Unit

    override suspend fun refresh() = Unit
}
