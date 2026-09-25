package com.ovalit.core.designsystem.component

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.withStyle
import com.ovalit.core.designsystem.theme.OvalitTheme

private const val ROLL_MILLIS = 240

/**
 * 숫자가 바뀌면 바뀐 자리의 숫자만 굴러갑니다. "30%"가 "32%"가 되면 "0"만 "2"로 구르고 "3"과 "%"는 그대로 있습니다.
 * 커지면 아래에서 올라오고 작아지면 위에서 내려옵니다. 처음 그릴 때는 움직이지 않고, 기기에서 애니메이션을 끄면
 * 바로 바뀝니다.
 *
 * 큰 지표 숫자에만 씁니다. 옆에 붙는 변화량까지 움직이면 눈이 두 군데로 갈립니다.
 */
@Composable
fun OvalitRollingText(
    text: String,
    modifier: Modifier = Modifier,
    style: TextStyle = OvalitTheme.typography.body,
    color: Color = OvalitTheme.colors.t1,
    maxLines: Int = 1,
    autoSize: TextAutoSize? = null,
) {
    // 자리 잡기는 글자 하나로 하고 구르는 모습은 그리는 단계에서만 덧그린다. 배치까지 움직이면 기준선과 칸 폭,
    // 좁은 칸에서 글자를 줄이는 규칙이 애니메이션 도중에 흔들린다.
    val last = remember { LastText(text) }
    val roll = remember(text) { Roll.between(last.value, text) }
    val progress = remember(text) { Animatable(if (roll.slots.isEmpty()) 1f else 0f) }
    var rolling by remember(text) { mutableStateOf(roll.slots.isNotEmpty()) }
    var layout by remember { mutableStateOf<TextLayoutResult?>(null) }
    val measurer = rememberTextMeasurer()
    SideEffect { last.value = text }
    LaunchedEffect(text) {
        progress.animateTo(1f, tween(ROLL_MILLIS))
        rolling = false
    }

    BasicText(
        // 구르는 자리는 도는 동안 비워 두고 아래에서 옛 숫자와 새 숫자를 따로 그린다
        text = buildAnnotatedString {
            text.forEachIndexed { index, char ->
                if (rolling && index in roll.slots) {
                    withStyle(SpanStyle(color = Color.Transparent)) { append(char) }
                } else {
                    append(char)
                }
            }
        },
        modifier = modifier.drawWithContent {
            drawContent()
            val laidOut = layout ?: return@drawWithContent
            if (!rolling) return@drawWithContent
            // 굴러가는 글자를 한 글자씩 따로 재서 그리면 자간이 전체 글자와 달라서 1px쯤 옆으로 비껴 그려진다.
            // 다 구른 뒤 제자리 글자로 바뀌는 순간 움찔하므로, 전체 글자를 같은 조건으로 한 번 더 그려 칸만 잘라 보인다.
            val input = laidOut.layoutInput
            fun layoutOf(value: String) = measurer.measure(
                text = AnnotatedString(value),
                style = input.style.merge(color = color),
                overflow = input.overflow,
                softWrap = input.softWrap,
                maxLines = input.maxLines,
                constraints = input.constraints,
            )
            val after = layoutOf(text)
            val before = layoutOf(roll.before)
            val done = progress.value
            roll.slots.forEach { index ->
                val box = laidOut.getBoundingBox(index)
                val travel = box.height * roll.direction
                clipRect(left = box.left, top = box.top, right = box.right, bottom = box.bottom) {
                    roll.oldIndex[index]?.let { oldIndex ->
                        val oldBox = before.getBoundingBox(oldIndex)
                        drawText(
                            textLayoutResult = before,
                            topLeft = Offset(box.left - oldBox.left, box.top - oldBox.top - travel * done),
                            alpha = 1f - done,
                        )
                    }
                    drawText(textLayoutResult = after, topLeft = Offset(0f, travel * (1f - done)), alpha = done)
                }
            }
        },
        style = style.merge(color = color),
        maxLines = maxLines,
        autoSize = autoSize,
        onTextLayout = { layout = it },
    )
}

private class LastText(var value: String)

/**
 * @property slots 새 글자에서 바뀐 자리입니다. 숫자는 오른쪽 끝을 맞춰 비교해서 "9%"가 "10%"가 되면 "%"는
 * 그대로이고 "9" 자리와 새로 생긴 "1" 자리가 굴러갑니다.
 * @property oldIndex 바뀐 자리마다 옛 글자에서 같은 자리의 순서입니다. 새로 생긴 자리에는 없습니다.
 */
private class Roll(val before: String, val slots: Set<Int>, val oldIndex: Map<Int, Int>, val direction: Float) {
    companion object {
        fun between(before: String, after: String): Roll {
            if (before == after) return Roll(before, emptySet(), emptyMap(), 1f)
            val shift = after.length - before.length
            val oldIndex = mutableMapOf<Int, Int>()
            val slots = after.indices.filter { index ->
                val previous = index - shift
                if (previous in before.indices) oldIndex[index] = previous
                before.getOrNull(previous) != after[index]
            }.toSet()
            val rising = (numberIn(after) ?: 0.0) >= (numberIn(before) ?: 0.0)
            return Roll(before, slots, oldIndex.filterKeys { it in slots }, if (rising) 1f else -1f)
        }
    }
}

// "30%", "1.49"처럼 단위가 붙은 글자에서 숫자만 읽는다
private fun numberIn(text: String): Double? =
    text.replace('−', '-').filter { it.isDigit() || it == '.' || it == '-' }.toDoubleOrNull()
