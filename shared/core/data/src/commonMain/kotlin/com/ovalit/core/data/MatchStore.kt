package com.ovalit.core.data

import com.ovalit.core.model.ImportProgress
import com.ovalit.core.model.Match
import com.ovalit.core.model.MatchId
import kotlin.time.Instant
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update

/**
 * 기기에 저장하는 내 경기와 받기 상태입니다.
 * 앱을 강제로 닫아도 다음에 남은 것만 이어 받으려면 경기 말고도 첫 수집 진행도와 멈춘 까닭, 마지막 확인 시각, 받다 남은 새 경기가 남아야 합니다.
 *
 * 로컬 DB는 아직 고르지 않아서 지금은 [InMemoryMatchStore]뿐입니다. DB를 고르면 이 인터페이스를 구현해 같은 자리에 넣습니다.
 * 읽기 흐름은 처음부터 저장된 값을 내보내야 합니다.
 * 기기에서 읽기 전에 빈 값을 먼저 내보내면 저장소가 첫 수집을 마치지 않았거나 남은 새 경기가 없는 것으로 압니다.
 */
interface MatchStore {
    val matches: Flow<List<Match>>

    val importProgress: Flow<ImportProgress?>

    val checkedAt: Flow<Instant?>

    val newMatches: Flow<NewMatchesBatch?>

    /** 끝난 경기는 바뀌지 않아 같은 ID가 이미 있으면 그대로 둡니다. 경기 목록은 ID를 키로 써서 같은 ID가 둘이면 화면이 죽습니다. */
    suspend fun save(match: Match)

    suspend fun setImportProgress(progress: ImportProgress?)

    suspend fun setCheckedAt(at: Instant?)

    suspend fun setNewMatches(batch: NewMatchesBatch?)

    /** 경기와 받기 상태를 모두 지웁니다. */
    suspend fun clear()
}

/**
 * 받고 있거나 받다 멈춘 새 경기입니다.
 *
 * @property total 이번에 받기 시작한 경기 수입니다. 멈췄다 이어 받아도 그대로라 진행 줄이 앞서 받은 데서 이어집니다.
 * @property left 아직 받지 못한 경기입니다. 최신 경기부터입니다.
 * @property stopped 받다 실패해 멈췄는지입니다. 멈춘 것은 다음에 새 경기를 받을 때 이어 받습니다.
 *   멈추지 않았는데 남아 있으면 받던 중에 앱이 꺼진 것이라 다시 켜자마자 이어 받습니다.
 */
data class NewMatchesBatch(
    val total: Int,
    val left: List<MatchId>,
    val stopped: Boolean = false,
)

/** 메모리에만 두는 [MatchStore]입니다. 앱을 다시 켜면 처음부터 시작합니다. */
class InMemoryMatchStore(
    matches: List<Match> = emptyList(),
    importProgress: ImportProgress? = null,
    checkedAt: Instant? = null,
    newMatches: NewMatchesBatch? = null,
) : MatchStore {
    private val savedMatches = MutableStateFlow(matches)
    private val progress = MutableStateFlow(importProgress)
    private val checked = MutableStateFlow(checkedAt)
    private val batch = MutableStateFlow(newMatches)

    override val matches: Flow<List<Match>> = savedMatches

    override val importProgress: Flow<ImportProgress?> = progress

    override val checkedAt: Flow<Instant?> = checked

    override val newMatches: Flow<NewMatchesBatch?> = batch

    override suspend fun save(match: Match) {
        savedMatches.update { saved -> if (saved.any { it.id == match.id }) saved else saved + match }
    }

    override suspend fun setImportProgress(progress: ImportProgress?) {
        this.progress.value = progress
    }

    override suspend fun setCheckedAt(at: Instant?) {
        checked.value = at
    }

    override suspend fun setNewMatches(batch: NewMatchesBatch?) {
        this.batch.value = batch
    }

    override suspend fun clear() {
        checked.value = null
        savedMatches.value = emptyList()
        progress.value = null
        batch.value = null
    }
}
