package com.ovalit.core.designsystem.theme

import androidx.compose.ui.unit.dp

object OvalitSpacing {
    val xs = 4.dp
    val sm = 8.dp
    val md = 12.dp
    val lg = 16.dp
    val xl = 24.dp
    val xxl = 32.dp

    /** 카드와 시트 안에서 글자를 면 가장자리에서 띄우는 여백입니다. */
    val gutter = 20.dp

    /**
     * 화면 가장자리에서 카드까지, 그리고 카드 밖에 바로 놓인 글자(상단 바, 칩, 목록)까지의 여백입니다.
     * 카드 테두리와 카드 밖 글자가 한 줄에 서도록 둘을 같게 둡니다(당근 SEED 화면 여백 16).
     */
    val screen = 16.dp
}
