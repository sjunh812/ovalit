package com.ovalit.core.data

import com.ovalit.core.model.Friend
import com.ovalit.core.model.FriendRequest
import com.ovalit.core.model.FriendRequestSource
import com.ovalit.core.model.MatchId
import com.ovalit.core.model.PlayerId
import com.ovalit.core.model.lastPlayedWith
import kotlin.time.Clock
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private val REFRESH_DELAY = 700.milliseconds

/** 친구 서버가 생기기 전까지 쓰는 가짜 친구입니다. 목업의 준호, 민석, 재현과 전적을 비공개한 서연이 있습니다. */
class FakeFriendRepository(
    private val clock: Clock = Clock.System,
    // 앱은 앱 수명 스코프를 넘겨 가짜 친구 경기를 메인 스레드 밖에서 만든다.
    // 친구 셋의 70일치 경기를 라운드까지 만드는 일이라 앱을 켤 때 메인 스레드에서 했더니 첫 화면이 몇 초 멈췄다.
    // 테스트는 넘기지 않아 만들자마자 친구가 있다.
    private val scope: CoroutineScope? = null,
) : FriendRepository {

    private val friendList = MutableStateFlow(if (scope == null) fakeFriends() else emptyList())

    // 연동을 해제하거나 다시 채울 때마다 올린다.
    // 친구를 만드는 계산은 중간에 멈추지 않아서, 그사이 해제했으면 다 만든 친구를 버린다.
    private val generation = MutableStateFlow(0)
    private var filling: Job? = scope?.let(::fill)
    private val requestList = MutableStateFlow(fakeRequests())
    private val rivalId = MutableStateFlow<PlayerId?>(null)
    private val sent = MutableStateFlow<Set<PlayerId>>(emptySet())

    override val friends: Flow<List<Friend>> = friendList
    override val requests: Flow<List<FriendRequest>> = requestList
    override val rival: Flow<PlayerId?> = rivalId
    override val sentRequests: Flow<Set<PlayerId>> = sent

    override suspend fun appUsersAmong(match: MatchId, players: Collection<PlayerId>): Set<PlayerId> {
        val known = StrangersUsingApp + friendList.value.map { it.id } + requestList.value.map { it.id }
        return players.filterTo(mutableSetOf()) { it in known }
    }

    override suspend fun sendRequest(id: PlayerId, match: MatchId) {
        sent.update { it + id }
    }

    override suspend fun accept(id: PlayerId) {
        val request = requestList.value.firstOrNull { it.id == id } ?: return
        requestList.update { requests -> requests.filterNot { it.id == id } }
        // 친구 한 명의 70일치 경기라 앱에서는 수락을 누른 메인 스레드에서 만들지 않는다.
        // 테스트는 스코프를 넘기지 않아 그 자리에서 만든다.
        val friend = if (scope == null) acceptedFriend(request) else withContext(Dispatchers.Default) { acceptedFriend(request) }
        friendList.update { friends -> friends + friend }
    }

    private fun acceptedFriend(request: FriendRequest) = Friend(
        id = request.id,
        riotId = request.riotId,
        playerCard = request.playerCard,
        statsPublic = true,
        matches = fakeMatches(
            now = clock.now(),
            seed = request.riotId.hashCode(),
            withFriends = false,
            owner = Owner(request.id, request.riotId, request.playerCard ?: MyCard, tier = 14),
        ),
    )

    override suspend fun decline(id: PlayerId) {
        requestList.update { requests -> requests.filterNot { it.id == id } }
    }

    override suspend fun unfriend(id: PlayerId) {
        friendList.update { friends -> friends.filterNot { it.id == id } }
        if (rivalId.value == id) rivalId.value = null
    }

    override suspend fun setRival(id: PlayerId?) {
        rivalId.value = id
    }

    // 받을 것은 없지만 당긴 티가 나게 잠깐 기다린다
    override suspend fun refresh() = delay(REFRESH_DELAY)

    // 실제 링크는 서버가 만들어 준다.
    // 경로만 서버의 초대 페이지(`/i/:code`)와 App Link에 맞춘다.
    override fun inviteLink(): String = "https://ovalit.netlify.app/i/K7Q2M"

    /** 가짜 ㅇㅂㅇ이 친구 이름을 찾을 때 씁니다. */
    internal fun currentFriends(): List<Friend> = friendList.value

    /** 연동을 해제하면 친구 관계도 사라집니다. */
    fun clear() {
        generation.update { it + 1 }
        filling?.cancel()
        friendList.value = emptyList()
        requestList.value = emptyList()
        rivalId.value = null
        sent.value = emptySet()
    }

    fun refill() {
        generation.update { it + 1 }
        filling?.cancel()
        if (scope == null) friendList.value = fakeFriends() else filling = fill(scope)
        requestList.value = fakeRequests()
        rivalId.value = null
        sent.value = emptySet()
    }

    // 채우는 동안 수락한 친구는 남기고 그 앞에 가짜 친구를 둔다.
    // 그사이 연동을 해제했거나 다시 채웠으면 쓰지 않는다.
    private fun fill(scope: CoroutineScope): Job {
        val started = generation.value
        return scope.launch {
            val made = fakeFriends()
            friendList.update { current ->
                if (generation.value != started) current else made.filterNot { f -> current.any { it.id == f.id } } + current
            }
        }
    }

    private fun fakeFriends(): List<Friend> {
        val now = clock.now()
        // 가짜 내 경기에 친구가 우리 팀으로 끼어 있다.
        // 실제 저장소도 기기에 저장된 내 경기에서 찾는다.
        val mine = fakeMatches(now)
        // 목업 S5의 민석은 다이아몬드 2다
        val seeds = listOf(101 to 17, 202 to 19, 303 to 14)
        return FakeFriendPlayers.zip(seeds) { player, (seed, tier) ->
            Friend(
                id = player.id,
                riotId = player.riotId,
                playerCard = player.card,
                statsPublic = true,
                matches = fakeMatches(now, seed, withFriends = false, owner = Owner(player.id, player.riotId, player.card, tier)),
                lastPlayedTogether = mine.lastPlayedWith(player.id),
            )
        } + Friend(PlayerId("fake-seoyeon"), "서연#KR7", FakeCards[10], statsPublic = false, matches = emptyList())
    }

    private fun fakeRequests() = listOf(
        FriendRequest(PlayerId("fake-jiwoo"), "지우#KR5", FakeCards[6], FriendRequestSource.SCOREBOARD),
        FriendRequest(PlayerId("fake-hyun"), "현#KR9", FakeCards[9], FriendRequestSource.INVITE_LINK),
    )
}
