package com.ovalit.core.data

import com.ovalit.core.model.Match
import com.ovalit.core.model.MatchId
import com.ovalit.core.model.MatchListEntry
import com.ovalit.core.model.OvalitError
import com.ovalit.core.model.OvalitException

/**
 * 내 경기를 주는 서버입니다.
 * 우리 서버가 넘긴 Riot 응답을 매퍼가 `core/model` 타입으로 옮겨 돌려줍니다.
 * 무엇을 언제 받을지는 [OfflineFirstMatchRepository]가 정하고, 여기는 부른 것만 그대로 받아 옵니다.
 *
 * 실패하면 까닭을 [OvalitException]으로 실어 던집니다. Riot 429는 [OvalitError.RiotBusy]입니다.
 */
interface MatchRemoteSource {

    /** 내 경기 ID 목록입니다. 순서는 상관없고 커스텀 게임도 거르지 않고 줍니다. */
    suspend fun matchList(): List<MatchListEntry>

    /** 경기 상세입니다. 상세를 보고서야 커스텀 게임인 걸 알았거나 Riot에 없는 경기면 `null`입니다. */
    suspend fun match(id: MatchId): Match?
}
