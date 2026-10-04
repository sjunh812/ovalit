package com.ovalit.core.ui

import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import com.ovalit.core.designsystem.theme.OvalitTheme
import com.ovalit.core.model.ContentCatalog
import com.ovalit.core.model.MatchMetrics
import com.ovalit.core.model.WeaponId
import com.ovalit.core.ui.resources.Res
import com.ovalit.core.ui.resources.kda_counts
import com.ovalit.core.ui.resources.kda_ratio
import com.ovalit.core.ui.resources.kda_with_counts
import com.ovalit.core.ui.resources.unknown_weapon
import org.jetbrains.compose.resources.stringResource

/** 값이 없는 칸에 띄우는 글자입니다. 0으로 채우면 "헤드샷 0%"처럼 틀린 숫자가 됩니다. */
const val NO_VALUE = "–"

@Composable
fun percentText(rate: Double?): String = rate?.let { MetricFormat.PERCENT.valueText(it) } ?: NO_VALUE

// 목업대로 50%를 넘으면 초록, 밑돌면 빨강이다. 색은 변화량에만 쓴다는 규칙의 예외로 docs/design.md에 적었다.
@Composable
fun winRateColor(rate: Double?): Color {
    val steps = rate?.let { MetricFormat.PERCENT.steps(it) } ?: return OvalitTheme.colors.t3
    return when {
        steps > 50 -> OvalitTheme.colors.pos
        steps < 50 -> OvalitTheme.colors.neg
        else -> OvalitTheme.colors.t2
    }
}

@Composable
fun ContentCatalog.weaponName(id: WeaponId): String = weapons[id]?.name ?: stringResource(Res.string.unknown_weapon)

// docs/design.md 에셋 규칙대로 --t2 실루엣이고 면을 깔지 않는다. 총마다 길이가 달라서 왼쪽 끝을 아래 글자에 맞춘다.
@Composable
fun WeaponThumb(weapon: WeaponId, name: String, width: Dp, height: Dp) {
    WeaponImage(
        weapon = weapon,
        name = name,
        modifier = Modifier.size(width = width, height = height),
        tint = OvalitTheme.colors.t2,
        alignment = Alignment.CenterStart,
    )
}

/**
 * KDA를 구간마다 칠합니다. 1 미만은 [below], 1~2는 `kda1`, 2~3은 `kda2`, 3 이상은 `kda3`입니다. 보이는 두 자리로 반올림한
 * 값으로 가릅니다. 1.995가 "2.00"으로 보이는데 1~2 색이면 틀려 보입니다.
 *
 * 경계를 바꾸거나 구간에 이름을 붙이면 등급처럼 읽혀서 그 전에 묻습니다(CLAUDE.md 지켜야 할 선).
 */
@Composable
fun kdaColor(kda: Double, below: Color): Color {
    val colors = OvalitTheme.colors
    return when (kdaTier(kda)) {
        KdaTier.BELOW_ONE -> below
        KdaTier.ONE -> colors.kda1
        KdaTier.TWO -> colors.kda2
        KdaTier.THREE -> colors.kda3
    }
}

internal enum class KdaTier { BELOW_ONE, ONE, TWO, THREE }

internal fun kdaTier(kda: Double): KdaTier {
    // 두 자리로 반올림한 값을 100배 한 정수다(1.00 → 100)
    val steps = MetricFormat.TWO_DECIMALS.steps(kda)
    return when {
        steps >= 300 -> KdaTier.THREE
        steps >= 200 -> KdaTier.TWO
        steps >= 100 -> KdaTier.ONE
        else -> KdaTier.BELOW_ONE
    }
}

/**
 * "1.92 (344/242/124)"의 두 조각입니다. KDA는 (킬 + 어시) ÷ 데스이고, 표본이 모자라거나 데스가 없으면 [kda]가 `null`이라
 * 합계만 적습니다. 킬과 어시를 더한 값이라 "평점"이라 부르지 않습니다(CLAUDE.md 지켜야 할 선).
 */
class KdaText(val kda: Double?, val counts: String)

@Composable
fun kdaText(kda: Double?, kills: Int, deaths: Int, assists: Int): KdaText = KdaText(
    kda = kda,
    counts = stringResource(Res.string.kda_counts, kills.withThousands(), deaths.withThousands(), assists.withThousands()),
)

/** KDA만 굵게 두고 구간 색을 칠한 한 줄입니다. */
@Composable
fun KdaText.annotated(): AnnotatedString {
    val kda = kda ?: return AnnotatedString(counts)
    val ratio = MetricFormat.TWO_DECIMALS.format(kda)
    val text = stringResource(Res.string.kda_with_counts, ratio, counts)
    val start = text.indexOf(ratio)
    return buildAnnotatedString {
        append(text)
        addStyle(SpanStyle(color = kdaColor(kda, below = OvalitTheme.colors.t2), fontWeight = FontWeight.SemiBold), start, start + ratio.length)
    }
}

/**
 * "KDA 2.13"입니다. 숫자만 굵게 두고 구간 색을 칠합니다.
 *
 * @param below 1 미만일 때 숫자 색입니다.
 * @param label "KDA" 글자에 덧씌울 모양입니다. 홈처럼 숫자보다 글자를 작게 둘 때 씁니다.
 */
@Composable
fun kdaRatioText(kda: Double, below: Color = OvalitTheme.colors.t2, label: SpanStyle? = null): AnnotatedString {
    val value = MetricFormat.TWO_DECIMALS.format(kda)
    val text = stringResource(Res.string.kda_ratio, value)
    val start = text.indexOf(value)
    return buildAnnotatedString {
        append(text)
        if (label != null) addStyle(label, 0, start)
        addStyle(SpanStyle(color = kdaColor(kda, below), fontWeight = FontWeight.SemiBold), start, start + value.length)
    }
}

/**
 * 판당 킬, 데스, 어시스트를 첫째 자리까지 적은 세 조각입니다. 홈과 프로필 통계가 같은 자릿수로 적게 한곳에 둡니다.
 * 경기가 없으면 `null`입니다.
 */
fun MatchMetrics.perMatchKda(): List<String>? {
    if (matches == 0) return null
    return listOf(kills, deaths, assists).map { MetricFormat.ONE_DECIMAL.format(it.toDouble() / matches) }
}
