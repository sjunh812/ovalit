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
 * 굵기 이름 그대로 그립니다(SemiBold 600, Bold 700). 한 단계 굵게(700, 800) 그렸더니 숫자가 뭉툭하게 두꺼워 보였습니다
 * (사용자 결정, 2026-10-03). [FontWeight.Bold]도 따로 등록합니다. 없으면 가장 가까운 SemiBold로 그려져서 스코어처럼
 * "굵게"를 달라고 한 자리가 SemiBold와 똑같이 보입니다.
 *
 * 결과를 기억해 둡니다. 매번 새 [FontFamily]를 돌려주면 이걸 받는 [OvalitTheme]이 글자 스타일을 화면 갱신마다 다시
 * 만듭니다.
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
