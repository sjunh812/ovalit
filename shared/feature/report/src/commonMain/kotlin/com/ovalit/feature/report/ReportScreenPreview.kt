package com.ovalit.feature.report

import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.tooling.preview.Preview
import com.ovalit.core.designsystem.preview.OvalitThemePreview
import com.ovalit.feature.report.component.MetricSheetBody
import com.ovalit.core.model.FixedMetric
import com.ovalit.core.model.PlayerId
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

@Preview(widthDp = 390, heightDp = 700)
@Composable
private fun MetricSheetDarkPreview() {
    OvalitThemePreview(darkTheme = true) {
        MetricSheetBody(FixedMetric.DAMAGE, ReportPreviewData.moved, Modifier.padding(24.dp))
    }
}

@Preview(widthDp = 390, heightDp = 700)
@Composable
private fun MetricSheetLightPreview() {
    OvalitThemePreview(darkTheme = false) {
        MetricSheetBody(FixedMetric.HEADSHOT_RATE, ReportPreviewData.moved, Modifier.padding(24.dp))
    }
}

// 액트가 바뀐 주에 세로선이 서고 평소 범위를 말하지 않는다
@Preview(widthDp = 390, heightDp = 700)
@Composable
private fun MetricSheetNewActPreview() {
    OvalitThemePreview {
        MetricSheetBody(FixedMetric.COMBAT_SCORE, ReportPreviewData.newAct, Modifier.padding(24.dp))
    }
}

@Preview(widthDp = 320, heightDp = 800, fontScale = 1.5f)
@Composable
private fun MetricSheetSmallLargeFontPreview() {
    OvalitThemePreview {
        MetricSheetBody(FixedMetric.KD, ReportPreviewData.moved, Modifier.padding(24.dp))
    }
}

// 친구가 없으면 초대를, 친구만 있으면 라이벌 고르기를 권한다
@Preview(widthDp = 390, heightDp = 1200)
@Composable
private fun ReportInviteNudgeDarkPreview() {
    ReportPreview(ReportPreviewData.moved, nudge = HomeNudge.INVITE_FRIEND)
}

@Preview(widthDp = 390, heightDp = 1200)
@Composable
private fun ReportRivalNudgeLightPreview() {
    ReportPreview(ReportPreviewData.moved, darkTheme = false, nudge = HomeNudge.PICK_RIVAL)
}

@Preview(widthDp = 320, heightDp = 1600, fontScale = 1.5f)
@Composable
private fun ReportRivalNudgeSmallLargeFontPreview() {
    ReportPreview(ReportPreviewData.moved, nudge = HomeNudge.PICK_RIVAL)
}

@Composable
private fun ReportPreview(
    report: WeeklyReport,
    darkTheme: Boolean = true,
    queueFilter: QueueFilter = QueueFilter.COMPETITIVE_AND_UNRATED,
    nudge: HomeNudge? = null,
) {
    val friends = listOf(
        FriendStanding(PlayerId("junho"), "준호#KR1", null),
        FriendStanding(PlayerId("minseok"), "민석#KR3", null),
        FriendStanding(PlayerId("jaehyun"), "재현#KR2", null),
    ).takeIf { nudge == HomeNudge.PICK_RIVAL }.orEmpty()
    OvalitThemePreview(darkTheme = darkTheme) {
        ReportScreen(uiState = ReportUiState.Success(queueFilter, report, friends = friends, nudge = nudge), onSelectQueue = {})
    }
}
