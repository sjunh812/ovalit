package com.ovalit.core.data

import com.ovalit.core.model.ImportProgress
import com.ovalit.core.model.Match
import com.ovalit.core.model.NewMatchesProgress
import com.ovalit.core.model.forFirstImport
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull

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
     * 새로 끝난 경기를 받는 중이면 몇 판 중 몇 판을 받았는지입니다. 받는 중이 아니면 `null`입니다. 실제 저장소는 받다 끊긴 경기가
     * 남아 있으면 앱을 다시 켜도 값을 내보냅니다. WorkManager는 이 값이 있을 때만 이어 받습니다.
     */
    val newMatchesProgress: Flow<NewMatchesProgress?>

    /**
     * 첫 수집 뒤에 새로 끝난 경기를 받습니다. 홈과 경기 탭을 당기거나, 앱이 다시 보일 때 부릅니다. 끝난 경기는 결과가 바뀌지
     * 않아 저장해 둔 경기는 다시 받지 않고 목록에 없는 경기만 받습니다. 첫 수집처럼 8주를 넘는 경기는 받지 않습니다.
     *
     * 최신 경기부터 한 판씩 받아 바로 저장하고 [newMatchesProgress]를 올립니다. 중간에 끊기거나 Riot이 429를 주면 거기서 멈추고,
     * 다음에 부를 때 남은 경기만 받습니다. 부른 화면이 사라져도 받기는 끝까지 갑니다. 멈추는 건 [deleteAll]뿐입니다.
     *
     * 이미 받는 중에 또 부르면 새로 요청하지 않고 앞의 것이 끝날 때까지 기다렸다가 0을 돌려줍니다. 두 화면이 각자 부르므로
     * 막는 일은 구현이 맡습니다. Riot 레이트 리밋은 앱 전체에 걸려 있습니다.
     *
     * @return 이 호출로 새로 받은 경기 수입니다.
     */
    suspend fun refresh(): Int

    /** 기기에 저장한 경기와 첫 수집 진행도를 모두 지우고, 받던 새 경기도 멈춥니다. Riot 계정 연동은 그대로 둡니다. */
    suspend fun deleteAll()
}

/**
 * 숫자를 모아 세는 화면(홈, 내 프로필, S6, S7)이 쓰는 경기입니다. 새 경기를 받는 동안은 받기 전 경기를 그대로 두고, 다 받은 뒤에
 * 한 번 내보냅니다. 첫 수집처럼 받는 대로 숫자가 움직이면 믿을 수 없습니다. 경기 목록은 늘어나기만 해서 받는 대로 채웁니다.
 */
fun MatchRepository.settledMatches(): Flow<List<Match>> =
    combine(observeMatches(), newMatchesProgress) { matches, progress -> matches.takeIf { progress == null } }.filterNotNull()
