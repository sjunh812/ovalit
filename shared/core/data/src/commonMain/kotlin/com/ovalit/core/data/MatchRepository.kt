package com.ovalit.core.data

import com.ovalit.core.model.ImportProgress
import com.ovalit.core.model.Match
import com.ovalit.core.model.MatchId
import com.ovalit.core.model.NewMatchesProgress
import com.ovalit.core.model.forFirstImport
import kotlin.time.Instant
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

interface MatchRepository {

    /** 저장해 둔 내 경기 전부입니다. 새 경기를 받으면 목록 전체를 다시 내보냅니다. */
    fun observeMatches(): Flow<List<Match>>

    /** S0-4 첫 수집이 어디까지 왔는지입니다. 수집을 시작하기 전이면 `null`입니다. */
    val importProgress: Flow<ImportProgress?>

    /**
     * 연동 직후 첫 수집입니다.
     * 최근 50경기를 받되 8주를 넘는 경기와 커스텀 게임은 잘라냅니다([forFirstImport]).
     * 받는 대로 [observeMatches]에 채웁니다.
     * 끝난 경기는 결과가 바뀌지 않아 이미 받은 경기는 다시 요청하지 않습니다.
     *
     * 받다 실패하면 [importProgress]의 `stoppedBy`에 까닭을 적고 [com.ovalit.core.model.OvalitException]으로 던집니다.
     * 적지 않으면 S0-4가 멈춘 까닭과 다시 시도 버튼을 띄우지 못하고 "불러오는 중"에 머뭅니다.
     * 다시 부르면 남은 것만 받습니다.
     * [deleteAll]을 부른 뒤에는 받던 경기를 더 저장하지 않습니다.
     *
     * 이미 끝났으면 아무것도 하지 않습니다.
     * 둘이 함께 부르면 뒤에 온 쪽은 앞의 것이 끝날 때까지 기다립니다.
     */
    suspend fun importRecent()

    /**
     * 마지막으로 경기 ID 목록을 받은 시각입니다.
     * 첫 수집이나 [refresh]가 목록을 받으면 바뀌고 앱이 다시 보일 때 이 시각으로 다시 확인할지 정합니다([NewMatchesWatcher]).
     * 당겨서 받은 것도 확인으로 쳐야 해서 저장소가 듭니다.
     * 목록을 받지 못하면 그대로라 다음에 다시 확인합니다.
     * 기기에 저장해([MatchStore]) 앱을 다시 켜도 이어 갑니다.
     */
    val checkedAt: Flow<Instant?>

    /**
     * 새로 끝난 경기를 받는 중이면 몇 판 중 몇 판을 받았는지입니다.
     * 받는 중이 아니면 `null`이고 받다 실패해 멈춰도 `null`입니다.
     * 받던 중에 앱이 꺼졌으면 다시 켤 때 값을 내보내고 남은 것을 이어 받습니다.
     * 남은 경기가 있는지는 멈춘 것까지 치는 [hasPendingNewMatches]로 봅니다.
     */
    val newMatchesProgress: Flow<NewMatchesProgress?>

    /**
     * 받다 남은 새 경기가 있는지입니다.
     * 받는 중이든 받다 실패해 멈췄든 남았으면 `true`입니다.
     * [newMatchesProgress]는 멈추면 `null`이라 그걸로는 남았는지 알 수 없습니다.
     * WorkManager가 이어 받을지 정할 때 씁니다.
     * 멈춘 것을 남지 않은 것으로 보면 이어 받지 않고 다 받았다고 알립니다.
     *
     * 기본 구현은 받는 중일 때만 `true`라 멈춘 것을 모릅니다.
     * 받다 멈출 수 있는 저장소는 다시 구현합니다.
     */
    val hasPendingNewMatches: Flow<Boolean>
        get() = newMatchesProgress.map { it != null }.distinctUntilChanged()

    /**
     * 지금 받고 있는 새 경기입니다.
     * 받는 중이 아니면 비어 있고 받다 멈춰도 비어 있습니다.
     * [settledMatches]가 다 받을 때까지 이 경기를 빼고 내보냅니다.
     *
     * 기본 구현은 늘 비어 있어 [settledMatches]가 받는 대로 내보냅니다.
     * 새 경기를 받는 저장소는 다시 구현합니다.
     */
    val receivingMatches: Flow<Set<MatchId>>
        get() = flowOf(emptySet())

    /**
     * 첫 수집 뒤에 새로 끝난 경기를 받습니다.
     * 홈과 경기 탭을 당기거나 앱이 다시 보일 때 부릅니다.
     * 첫 수집이 끝나지 않았으면 받지 않고 0을 돌려줍니다.
     * 그때 받으면 50판 제한 없이 8주치를 다 받고 첫 수집과 같은 경기를 두 번 받습니다.
     * 끝난 경기는 결과가 바뀌지 않아 저장해 둔 경기는 다시 받지 않고 목록에 없는 경기만 받습니다.
     * 첫 수집처럼 8주를 넘는 경기와 커스텀 게임은 받지 않고 첫 수집이 50판에서 잘라낸 경기도 받지 않습니다.
     * 상세를 받아 봤지만 저장하지 않은 경기도 다시 받지 않고 끝나지 않았던 경기만 한 번 더 받아 봅니다.
     *
     * 최신 경기부터 한 판씩 받아 바로 저장하고 [newMatchesProgress]를 올립니다.
     * 중간에 끊기거나 Riot이 429를 주면 거기서 멈추고 까닭을 [com.ovalit.core.model.OvalitException]으로 던집니다.
     * 다음에 부를 때는 목록을 다시 받지 않고 남은 경기만 받습니다.
     * 그사이 8주 밖으로 나간 경기는 빼고 남은 게 없으면 목록을 새로 받습니다.
     * 부른 화면이 사라져도 받기는 끝까지 갑니다.
     * 멈추는 건 [deleteAll]뿐입니다.
     *
     * 이미 받는 중에 또 부르면 새로 요청하지 않고 앞의 것이 끝날 때까지 기다렸다가 0을 돌려줍니다.
     * 앞의 것이 실패하면 같이 던집니다.
     * 두 화면이 각자 부르므로 막는 일은 구현이 맡습니다.
     * Riot 레이트 리밋은 앱 전체에 걸려 있습니다.
     *
     * @return 이 호출로 새로 받은 경기 수입니다.
     */
    suspend fun refresh(): Int

    /** 기기에 저장한 경기와 첫 수집 진행도를 모두 지우고 받던 새 경기도 멈춥니다. Riot 계정 연동은 그대로 둡니다. */
    suspend fun deleteAll()
}

/**
 * 숫자를 모아 세는 화면(홈, 내 프로필, S6, S7, 친구 순위)이 쓰는 경기입니다.
 * 새 경기를 받는 동안은 받고 있는 경기([MatchRepository.receivingMatches])를 뺀 받기 전 경기를 내보내고, 다 받은 뒤에 한 번 더 내보냅니다.
 * 첫 수집처럼 받는 대로 숫자가 움직이면 믿을 수 없습니다.
 * 받는 중에 연 화면도 받기 전 경기로 바로 숫자를 띄웁니다.
 * 다 받을 때까지 아무것도 내보내지 않으면 그동안 스켈레톤만 뜹니다.
 * 한 판씩 저장될 때마다 같은 목록이 다시 오지만 내보내지 않아 화면이 다시 세지 않습니다.
 * 경기 목록은 늘어나기만 해서 받는 대로 채웁니다.
 */
fun MatchRepository.settledMatches(): Flow<List<Match>> =
    combine(observeMatches(), receivingMatches) { matches, receiving ->
        if (receiving.isEmpty()) matches else matches.filterNot { it.id in receiving }
    }.distinctUntilChanged()
