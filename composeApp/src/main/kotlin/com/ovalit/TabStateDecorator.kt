package com.ovalit

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.SaveableStateHolder
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.lifecycle.viewmodel.ViewModelStoreProvider
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import androidx.lifecycle.viewmodel.compose.rememberViewModelStoreOwner
import androidx.lifecycle.viewmodel.compose.rememberViewModelStoreProvider
import androidx.navigation3.runtime.NavEntryDecorator
import androidx.navigation3.runtime.NavKey
import androidx.savedstate.compose.LocalSavedStateRegistryOwner

/**
 * 탭을 바꿔도 탭 화면의 스크롤과 ViewModel을 남겨 두는 장식입니다. 탭을 고르면 앞 탭이 스택에서 빠지는데, Navigation 3 기본
 * 장식은 빠질 때 둘 다 지웁니다.
 *
 * 탭 화면은 홈이 스택에 있는 동안 남겨 두고, 연동을 해제해 홈까지 빠지면 [clearKept]로 지웁니다. 탭이 아닌 화면은 기본 장식처럼
 * 빠질 때 지웁니다. 저장한 상태와 ViewModel은 함께 지워야 해서 한 장식에 둡니다.
 */
internal class TabStateDecorator(
    private val holder: SaveableStateHolder,
    private val provider: ViewModelStoreProvider,
    private val keepTabs: () -> Boolean,
) {
    private val kept = mutableSetOf<Any>()

    val decorator: NavEntryDecorator<NavKey> = NavEntryDecorator(
        onPop = { contentKey ->
            if (isTabContentKey(contentKey) && keepTabs()) {
                kept += contentKey
            } else {
                clear(contentKey)
                kept -= contentKey
            }
        },
    ) { entry ->
        holder.SaveableStateProvider(entry.contentKey) {
            val owner = rememberViewModelStoreOwner(entry.contentKey, provider, LocalSavedStateRegistryOwner.current)
            CompositionLocalProvider(LocalViewModelStoreOwner provides owner) { entry.Content() }
        }
    }

    /** 남겨 둔 탭 화면을 모두 지웁니다. 홈까지 스택에서 빠졌을 때 부릅니다. */
    fun clearKept() {
        kept.forEach(::clear)
        kept.clear()
    }

    private fun clear(contentKey: Any) {
        holder.removeState(contentKey)
        provider.clearKey(contentKey)
    }
}

/** @param keepTabs 탭이 빠질 때 남겨 둘지입니다. 빠진 뒤에 부르니 그때의 스택을 보고 정합니다. */
@Composable
internal fun rememberTabStateDecorator(keepTabs: () -> Boolean): TabStateDecorator {
    val holder = rememberSaveableStateHolder()
    val provider = rememberViewModelStoreProvider(parent = checkNotNull(LocalViewModelStoreOwner.current))
    val currentKeepTabs = rememberUpdatedState(keepTabs)
    return remember(holder, provider) { TabStateDecorator(holder, provider) { currentKeepTabs.value() } }
}

/** 탭 화면의 contentKey입니다. 장식은 이 접두어로 탭을 가립니다. 상태 저장에 쓰여서 문자열로 둡니다. */
internal fun tabContentKey(key: NavKey): Any = "$TAB_PREFIX$key"

private fun isTabContentKey(contentKey: Any): Boolean = contentKey is String && contentKey.startsWith(TAB_PREFIX)

private const val TAB_PREFIX = "tab:"
