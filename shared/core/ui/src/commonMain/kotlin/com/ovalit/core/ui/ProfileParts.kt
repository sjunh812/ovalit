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

// 목업대로 50%를 넘으면 초록, 밑돌면 빨강이다. 색은 변화량에만 쓴다는 규칙의 예외로 CLAUDE.md에 적었다.
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

// 기본 스킨 그림을 글자색 한 가지로 칠한 실루엣이다. 그림 그대로면 짙은 회색 총이 다크 바탕에 묻혀서 면을 깔아야
// 했는데, 그 면이 칸마다 상자처럼 떠 보였다. 모양만 남기면 두 테마 모두 면 없이 보인다. 총마다 길이가 달라서
// 가운데에 두면 들쭉날쭉해 보여 왼쪽 끝을 아래 글자와 맞춘다.
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
 * op.gg처럼 KDA를 구간마다 칠합니다. 1 미만은 [below] 그대로 두고 1~2는 청록, 2~3은 파랑, 3 이상은 주황입니다. 보이는
 * 두 자리로 반올림한 값으로 가릅니다. 1.995가 "2.00"으로 보이는데 청록이면 틀려 보입니다. 오르내림의 pos, neg와는
 * 다른 토큰이라 KDA가 "지난주보다 올랐다"로 읽히지 않습니다.
 */
@Composable
fun kdaColor(kda: Double, below: Color): Color {
    val colors = OvalitTheme.colors
    return when (kdaTier(kda)) {
        KdaTier.BELOW_ONE -> below
        KdaTier.ONE -> colors.kdaTier1
        KdaTier.TWO -> colors.kdaTier2
        KdaTier.THREE -> colors.kdaTier3
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
 * "1.92 (344/242/124)"의 두 조각입니다. KDA는 (킬 + 어시) ÷ 데스이고, 표본이 모자라거나 데스가 없으면 [ratio]가 없어서
 * 합계만 적습니다. 킬과 어시를 더한 값이라 "평점"이라 부르지 않습니다(CLAUDE.md 지켜야 할 선).
 *
 * @property value 구간 색을 고르는 KDA입니다.
 */
class KdaText(val ratio: String?, val counts: String, val value: Double? = null)

@Composable
fun kdaText(kda: Double?, kills: Int, deaths: Int, assists: Int): KdaText = KdaText(
    ratio = kda?.let { MetricFormat.TWO_DECIMALS.format(it) },
    counts = stringResource(Res.string.kda_counts, kills.withThousands(), deaths.withThousands(), assists.withThousands()),
    value = kda,
)

/** KDA만 굵게 두고 구간 색을 칠한 한 줄입니다. */
@Composable
fun KdaText.annotated(): AnnotatedString {
    val ratio = ratio ?: return AnnotatedString(counts)
    val text = stringResource(Res.string.kda_with_counts, ratio, counts)
    val color = value?.let { kdaColor(it, below = OvalitTheme.colors.t2) } ?: OvalitTheme.colors.t2
    return buildAnnotatedString {
        append(text)
        addStyle(SpanStyle(color = color, fontWeight = FontWeight.SemiBold), 0, ratio.length)
    }
}

/**
 * "KDA 2.13"입니다. 숫자만 굵게 두고 구간 색을 칠합니다. 프로필 요원 칸, 홈 요원 칸, S7이 같이 씁니다.
 *
 * @param below 1 미만일 때 숫자 색입니다.
 */
@Composable
fun kdaRatioText(kda: Double, below: Color = OvalitTheme.colors.t2): AnnotatedString {
    val value = MetricFormat.TWO_DECIMALS.format(kda)
    val text = stringResource(Res.string.kda_ratio, value)
    val start = text.indexOf(value)
    return buildAnnotatedString {
        append(text)
        addStyle(SpanStyle(color = kdaColor(kda, below), fontWeight = FontWeight.SemiBold), start, start + value.length)
    }
}
