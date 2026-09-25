package com.ovalit.core.data

import com.ovalit.core.model.Friend
import com.ovalit.core.model.FriendRequest
import com.ovalit.core.model.PlayerId
import kotlinx.coroutines.flow.Flow

interface FriendRepository {
    val friends: Flow<List<Friend>>

    /** 나에게 온 요청입니다. 수락하기 전에는 상대 전적을 보여주지 않습니다. */
    val requests: Flow<List<FriendRequest>>

    /** S5에서 고른 라이벌입니다. 고르지 않았으면 `null`이고 홈에 라이벌 칸을 두지 않습니다. */
    val rival: Flow<PlayerId?>

    /** 내가 보내 놓고 아직 답을 못 받은 요청입니다. */
    val sentRequests: Flow<Set<PlayerId>>

    /**
     * [players] 중 우리 앱에 연동한 사람입니다. 연동하지 않은 사람에게는 요청을 보낼 수 없어서 스코어보드에서
     * 요청 대신 초대 링크를 권합니다.
     */
    suspend fun appUsersAmong(players: Collection<PlayerId>): Set<PlayerId>

    /** 같이 뛴 경기의 스코어보드에서 보내는 요청입니다. 상대가 수락하면 친구가 됩니다. */
    suspend fun sendRequest(id: PlayerId)

    suspend fun accept(id: PlayerId)

    suspend fun decline(id: PlayerId)

    /** 친구를 끊으면 라이벌에서도 빠집니다. */
    suspend fun unfriend(id: PlayerId)

    suspend fun setRival(id: PlayerId?)

    /** 받은 사람이 앱에서 열면 나에게 친구 요청이 옵니다. */
    fun inviteLink(): String
}
