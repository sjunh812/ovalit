package com.ovalit.core.data

import com.ovalit.core.model.Match
import kotlinx.coroutines.flow.Flow

interface MatchRepository {

    /** 저장해 둔 내 경기 전부입니다. 새 경기를 받아 오면 다시 흘려보냅니다. */
    fun observeMatches(): Flow<List<Match>>

    /** 기기에 저장한 경기를 모두 지웁니다. Riot 계정 연동은 그대로 둡니다. */
    suspend fun deleteAll()
}
