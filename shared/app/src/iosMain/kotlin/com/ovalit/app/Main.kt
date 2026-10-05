@file:OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)

package com.ovalit.app

import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.cstr
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.toCValues
import platform.Foundation.NSStringFromClass
import platform.UIKit.UIApplicationMain
import kotlinx.cinterop.autoreleasepool

/** iOS 앱의 시작점입니다. Xcode 프로젝트 없이 이 실행 파일을 앱 번들로 묶어 시뮬레이터에 깝니다(assembleIosSimulatorApp). */
fun main(args: Array<String>) {
    memScoped {
        val argv = (arrayOf(IOS_EXECUTABLE) + args).map { it.cstr.ptr }.toCValues()
        autoreleasepool {
            UIApplicationMain(args.size + 1, argv, null, NSStringFromClass(OvalitAppDelegate))
        }
    }
}

private const val IOS_EXECUTABLE = "Ovalit"
