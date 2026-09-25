package com.ovalit.core.designsystem.component

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Indication
import androidx.compose.foundation.IndicationNodeFactory
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.node.DelegatableNode
import androidx.compose.ui.node.DrawModifierNode
import androidx.compose.ui.node.invalidateDraw
import androidx.compose.ui.unit.dp
import com.ovalit.core.designsystem.theme.OvalitTheme
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlin.math.max
import kotlin.math.min

private const val PRESS_MILLIS = 80
private const val RELEASE_MILLIS = 180
private const val HIGHLIGHT_ALPHA = 0.12f

// 작은 버튼은 4%까지 줄이고, 화면 폭만 한 줄은 양옆이 4dp씩만 들어오게 줄인다. 같은 비율로 줄이면 긴 줄이
// 눈에 띄게 오그라든다.
private const val MAX_SHRINK_RATIO = 0.04f
private val MaxShrink = 8.dp

/**
 * 누른 면의 기본 모양입니다. 네모 그대로나 원은 쓰지 않고 모서리를 적당히 둥글린 사각형으로 깝니다. 화면 폭만 한 줄도
 * 누르면 양옆이 들어오면서 이 모양이 됩니다.
 */
val OvalitPressShape: Shape = RoundedCornerShape(12.dp)

/**
 * 누르는 즉시 옅은 면을 깔고 누른 것을 살짝 줄입니다. 손을 떼면 제 크기로 돌아옵니다.
 *
 * 물결은 쓰지 않습니다. 누른 뒤에 천천히 퍼져서 앱이 늦게 반응하는 것처럼 느껴집니다.
 * 면은 [shape] 모양으로 깝니다. 기본은 [OvalitPressShape]이고, 눌리는 영역보다 보이는 모양이 작으면 보이는 쪽에 답니다.
 */
@Stable
class OvalitPressIndication(
    private val color: Color,
    private val shape: Shape = OvalitPressShape,
) : IndicationNodeFactory {
    override fun create(interactionSource: InteractionSource): DelegatableNode =
        PressNode(interactionSource, color, shape)

    override fun equals(other: Any?): Boolean =
        other is OvalitPressIndication && other.color == color && other.shape == shape

    override fun hashCode(): Int = 31 * color.hashCode() + shape.hashCode()
}

/** 버튼처럼 제 모양이 따로 있는 곳에 씁니다. 이때는 `clip`보다 앞에 둡니다. */
@Composable
fun pressIndication(shape: Shape = OvalitPressShape, color: Color = OvalitTheme.colors.t2): Indication =
    remember(shape, color) { OvalitPressIndication(color, shape) }

private class PressNode(
    private val interactionSource: InteractionSource,
    private val color: Color,
    private val shape: Shape,
) : Modifier.Node(), DrawModifierNode {
    private var pressed = Animatable(0f)
    private var pressing: Job? = null
    private var releasing: Job? = null

    // 목록에서 줄이 화면 밖으로 나가면 애니메이션이 도중에 끊긴다. 다시 쓰일 때 줄어든 채로 나오지 않게 되돌린다.
    override fun onDetach() {
        pressed = Animatable(0f)
    }

    override fun onAttach() {
        coroutineScope.launch {
            val presses = mutableListOf<PressInteraction.Press>()
            interactionSource.interactions.collect { interaction ->
                when (interaction) {
                    is PressInteraction.Press -> presses += interaction
                    is PressInteraction.Release -> presses -= interaction.press
                    is PressInteraction.Cancel -> presses -= interaction.press
                    else -> return@collect
                }
                if (presses.isNotEmpty()) press() else release()
            }
        }
    }

    private fun press() {
        releasing?.cancel()
        pressing = coroutineScope.launch {
            pressed.animateTo(1f, tween(PRESS_MILLIS)) { invalidateDraw() }
        }
    }

    private fun release() {
        val pressedJob = pressing
        releasing = coroutineScope.launch {
            // 톡 치고 바로 떼면 누른 모습이 한 번도 안 보인다. 다 눌린 뒤에 돌아오게 한다.
            pressedJob?.join()
            pressed.animateTo(0f, tween(RELEASE_MILLIS)) { invalidateDraw() }
        }
    }

    override fun ContentDrawScope.draw() {
        val amount = pressed.value
        if (amount == 0f) {
            drawContent()
            return
        }
        val shrink = min(MAX_SHRINK_RATIO, MaxShrink.toPx() / max(size.width, size.height))
        scale(1f - shrink * amount, pivot = center) {
            this@draw.drawContent()
            drawOutline(
                outline = shape.createOutline(size, layoutDirection, this),
                color = color,
                alpha = HIGHLIGHT_ALPHA * amount,
            )
        }
    }
}
