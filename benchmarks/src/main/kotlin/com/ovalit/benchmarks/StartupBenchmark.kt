package com.ovalit.benchmarks

import androidx.benchmark.macro.BaselineProfileMode
import androidx.benchmark.macro.CompilationMode
import androidx.benchmark.macro.StartupMode
import androidx.benchmark.macro.StartupTimingMetric
import androidx.benchmark.macro.junit4.MacrobenchmarkRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * 앱을 처음부터 켜서 첫 화면이 뜨기까지 잽니다.
 * 프로필 없이 켠 것과 Baseline Profile로 미리 컴파일한 것을 같은 기기에서 나란히 봅니다.
 * 기기끼리는 견주지 않습니다.
 */
@RunWith(AndroidJUnit4::class)
class StartupBenchmark {

    @get:Rule
    val rule = MacrobenchmarkRule()

    /** Play에서 처음 깔았는데 프로필이 아직 안 깔린 때입니다. */
    @Test
    fun startupWithoutProfile() = startup(CompilationMode.None())

    /** 앱에 넣은 Baseline Profile이 깔린 때입니다. 프로필이 없으면 실패합니다. */
    @Test
    fun startupWithProfile() = startup(CompilationMode.Partial(BaselineProfileMode.Require))

    private fun startup(compilationMode: CompilationMode) = rule.measureRepeated(
        packageName = PACKAGE_NAME,
        metrics = listOf(StartupTimingMetric()),
        compilationMode = compilationMode,
        startupMode = StartupMode.COLD,
        iterations = 10,
        setupBlock = { pressHome() },
    ) {
        startActivityAndWait()
    }
}
