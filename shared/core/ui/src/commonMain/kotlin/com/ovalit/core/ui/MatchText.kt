package com.ovalit.core.ui

import androidx.compose.runtime.Composable
import com.ovalit.core.model.AgentId
import com.ovalit.core.model.ContentCatalog
import com.ovalit.core.model.MapId
import com.ovalit.core.model.Queue
import com.ovalit.core.model.QueueFilter
import com.ovalit.core.ui.resources.Res
import com.ovalit.core.ui.resources.day_date
import com.ovalit.core.ui.resources.day_today
import com.ovalit.core.ui.resources.day_yesterday
import com.ovalit.core.ui.resources.list_separator
import com.ovalit.core.ui.resources.queue_competitive
import com.ovalit.core.ui.resources.queue_deathmatch
import com.ovalit.core.ui.resources.queue_escalation
import com.ovalit.core.ui.resources.queue_filter_competitive
import com.ovalit.core.ui.resources.queue_filter_competitive_and_unrated
import com.ovalit.core.ui.resources.queue_filter_other
import com.ovalit.core.ui.resources.queue_filter_unrated
import com.ovalit.core.ui.resources.queue_other
import com.ovalit.core.ui.resources.queue_premier
import com.ovalit.core.ui.resources.queue_replication
import com.ovalit.core.ui.resources.queue_snowball_fight
import com.ovalit.core.ui.resources.queue_spike_rush
import com.ovalit.core.ui.resources.queue_swiftplay
import com.ovalit.core.ui.resources.queue_team_deathmatch
import com.ovalit.core.ui.resources.queue_unrated
import com.ovalit.core.ui.resources.time_clock
import com.ovalit.core.ui.resources.time_hours_ago
import com.ovalit.core.ui.resources.time_just_now
import com.ovalit.core.ui.resources.time_minutes_ago
import com.ovalit.core.ui.resources.time_short_date
import com.ovalit.core.ui.resources.unknown_agent
import com.ovalit.core.ui.resources.unknown_map
import com.ovalit.core.ui.resources.weekday_friday
import com.ovalit.core.ui.resources.weekday_monday
import com.ovalit.core.ui.resources.weekday_saturday
import com.ovalit.core.ui.resources.weekday_sunday
import com.ovalit.core.ui.resources.weekday_thursday
import com.ovalit.core.ui.resources.weekday_tuesday
import com.ovalit.core.ui.resources.weekday_wednesday
import kotlin.time.Instant
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.datetime.number
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

val QueueFilter.label: StringResource
    get() = when (this) {
        QueueFilter.COMPETITIVE_AND_UNRATED -> Res.string.queue_filter_competitive_and_unrated
        QueueFilter.COMPETITIVE -> Res.string.queue_filter_competitive
        QueueFilter.UNRATED -> Res.string.queue_filter_unrated
        QueueFilter.OTHER -> Res.string.queue_filter_other
    }

val Queue.label: StringResource
    get() = when (this) {
        Queue.COMPETITIVE -> Res.string.queue_competitive
        Queue.UNRATED -> Res.string.queue_unrated
        Queue.PREMIER -> Res.string.queue_premier
        Queue.SPIKE_RUSH -> Res.string.queue_spike_rush
        Queue.SWIFTPLAY -> Res.string.queue_swiftplay
        Queue.REPLICATION -> Res.string.queue_replication
        Queue.ESCALATION -> Res.string.queue_escalation
        Queue.DEATHMATCH -> Res.string.queue_deathmatch
        Queue.TEAM_DEATHMATCH -> Res.string.queue_team_deathmatch
        Queue.SNOWBALL_FIGHT -> Res.string.queue_snowball_fight
        Queue.OTHER -> Res.string.queue_other
    }

@Composable
fun ContentCatalog.agentName(id: AgentId): String = agents[id] ?: stringResource(Res.string.unknown_agent)

@Composable
fun ContentCatalog.mapName(id: MapId): String = maps[id] ?: stringResource(Res.string.unknown_map)

/**
 * 날짜 머리 아래 놓이는 경기 줄의 시각입니다.
 * 오늘 경기는 "2시간 전"처럼, 그 전 경기는 날짜가 머리에 있으니 "23:40"처럼 시각만 적습니다.
 */
@Composable
fun matchTimeLabel(startedAt: Instant, now: Instant, timeZone: TimeZone): String {
    val start = startedAt.toLocalDateTime(timeZone)
    if (start.date != now.toLocalDateTime(timeZone).date) {
        return clockText(start)
    }
    return agoLabel(startedAt, now)
}

/** "23:40"처럼 시와 분만 적습니다. */
@Composable
fun clockText(at: LocalDateTime): String = stringResource(Res.string.time_clock, at.hour.twoDigits(), at.minute.twoDigits())

/** 이름을 늘어놓을 때 씁니다. 한국어는 ", ", 일본어는 "、"입니다. */
@Composable
fun List<String>.joinedForDisplay(): String = joinToString(stringResource(Res.string.list_separator))

/** 날짜 머리 없이 몇 줄만 보여줄 때의 시각입니다. "2시간 전", "어제", "9/22" 순으로 씁니다. */
@Composable
fun recentMatchTimeLabel(startedAt: Instant, now: Instant, timeZone: TimeZone): String {
    val day = startedAt.toLocalDateTime(timeZone).date
    val today = now.toLocalDateTime(timeZone).date
    return when (day) {
        today -> agoLabel(startedAt, now)
        today.minus(1, DateTimeUnit.DAY) -> stringResource(Res.string.day_yesterday)
        else -> stringResource(Res.string.time_short_date, day.month.number, day.day)
    }
}

@Composable
private fun agoLabel(startedAt: Instant, now: Instant): String {
    val minutes = (now - startedAt).inWholeMinutes
    return when {
        minutes < 1 -> stringResource(Res.string.time_just_now)
        minutes < 60 -> stringResource(Res.string.time_minutes_ago, minutes.toInt())
        else -> stringResource(Res.string.time_hours_ago, (minutes / 60).toInt())
    }
}

private fun Int.twoDigits() = toString().padStart(2, '0')

/** 경기 목록의 날짜 머리입니다. 오늘과 어제는 말로, 그 전은 "9월 22일 화요일"로 씁니다. */
@Composable
fun dayLabel(date: LocalDate, today: LocalDate): String = when (date) {
    today -> stringResource(Res.string.day_today)
    today.minus(1, DateTimeUnit.DAY) -> stringResource(Res.string.day_yesterday)
    else -> stringResource(Res.string.day_date, date.month.number, date.day, weekdayName(date.dayOfWeek))
}

@Composable
private fun weekdayName(day: DayOfWeek): String = stringResource(
    when (day) {
        DayOfWeek.MONDAY -> Res.string.weekday_monday
        DayOfWeek.TUESDAY -> Res.string.weekday_tuesday
        DayOfWeek.WEDNESDAY -> Res.string.weekday_wednesday
        DayOfWeek.THURSDAY -> Res.string.weekday_thursday
        DayOfWeek.FRIDAY -> Res.string.weekday_friday
        DayOfWeek.SATURDAY -> Res.string.weekday_saturday
        DayOfWeek.SUNDAY -> Res.string.weekday_sunday
    },
)
