package com.ovalit.core.model

import kotlinx.datetime.LocalDate

/** 연동한 Riot 계정입니다. [riotId]는 `이름#태그` 모양입니다. */
data class Account(
    val riotId: String,
    val linkedOn: LocalDate,
)
