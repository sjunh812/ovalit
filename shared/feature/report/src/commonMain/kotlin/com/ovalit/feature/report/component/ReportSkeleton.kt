package com.ovalit.feature.report.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.unit.dp
import com.ovalit.core.designsystem.component.OvalitCard
import com.ovalit.core.designsystem.component.OvalitCardGap
import com.ovalit.core.designsystem.component.OvalitSkeleton
import com.ovalit.core.designsystem.component.SkeletonBlock
import com.ovalit.core.designsystem.theme.OvalitSpacing
import com.ovalit.feature.report.resources.Res
import com.ovalit.feature.report.resources.report_loading
import org.jetbrains.compose.resources.stringResource

/**
 * 리포트를 만들기 전 홈의 모양입니다. 칩, 기간, 승패 줄, 고정 네 칸과 그 밑 두 줄, 달라진 점 세 칸, 개선 포인트 자리를 잡습니다.
 * 홈처럼 카드에 담아 내용이 나타날 때 카드 자리가 움직이지 않습니다. 카드 면은 깜빡이지 않고 안의 칸만 깜빡입니다.
 */
@Composable
internal fun ReportSkeleton(modifier: Modifier = Modifier) {
    val description = stringResource(Res.string.report_loading)
    // 깜빡이는 덩어리가 여럿이어도 낭독기에는 한 줄만 읽힌다
    Column(modifier = modifier.fillMaxWidth().clearAndSetSemantics { contentDescription = description }) {
        OvalitSkeleton(description = description, modifier = Modifier.padding(horizontal = OvalitSpacing.gutter)) {
            Row(horizontalArrangement = Arrangement.spacedBy(OvalitSpacing.xs)) {
                SkeletonBlock(width = 88.dp, height = 34.dp, radius = 10.dp)
                SkeletonBlock(width = 52.dp, height = 34.dp, radius = 10.dp)
                SkeletonBlock(width = 52.dp, height = 34.dp, radius = 10.dp)
                SkeletonBlock(width = 52.dp, height = 34.dp, radius = 10.dp)
            }
        }
        Spacer(Modifier.height(OvalitSpacing.md))
        SkeletonCard(description) {
            SkeletonBlock(width = 96.dp, height = 28.dp)
            // 승패 줄이다. 없으면 리포트가 뜰 때 그 아래가 한 줄만큼 내려간다.
            Spacer(Modifier.height(OvalitSpacing.md))
            SkeletonBlock(width = 150.dp, height = 14.dp)
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
        }
        Spacer(Modifier.height(OvalitCardGap))
        SkeletonCard(description) {
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
        }
        Spacer(Modifier.height(OvalitCardGap))
        SkeletonCard(description) {
            SkeletonBlock(width = 260.dp, height = 18.dp)
            Spacer(Modifier.height(8.dp))
            SkeletonBlock(width = 200.dp, height = 12.dp)
        }
    }
}

@Composable
private fun SkeletonCard(description: String, content: @Composable ColumnScope.() -> Unit) {
    OvalitCard {
        OvalitSkeleton(description = description, modifier = Modifier.padding(horizontal = OvalitSpacing.gutter)) {
            Column(modifier = Modifier.fillMaxWidth(), content = content)
        }
    }
}
