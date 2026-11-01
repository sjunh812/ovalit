package com.ovalit.core.model

/** 경기 목록 날짜 머리에 붙이는 그날 승패입니다. */
data class DayRecord(val wins: Int, val losses: Int, val draws: Int)

/**
 * 그날 경기의 승패입니다.
 * 내 경기 목록(S2)과 친구 경기 목록이 같이 씁니다.
 * 받은 경기가 이미 고른 큐와 필터로 거른 것이라 보이는 줄만 셉니다.
 *
 * 데스매치와 건틀릿은 승패가 아니라 등수로 끝나서 세지 않습니다.
 * 경기 줄도 그런 판에는 승패를 적지 않습니다.
 * 이기거나 진 판이 하나도 없으면 "0승 0패"가 되니 `null`입니다.
 */
fun List<Match>.dayRecord(): DayRecord? {
    val twoTeams = filter { it.format == MatchFormat.ROUNDS || it.format == MatchFormat.TEAM_POINTS }
    val wins = twoTeams.count { it.myTeamWon == true }
    val losses = twoTeams.count { it.myTeamWon == false }
    if (wins + losses == 0) return null
    return DayRecord(wins = wins, losses = losses, draws = twoTeams.size - wins - losses)
}
