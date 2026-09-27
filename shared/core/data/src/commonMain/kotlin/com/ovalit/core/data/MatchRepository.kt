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

    /**
     * 첫 수집 뒤에 새로 끝난 경기를 받습니다. 홈과 경기 탭을 당겨서 부릅니다. 끝난 경기는 결과가 바뀌지 않아
     * 저장해 둔 경기는 다시 받지 않고 목록에 없는 경기만 받습니다.
     *
     * 이미 받는 중에 또 부르면 새로 요청하지 않고 앞의 것이 끝날 때까지 기다렸다가 0을 돌려줍니다. 두 화면이 각자 부르므로
     * 막는 일은 구현이 맡습니다. Riot 레이트 리밋은 앱 전체에 걸려 있습니다.
     *
     * @return 이 호출로 새로 받은 경기 수입니다.
     */
    suspend fun refresh(): Int

    /** 기기에 저장한 경기와 첫 수집 진행도를 모두 지웁니다. Riot 계정 연동은 그대로 둡니다. */
    suspend fun deleteAll()
}
