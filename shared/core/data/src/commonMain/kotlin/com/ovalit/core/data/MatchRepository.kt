package com.ovalit.core.data

import com.ovalit.core.model.ImportProgress
import com.ovalit.core.model.Match
import com.ovalit.core.model.forFirstImport
import kotlinx.coroutines.flow.Flow

interface MatchRepository {

    /** 저장해 둔 내 경기 전부입니다. 새 경기를 받으면 목록 전체를 다시 내보냅니다. */
    fun observeMatches(): Flow<List<Match>>

    /** S0-4 첫 수집이 어디까지 왔는지입니다. 수집을 시작하기 전이면 `null`입니다. */
    val importProgress: Flow<ImportProgress?>

    /**
     * 연동 직후 첫 수집입니다. 최근 50경기를 받되 8주를 넘는 경기는 잘라냅니다([forFirstImport]).
     * 받는 대로 [observeMatches]에 채웁니다. 끝난 경기는 결과가 바뀌지 않아 이미 받은 경기는 다시 요청하지 않습니다.
     */
    suspend fun importRecent()

    /** 기기에 저장한 경기를 모두 지웁니다. Riot 계정 연동은 그대로 둡니다. */
    suspend fun deleteAll()
}
