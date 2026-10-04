package com.ovalit.core.ui

import com.ovalit.core.model.BuyType
import com.ovalit.core.ui.resources.Res
import com.ovalit.core.ui.resources.buy_eco
import com.ovalit.core.ui.resources.buy_force
import com.ovalit.core.ui.resources.buy_full
import com.ovalit.core.ui.resources.buy_pistol
import org.jetbrains.compose.resources.StringResource

val BuyType.label: StringResource
    get() = when (this) {
        BuyType.PISTOL -> Res.string.buy_pistol
        BuyType.ECO -> Res.string.buy_eco
        BuyType.FORCE_BUY -> Res.string.buy_force
        BuyType.FULL_BUY -> Res.string.buy_full
    }
