package com.ovalit.benchmarks

import androidx.benchmark.macro.BaselineProfileMode
import androidx.benchmark.macro.CompilationMode
import androidx.benchmark.macro.FrameTimingMetric
import androidx.benchmark.macro.junit4.MacrobenchmarkRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * 홈의 프레임을 잽니다. 끊김은 릴리스처럼 줄인 빌드로만 잽니다(CLAUDE.md 그리기).
 * 둘 다 Baseline Profile이 깔린 상태로 잽니다.
 *
 * 시작 방식(startupMode)을 주지 않습니다.
 * 주면 준비를 마친 뒤 재기 전에 앱을 다시 죽여서 홈이 아니라 꺼진 앱에서 재게 됩니다.
 * 대신 준비 맨 앞에서 앱을 죽여 매번 인트로부터 시작합니다.
 */
@RunWith(AndroidJUnit4::class)
class HomeBenchmark {

    @get:Rule
    val rule = MacrobenchmarkRule()

    /**
     * 첫 수집을 마치고 "리포트 보기"를 눌러 홈이 밀려 들어올 때입니다.
     * 홈 계산과 나눠 그리기(OvalitStaged)가 전환을 멈추게 하는지 봅니다.
     */
    @Test
    fun openHomeAfterImport() = rule.measureRepeated(
        packageName = PACKAGE_NAME,
        metrics = listOf(FrameTimingMetric()),
        compilationMode = CompilationMode.Partial(BaselineProfileMode.Require),
        iterations = 5,
        setupBlock = {
            killProcess()
            startActivityAndWait()
            finishFirstImport()
        },
    ) {
        openReport()
    }

    /** 다 그린 홈을 끝까지 내렸다가 올릴 때입니다. */
    @Test
    fun scrollHome() = rule.measureRepeated(
        packageName = PACKAGE_NAME,
        metrics = listOf(FrameTimingMetric()),
        compilationMode = CompilationMode.Partial(BaselineProfileMode.Require),
        iterations = 5,
        setupBlock = {
            killProcess()
            startActivityAndWait()
            passOnboarding()
        },
    ) {
        scrollHome()
    }
}
