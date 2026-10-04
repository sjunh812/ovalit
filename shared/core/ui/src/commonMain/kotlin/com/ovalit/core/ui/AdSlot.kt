package com.ovalit.core.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf

/**
 * 광고를 두는 자리입니다. 오래 머물며 내려 보는 곳 사이사이에만 둡니다(CLAUDE.md 화면). 앱을 열거나 닫을 때, 탭바 옆, 온보딩과
 * 첫 수집, 친구·ㅇㅂㅇ 흐름, 설정, 바텀시트에는 두지 않습니다.
 */
enum class AdPlacement {
    /** S2 경기 목록의 날짜 묶음 사이입니다. */
    MATCH_LIST,

    /** 홈의 이번 주 무기 카드 밑입니다. */
    HOME,

    /** S3 경기 상세 맨 아래입니다. */
    MATCH_DETAIL,
}

/**
 * 광고를 받아 그리는 쪽입니다. 앱 모듈이 AdMob으로 채웁니다. 광고를 받기 전이나 못 받았으면 아무것도 그리지 않습니다.
 *
 * [frame]은 받은 광고를 감쌀 틀입니다. 홈은 카드에, S3는 위에 선을 긋고 담습니다. 광고가 없을 때 빈 카드나 선만 남지 않게
 * 틀까지 광고를 받은 뒤에 그립니다.
 */
fun interface AdRenderer {
    @Composable
    fun Render(placement: AdPlacement, key: String, frame: @Composable (content: @Composable () -> Unit) -> Unit)
}

/** 광고를 그리는 쪽입니다. 없으면 광고 자리는 비어 있습니다. iOS와 UI 테스트가 그렇습니다. */
val LocalAdRenderer = staticCompositionLocalOf<AdRenderer?> { null }

/**
 * [placement]에 광고 하나를 둡니다. [key]는 같은 자리에 광고가 여럿일 때 가르는 이름이고, 같은 키는 스크롤해 다시 보여도
 * 같은 광고를 씁니다.
 */
@Composable
fun AdSlot(
    placement: AdPlacement,
    key: String = placement.name,
    frame: @Composable (content: @Composable () -> Unit) -> Unit = { it() },
) {
    LocalAdRenderer.current?.Render(placement, key, frame)
}
