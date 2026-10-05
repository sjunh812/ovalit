package com.ovalit.app

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ovalit.core.data.UserPreferencesRepository
import com.ovalit.core.model.ThemePreference
import kotlinx.coroutines.flow.map
import org.koin.compose.koinInject

/** 설정에서 고른 테마로 지금 어두운 테마인지 정합니다. "시스템 설정"이면 기기를 따릅니다. */
@Composable
fun rememberDarkTheme(): Boolean {
    val preferences = koinInject<UserPreferencesRepository>()
    val themes = remember(preferences) { preferences.preferences.map { it.theme } }
    val theme by themes.collectAsStateWithLifecycle(initialValue = ThemePreference.SYSTEM)
    return when (theme) {
        ThemePreference.SYSTEM -> isSystemInDarkTheme()
        ThemePreference.DARK -> true
        ThemePreference.LIGHT -> false
    }
}
