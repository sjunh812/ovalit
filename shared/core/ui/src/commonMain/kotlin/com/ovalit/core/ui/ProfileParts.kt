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
 * "1.92 (344/242/124)"의 두 조각입니다. KDA는 (킬 + 어시) ÷ 데스이고, 표본이 모자라거나 데스가 없으면 [ratio]가 없어서
 * 합계만 적습니다. 킬과 어시를 더한 값이라 "평점"이라 부르지 않고 색도 칠하지 않습니다(CLAUDE.md 지켜야 할 선).
 */
class KdaText(val ratio: String?, val counts: String)

@Composable
fun kdaText(kda: Double?, kills: Int, deaths: Int, assists: Int): KdaText = KdaText(
    ratio = kda?.let { MetricFormat.TWO_DECIMALS.format(it) },
    counts = stringResource(Res.string.kda_counts, kills.withThousands(), deaths.withThousands(), assists.withThousands()),
)

/** KDA만 한 단계 밝고 굵게 둔 한 줄입니다. */
@Composable
fun KdaText.annotated(): AnnotatedString {
    val ratio = ratio ?: return AnnotatedString(counts)
    val text = stringResource(Res.string.kda_with_counts, ratio, counts)
    val strong = SpanStyle(color = OvalitTheme.colors.t2, fontWeight = FontWeight.SemiBold)
    return buildAnnotatedString {
        append(text)
        addStyle(strong, 0, ratio.length)
    }
}

/** "KDA 2.13"입니다. 숫자만 한 단계 밝고 굵게 둡니다. 홈 고정 칸 밑 줄과 프로필 요원 칸이 같이 씁니다. */
@Composable
fun kdaRatioText(kda: Double): AnnotatedString {
    val value = MetricFormat.TWO_DECIMALS.format(kda)
    val text = stringResource(Res.string.kda_ratio, value)
    val start = text.indexOf(value)
    return buildAnnotatedString {
        append(text)
        addStyle(SpanStyle(color = OvalitTheme.colors.t2, fontWeight = FontWeight.SemiBold), start, start + value.length)
    }
}
