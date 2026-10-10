package com.ovalit.core.ui

import androidx.compose.runtime.Composable
import com.ovalit.core.model.Ping
import com.ovalit.core.model.PingAnswer
import com.ovalit.core.model.PingMember
import com.ovalit.core.model.PlayerId
import com.ovalit.core.ui.resources.Res
import com.ovalit.core.ui.resources.ping_answer_no
import com.ovalit.core.ui.resources.ping_answer_other
import com.ovalit.core.ui.resources.ping_answer_pending
import com.ovalit.core.ui.resources.ping_answer_yes
import com.ovalit.core.ui.resources.ping_called_by
import com.ovalit.core.ui.resources.ping_called_by_me
import com.ovalit.core.ui.resources.ping_called_just_now
import com.ovalit.core.ui.resources.ping_called_minutes_ago
import com.ovalit.core.ui.resources.ping_day_dawn
import com.ovalit.core.ui.resources.ping_day_today
import com.ovalit.core.ui.resources.ping_day_tomorrow
import com.ovalit.core.ui.resources.ping_home_received
import com.ovalit.core.ui.resources.ping_home_received_now
import com.ovalit.core.ui.resources.ping_home_sent
import com.ovalit.core.ui.resources.ping_home_sent_now
import com.ovalit.core.ui.resources.ping_in_hours
import com.ovalit.core.ui.resources.ping_in_hours_minutes
import com.ovalit.core.ui.resources.ping_in_minutes
import com.ovalit.core.ui.resources.ping_my_answer_no
import com.ovalit.core.ui.resources.ping_my_answer_other
import com.ovalit.core.ui.resources.ping_my_answer_pending
import com.ovalit.core.ui.resources.ping_my_answer_yes
import com.ovalit.core.ui.resources.ping_started
import com.ovalit.core.ui.resources.ping_summary_outgoing
import com.ovalit.core.ui.resources.ping_time_dawn
import com.ovalit.core.ui.resources.ping_time_now
import com.ovalit.core.ui.resources.ping_time_tomorrow
import com.ovalit.core.ui.resources.ping_when
import kotlin.time.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.compose.resources.stringResource

/** "21:00"입니다. 자정을 넘긴 오늘 밤이면 "새벽 00:30", 그보다 뒤면 "내일 09:00"입니다([pingDayOf]). */
@Composable
fun pingClockText(at: Instant, now: Instant, timeZone: TimeZone): String {
    val clock = clockOf(at, timeZone)
    return when (pingDayOf(at, now, timeZone)) {
        PingDay.TODAY -> clock
        PingDay.DAWN -> stringResource(Res.string.ping_time_dawn, clock)
        PingDay.TOMORROW -> stringResource(Res.string.ping_time_tomorrow, clock)
    }
}

/** 하자고 한 시각이 언제인지입니다. */
enum class PingDay { TODAY, DAWN, TOMORROW }

/**
 * [at]이 오늘인지, 자정을 넘긴 오늘 밤인지, 내일인지 가립니다. 밤 11시에 고른 0시 30분을 "내일"이라 하면 하루 뒤처럼 읽혀서,
 * 다음 날 6시 전이면 [PingDay.DAWN]입니다. 지금이 이미 새벽이면 다음 날 새벽은 하루 뒤라 [PingDay.TOMORROW]입니다.
 */
fun pingDayOf(at: Instant, now: Instant, timeZone: TimeZone): PingDay {
    val target = at.toLocalDateTime(timeZone)
    val current = now.toLocalDateTime(timeZone)
    return when {
        target.date == current.date -> PingDay.TODAY
        target.hour < DAWN_END_HOUR && current.hour >= DAWN_END_HOUR -> PingDay.DAWN
        else -> PingDay.TOMORROW
    }
}

private const val DAWN_END_HOUR = 6

/** 크게 적는 시각입니다. 바로 하자고 했으면 "지금"입니다. 오늘이 아니면 [pingHeroDay]를 앞에 같이 크게 적습니다. */
@Composable
fun pingHeroTime(ping: Ping, timeZone: TimeZone): String =
    if (ping.isNow) stringResource(Res.string.ping_time_now) else clockOf(ping.startsAt, timeZone)

/**
 * 큰 시각 앞에 같이 크게 적는 "내일"이나 "새벽"입니다. 오늘이면 `null`입니다. 옆 작은 글자에만 두면 큰 "16:00"이 오늘로
 * 읽힙니다.
 */
@Composable
fun pingHeroDay(ping: Ping, now: Instant, timeZone: TimeZone): String? {
    if (ping.isNow) return null
    return when (pingDayOf(ping.startsAt, now, timeZone)) {
        PingDay.TODAY -> null
        PingDay.DAWN -> stringResource(Res.string.ping_day_dawn)
        PingDay.TOMORROW -> stringResource(Res.string.ping_day_tomorrow)
    }
}

/**
 * 큰 시각 옆 "오늘 · 35분 뒤"입니다. 오늘이 아니면 날짜는 큰 글자 앞에 있어서 "19시간 35분 뒤"만 적습니다. 바로 하자고 한 것은
 * 큰 글자가 이미 "지금"이라 "3분 전에 불렀어요"로 언제 불렀는지를 적고, 시각이 지나면 "시작했어요"입니다.
 */
@Composable
fun pingWhenText(ping: Ping, now: Instant, timeZone: TimeZone): String {
    if (ping.isNow) {
        val ago = (now - ping.createdAt).inWholeMinutes.toInt()
        return if (ago < 1) stringResource(Res.string.ping_called_just_now) else stringResource(Res.string.ping_called_minutes_ago, ago)
    }
    val left = ping.startsAt - now
    if (!left.isPositive()) return stringResource(Res.string.ping_started)
    // 1분 안쪽 끝자리는 올린다. 35분 40초 남았으면 "36분 뒤"다. 0분 뒤라고 적지 않는다.
    val minutes = (left.inWholeSeconds + 59) / 60
    val hours = (minutes / 60).toInt()
    val rest = (minutes % 60).toInt()
    val remaining = when {
        hours == 0 -> stringResource(Res.string.ping_in_minutes, rest)
        rest == 0 -> stringResource(Res.string.ping_in_hours, hours)
        else -> stringResource(Res.string.ping_in_hours_minutes, hours, rest)
    }
    if (pingDayOf(ping.startsAt, now, timeZone) != PingDay.TODAY) return remaining
    return stringResource(Res.string.ping_when, stringResource(Res.string.ping_day_today), remaining)
}

/**
 * 카드 머리의 "민석의 초대"나 "내 초대"입니다. 받은 것과 보낸 것이 여기서 갈립니다.
 *
 * 닉네임 뒤에는 받침에 따라 바뀌는 조사를 붙이지 않습니다(CLAUDE.md 용어). "봉봉이"는 "봉봉이이"로 읽히고 "Tom"은 받침을
 * 알 수 없어서 "의", "에게"처럼 늘 같은 조사만 씁니다.
 */
@Composable
fun pingCalledBy(ping: Ping, me: PlayerId): String =
    if (ping.isHostedBy(me)) {
        stringResource(Res.string.ping_called_by_me)
    } else {
        stringResource(Res.string.ping_called_by, ping.host.riotId.substringBefore('#'))
    }

/** 홈 한 줄의 "민석의 초대 · 21:00"이나 "내 초대 · 지금"입니다. */
@Composable
fun pingHomeTitle(ping: Ping, me: PlayerId, now: Instant, timeZone: TimeZone): String {
    val time = pingClockText(ping.startsAt, now, timeZone)
    if (ping.isHostedBy(me)) {
        return if (ping.isNow) stringResource(Res.string.ping_home_sent_now) else stringResource(Res.string.ping_home_sent, time)
    }
    val host = ping.host.riotId.substringBefore('#')
    return if (ping.isNow) {
        stringResource(Res.string.ping_home_received_now, host)
    } else {
        stringResource(Res.string.ping_home_received, host, time)
    }
}

/** 보낸 것이면 "4명 중 1명 참석", 받은 것이면 내 답("참석으로 답했어요")입니다. */
@Composable
fun pingSummary(ping: Ping, me: PlayerId, now: Instant, timeZone: TimeZone): String {
    if (ping.isHostedBy(me)) return stringResource(Res.string.ping_summary_outgoing, ping.members.size, ping.yesCount)
    val mine = ping.memberOf(me)
    return when (mine?.answer) {
        PingAnswer.YES -> stringResource(Res.string.ping_my_answer_yes)
        PingAnswer.NO -> stringResource(Res.string.ping_my_answer_no)
        PingAnswer.OTHER_TIME -> stringResource(
            Res.string.ping_my_answer_other,
            mine.proposedAt?.let { pingClockText(it, now, timeZone) } ?: NO_VALUE,
        )
        PingAnswer.PENDING, null -> stringResource(Res.string.ping_my_answer_pending)
    }
}

/** 친구 한 사람의 답입니다. "참석", "불참", "22:00 제안", "응답 전"처럼 상태 말로 적습니다. */
@Composable
fun pingAnswerText(member: PingMember, now: Instant, timeZone: TimeZone): String = when (member.answer) {
    PingAnswer.YES -> stringResource(Res.string.ping_answer_yes)
    PingAnswer.NO -> stringResource(Res.string.ping_answer_no)
    PingAnswer.PENDING -> stringResource(Res.string.ping_answer_pending)
    PingAnswer.OTHER_TIME -> stringResource(
        Res.string.ping_answer_other,
        member.proposedAt?.let { pingClockText(it, now, timeZone) } ?: NO_VALUE,
    )
}

@Composable
private fun clockOf(at: Instant, timeZone: TimeZone): String = clockText(at.toLocalDateTime(timeZone))
