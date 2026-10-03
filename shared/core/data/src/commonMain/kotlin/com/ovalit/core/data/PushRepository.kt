package com.ovalit.core.data

/** 이 기기로 알림을 받을 FCM 토큰을 서버에 맡깁니다. 서버는 ㅇㅂㅇ을 이 토큰으로 보냅니다. */
interface PushRepository {

    suspend fun register(token: String)

    suspend fun unregister(token: String)
}

/** 서버가 붙기 전까지 쓰는 가짜입니다. 받은 토큰을 들고만 있습니다. */
class FakePushRepository : PushRepository {

    var token: String? = null
        private set

    override suspend fun register(token: String) {
        this.token = token
    }

    override suspend fun unregister(token: String) {
        if (this.token == token) this.token = null
    }
}
