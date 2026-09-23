package com.ovalit.core.designsystem.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import com.ovalit.core.designsystem.resources.Res
import com.ovalit.core.designsystem.resources.pretendard_variable
import org.jetbrains.compose.resources.Font

/**
 * Pretendard Variable 한 벌입니다.
 *
 * 파일은 하나인데 굵기마다 따로 등록합니다. 가변 폰트는 `wght` 축 값을 같이 넘겨야 그
 * 굵기로 그려집니다. 안 넘기면 전부 기본 굵기로 나오고, 굵게 써야 할 자리는 시스템이
 * 억지로 두껍게 흉내 냅니다.
 *
 * 결과를 기억해 둡니다. 매번 새 [FontFamily]를 돌려주면 이걸 받는 [OvalitTheme]이 글자
 * 스타일 아홉 개를 화면 갱신마다 다시 만듭니다.
 */
@Composable
fun ovalitFontFamily(): FontFamily {
    val regular = Font(
        Res.font.pretendard_variable,
        weight = FontWeight.Normal,
        variationSettings = FontVariation.Settings(FontVariation.weight(400)),
    )
    val medium = Font(
        Res.font.pretendard_variable,
        weight = FontWeight.Medium,
        variationSettings = FontVariation.Settings(FontVariation.weight(500)),
    )
    val semiBold = Font(
        Res.font.pretendard_variable,
        weight = FontWeight.SemiBold,
        variationSettings = FontVariation.Settings(FontVariation.weight(600)),
    )

    return remember(regular, medium, semiBold) { FontFamily(regular, medium, semiBold) }
}
