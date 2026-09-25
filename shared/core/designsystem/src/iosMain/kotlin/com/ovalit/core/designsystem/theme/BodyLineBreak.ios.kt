package com.ovalit.core.designsystem.theme

import androidx.compose.ui.text.style.LineBreak

// iOS에는 조합을 고르는 생성자가 없어서 정해 둔 값 중 가장 가까운 것을 쓴다. 출시는 안드로이드뿐이다.
internal actual val BodyLineBreak: LineBreak = LineBreak.Paragraph
