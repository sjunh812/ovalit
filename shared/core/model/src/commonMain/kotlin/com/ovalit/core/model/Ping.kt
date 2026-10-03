package com.ovalit.core.model

import kotlin.jvm.JvmInline
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

/** 한 번에 부를 수 있는 친구 수입니다. 발로란트 파티가 다섯 명이라 나까지 다섯입니다. */
const val MAX_PING_FRIENDS = 4

/** 하자고 한 시각부터 이만큼 지나면 끝난 ㅇㅂㅇ입니다. 서버도 같은 값으로 지웁니다. */
val PingLength = 1.hours

// 보낸 시각과 이만큼 안쪽이면 "지금"으로 적는다
private val NowWindow = 5.minutes

// 시간 고르기에 띄우는 칸의 간격이다
private val SlotStep = 30.minutes
// 다음 날 같은 시각 전까지다. 서버는 24시간 앞까지 받는다.
private const val SLOT_COUNT = 47

@JvmInline
value class PingId(val value: String)

/**
 * 친구에게 보낸 "오발있?"(ㅇㅂㅇ)입니다. 앱 이름 "오늘 발로란트 할 사람 있어?"를 그대로 묻는 기능입니다.
 *
 * 보낸 사람이 시각을 정하면 받은 친구는 갈게요, 다른 시간, 못 가요 중 하나로 답합니다. 다른 시간이 오면 보낸 사람이 그 시각으로
 * 옮길 수 있고, 그러면 모두에게 다시 묻습니다. 서로 수락한 친구에게만 보내고, 글을 적는 칸은 없습니다. 정해 둔 답만 오가서
 * 신고나 차단을 둘 글이 생기지 않습니다.
 *
 * @property startsAt 하자고 한 시각입니다. "지금"으로 보내면 보낸 시각과 같습니다.
 * @property members 받은 친구들입니다. 보낸 사람은 들어 있지 않습니다.
 */
data class Ping(
    val id: PingId,
    val host: PingPerson,
    val startsAt: Instant,
    val createdAt: Instant,
    val members: List<PingMember>,
) {
    val expiresAt: Instant get() = startsAt + PingLength

    /** 보낸 시각에 바로 하자고 한 ㅇㅂㅇ입니다. 화면에는 시각 대신 "지금"으로 적습니다. */
    val isNow: Boolean get() = startsAt - createdAt < NowWindow

    val yesCount: Int get() = members.count { it.answer == PingAnswer.YES }

    /** 더 부를 수 있는 자리입니다. 못 간다고 한 친구는 자리를 비운 것으로 칩니다. 서버와 같은 규칙입니다. */
    val openSeats: Int get() = (MAX_PING_FRIENDS - members.count { it.answer != PingAnswer.NO }).coerceAtLeast(0)

    fun isActive(now: Instant): Boolean = now < expiresAt

    fun isHostedBy(id: PlayerId): Boolean = host.id == id

    fun memberOf(id: PlayerId): PingMember? = members.firstOrNull { it.person.id == id }
}

data class PingPerson(val id: PlayerId, val riotId: String)

/** @property proposedAt [PingAnswer.OTHER_TIME]로 답할 때 낸 시각입니다. 다른 답이면 `null`입니다. */
data class PingMember(
    val person: PingPerson,
    val answer: PingAnswer,
    val proposedAt: Instant? = null,
)

enum class PingAnswer {
    PENDING,
    YES,
    OTHER_TIME,
    NO,
}

/** 받은 친구의 답을 바꿉니다. 다른 시간이 아니면 낸 시각을 지웁니다. */
fun Ping.answered(by: PlayerId, answer: PingAnswer, proposedAt: Instant? = null): Ping = copy(
    members = members.map { member ->
        if (member.person.id != by) {
            member
        } else {
            member.copy(answer = answer, proposedAt = proposedAt.takeIf { answer == PingAnswer.OTHER_TIME })
        }
    },
)

/** 보낸 사람이 친구를 더 부릅니다. 더한 친구는 앞사람 뒤에 붙고 아직 답하지 않은 것으로 둡니다. */
fun Ping.invited(people: List<PingPerson>): Ping = copy(
    members = members + people.filter { person -> memberOf(person.id) == null }.map { PingMember(it, PingAnswer.PENDING) },
)

/**
 * 보낸 사람이 시각을 옮깁니다. 그 시각을 낸 친구만 참석으로 두고 나머지는 모두 다시 묻습니다. 9시에 간다던 친구가 10시에도
 * 되는지는 모르고, 9시에 안 된다던 친구가 10시에는 될 수도 있습니다.
 */
fun Ping.movedTo(startsAt: Instant): Ping = copy(
    startsAt = startsAt,
    members = members.map { member ->
        val proposedThis = member.answer == PingAnswer.OTHER_TIME && member.proposedAt == startsAt
        member.copy(answer = if (proposedThis) PingAnswer.YES else PingAnswer.PENDING, proposedAt = null)
    },
)

/**
 * 홈 맨 위에 하나만 띄울 ㅇㅂㅇ입니다. 아직 답하지 않은 받은 것, 내가 보낸 것, 답한 받은 것 순서입니다. 답해야 하는 게 먼저
 * 눈에 띄어야 합니다.
 */
fun List<Ping>.forHome(me: PlayerId): Ping? =
    firstOrNull { !it.isHostedBy(me) && it.memberOf(me)?.answer == PingAnswer.PENDING }
        ?: firstOrNull { it.isHostedBy(me) }
        ?: firstOrNull()

/**
 * 시간 고르기에 띄우는 시각입니다. [now] 뒤 첫 정각이나 30분부터 30분마다 다음 날 같은 시각 전까지입니다. "지금"은 따로
 * 둡니다. 13시 12분이면 13시 30분, 14시, … 다음 날 12시 30분입니다. 처음에는 여섯 시간 앞까지였는데 밤늦게 하는 사람은
 * 고를 시각이 없었습니다(사용자 요청, 2026-10-03).
 */
fun pingSlots(now: Instant, timeZone: TimeZone): List<Instant> {
    val local = now.toLocalDateTime(timeZone)
    // 지난 30분 칸의 시작으로 내린 뒤 한 칸 올린다
    val passed = (local.minute % 30 * 60_000L + local.second * 1_000L + local.nanosecond / 1_000_000).milliseconds
    val first = now - passed + SlotStep
    return List(SLOT_COUNT) { index -> first + SlotStep * index }
}
