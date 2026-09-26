package com.ovalit.feature.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ovalit.core.designsystem.theme.OvalitTheme
import com.ovalit.core.model.WeaponCategory
import com.ovalit.feature.profile.resources.Res
import com.ovalit.feature.profile.resources.weapons_category_machine_gun
import com.ovalit.feature.profile.resources.weapons_category_melee
import com.ovalit.feature.profile.resources.weapons_category_pistol
import com.ovalit.feature.profile.resources.weapons_category_rifle
import com.ovalit.feature.profile.resources.weapons_category_shotgun
import com.ovalit.feature.profile.resources.weapons_category_smg
import com.ovalit.feature.profile.resources.weapons_category_sniper
import org.jetbrains.compose.resources.StringResource

// 값 없는 칸 글자, 퍼센트, 승률 색, 무기 이름과 실루엣은 S5와 같이 쓰려고 core/ui의 ProfileParts.kt에 있다.

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
