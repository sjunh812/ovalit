package com.ovalit.core.testing

import com.ovalit.core.data.FriendRepository
import com.ovalit.core.model.Friend
import com.ovalit.core.model.FriendRequest
import com.ovalit.core.model.PlayerId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

/**
 * 친구 목록과 라이벌을 정해 두는 [FriendRepository]입니다.
 * 받은 요청도 보낸 요청도 없고, 수락하거나 라이벌을 바꿔도 목록은 그대로입니다.
 * [refresh]는 부른 횟수만 셉니다.
 *
 * 요청을 수락하거나 라이벌을 고르면 목록이 바뀌어야 하는 테스트는 [com.ovalit.core.data.FakeFriendRepository]를 씁니다.
 */
class TestFriendRepository(
    friends: List<Friend> = emptyList(),
    rival: PlayerId? = null,
) : FriendRepository {

    override val friends: Flow<List<Friend>> = flowOf(friends)

    override val requests: Flow<List<FriendRequest>> = flowOf(emptyList())

    override val rival: Flow<PlayerId?> = flowOf(rival)

    override val sentRequests: Flow<Set<PlayerId>> = flowOf(emptySet())

    var refreshed = 0
        private set

    override suspend fun appUsersAmong(players: Collection<PlayerId>) = emptySet<PlayerId>()

    override suspend fun sendRequest(id: PlayerId) = Unit

    override suspend fun accept(id: PlayerId) = Unit

    override suspend fun decline(id: PlayerId) = Unit

    override suspend fun unfriend(id: PlayerId) = Unit

    override suspend fun setRival(id: PlayerId?) = Unit

    override suspend fun refresh() {
        refreshed++
    }

    override fun inviteLink() = ""
}
