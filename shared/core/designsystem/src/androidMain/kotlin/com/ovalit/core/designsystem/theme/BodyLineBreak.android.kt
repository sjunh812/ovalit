package com.ovalit.core.designsystem.theme

import androidx.compose.ui.text.style.LineBreak

// 줄을 끝까지 채우되 어절 가운데서는 끊지 않는다. 웹의 keep-all과 같은 모양이다.
// 어절 단위로 끊는 건 안드로이드 13부터 되고, 그 아래에서는 기본 규칙으로 돌아간다.
internal actual val BodyLineBreak: LineBreak = LineBreak(
    strategy = LineBreak.Strategy.HighQuality,
    strictness = LineBreak.Strictness.Normal,
    wordBreak = LineBreak.WordBreak.Phrase,
)
