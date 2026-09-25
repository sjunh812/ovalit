package com.ovalit.core.ui

import com.ovalit.core.model.Role
import com.ovalit.core.ui.resources.Res
import com.ovalit.core.ui.resources.role_controller
import com.ovalit.core.ui.resources.role_duelist
import com.ovalit.core.ui.resources.role_initiator
import com.ovalit.core.ui.resources.role_sentinel
import org.jetbrains.compose.resources.StringResource

val Role.label: StringResource
    get() = when (this) {
        Role.DUELIST -> Res.string.role_duelist
        Role.INITIATOR -> Res.string.role_initiator
        Role.CONTROLLER -> Res.string.role_controller
        Role.SENTINEL -> Res.string.role_sentinel
    }
