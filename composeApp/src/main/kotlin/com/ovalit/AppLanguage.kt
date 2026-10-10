package com.ovalit

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * 지금 화면 문구의 언어입니다("ko", "ja"). 요원·맵·무기·티어 이름을 이 언어로 받습니다.
 *
 * 기기 언어가 아니라 리소스 설정의 언어를 읽어야 화면 문구와 이름의 언어가 맞습니다.
 * 앱이 켜진 채로 앱별 언어를 바꾸면 프로세스는 그대로이고 화면만 다시 만들어지므로, 화면을 만들 때마다 [update]로 다시 읽습니다.
 */
object AppLanguage {
    private val language = MutableStateFlow("ko")

    val current: StateFlow<String> = language.asStateFlow()

    fun update(context: Context) {
        language.value = context.resources.configuration.locales[0].language
    }
}
