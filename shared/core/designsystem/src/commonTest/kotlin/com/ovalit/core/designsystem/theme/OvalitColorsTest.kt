package com.ovalit.core.designsystem.theme

import androidx.compose.ui.graphics.Color
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

    // 발로란트 빨강 위 흰 글자는 3.4:1이라 본문 기준(4.5)에 못 미친다. 사용자 결정(2026-10-03)으로 흰색을 쓰고, 더 내려가지
    // 않게 3:1을 바닥으로 둔다.
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

    // 사용자 요청(2026-09-29): 1~2 구간 청록이 오르내림의 초록처럼 보였다. OKLab 거리 15 밑이면 색으로 가르기 어렵다.
    // 주황(kda3)은 등급 맨 위라 빨강과 가까운 걸 알고 두었다.
    @Test
    fun `KDA 1에서 3 사이 구간 색은 오르내림 색이나 액센트와 섞여 보이지 않는다`() {
        forEachTheme { name, colors ->
            val others = mapOf("pos" to colors.pos, "neg" to colors.neg, "accentInk" to colors.accentInk, "kda3" to colors.kda3)
            listOf("kda1" to colors.kda1, "kda2" to colors.kda2).forEach { (token, color) ->
                others.forEach { (other, otherColor) ->
                    val distance = oklabDistance(color, otherColor)
                    assertTrue(distance >= 15.0, "$name $token 와 $other 의 OKLab 거리가 ${distance.rounded()}라 15에 못 미친다")
                }
            }
            val bands = oklabDistance(colors.kda1, colors.kda2)
            assertTrue(bands >= 15.0, "$name kda1 과 kda2 의 OKLab 거리가 ${bands.rounded()}라 15에 못 미친다")
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
