package com.ovalit.core.data

import com.ovalit.core.model.AccountSettings
import com.ovalit.core.model.PingReminder
import kotlinx.coroutines.flow.Flow

/**
 * 서버가 들고 있는 계정 설정입니다(`GET /me`, `PATCH /me`). 기기 설정([UserPreferencesRepository])과 달리 서버가 이 값을 보고
 * 친구에게 전적을 내줄지, ㅇㅂㅇ 시작 전 알림을 언제 보낼지 정합니다.
 *
 * 바꾸면 서버 답을 기다리지 않고 [settings]로 먼저 내보내서 스위치가 바로 움직입니다. 서버에는 바꾼 순서대로 하나씩 보내고,
 * 실패하면 그 값만 되돌린 뒤 까닭을 담은 [com.ovalit.core.model.OvalitException]을 던집니다.
 */
interface AccountSettingsRepository {

    /** 서버 답을 기다리지 않고 바로 내보냅니다. 설정 화면이 이 값을 기다리느라 비어 있으면 안 됩니다. */
    val settings: Flow<AccountSettings>

    suspend fun setStatsPublic(public: Boolean)

    suspend fun setPingReminder(reminder: PingReminder)

    /** 서버 값을 다시 받습니다. 앱을 다시 깔고 연동했거나 다른 기기에서 바꿨으면 기기가 아는 값과 다릅니다. */
    suspend fun refresh()
}
