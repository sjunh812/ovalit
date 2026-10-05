package com.ovalit.app

import com.ovalit.core.data.Analytics
import com.ovalit.core.data.ImportScheduler
import com.ovalit.core.data.NoAnalytics
import com.ovalit.core.data.di.ApplicationScope
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.flow.flowOf
import org.koin.core.context.startKoin
import org.koin.dsl.module
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSLocale
import platform.Foundation.NSURL
import platform.Foundation.NSUserDomainMask
import platform.Foundation.preferredLanguages

private val iosModule = module {
    single<ImportScheduler> { InProcessImportScheduler(scope = get(ApplicationScope), account = get(), matches = get()) }
    // 사용 통계는 안드로이드의 Firebase만 받는다
    single<Analytics> { NoAnalytics }
}

internal fun startOvalitKoin() {
    startKoin {
        modules(ovalitModules(preferencesPath = ::preferencesPath, language = flowOf(appLanguage())) + iosModule)
    }
}

@OptIn(ExperimentalForeignApi::class)
private fun preferencesPath(): String {
    val documents: NSURL? = NSFileManager.defaultManager.URLForDirectory(
        directory = NSDocumentDirectory,
        inDomain = NSUserDomainMask,
        appropriateForURL = null,
        create = true,
        error = null,
    )
    return requireNotNull(documents?.path) + "/ovalit.preferences_pb"
}

// 화면 문구처럼 일본어면 일본어 이름을, 아니면 한국어 이름을 받는다. iOS는 앱 언어를 바꾸면 앱을 다시 띄워서 켤 때 한 번 정한다.
private fun appLanguage(): String =
    if ((NSLocale.preferredLanguages.firstOrNull() as? String)?.startsWith("ja") == true) "ja" else "ko"
