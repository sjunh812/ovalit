package com.ovalit.feature.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.ovalit.core.designsystem.theme.OvalitTheme
import com.ovalit.core.model.AgentId
import com.ovalit.core.model.ContentCatalog
import com.ovalit.core.model.WeaponCategory
import com.ovalit.core.model.WeaponId
import com.ovalit.core.ui.WeaponImage
import com.ovalit.feature.profile.resources.Res
import com.ovalit.feature.profile.resources.unknown_agent
import com.ovalit.feature.profile.resources.unknown_weapon
import com.ovalit.feature.profile.resources.value_percent
import com.ovalit.feature.profile.resources.weapons_category_machine_gun
import com.ovalit.feature.profile.resources.weapons_category_melee
import com.ovalit.feature.profile.resources.weapons_category_pistol
import com.ovalit.feature.profile.resources.weapons_category_rifle
import com.ovalit.feature.profile.resources.weapons_category_shotgun
import com.ovalit.feature.profile.resources.weapons_category_smg
import com.ovalit.feature.profile.resources.weapons_category_sniper
import kotlin.math.roundToInt
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

internal const val NO_VALUE = "–"

// 목업대로 강조할 한 줄만 금색이고 나머지는 흐리게 칠한다
@Composable
internal fun ShareBar(fraction: Float, highlighted: Boolean, modifier: Modifier = Modifier) {
    val colors = OvalitTheme.colors
    Box(modifier = modifier.height(4.dp).background(colors.fill, RoundedCornerShape(2.dp))) {
        Box(
            Modifier
                .fillMaxWidth(fraction.coerceIn(0f, 1f))
                .height(4.dp)
                .background(if (highlighted) colors.accent else colors.t4, RoundedCornerShape(2.dp)),
        )
    }
}

internal val WeaponCategory.label: StringResource
    get() = when (this) {
        WeaponCategory.RIFLE -> Res.string.weapons_category_rifle
        WeaponCategory.PISTOL -> Res.string.weapons_category_pistol
        WeaponCategory.SMG -> Res.string.weapons_category_smg
        WeaponCategory.SNIPER -> Res.string.weapons_category_sniper
        WeaponCategory.SHOTGUN -> Res.string.weapons_category_shotgun
        WeaponCategory.MACHINE_GUN -> Res.string.weapons_category_machine_gun
        WeaponCategory.MELEE -> Res.string.weapons_category_melee
    }

@Composable
internal fun ContentCatalog.agentName(id: AgentId): String = agents[id] ?: stringResource(Res.string.unknown_agent)

@Composable
internal fun ContentCatalog.weaponName(id: WeaponId): String =
    weapons[id]?.name ?: stringResource(Res.string.unknown_weapon)

internal fun Int.withThousands(): String = toString().reversed().chunked(3).joinToString(",").reversed()

internal fun Double.percentSteps(): Int = (this * 100).roundToInt()

@Composable
internal fun percentText(rate: Double?): String =
    rate?.let { stringResource(Res.string.value_percent, it.percentSteps().toString()) } ?: NO_VALUE

// 기본 스킨 그림을 글자색 한 가지로 칠한 실루엣이다. 그림 그대로면 짙은 회색 총이 다크 바탕에 묻혀서 면을 깔아야
// 했는데, 그 면이 칸마다 상자처럼 떠 보였다. 모양만 남기면 두 테마 모두 면 없이 보인다. 총마다 길이가 달라서
// 가운데에 두면 들쭉날쭉해 보여 왼쪽 끝을 아래 글자와 맞춘다.
@Composable
internal fun WeaponThumb(weapon: WeaponId, name: String, width: Dp, height: Dp) {
    WeaponImage(
        weapon = weapon,
        name = name,
        modifier = Modifier.size(width = width, height = height),
        tint = OvalitTheme.colors.t2,
        alignment = Alignment.CenterStart,
    )
}

// 목업대로 50%를 넘으면 초록, 밑돌면 빨강이다. 색은 변화량에만 쓴다는 규칙의 예외로 CLAUDE.md에 적었다.
@Composable
internal fun winRateColor(rate: Double?): Color {
    val steps = rate?.percentSteps() ?: return OvalitTheme.colors.t3
    return when {
        steps > 50 -> OvalitTheme.colors.pos
        steps < 50 -> OvalitTheme.colors.neg
        else -> OvalitTheme.colors.t2
    }
}
