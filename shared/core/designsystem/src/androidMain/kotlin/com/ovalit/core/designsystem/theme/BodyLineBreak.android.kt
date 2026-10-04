package com.ovalit.core.designsystem.theme

import androidx.compose.ui.text.style.LineBreak

// 줄을 끝까지 채우되 어절 가운데서는 끊지 않는다(웹의 keep-all).
// 어절 단위 끊기는 안드로이드 13부터 되고, 그 아래에서는 기본 규칙을 따른다.
internal actual val BodyLineBreak: LineBreak = LineBreak(
    strategy = LineBreak.Strategy.HighQuality,
    strictness = LineBreak.Strictness.Normal,
    wordBreak = LineBreak.WordBreak.Phrase,
)
