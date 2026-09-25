package com.ovalit.feature.report.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ovalit.core.designsystem.component.OvalitSkeleton
import com.ovalit.core.designsystem.component.SkeletonBlock
import com.ovalit.core.designsystem.theme.OvalitSpacing
import com.ovalit.feature.report.resources.Res
import com.ovalit.feature.report.resources.report_loading
import org.jetbrains.compose.resources.stringResource

/** 리포트를 만들기 전 홈의 모양입니다. 칩, 기간, 고정 네 칸, 달라진 점 세 칸, 개선 포인트 자리를 잡습니다. */
@Composable
internal fun ReportSkeleton(modifier: Modifier = Modifier) {
    OvalitSkeleton(description = stringResource(Res.string.report_loading), modifier = modifier) {
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = OvalitSpacing.gutter)) {
            Row(horizontalArrangement = Arrangement.spacedBy(OvalitSpacing.xs)) {
                SkeletonBlock(width = 88.dp, height = 34.dp, radius = 10.dp)
                SkeletonBlock(width = 52.dp, height = 34.dp, radius = 10.dp)
                SkeletonBlock(width = 52.dp, height = 34.dp, radius = 10.dp)
                SkeletonBlock(width = 52.dp, height = 34.dp, radius = 10.dp)
            }
            Spacer(Modifier.height(OvalitSpacing.xl))
            SkeletonBlock(width = 96.dp, height = 28.dp)
            Spacer(Modifier.height(OvalitSpacing.lg))
            Row {
                repeat(4) {
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        SkeletonBlock(width = 44.dp, height = 12.dp)
                        SkeletonBlock(width = 58.dp, height = 28.dp)
                        SkeletonBlock(width = 26.dp, height = 12.dp)
                    }
                }
            }
            Spacer(Modifier.height(OvalitSpacing.lg))
            SkeletonBlock(width = 220.dp, height = 12.dp)
            Spacer(Modifier.height(8.dp))
            SkeletonBlock(width = 170.dp, height = 12.dp)
            Spacer(Modifier.height(36.dp))
            SkeletonBlock(width = 72.dp, height = 18.dp)
            Spacer(Modifier.height(OvalitSpacing.lg))
            Row(horizontalArrangement = Arrangement.spacedBy(OvalitSpacing.lg)) {
                repeat(3) {
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        SkeletonBlock(width = 56.dp, height = 12.dp)
                        SkeletonBlock(width = 64.dp, height = 28.dp)
                        SkeletonBlock(width = 72.dp, height = 12.dp)
                    }
                }
            }
            Spacer(Modifier.height(36.dp))
            SkeletonBlock(width = 260.dp, height = 18.dp)
            Spacer(Modifier.height(8.dp))
            SkeletonBlock(width = 200.dp, height = 12.dp)
        }
    }
}
