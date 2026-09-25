package com.ovalit.core.ui

import com.ovalit.core.model.Focus
import com.ovalit.core.ui.resources.Res
import com.ovalit.core.ui.resources.focus_aim
import com.ovalit.core.ui.resources.focus_aim_description
import com.ovalit.core.ui.resources.focus_consistency
import com.ovalit.core.ui.resources.focus_consistency_description
import com.ovalit.core.ui.resources.focus_none
import com.ovalit.core.ui.resources.focus_none_description
import com.ovalit.core.ui.resources.focus_round_play
import com.ovalit.core.ui.resources.focus_round_play_description
import org.jetbrains.compose.resources.StringResource

val Focus.label: StringResource
    get() = when (this) {
        Focus.AIM -> Res.string.focus_aim
        Focus.ROUND_PLAY -> Res.string.focus_round_play
        Focus.CONSISTENCY -> Res.string.focus_consistency
        Focus.NONE -> Res.string.focus_none
    }

/** 고르면 무엇이 앞에 오는지입니다. 계산하는 지표만 적습니다. */
val Focus.description: StringResource
    get() = when (this) {
        Focus.AIM -> Res.string.focus_aim_description
        Focus.ROUND_PLAY -> Res.string.focus_round_play_description
        Focus.CONSISTENCY -> Res.string.focus_consistency_description
        Focus.NONE -> Res.string.focus_none_description
    }
