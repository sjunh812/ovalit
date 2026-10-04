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
 * 파일은 하나지만 굵기마다 `wght` 축 값을 넘겨 따로 등록합니다. 안 넘기면 모두 기본 굵기로 나오고, 굵은 자리는 시스템이
 * 억지로 두껍게 흉내 냅니다.
 *
 * 굵기는 이름 그대로입니다(SemiBold 600, Bold 700). [FontWeight.Bold]도 꼭 등록합니다. 빠지면 Bold 자리가 가장 가까운
 * SemiBold로 그려져 구분이 안 됩니다.
 *
 * 결과를 기억해 둡니다. 매번 새 [FontFamily]를 돌려주면 [OvalitTheme]이 화면 갱신마다 글자 스타일을 다시 만듭니다.
 */
@Composable
fun ovalitFontFamily(): FontFamily {
    val regular = pretendard(FontWeight.Normal, axis = 400)
    val medium = pretendard(FontWeight.Medium, axis = 500)
    val semiBold = pretendard(FontWeight.SemiBold, axis = SEMI_BOLD_AXIS)
    val bold = pretendard(FontWeight.Bold, axis = BOLD_AXIS)

    return remember(regular, medium, semiBold, bold) { FontFamily(regular, medium, semiBold, bold) }
}

private const val SEMI_BOLD_AXIS = 600
private const val BOLD_AXIS = 700

@Composable
private fun pretendard(weight: FontWeight, axis: Int) = Font(
    Res.font.pretendard_variable,
    weight = weight,
    variationSettings = FontVariation.Settings(FontVariation.weight(axis)),
)
