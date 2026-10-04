package com.ovalit.core.ui

import com.ovalit.core.model.WeaponCategory
import com.ovalit.core.ui.resources.Res
import com.ovalit.core.ui.resources.weapon_category_machine_gun
import com.ovalit.core.ui.resources.weapon_category_melee
import com.ovalit.core.ui.resources.weapon_category_pistol
import com.ovalit.core.ui.resources.weapon_category_rifle
import com.ovalit.core.ui.resources.weapon_category_shotgun
import com.ovalit.core.ui.resources.weapon_category_smg
import com.ovalit.core.ui.resources.weapon_category_sniper
import org.jetbrains.compose.resources.StringResource

val WeaponCategory.label: StringResource
    get() = when (this) {
        WeaponCategory.RIFLE -> Res.string.weapon_category_rifle
        WeaponCategory.PISTOL -> Res.string.weapon_category_pistol
        WeaponCategory.SMG -> Res.string.weapon_category_smg
        WeaponCategory.SNIPER -> Res.string.weapon_category_sniper
        WeaponCategory.SHOTGUN -> Res.string.weapon_category_shotgun
        WeaponCategory.MACHINE_GUN -> Res.string.weapon_category_machine_gun
        WeaponCategory.MELEE -> Res.string.weapon_category_melee
    }
