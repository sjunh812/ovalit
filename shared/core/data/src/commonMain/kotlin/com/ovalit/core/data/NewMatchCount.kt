package com.ovalit.core.data

import kotlinx.coroutines.flow.first

/**
 * [during]에 새 경기를 받고, 그사이 목록에 새로 들어온 경기 수를 돌려줍니다. 당긴 결과를 "새 경기 3판을 불러왔어요"로 알릴 때
 * 씁니다. 첫 수집을 마치지 않았으면(저장된 데이터를 지운 뒤) 새 경기를 받지 않아서 `null`입니다.
 *
 * [MatchRepository.refresh]가 돌려주는 수를 쓰지 않습니다. 앱이 다시 보여 받던 것에 붙으면 그사이 경기가 들어와도 0입니다.
 */
suspend fun MatchRepository.countNewMatches(during: suspend () -> Unit): Int? {
    val imported = importProgress.first()?.isDone == true
    val before = observeMatches().first().mapTo(HashSet()) { it.id }
    during()
    if (!imported) return null
    return observeMatches().first().count { it.id !in before }
}
