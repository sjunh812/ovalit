package com.ovalit.core.testing

import com.ovalit.core.data.AccountRepository
import com.ovalit.core.model.Account
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

/** 연동하지 않은 계정입니다. 해제해도 아무 일이 없습니다. */
object NoAccount : AccountRepository {
    override val account: Flow<Account?> = flowOf(null)

    override suspend fun unlink() = Unit
}
