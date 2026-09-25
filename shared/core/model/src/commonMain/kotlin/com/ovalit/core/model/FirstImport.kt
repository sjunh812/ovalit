package com.ovalit.core.model

import kotlin.time.Duration.Companion.days
import kotlin.time.Instant

/** S0-4 첫 수집은 최근 50경기를 받되 8주를 넘는 경기는 잘라냅니다. 집계 최대 4주와 비교 4주를 합친 길이입니다. */
const val FIRST_IMPORT_MATCHES = 50
const val FIRST_IMPORT_WEEKS = 8

/** 경기 ID 목록에서 첫 수집에 받을 경기를 고릅니다. 최근 경기부터 받습니다. */
fun <T> Iterable<T>.forFirstImport(now: Instant, startedAt: (T) -> Instant): List<T> = this
    .filter { now - startedAt(it) <= (FIRST_IMPORT_WEEKS * 7).days }
    .sortedByDescending(startedAt)
    .take(FIRST_IMPORT_MATCHES)

/**
 * 첫 수집이 어디까지 왔는지입니다.
 *
 * @property results 받은 경기의 승패를 받은 순서대로 담습니다. S0-4 아래 막대를 이걸로 칠합니다.
 */
data class ImportProgress(
    val total: Int,
    val results: List<Boolean?>,
) {
    val loaded: Int get() = results.size

    val isDone: Boolean get() = loaded >= total
}
