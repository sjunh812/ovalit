package com.ovalit.core.designsystem.theme

import androidx.compose.ui.graphics.Color
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cbrt
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sqrt
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * 색 토큰이 지켜야 할 대비를 검사합니다. 눈으로는 다크에서 통과한 조합이 라이트에서
 * 무너지는 걸 잘 못 잡습니다.
 */
class OvalitColorsTest {

    @Test
    fun `본문 글자색은 배경 대비 4_5 대 1을 넘는다`() {
        forEachTheme { name, colors ->
            assertContrast(name, "t1", colors.t1, colors.bg, atLeast = 4.5)
            assertContrast(name, "t2", colors.t2, colors.bg, atLeast = 4.5)
        }
    }

    @Test
    fun `보조 글자색은 배경 대비 3 대 1을 넘는다`() {
        forEachTheme { name, colors ->
            assertContrast(name, "t3", colors.t3, colors.bg, atLeast = 3.0)
        }
    }

    // 좋아짐과 나빠짐을 색으로만 구분하므로 여기서 대비가 모자라면 변화를 못 읽는다.
    @Test
    fun `변화 색은 배경 대비 3 대 1을 넘는다`() {
        forEachTheme { name, colors ->
            assertContrast(name, "pos", colors.pos, colors.bg, atLeast = 3.0)
            assertContrast(name, "neg", colors.neg, colors.bg, atLeast = 3.0)
        }
    }

    // 발로란트 빨강 위 흰 글자는 3.4:1이라 본문 기준(4.5)에 못 미치는 걸 알고 고른 값이다. 더 내려가지 않게 3:1을 바닥으로 둔다.
    @Test
    fun `버튼 글자는 accent 면 위에서 3 대 1을 넘는다`() {
        forEachTheme { name, colors ->
            assertContrast(name, "onAccent", colors.onAccent, colors.accent, atLeast = 3.0)
        }
    }

    @Test
    fun `액센트를 글자로 쓸 때는 accentInk를 쓴다`() {
        // 라이트에서 accent를 그대로 글자로 올리면 대비가 모자란다. accentInk가 그 대안이다.
        assertContrast("라이트", "accentInk", OvalitLightColors.accentInk, OvalitLightColors.bg, atLeast = 4.5)
        assertContrast("다크", "accentInk", OvalitDarkColors.accentInk, OvalitDarkColors.bg, atLeast = 4.5)
    }

    // 비공식 고지를 여기 색으로 찍는다. 안 읽히면 고지를 안 한 것과 같다.
    @Test
    fun `고지에 쓰는 t3는 배경 대비 3 대 1을 넘는다`() {
        forEachTheme { name, colors ->
            assertContrast(name, "t3", colors.t3, colors.bg, atLeast = 3.0)
        }
    }

    // 홈, 내 프로필, S5의 글자는 대부분 카드 위에 놓인다
    @Test
    fun `카드 위 글자색도 배경 위와 같은 대비를 넘는다`() {
        forEachTheme { name, colors ->
            assertContrast(name, "t1", colors.t1, colors.card, atLeast = 4.5)
            assertContrast(name, "t2", colors.t2, colors.card, atLeast = 4.5)
            assertContrast(name, "t3", colors.t3, colors.card, atLeast = 3.0)
            assertContrast(name, "pos", colors.pos, colors.card, atLeast = 3.0)
            assertContrast(name, "neg", colors.neg, colors.card, atLeast = 3.0)
            listOf("kda1" to colors.kda1, "kda2" to colors.kda2, "kda3" to colors.kda3).forEach { (token, color) ->
                assertContrast(name, token, color, colors.card, atLeast = 4.5)
            }
        }
    }

    @Test
    fun `카드는 캔버스보다 밝아 묶음이 갈린다`() {
        forEachTheme { name, colors ->
            assertTrue(relativeLuminance(colors.card) > relativeLuminance(colors.canvas), "$name 카드가 캔버스보다 밝지 않다")
        }
    }

    // KDA는 리스트의 작은 글자로도 뜬다. 두 바탕 어디서든 본문 대비를 넘겨야 한다.
    @Test
    fun `KDA 구간 색은 두 바탕 모두에서 4_5 대 1을 넘는다`() {
        forEachTheme { name, colors ->
            listOf("kda1" to colors.kda1, "kda2" to colors.kda2, "kda3" to colors.kda3).forEach { (token, color) ->
                assertContrast(name, token, color, colors.bg, atLeast = 4.5)
                assertContrast(name, token, color, colors.raised, atLeast = 4.5)
            }
        }
    }

    // 흰 바탕에서 읽히는 초록과 빨강은 오르내림 색과 거리가 5 안팎이다. 그래서 거리로는 같은 색만 막고, 색상이 연두와
    // 산호 쪽으로 옮겨 갔는지 따로 본다.
    @Test
    fun `KDA 구간 색은 오르내림 색이나 액센트와 섞여 보이지 않는다`() {
        forEachTheme { name, colors ->
            val tokens = listOf("kda1" to colors.kda1, "kda2" to colors.kda2, "kda3" to colors.kda3)
            tokens.forEach { (token, color) ->
                mapOf("pos" to colors.pos, "neg" to colors.neg).forEach { (other, otherColor) ->
                    val distance = oklabDistance(color, otherColor)
                    assertTrue(distance >= 4.0, "$name $token 와 $other 의 OKLab 거리가 ${distance.rounded()}라 4에 못 미친다")
                }
            }
            assertTrue(oklabHue(colors.pos) - oklabHue(colors.kda1) >= 15.0, "$name kda1 이 pos보다 연두 쪽으로 15도 넘게 옮겨 가지 않았다")
            assertTrue(oklabHue(colors.kda3) - oklabHue(colors.neg) >= 4.0, "$name kda3 이 neg보다 산호 쪽으로 옮겨 가지 않았다")
            listOf("kda1" to colors.kda1, "kda2" to colors.kda2).forEach { (token, color) ->
                mapOf("accentInk" to colors.accentInk, "kda3" to colors.kda3).forEach { (other, otherColor) ->
                    val distance = oklabDistance(color, otherColor)
                    assertTrue(distance >= 15.0, "$name $token 와 $other 의 OKLab 거리가 ${distance.rounded()}라 15에 못 미친다")
                }
            }
            val bands = oklabDistance(colors.kda1, colors.kda2)
            assertTrue(bands >= 15.0, "$name kda1 과 kda2 의 OKLab 거리가 ${bands.rounded()}라 15에 못 미친다")
            // 맨 위 칸은 빨강이라 액센트와 같은 계열이다. 라이트에서는 산호 쪽으로 옮겨 8 남짓만 뗀다.
            val top = oklabDistance(colors.kda3, colors.accentInk)
            assertTrue(top >= 8.0, "$name kda3 와 accentInk 의 OKLab 거리가 ${top.rounded()}라 8에 못 미친다")
        }
    }

    // 칩은 옅은 면에 같은 계열의 짙은 글자를 올린다. 작은 글자라 본문 기준을 넘겨야 한다.
    @Test
    fun `MVP 칩 글자는 칩 면 위에서 4_5 대 1을 넘는다`() {
        forEachTheme { name, colors ->
            assertContrast(name, "mvp", colors.mvp, colors.mvpContainer, atLeast = 4.5)
            assertContrast(name, "teamMvp", colors.teamMvp, colors.teamMvpContainer, atLeast = 4.5)
            assertContrast(name, "ace", colors.ace, colors.aceContainer, atLeast = 4.5)
            assertContrast(name, "clutch", colors.clutch, colors.clutchContainer, atLeast = 4.5)
        }
    }

    @Test
    fun `라이트와 다크는 서로 다른 값을 쓴다`() {
        assertTrue(OvalitLightColors.bg != OvalitDarkColors.bg)
        assertTrue(OvalitLightColors.t1 != OvalitDarkColors.t1)
        assertTrue(OvalitLightColors.pos != OvalitDarkColors.pos)
        assertTrue(OvalitLightColors.neg != OvalitDarkColors.neg)
    }

    private fun forEachTheme(block: (name: String, colors: OvalitColors) -> Unit) {
        block("다크", OvalitDarkColors)
        block("라이트", OvalitLightColors)
    }

    private fun assertContrast(
        theme: String,
        token: String,
        foreground: Color,
        background: Color,
        atLeast: Double,
    ) {
        val ratio = contrastRatio(foreground, background)
        assertTrue(
            ratio >= atLeast,
            "$theme $token 대비가 ${ratio.rounded()}:1 이라 $atLeast:1 에 못 미친다",
        )
    }
}

/** WCAG 2.1 상대 명도 기준 대비입니다. */
private fun contrastRatio(a: Color, b: Color): Double {
    val la = relativeLuminance(a)
    val lb = relativeLuminance(b)
    return (max(la, lb) + 0.05) / (min(la, lb) + 0.05)
}

private fun relativeLuminance(color: Color): Double {
    fun channel(value: Float): Double {
        val c = value.toDouble()
        return if (c <= 0.03928) c / 12.92 else ((c + 0.055) / 1.055).pow(2.4)
    }
    return 0.2126 * channel(color.red) + 0.7152 * channel(color.green) + 0.0722 * channel(color.blue)
}

/** 두 색의 OKLab 거리에 100을 곱한 값입니다. 사람 눈에 비슷해 보이는 정도를 셉니다. */
private fun oklabDistance(a: Color, b: Color): Double {
    val (l1, a1, b1) = oklab(a)
    val (l2, a2, b2) = oklab(b)
    return 100 * sqrt((l1 - l2).pow(2) + (a1 - a2).pow(2) + (b1 - b2).pow(2))
}

/** OKLab 색상각(도)입니다. 0이 분홍빨강, 90 남짓이 노랑, 150 남짓이 초록입니다. */
private fun oklabHue(color: Color): Double {
    val (_, a, b) = oklab(color)
    val degrees = atan2(b, a) * 180 / PI
    return if (degrees < 0) degrees + 360 else degrees
}

private fun oklab(color: Color): Triple<Double, Double, Double> {
    fun linear(value: Float): Double {
        val c = value.toDouble()
        return if (c <= 0.04045) c / 12.92 else ((c + 0.055) / 1.055).pow(2.4)
    }
    val r = linear(color.red)
    val g = linear(color.green)
    val b = linear(color.blue)
    val l = cbrt(0.4122214708 * r + 0.5363325363 * g + 0.0514459929 * b)
    val m = cbrt(0.2119034982 * r + 0.6806995451 * g + 0.1073969566 * b)
    val s = cbrt(0.0883024619 * r + 0.2817188376 * g + 0.6299787005 * b)
    return Triple(
        0.2104542553 * l + 0.7936177850 * m - 0.0040720468 * s,
        1.9779984951 * l - 2.4285922050 * m + 0.4505937099 * s,
        0.0259040371 * l + 0.7827717662 * m - 0.8086757660 * s,
    )
}

private fun Double.rounded(): String {
    val scaled = (this * 100).toInt() / 100.0
    return scaled.toString()
}
