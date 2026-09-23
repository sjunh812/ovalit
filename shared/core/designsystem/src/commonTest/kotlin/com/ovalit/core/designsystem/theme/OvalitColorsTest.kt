package com.ovalit.core.designsystem.theme

import androidx.compose.ui.graphics.Color
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
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

    @Test
    fun `버튼 글자는 accent 면 위에서 4_5 대 1을 넘는다`() {
        forEachTheme { name, colors ->
            assertContrast(name, "onAccent", colors.onAccent, colors.accent, atLeast = 4.5)
        }
    }

    @Test
    fun `금색을 글자로 쓸 때는 accentInk를 쓴다`() {
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

private fun Double.rounded(): String {
    val scaled = (this * 100).toInt() / 100.0
    return scaled.toString()
}
