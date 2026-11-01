package com.ovalit.benchmarks

import androidx.benchmark.macro.MacrobenchmarkScope
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Direction
import androidx.test.uiautomator.StaleObjectException
import androidx.test.uiautomator.UiObject2
import androidx.test.uiautomator.Until

/** 재는 앱입니다. benchmarkRelease와 nonMinifiedRelease 모두 릴리스와 같은 ID입니다. */
const val PACKAGE_NAME = "com.ovalit"

// 버튼과 화면은 글자로 찾는다.
// 기기 언어가 한국어거나 앱이 모르는 언어(기본 문구가 한국어다)일 때만 찾을 수 있다.
// 일본어 기기에서는 글자가 달라 찾지 못한다.

private const val WAIT_MILLIS = 10_000L

// 가짜 서버가 첫 수집 50경기를 한 판씩 준다.
// 느린 에뮬레이터에서도 넉넉하게 기다린다.
private const val IMPORT_WAIT_MILLIS = 60_000L

private const val REPORT_BUTTON = "리포트 보기"

// 경기 탭 거르기 버튼의 설명이다
private const val FILTER_BUTTON = "필터"

/**
 * 인트로부터 첫 수집을 거쳐 홈까지 갑니다.
 * 지금은 메모리 저장소라 앱을 켤 때마다 인트로부터 시작합니다.
 * 실제 저장소가 붙으면 다시 켠 앱은 바로 홈이라 이 길은 처음 한 번만 지납니다.
 */
fun MacrobenchmarkScope.passOnboarding() {
    finishFirstImport()
    openReport()
}

/** 인트로와 연동 동의를 지나 S0-4에서 첫 수집이 끝날 때까지 기다립니다. */
fun MacrobenchmarkScope.finishFirstImport() {
    advance(from = "Riot 계정으로 시작하기", to = "Riot 계정으로 계속하기")
    advance(from = "Riot 계정으로 계속하기", to = "어떤 게 제일 궁금하세요?")
    waitForText(REPORT_BUTTON, IMPORT_WAIT_MILLIS)
}

/** S0-4의 "리포트 보기"를 눌러 홈이 다 뜰 때까지 기다립니다. */
fun MacrobenchmarkScope.openReport() {
    clickText(REPORT_BUTTON)
    waitForHome()
}

/** 홈 리포트가 다 그려질 때까지 기다립니다. 고정 칸의 첫 지표 이름으로 봅니다. */
fun MacrobenchmarkScope.waitForHome() {
    waitForText("피해량")
}

/**
 * 홈을 끝까지 내렸다가 거의 맨 위까지 다시 올립니다.
 * 세 번 내리면 끝에 닿고 올릴 때는 두 번만 넘겨 맨 위에 닿기 전에 멈춥니다.
 * 맨 위에서 한 번 더 넘기면 당겨서 새로고침이 돌아 가짜 서버가 새 경기를 주고 홈을 다시 세서 스크롤이 아닌 프레임이 섞입니다.
 * 끝에 닿았는지는 `UiObject2.fling`이 돌려주는 값으로 알 수 없습니다.
 * Compose 화면은 처음 넘길 때부터 더 넘길 수 없다고 돌려줍니다.
 */
fun MacrobenchmarkScope.scrollHome() {
    repeat(3) { fling(Direction.DOWN) }
    repeat(2) { fling(Direction.UP) }
}

/** 경기 탭을 열어 목록을 넘기고 첫 경기의 S3를 열었다 닫습니다. */
fun MacrobenchmarkScope.visitMatches() {
    clickText("경기")
    // 탭 이름 "경기"는 홈에도 있어서 경기 탭에만 있는 거르기 버튼으로 바뀐 걸 본다
    checkNotNull(device.wait(Until.findObject(By.desc(FILTER_BUTTON)), WAIT_MILLIS)) { "경기 탭이 열리지 않았어요" }
    fling(Direction.DOWN)
    fling(Direction.UP)
    // 누를 수 있는 것 중 화면 폭을 거의 다 쓰는 건 경기 줄뿐이다.
    // 칩과 거르기 버튼은 좁다.
    val row = scrollable().findObjects(By.clickable(true)).firstOrNull { it.visibleBounds.width() > device.displayWidth * 0.8 }
    checkNotNull(row) { "누를 경기 줄을 찾지 못했어요" }.click()
    waitForText("스코어보드")
    fling(Direction.DOWN)
    device.pressBack()
    check(device.wait(Until.gone(By.text("스코어보드")), WAIT_MILLIS)) { "경기 상세에서 돌아오지 못했어요" }
}

/** 친구 탭과 설정 탭을 지나 설정의 "프로필 보기"로 내 프로필을 열고 홈으로 돌아옵니다. */
fun MacrobenchmarkScope.visitOtherTabs() {
    clickText("친구")
    device.waitForIdle()
    clickText("설정")
    clickText("프로필 보기")
    check(device.wait(Until.gone(By.text("프로필 보기")), WAIT_MILLIS)) { "내 프로필이 열리지 않았어요" }
    fling(Direction.DOWN)
    device.pressBack()
    clickText("홈")
    waitForHome()
}

private fun MacrobenchmarkScope.waitForText(text: String, timeout: Long = WAIT_MILLIS): UiObject2 =
    checkNotNull(device.wait(Until.findObject(By.text(text)), timeout)) {
        "화면에서 \"$text\"를 찾지 못했어요(앞에 뜬 앱 ${device.currentPackageName})"
    }

private fun MacrobenchmarkScope.clickText(text: String, timeout: Long = WAIT_MILLIS) {
    waitForText(text, timeout).click()
}

// 스플래시가 걷히는 동안에는 눌러도 화면에 닿지 않을 때가 있어 다음 화면이 뜰 때까지 다시 누른다.
private fun MacrobenchmarkScope.advance(from: String, to: String) {
    repeat(3) {
        device.wait(Until.findObject(By.text(from)), WAIT_MILLIS)?.click()
        if (device.wait(Until.hasObject(By.text(to)), WAIT_MILLIS / 2)) return
    }
    error("\"$from\"를 눌러도 \"$to\"가 뜨지 않았어요")
}

// 화면이 넘어가는 동안 찾은 목록은 곧 사라져서 다시 찾아 넘긴다.
private fun MacrobenchmarkScope.fling(direction: Direction) {
    repeat(3) {
        try {
            scrollable().fling(direction)
            return
        } catch (_: StaleObjectException) {
            device.waitForIdle()
        }
    }
    error("목록을 넘기지 못했어요")
}

// 화면을 넘기는 손가락이 가장자리에서 시작하면 시스템 뒤로 가기 제스처로 읽힌다.
private fun MacrobenchmarkScope.scrollable(): UiObject2 =
    checkNotNull(device.wait(Until.findObject(By.scrollable(true)), WAIT_MILLIS)) {
        // 다른 앱(광고가 연 스토어나 브라우저)이 앞에 떴는지 알 수 있게 지금 앱을 같이 적는다
        "넘길 수 있는 목록을 찾지 못했어요(앞에 뜬 앱 ${device.currentPackageName})"
    }
        .also { it.setGestureMargin(device.displayWidth / 5) }
