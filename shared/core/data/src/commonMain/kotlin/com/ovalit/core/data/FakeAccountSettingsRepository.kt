package com.ovalit.core.data

import com.ovalit.core.model.AccountSettings
import com.ovalit.core.model.OvalitError
import com.ovalit.core.model.OvalitException
import com.ovalit.core.model.PingReminder
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow

private val ANSWER_DELAY = 300.milliseconds

/**
 * 서버가 붙기 전까지 쓰는 가짜 계정 설정입니다.
 * 서버 대신 메모리에 두고 [latency]만큼 기다렸다가 답합니다.
 * 먼저 바꿔 보여 주고 실패하면 되돌리는 흐름은 실제 저장소와 같은 [AccountSettingsSync]가 맡습니다.
 *
 * @param latency 서버에 보내고 답을 받기까지 걸리는 시간입니다. 테스트는 가상 시간으로 넘깁니다.
 */
class FakeAccountSettingsRepository(
    private val latency: Duration = ANSWER_DELAY,
) : AccountSettingsRepository {

    // 서버의 users 줄 대신이다
    private var stored = AccountSettings.Default

    // 연동하거나 해제할 때마다 늘린다. 그 전에 보낸 요청은 지운 계정의 것이다.
    private var linkedAccount = 0

    /** `null`이 아니면 서버를 부르는 일이 모두 이 까닭으로 실패합니다. 실패 안내를 시험할 때 씁니다. */
    var failure: OvalitError? = null

    private val sync = AccountSettingsSync(
        send = { patch -> answer { patch.applyTo(stored).also { stored = it } } },
        fetch = { answer { stored } },
    )

    override val settings: Flow<AccountSettings> = sync.settings

    override suspend fun setStatsPublic(public: Boolean) = sync.change(AccountSettingsPatch(statsPublic = public))

    override suspend fun setPingReminder(reminder: PingReminder) = sync.change(AccountSettingsPatch(pingReminder = reminder))

    override suspend fun refresh() = sync.refresh()

    /** 서버는 연동을 해제하면 계정 줄을 지우고 다시 연동하면 기본값으로 새로 만듭니다. 연동할 때와 해제할 때 부릅니다. */
    fun reset() {
        linkedAccount++
        stored = AccountSettings.Default
        sync.reset()
    }

    private suspend fun answer(read: () -> AccountSettings): AccountSettings {
        val sentBy = linkedAccount
        delay(latency)
        failure?.let { throw OvalitException(it) }
        // 연동을 해제한 뒤에 닿은 요청은 서버가 세션을 지워 401로 막는다
        if (linkedAccount != sentBy) throw OvalitException(OvalitError.SessionExpired)
        return read()
    }
}
