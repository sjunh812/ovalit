package com.ovalit.core.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf

/**
 * 광고를 두는 자리입니다.
 * 오래 머물며 내려 보는 곳 사이사이에만 둡니다(docs/screens.md).
 * 앱을 열거나 닫을 때, 탭바 옆, 온보딩과 첫 수집, ㅇㅂㅇ 초대 화면, 설정, 바텀시트에는 두지 않습니다.
 *
 * @property inCard 카드 안에 담는 자리입니다.
 *   카드에 위아래 안쪽 여백이 있어 광고 줄은 여백을 빼고 그립니다.
 */
enum class AdPlacement(val inCard: Boolean) {
    /** S2 경기 목록의 날짜 묶음 사이입니다. 자리는 [listAdBefore]가 정합니다. */
    MATCH_LIST(inCard = false),

    /** 친구 경기 목록의 날짜 묶음 사이입니다. 경기 목록과 같은 규칙입니다. */
    FRIEND_MATCH_LIST(inCard = false),

    /** 홈의 달라진 점 카드 밑입니다. 이번 주 숫자를 다 본 바로 뒤입니다. */
    HOME(inCard = true),

    /** 내 프로필과 친구 프로필의 무기 카드와 최근 경기 카드 사이입니다. */
    PROFILE(inCard = true),

    /** 친구 탭 맨 아래, 친구 목록 카드 밑입니다. 오발있?과 받은 요청 카드에는 버튼이 있어 그 사이에 두지 않습니다. */
    FRIENDS(inCard = true),

    /** S3 경기 상세 맨 아래입니다. */
    MATCH_DETAIL(inCard = false),
}

/**
 * 광고를 받아 그리는 쪽입니다.
 * 앱 모듈이 AdMob으로 구현합니다.
 * 광고를 받기 전이나 못 받았으면 아무것도 그리지 않습니다.
 *
 * [frame]은 받은 광고를 감쌀 틀입니다(홈은 카드, S3는 위에 그은 선).
 * 빈 카드나 선만 남지 않게 틀도 광고를 받은 뒤에 그립니다.
 */
interface AdRenderer {
    @Composable
    fun Render(placement: AdPlacement, key: String, frame: @Composable (content: @Composable () -> Unit) -> Unit)

    /** 보상형 광고로 "24시간 광고 없이 보기"를 고를 수 있는지입니다. 보상형 광고 단위 ID가 없으면 `false`입니다. */
    val canOfferAdFree: Boolean

    /** "광고 없이 볼까요?" 시트를 띄웁니다. 설정 줄이 씁니다. 광고 줄의 ×는 앱 모듈이 따로 받습니다. */
    fun offerAdFree()
}

/** `null`이면(iOS, UI 테스트) 광고 자리를 비웁니다. */
val LocalAdRenderer = staticCompositionLocalOf<AdRenderer?> { null }

/**
 * [placement]에 광고 하나를 둡니다.
 * [key]는 같은 자리에 광고가 여럿일 때 가르는 이름이고, 같은 키는 스크롤해 다시 보여도 같은 광고를 씁니다.
 */
@Composable
fun AdSlot(
    placement: AdPlacement,
    key: String = placement.name,
    frame: @Composable (content: @Composable () -> Unit) -> Unit = { it() },
) {
    LocalAdRenderer.current?.Render(placement, key, frame)
}

/**
 * 날짜로 묶은 경기 목록에서 광고를 둘 묶음의 번호입니다. 그 묶음 바로 앞에 광고 한 줄을 둡니다.
 * 한 날짜 안에 끼우면 그날 경기처럼 읽혀서 묶음 사이에만 둡니다.
 * [FIRST_LIST_AD_AFTER]줄을 지난 첫 묶음 앞에 하나, 그 뒤로는 [LIST_AD_EVERY]줄이 넘을 때마다 하나입니다.
 *
 * @param daySizes 날짜 묶음마다의 경기 수입니다. 위에서부터입니다.
 */
fun listAdBefore(daySizes: List<Int>): Set<Int> {
    val before = mutableSetOf<Int>()
    var rowsSinceAd = 0
    daySizes.forEachIndexed { index, size ->
        if (rowsSinceAd >= if (before.isEmpty()) FIRST_LIST_AD_AFTER else LIST_AD_EVERY) {
            before += index
            rowsSinceAd = 0
        }
        rowsSinceAd += size
    }
    return before
}

// 첫 광고가 첫 화면 안에 들어오게 세 줄 뒤에 둔다. 시작 기준선이다.
const val FIRST_LIST_AD_AFTER = 3
const val LIST_AD_EVERY = 8
