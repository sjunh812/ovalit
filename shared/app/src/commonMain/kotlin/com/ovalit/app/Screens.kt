package com.ovalit.app

import androidx.navigation3.runtime.NavKey
import com.ovalit.core.model.QueueFilter
import kotlinx.serialization.Serializable

/**
 * 스택을 저장할 때 쓸 직렬화기를 한 번에 등록하려고 화면 키를 sealed interface로 묶습니다([BackStackConfiguration]).
 * 경기 ID나 친구 PUUID를 든 키는 `toString()`에 그 값을 담지 않습니다.
 * 키가 섞인 예외 문구가 비정상 종료 보고로 나가면 Riot이 준 값이 같이 나갑니다(CLAUDE.md 지켜야 할 선).
 */
@Serializable
internal sealed interface Screen : NavKey

@Serializable
internal data object Intro : Screen

@Serializable
internal data object Consent : Screen

@Serializable
internal data object Import : Screen

@Serializable
internal data object Report : Screen

@Serializable
internal data object Settings : Screen

@Serializable
internal data object Matches : Screen

@Serializable
internal data class MatchDetail(val id: String) : Screen {
    override fun toString(): String = "MatchDetail"
}

@Serializable
internal data object Friends : Screen

@Serializable
internal data class FriendProfile(val id: String) : Screen {
    override fun toString(): String = "FriendProfile"
}

@Serializable
internal data class FriendMatches(val id: String) : Screen {
    override fun toString(): String = "FriendMatches"
}

@Serializable
internal data object Profile : Screen

/** S7입니다. 홈, 내 프로필, S5 칩이 고른 큐를 이름으로 들고 가서 카드에서 본 숫자와 같게 셉니다. */
@Serializable
internal data class Agents(val queue: String = QueueFilter.PROFILE.name) : Screen

/** S6입니다. 큐는 [Agents]와 같습니다. */
@Serializable
internal data class Weapons(val queue: String = QueueFilter.PROFILE.name) : Screen

@Serializable
internal data class FriendAgents(val id: String, val queue: String = QueueFilter.PROFILE.name) : Screen {
    override fun toString(): String = "FriendAgents"
}

@Serializable
internal data class PingDetail(val id: String) : Screen {
    override fun toString(): String = "PingDetail"
}

@Serializable
internal data class FriendWeapons(val id: String, val queue: String = QueueFilter.PROFILE.name) : Screen {
    override fun toString(): String = "FriendWeapons"
}

/**
 * 홈 친구 비교의 전체 순위입니다.
 * 큐 칩은 저장하지 않아서 홈에서 고른 큐와 지표를 이름으로 들고 갑니다.
 */
@Serializable
internal data class FriendRanking(val queue: String, val metric: String) : Screen

// 사용 통계에 쓰는 화면 이름이다.
// 키를 그대로 쓰면 친구·경기 ID가 섞이고 릴리스에서는 난독화된 이름이 된다.
// Screen을 받아 else 없이 갈라서 새 화면을 표에 빠뜨리면 컴파일이 멈춘다.
internal fun screenName(key: Screen): String = when (key) {
    Intro -> "intro"
    Consent -> "consent"
    Import -> "import"
    Report -> "report"
    Matches -> "matches"
    is MatchDetail -> "match_detail"
    Friends -> "friends"
    is FriendProfile -> "friend_profile"
    is FriendMatches -> "friend_matches"
    Profile -> "my_profile"
    is Agents -> "agents"
    is Weapons -> "weapons"
    is FriendAgents -> "friend_agents"
    is FriendWeapons -> "friend_weapons"
    is PingDetail -> "ping_detail"
    is FriendRanking -> "friend_ranking"
    Settings -> "settings"
}
