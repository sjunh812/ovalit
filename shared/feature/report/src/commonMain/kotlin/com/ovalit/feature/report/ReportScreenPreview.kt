package com.ovalit.feature.report

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.ovalit.core.designsystem.preview.OvalitThemePreview
import com.ovalit.core.model.WeeklyReport

@Preview(widthDp = 360, heightDp = 1000)
@Composable
private fun ReportMovedDarkPreview() {
    ReportPreview(ReportPreviewData.moved, darkTheme = true)
}

@Preview(widthDp = 360, heightDp = 1000)
@Composable
private fun ReportMovedLightPreview() {
    ReportPreview(ReportPreviewData.moved, darkTheme = false)
}

// 집계 기간이 2주로 넓어졌고 움직인 지표가 없는 주
@Preview(widthDp = 360, heightDp = 1000)
@Composable
private fun ReportSteadyPreview() {
    ReportPreview(ReportPreviewData.steady)
}

// 새 액트 첫 주처럼 비교할 기록이 없는 주. "큰 변화 없음"이 뜨면 안 된다.
@Preview(widthDp = 360, heightDp = 1000)
@Composable
private fun ReportUnknownPreview() {
    ReportPreview(ReportPreviewData.unknown)
}

@Preview(widthDp = 360, heightDp = 800)
@Composable
private fun ReportNotEnoughPreview() {
    ReportPreview(ReportPreviewData.notEnough)
}

// 2열 칸에서 숫자와 근거가 넘치지 않는지 본다.
@Preview(widthDp = 320, heightDp = 568)
@Composable
private fun ReportSmallPreview() {
    ReportPreview(ReportPreviewData.moved)
}

@Preview(widthDp = 360, heightDp = 1400, fontScale = 1.5f)
@Composable
private fun ReportLargeFontPreview() {
    ReportPreview(ReportPreviewData.moved)
}

@Composable
private fun ReportPreview(report: WeeklyReport, darkTheme: Boolean = true) {
    OvalitThemePreview(darkTheme = darkTheme) {
        ReportScreen(uiState = ReportUiState.Success(report))
    }
}
