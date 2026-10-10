package com.ovalit.core.model

/**
 * ㅇㅂㅇ 시작 몇 분 전에 알릴지입니다. 알림은 서버 크론이 보내서 값도 서버(`users.remind_before`)에 둡니다.
 *
 * [minutes]가 `GET`·`PATCH /me`의 `remindBefore`입니다. 서버는 0, 10, 30, 60만 받고 0이면 알리지 않습니다.
 */
enum class PingReminder(val minutes: Int) {
    TEN_MINUTES(10),
    THIRTY_MINUTES(30),
    ONE_HOUR(60),
    OFF(0),
    ;

    companion object {
        /** 서버가 준 `remindBefore`를 옮깁니다. 서버가 새로 받기 시작한 값처럼 모르는 값이면 `null`입니다. */
        fun fromMinutes(minutes: Int): PingReminder? = entries.firstOrNull { it.minutes == minutes }
    }
}

/**
 * 서버가 들고 있는 계정 설정입니다.
 * 서버가 이 값을 보고 친구에게 전적을 내줄지, ㅇㅂㅇ 시작 전 알림을 언제 보낼지 정해서 기기에 두지 않습니다.
 * 기기에만 두면 전적 공개를 꺼도 서버가 몰라 친구에게 그대로 보입니다.
 * 기기에만 두는 설정은 [UserPreferences]에 있습니다.
 *
 * @property statsPublic `/me`의 `statsPublic`입니다. 끄면 친구도 내 프로필과 경기를 볼 수 없습니다. 친구 비교에서도 빠집니다.
 * @property pingReminder `/me`의 `remindBefore`입니다. 내가 부르거나 간다고 답한 ㅇㅂㅇ을 시작 몇 분 전에 알릴지입니다.
 */
data class AccountSettings(
    val statsPublic: Boolean,
    val pingReminder: PingReminder,
) {
    companion object {
        /** 서버가 새 계정에 넣는 값과 같습니다(`users`의 기본값). */
        val Default = AccountSettings(statsPublic = true, pingReminder = PingReminder.TEN_MINUTES)
    }
}
