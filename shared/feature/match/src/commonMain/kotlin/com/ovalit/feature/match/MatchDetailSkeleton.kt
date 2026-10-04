package com.ovalit.feature.match

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.ovalit.core.designsystem.component.OvalitBackTopBar
import com.ovalit.core.designsystem.component.OvalitSkeleton
import com.ovalit.core.designsystem.component.SkeletonBlock
import com.ovalit.core.designsystem.theme.OvalitSpacing
import com.ovalit.core.designsystem.theme.OvalitTheme
import com.ovalit.core.ui.resources.Res as CoreUiRes
import com.ovalit.core.ui.resources.loading
import org.jetbrains.compose.resources.stringResource

/**
 * S3를 세는 동안 보이는 스켈레톤입니다. 배너와 뒤로 가기, 맵 이름과 스코어, 라운드 막대, 탭, 스코어보드 줄 자리를 잡습니다.
 * 배너 자리는 다른 그림 자리처럼 `--fill`로 칠하고 아래를 바탕색으로 흐리게 잇습니다.
 */
@Composable
internal fun MatchDetailSkeleton(bannerHeight: Dp, onBack: () -> Unit) {
    val colors = OvalitTheme.colors
    val description = stringResource(CoreUiRes.string.loading)
    Column(modifier = Modifier.fillMaxSize()) {
        Box(modifier = Modifier.fillMaxWidth().height(bannerHeight)) {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(Brush.verticalGradient(0.4f to colors.fill, 1f to colors.bg)),
            )
            OvalitBackTopBar(
                onBack = onBack,
                modifier = Modifier.windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal)),
            )
            OvalitSkeleton(
                description = description,
                modifier = Modifier.align(Alignment.BottomStart).padding(start = OvalitSpacing.gutter, bottom = 14.dp),
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    SkeletonBlock(width = 96.dp, height = 28.dp)
                    SkeletonBlock(width = 120.dp, height = 40.dp)
                }
            }
        }
        OvalitSkeleton(description = description, modifier = Modifier.padding(horizontal = OvalitSpacing.gutter)) {
            Column {
                Box(Modifier.fillMaxWidth().height(14.dp).background(colors.fill, RoundedCornerShape(3.dp)))
                Spacer(Modifier.height(30.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                    SkeletonBlock(width = 64.dp, height = 16.dp)
                    SkeletonBlock(width = 44.dp, height = 16.dp)
                    SkeletonBlock(width = 44.dp, height = 16.dp)
                }
                Spacer(Modifier.height(28.dp))
                repeat(10) {
                    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                        SkeletonBlock(width = 36.dp, height = 36.dp, radius = 9.dp)
                        Spacer(Modifier.width(12.dp))
                        SkeletonBlock(width = 96.dp, height = 14.dp)
                        Spacer(Modifier.weight(1f))
                        SkeletonBlock(width = 84.dp, height = 14.dp)
                        Spacer(Modifier.width(16.dp))
                        SkeletonBlock(width = 28.dp, height = 14.dp)
                    }
                }
            }
        }
    }
}
