package com.ovalit.core.model

/**
 * 새로 받을 경기가 이만큼 이상이면 홈과 경기 탭 맨 위에 진행 줄을 띄웁니다.
 * 적으면 몇 초 안에 끝나서 당김 표시만 돌다 맙니다.
 * 시작 기준선입니다.
 */
const val NEW_MATCHES_SHOWN_FROM = 5

/**
 * 받는 중에 앱이 가려졌을 때 남은 경기가 이만큼 이상이면 WorkManager에 넘깁니다. 앱을 닫아도 이어 받고 다 받으면 알립니다.
 * 적으면 몇 초 안에 끝나서 알림이 오히려 귀찮습니다.
 * 시작 기준선입니다.
 */
const val NEW_MATCHES_IN_BACKGROUND_FROM = 20

/**
 * 첫 수집 뒤에 새로 끝난 경기를 받는 중 어디까지 왔는지입니다. 최신 경기부터 받습니다.
 *
 * @property total 이번에 받을 경기 수입니다. 경기 ID 목록을 받은 뒤에 정해집니다.
 */
data class NewMatchesProgress(
    val total: Int,
    val received: Int,
) {
    val remaining: Int get() = total - received

    val isShown: Boolean get() = total >= NEW_MATCHES_SHOWN_FROM
}
