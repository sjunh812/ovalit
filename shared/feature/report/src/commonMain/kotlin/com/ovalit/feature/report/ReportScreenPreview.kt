package com.ovalit.feature.report

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.ovalit.core.designsystem.preview.OvalitThemePreview
import com.ovalit.core.model.QueueFilter
import com.ovalit.core.model.WeeklyReport

@Preview(widthDp = 390, heightDp = 844)
@Composable
private fun ReportMovedDarkPreview() {
    ReportPreview(ReportPreviewData.moved, darkTheme = true)
}

@Preview(widthDp = 390, heightDp = 844)
@Composable
private fun ReportMovedLightPreview() {
    ReportPreview(ReportPreviewData.moved, darkTheme = false)
}

// 집계 기간이 2주로 넓어졌고 움직인 지표가 없는 주
@Preview(widthDp = 390, heightDp = 844)
@Composable
private fun ReportSteadyPreview() {
    ReportPreview(ReportPreviewData.steady)
}

// 새 액트 첫 주처럼 비교할 기록이 없는 주. "큰 변화 없음"이 뜨면 안 된다.
@Preview(widthDp = 390, heightDp = 844)
@Composable
private fun ReportUnknownPreview() {
    ReportPreview(ReportPreviewData.unknown)
}

// 월요일 아침처럼 이번 주에 뛴 경기가 없는 때
@Preview(widthDp = 390, heightDp = 844)
@Composable
private fun ReportLastWeekPreview() {
    ReportPreview(ReportPreviewData.lastWeek)
}

@Preview(widthDp = 390, heightDp = 844)
@Composable
private fun ReportOtherQueuePreview() {
    ReportPreview(ReportPreviewData.otherQueue, queueFilter = QueueFilter.OTHER)
}

@Preview(widthDp = 390, heightDp = 844)
@Composable
private fun ReportNotEnoughPreview() {
    ReportPreview(ReportPreviewData.notEnough)
}

@Preview(widthDp = 390, heightDp = 844)
@Composable
private fun ReportNothingPlayedPreview() {
    ReportPreview(ReportPreviewData.nothingPlayed)
}

// 고정 4칸이 한 줄이라 가장 빡빡하다. 숫자가 칸을 넘지 않는지 본다.
@Preview(widthDp = 320, heightDp = 568)
@Composable
private fun ReportSmallPreview() {
    ReportPreview(ReportPreviewData.moved)
}

@Preview(widthDp = 390, heightDp = 1000, fontScale = 1.5f)
@Composable
private fun ReportLargeFontPreview() {
    ReportPreview(ReportPreviewData.moved)
}

@Composable
private fun ReportPreview(
    report: WeeklyReport,
    darkTheme: Boolean = true,
    queueFilter: QueueFilter = QueueFilter.COMPETITIVE_AND_UNRATED,
) {
    OvalitThemePreview(darkTheme = darkTheme) {
        ReportScreen(uiState = ReportUiState.Success(queueFilter, report), onSelectQueue = {})
    }
}
