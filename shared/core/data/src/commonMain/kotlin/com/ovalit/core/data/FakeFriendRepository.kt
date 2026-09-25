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

    override val friends: Flow<List<Friend>> = friendList
    override val requests: Flow<List<FriendRequest>> = requestList
    override val rival: Flow<PlayerId?> = rivalId

    override suspend fun accept(id: PlayerId) {
        val request = requestList.value.firstOrNull { it.id == id } ?: return
        requestList.update { requests -> requests.filterNot { it.id == id } }
        friendList.update { friends ->
            friends + Friend(
                id = request.id,
                riotId = request.riotId,
                statsPublic = true,
                matches = fakeMatches(clock.now(), seed = request.riotId.hashCode(), withFriends = false),
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
    }

    fun refill() {
        friendList.value = fakeFriends()
        requestList.value = fakeRequests()
        rivalId.value = null
    }

    private fun fakeFriends(): List<Friend> {
        val now = clock.now()
        val (junho, minseok, jaehyun) = FakeFriendIds
        return listOf(
            Friend(junho, "준호#KR1", statsPublic = true, matches = fakeMatches(now, seed = 101, withFriends = false)),
            Friend(minseok, "민석#KR3", statsPublic = true, matches = fakeMatches(now, seed = 202, withFriends = false)),
            Friend(jaehyun, "재현#KR2", statsPublic = true, matches = fakeMatches(now, seed = 303, withFriends = false)),
            Friend(PlayerId("fake-seoyeon"), "서연#KR7", statsPublic = false, matches = emptyList()),
        )
    }

    private fun fakeRequests() = listOf(
        FriendRequest(PlayerId("fake-jiwoo"), "지우#KR5", FriendRequestSource.SCOREBOARD),
        FriendRequest(PlayerId("fake-hyun"), "현#KR9", FriendRequestSource.INVITE_LINK),
    )
}
