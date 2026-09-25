package com.ovalit.core.data

import com.ovalit.core.model.Account
import kotlinx.coroutines.flow.Flow

interface AccountRepository {

    /** 연동을 해제했으면 `null`입니다. */
    val account: Flow<Account?>

    /** 연동을 끊고 저장한 경기와 리포트를 모두 지웁니다. */
    suspend fun unlink()
}
