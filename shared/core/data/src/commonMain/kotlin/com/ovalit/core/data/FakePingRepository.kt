package com.ovalit.core.data

import com.ovalit.core.model.Ping
import com.ovalit.core.model.PingAnswer
import com.ovalit.core.model.PingId
import com.ovalit.core.model.PingMember
import com.ovalit.core.model.PingPerson
import com.ovalit.core.model.PlayerId
import com.ovalit.core.model.answered
import com.ovalit.core.model.invited
import com.ovalit.core.model.movedTo
import com.ovalit.core.model.pingSlots
import kotlin.time.Clock
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.TimeZone

/**
 * 서버가 붙기 전까지 쓰는 가짜 ㅇㅂㅇ입니다. 처음에는 민석이 보낸 ㅇㅂㅇ 하나가 와 있습니다. 내가 보내면 가짜 친구들이 잠시
 * 뒤에 답합니다. 첫째는 갈게요, 둘째는 다음 30분 칸을 내고, 셋째는 답하지 않고, 넷째는 못 간다고 합니다. 더 부른 친구는
 * 간다고 합니다.
 *
 * @param replyDelay 가짜 친구가 답하기까지 기다리는 시간입니다. 테스트는 가상 시간으로 넘깁니다.
 */
class FakePingRepository(
    private val friendRepository: FakeFriendRepository,
    private val clock: Clock = Clock.System,
    private val timeZone: TimeZone = TimeZone.currentSystemDefault(),
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default),
    private val replyDelay: Duration = 3.seconds,
) : PingRepository {

    private val all = MutableStateFlow(listOf(seedIncoming()))
    private var nextId = 0

    override val pings: Flow<List<Ping>> = all.map { list ->
        list.filter { it.isActive(clock.now()) }.sortedByDescending { it.createdAt }
    }

    override suspend fun send(friends: List<PlayerId>, startsAt: Instant): PingSendResult {
        val now = clock.now()
        if (all.value.any { it.isHostedBy(Me) && it.isActive(now) }) return PingSendResult.ALREADY_ACTIVE
        val known = friendRepository.currentFriends().associateBy { it.id }
        val ping = Ping(
            id = PingId("fake-ping-${nextId++}"),
            host = PingPerson(Me, MY_RIOT_ID),
            startsAt = startsAt,
            createdAt = now,
            members = friends.mapNotNull { id -> known[id]?.let { PingMember(PingPerson(id, it.riotId), PingAnswer.PENDING) } },
        )
        all.update { it + ping }
        scope.launch { answerLikeFriends(ping) }
        return PingSendResult.SENT
    }

    override suspend fun reply(id: PingId, answer: PingAnswer, proposedAt: Instant?) {
        update(id) { it.answered(Me, answer, proposedAt) }
    }

    override suspend fun moveTo(id: PingId, startsAt: Instant) {
        update(id) { it.movedTo(startsAt) }
        // 다시 물으면 답하지 않았던 친구 하나가 새 시각에 간다고 한다
        scope.launch {
            delay(replyDelay)
            update(id) { ping ->
                ping.members.firstOrNull { it.answer == PingAnswer.PENDING }
                    ?.let { ping.answered(it.person.id, PingAnswer.YES) } ?: ping
            }
        }
    }

    // 더 부른 친구는 잠시 뒤 간다고 한다
    override suspend fun invite(id: PingId, friends: List<PlayerId>): PingInviteResult {
        val ping = all.value.firstOrNull { it.id == id } ?: return PingInviteResult.FULL
        if (friends.size > ping.openSeats) return PingInviteResult.FULL
        val known = friendRepository.currentFriends().associateBy { it.id }
        val people = friends.mapNotNull { friend -> known[friend]?.let { PingPerson(friend, it.riotId) } }
        update(id) { it.invited(people) }
        scope.launch {
            people.forEach { person ->
                delay(replyDelay)
                update(id) { it.answered(person.id, PingAnswer.YES) }
            }
        }
        return PingInviteResult.INVITED
    }

    override suspend fun cancel(id: PingId) {
        all.update { list -> list.filterNot { it.id == id } }
    }

    override suspend fun refresh() = Unit

    /** 연동을 해제하면 ㅇㅂㅇ도 사라집니다. */
    fun clear() {
        all.value = emptyList()
    }

    fun refill() {
        all.value = listOf(seedIncoming())
    }

    private suspend fun answerLikeFriends(sent: Ping) {
        val answers = listOf(
            PingAnswer.YES to null,
            // 30분 단위 칸에서 하나 뒤를 낸다. 실제 앱도 칸에서만 고른다.
            PingAnswer.OTHER_TIME to pingSlots(sent.startsAt, timeZone).first(),
            null to null,
            PingAnswer.NO to null,
        )
        sent.members.zip(answers).forEach { (member, reply) ->
            delay(replyDelay)
            val (answer, proposedAt) = reply
            if (answer != null) update(sent.id) { it.answered(member.person.id, answer, proposedAt) }
        }
    }

    private fun update(id: PingId, change: (Ping) -> Ping) {
        all.update { list -> list.map { if (it.id == id) change(it) else it } }
    }

    // 민석이 한 시간쯤 뒤에 하자고 준호와 나를 불렀고 재현은 이미 간다고 했다
    private fun seedIncoming(): Ping {
        val now = clock.now()
        val (junho, minseok, jaehyun) = FakeFriendPlayers
        return Ping(
            id = PingId("fake-ping-incoming"),
            host = PingPerson(minseok.id, minseok.riotId),
            startsAt = pingSlots(now, timeZone)[1],
            createdAt = now - 5.minutes,
            members = listOf(
                PingMember(PingPerson(Me, MY_RIOT_ID), PingAnswer.PENDING),
                PingMember(PingPerson(junho.id, junho.riotId), PingAnswer.PENDING),
                PingMember(PingPerson(jaehyun.id, jaehyun.riotId), PingAnswer.YES),
            ),
        )
    }
}
