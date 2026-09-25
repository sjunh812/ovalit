package com.ovalit.core.ui

import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp

/**
 * 폭이 정해진 칸에서 글자를 키운 사용자에게 "전투점수"가 "전투"로 잘리지 않게, 줄을 바꾸는 대신 글자를
 * 줄입니다. `maxLines = 1`과 같이 씁니다. [min]도 sp라서 글자 크기 설정을 따라 같이 커집니다.
 */
fun shrinkToFit(size: TextUnit, min: TextUnit = 9.sp) = TextAutoSize.StepBased(minFontSize = min, maxFontSize = size)
