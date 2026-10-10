package com.ovalit.core.data

import com.ovalit.core.model.AccountSettings
import com.ovalit.core.model.PingReminder
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * `PATCH /me`의 본문입니다.
 * 서버는 보낸 값만 바꾸니 그대로 둘 값은 `null`입니다.
 * [pingReminder]는 [PingReminder.minutes]로 `remindBefore`에 담습니다.
 */
internal data class AccountSettingsPatch(
    val statsPublic: Boolean? = null,
    val pingReminder: PingReminder? = null,
) {
    fun applyTo(settings: AccountSettings) = AccountSettings(
        statsPublic = statsPublic ?: settings.statsPublic,
        pingReminder = pingReminder ?: settings.pingReminder,
    )
}

/**
 * 계정 설정을 먼저 바꿔 보여 주고 서버에는 하나씩 보냅니다. 가짜와 실제 저장소가 같이 씁니다.
 *
 * 보여 주는 값은 서버가 마지막으로 돌려준 값에 아직 답을 못 받은 변경을 바꾼 순서대로 얹은 것입니다.
 * 실패한 변경은 빼고 다시 얹으니 그 값만 돌아가고 그 뒤에 바꾼 다른 값은 남습니다.
 *
 * @param send `PATCH /me`입니다. 서버가 바꾼 뒤의 값을 돌려줍니다.
 *   실패하면 [com.ovalit.core.model.OvalitException]을 던집니다.
 * @param fetch `GET /me`입니다.
 */
internal class AccountSettingsSync(
    private val send: suspend (AccountSettingsPatch) -> AccountSettings,
    private val fetch: suspend () -> AccountSettings,
) {
    private val state = MutableStateFlow(State(confirmed = AccountSettings.Default))

    // 동시에 보내면 늦게 보낸 값이 먼저 닿아 서버에 옛 값이 남을 수 있다.
    // 다시 받기도 이 순서를 따른다. 바꾸기 전에 읽은 답이 늦게 오면 방금 바꾼 값을 옛 값으로 덮는다.
    private val oneByOne = Mutex()

    val settings: Flow<AccountSettings> = state.map { it.shown }.distinctUntilChanged()

    suspend fun change(patch: AccountSettingsPatch) {
        state.update { it.copy(pending = it.pending + patch) }
        oneByOne.withLock {
            // 기다리는 사이 연동을 해제해 비웠으면 보내지 않는다
            if (patch !in state.value.pending) return
            var answer: AccountSettings? = null
            try {
                answer = send(patch)
            } finally {
                // 실패하거나 취소돼도 뺀다. 남겨 두면 서버에 없는 값이 계속 보인다.
                state.update { now ->
                    if (patch in now.pending) State(confirmed = answer ?: now.confirmed, pending = now.pending - patch) else now
                }
            }
        }
    }

    suspend fun refresh() {
        oneByOne.withLock {
            val answer = fetch()
            state.update { it.copy(confirmed = answer) }
        }
    }

    /** 처음 값으로 돌아갑니다. 답을 기다리던 변경도 버립니다. */
    fun reset() {
        state.value = State(confirmed = AccountSettings.Default)
    }

    private data class State(
        val confirmed: AccountSettings,
        val pending: List<AccountSettingsPatch> = emptyList(),
    ) {
        val shown: AccountSettings get() = pending.fold(confirmed) { settings, patch -> patch.applyTo(settings) }
    }
}
