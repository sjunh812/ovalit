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
 * 리포트가 뜨기 전 홈의 자리를 잡는 스켈레톤입니다.
 * 홈과 같은 카드에 담아 내용이 나타날 때 카드가 움직이지 않고, 카드 안의 칸만 깜빡입니다.
 *
 * @param withChips 칩 자리도 잡을지입니다.
 *   새 경기를 받느라 리포트만 기다릴 때는 칩이 이미 있어 끕니다.
 */
@Composable
internal fun ReportSkeleton(modifier: Modifier = Modifier, withChips: Boolean = true) {
    val description = stringResource(Res.string.report_loading)
    // 깜빡이는 덩어리가 여럿이어도 화면 읽기 프로그램에는 한 줄만 읽히게 한다
    Column(modifier = modifier.fillMaxWidth().clearAndSetSemantics { contentDescription = description }) {
        if (withChips) {
            OvalitSkeleton(description = description, modifier = Modifier.padding(horizontal = OvalitSpacing.gutter)) {
                Row(horizontalArrangement = Arrangement.spacedBy(OvalitSpacing.xs)) {
                    SkeletonBlock(width = 88.dp, height = 34.dp, radius = 10.dp)
                    SkeletonBlock(width = 52.dp, height = 34.dp, radius = 10.dp)
                    SkeletonBlock(width = 52.dp, height = 34.dp, radius = 10.dp)
                    SkeletonBlock(width = 52.dp, height = 34.dp, radius = 10.dp)
                }
            }
            Spacer(Modifier.height(OvalitSpacing.md))
        }
        SkeletonCard(description) {
            SkeletonBlock(width = 96.dp, height = 28.dp)
            // 승패 줄이다. 없으면 리포트가 뜰 때 그 아래가 한 줄만큼 내려간다.
            Spacer(Modifier.height(OvalitSpacing.md))
            SkeletonBlock(width = 150.dp, height = 14.dp)
            // 짚을 점 헤드라인과 무기·요원 줄이다. 고정 칸 위에 온다.
            Spacer(Modifier.height(18.dp))
            SkeletonBlock(width = 220.dp, height = 18.dp)
            Spacer(Modifier.height(10.dp))
            SkeletonBlock(width = 170.dp, height = 12.dp)
            Spacer(Modifier.height(OvalitSpacing.lg))
            // 고정 칸 다섯 개를 세 칸씩 두 줄로 둔다
            Column(verticalArrangement = Arrangement.spacedBy(OvalitSpacing.lg)) {
                listOf(3, 2).forEach { cells ->
                    Row {
                        repeat(3) { index ->
                            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                if (index < cells) {
                                    SkeletonBlock(width = 44.dp, height = 12.dp)
                                    SkeletonBlock(width = 58.dp, height = 26.dp)
                                    SkeletonBlock(width = 72.dp, height = 12.dp)
                                }
                            }
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(OvalitCardGap))
        SkeletonCard(description) {
            SkeletonBlock(width = 72.dp, height = 18.dp)
            Spacer(Modifier.height(OvalitSpacing.lg))
            Row(horizontalArrangement = Arrangement.spacedBy(OvalitSpacing.lg)) {
                repeat(3) {
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        SkeletonBlock(width = 56.dp, height = 12.dp)
                        SkeletonBlock(width = 64.dp, height = 26.dp)
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
