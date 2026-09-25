package com.ovalit.core.data

import com.ovalit.core.model.Match
import kotlinx.coroutines.flow.Flow

interface MatchRepository {

    /** 저장해 둔 내 경기 전부입니다. 새 경기를 받아 오면 다시 흘려보냅니다. */
    fun observeMatches(): Flow<List<Match>>
}
