package com.ovalit.core.data

import com.ovalit.core.model.Friend
import com.ovalit.core.model.FriendRequest
import com.ovalit.core.model.FriendRequestSource
import com.ovalit.core.model.PlayerId
import kotlin.time.Clock
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update

/** 친구 서버가 생기기 전까지 쓰는 가짜 친구입니다. 목업의 준호, 민석, 재현이 있습니다. */
class FakeFriendRepository(
    private val clock: Clock = Clock.System,
) : FriendRepository {

    private val friendList = MutableStateFlow(fakeFriends())
    private val requestList = MutableStateFlow(fakeRequests())
    private val rivalId = MutableStateFlow<PlayerId?>(null)
    private val sent = MutableStateFlow<Set<PlayerId>>(emptySet())

    override val friends: Flow<List<Friend>> = friendList
    override val requests: Flow<List<FriendRequest>> = requestList
    override val rival: Flow<PlayerId?> = rivalId
    override val sentRequests: Flow<Set<PlayerId>> = sent

    override suspend fun appUsersAmong(players: Collection<PlayerId>): Set<PlayerId> {
        val known = StrangersUsingApp + friendList.value.map { it.id } + requestList.value.map { it.id }
        return players.filterTo(mutableSetOf()) { it in known }
    }

    override suspend fun sendRequest(id: PlayerId) {
        sent.update { it + id }
    }

    override suspend fun accept(id: PlayerId) {
        val request = requestList.value.firstOrNull { it.id == id } ?: return
        requestList.update { requests -> requests.filterNot { it.id == id } }
        friendList.update { friends ->
            friends + Friend(
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
        }
    }

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

    override fun inviteLink(): String = "https://ovalit.netlify.app/invite/K7Q2M"

    /** 연동을 해제하면 친구 관계도 사라집니다. */
    fun clear() {
        friendList.value = emptyList()
        requestList.value = emptyList()
        rivalId.value = null
        sent.value = emptySet()
    }

    fun refill() {
        friendList.value = fakeFriends()
        requestList.value = fakeRequests()
        rivalId.value = null
        sent.value = emptySet()
    }

    private fun fakeFriends(): List<Friend> {
        val now = clock.now()
        // 목업 S5의 민석은 다이아몬드 2다
        val seeds = listOf(101 to 17, 202 to 19, 303 to 14)
        return FakeFriendPlayers.zip(seeds) { player, (seed, tier) ->
            Friend(
                id = player.id,
                riotId = player.riotId,
                playerCard = player.card,
                statsPublic = true,
                matches = fakeMatches(now, seed, withFriends = false, owner = Owner(player.id, player.riotId, player.card, tier)),
            )
        } + Friend(PlayerId("fake-seoyeon"), "서연#KR7", FakeCards[10], statsPublic = false, matches = emptyList())
    }

    private fun fakeRequests() = listOf(
        FriendRequest(PlayerId("fake-jiwoo"), "지우#KR5", FakeCards[6], FriendRequestSource.SCOREBOARD),
        FriendRequest(PlayerId("fake-hyun"), "현#KR9", FakeCards[9], FriendRequestSource.INVITE_LINK),
    )
}
