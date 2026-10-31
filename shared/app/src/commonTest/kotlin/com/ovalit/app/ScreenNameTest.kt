package com.ovalit.app

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ScreenNameTest {

    // 사용 통계에 Riot이 준 값(PUUID, 경기 ID)을 보내면 안 된다(CLAUDE.md 지켜야 할 선, 헌법 IV).
    // 경로 키에 친구 PUUID나 경기 ID가 들어간 화면도 고정 표의 이름만 보낸다.
    @Test
    fun `화면 이름은 고정 표의 값뿐이고 경로 키의 ID를 담지 않는다`() {
        val id = "puuid-or-match-id-1234"
        val keys = listOf(
            Intro, Consent, Import, Report, Matches, MatchDetail(id), Friends, FriendProfile(id), FriendMatches(id),
            Profile, Agents, Weapons, FriendAgents(id), FriendWeapons(id), PingDetail(id), FriendRanking(queue = id, metric = id),
            Settings,
        )

        val names = keys.map(::screenName)

        names.forEach { name ->
            assertFalse(id in name, "$name 에 경로 키의 ID가 들어갔다")
            assertTrue(name.matches(FixedName), "$name 이 고정 표의 꼴(소문자와 밑줄)이 아니다")
        }
        assertEquals(names.size, names.toSet().size, "두 화면이 같은 이름을 쓴다")
    }

    // 키가 섞인 예외 문구가 비정상 종료 보고로 나가도 경기 ID와 PUUID가 나가지 않는다
    @Test
    fun `경로 키를 글자로 바꿔도 ID를 담지 않는다`() {
        val id = "puuid-or-match-id-1234"
        listOf(MatchDetail(id), FriendProfile(id), FriendMatches(id), FriendAgents(id), FriendWeapons(id), PingDetail(id)).forEach { key ->
            assertFalse(id in key.toString(), "$key")
        }
    }
}

private val FixedName = Regex("[a-z_]+")
