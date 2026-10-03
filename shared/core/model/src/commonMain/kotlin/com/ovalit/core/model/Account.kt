package com.ovalit.core.model

import kotlinx.datetime.LocalDate

/**
 * 연동한 Riot 계정입니다. [riotId]는 `이름#태그` 모양입니다.
 *
 * @property id 내 PUUID입니다. 화면에 띄우지 않고, 서버가 내려준 ㅇㅂㅇ에서 내가 보낸 것인지 가릴 때 씁니다.
 */
data class Account(
    val id: PlayerId,
    val riotId: String,
    val linkedOn: LocalDate,
)
