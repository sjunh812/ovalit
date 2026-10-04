package com.ovalit.feature.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.ovalit.core.designsystem.component.OvalitBackTopBar
import com.ovalit.core.designsystem.component.OvalitSkeleton
import com.ovalit.core.designsystem.component.OvalitText
import com.ovalit.core.designsystem.component.OvalitTopBarCaption
import com.ovalit.core.designsystem.component.SkeletonBlock
import com.ovalit.core.designsystem.theme.OvalitSpacing
import com.ovalit.core.designsystem.theme.OvalitTheme
import com.ovalit.core.ui.resources.Res as CoreUiRes
import com.ovalit.core.ui.resources.act_matches
import com.ovalit.core.ui.resources.loading
import com.ovalit.feature.profile.resources.Res
import com.ovalit.feature.profile.resources.records_hidden
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

// 값 없는 칸 글자, 퍼센트, 승률 색, 무기 이름과 실루엣은 S5와 같이 쓰려고 core/ui의 ProfileParts.kt에 있다.

// 목업대로 강조할 한 줄만 액센트로 칠하고 나머지는 흐리게 둔다
@Composable
internal fun ShareBar(fraction: Float, highlighted: Boolean, modifier: Modifier = Modifier) {
    val colors = OvalitTheme.colors
    Box(modifier = modifier.height(4.dp).background(colors.fill, RoundedCornerShape(2.dp))) {
        Box(
            Modifier
                .fillMaxWidth(fraction.coerceIn(0f, 1f))
                .height(4.dp)
                .background(if (highlighted) colors.accent else colors.t4, RoundedCornerShape(2.dp)),
        )
    }
}

// S6과 S7의 맨 위 줄이다. 친구 기록이면 "민석의 요원"처럼 이름 뒤에 ownerSuffix를 붙이고, 이름이 길면 이름만 줄여
// "의 요원"을 남긴다.
@Composable
internal fun RecordsTopBar(
    title: StringResource,
    ownerSuffix: StringResource,
    ownerName: String?,
    matches: Int,
    onBack: () -> Unit,
) {
    val style = OvalitTheme.typography.titleL
    OvalitBackTopBar(
        onBack = onBack,
        title = {
            if (ownerName == null) {
                OvalitText(stringResource(title), style = style, maxLines = 1, overflow = TextOverflow.Ellipsis)
            } else {
                OvalitText(
                    text = ownerName,
                    modifier = Modifier.weight(1f, fill = false),
                    style = style,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                OvalitText(stringResource(ownerSuffix), style = style, maxLines = 1)
            }
        },
    ) {
        OvalitTopBarCaption(stringResource(CoreUiRes.string.act_matches, matches))
    }
}

// 보던 친구를 끊었거나 친구가 전적을 비공개로 바꿨을 때 띄운다. 숫자는 하나도 남기지 않는다.
@Composable
internal fun RecordsHidden(title: StringResource, onBack: () -> Unit) {
    Column(modifier = Modifier.fillMaxSize().safeDrawingPadding()) {
        OvalitBackTopBar(onBack = onBack, title = stringResource(title))
        OvalitText(
            text = stringResource(Res.string.records_hidden),
            modifier = Modifier.padding(OvalitSpacing.gutter),
            color = OvalitTheme.colors.t2,
        )
    }
}

/**
 * S7을 세는 동안의 모양입니다. 제목 밑에 주 역할 묶음과 요원 줄 다섯을 잡습니다. 빈 바탕으로 밀려 들어오다 전환 한가운데서
 * 표가 튀어나와 번쩍였습니다.
 *
 * @param title 내 기록이면 제목을 그대로 둡니다. 친구 기록은 이름을 받기 전이라 `null`이고 제목 자리만 잡습니다.
 */
@Composable
internal fun AgentsSkeleton(title: StringResource?, onBack: () -> Unit) {
    val description = stringResource(CoreUiRes.string.loading)
    Column(modifier = Modifier.fillMaxSize().safeDrawingPadding()) {
        SkeletonTopBar(title, onBack, description)
        OvalitSkeleton(description = description, modifier = Modifier.padding(horizontal = OvalitSpacing.gutter)) {
            Column {
                Spacer(Modifier.height(22.dp))
                SkeletonBlock(width = 56.dp, height = 12.dp)
                Spacer(Modifier.height(10.dp))
                SkeletonBlock(width = 150.dp, height = 28.dp)
                Spacer(Modifier.height(12.dp))
                SkeletonBlock(width = 240.dp, height = 12.dp)
                Spacer(Modifier.height(36.dp))
                repeat(5) {
                    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                        SkeletonBlock(width = 34.dp, height = 34.dp, radius = 9.dp)
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            SkeletonBlock(width = 56.dp, height = 15.dp)
                            SkeletonBlock(width = 120.dp, height = 12.dp)
                        }
                        SkeletonBlock(width = 120.dp, height = 15.dp)
                    }
                }
            }
        }
    }
}

/**
 * S6을 세는 동안의 모양입니다. 주력 무기 세 줄 표(무기 그림, 이름, 숫자 세 칸)와 계열별 막대 줄 자리를 실제 표와 같은 자리에
 * 잡습니다. 요원 화면 틀을 같이 쓰니 무기 표가 나타날 때 줄 모양이 달라 어긋나 보였습니다(사용자 요청, 2026-10-04).
 */
@Composable
internal fun WeaponsSkeleton(title: StringResource?, onBack: () -> Unit) {
    val description = stringResource(CoreUiRes.string.loading)
    Column(modifier = Modifier.fillMaxSize().safeDrawingPadding()) {
        SkeletonTopBar(title, onBack, description)
        OvalitSkeleton(description = description, modifier = Modifier.padding(horizontal = OvalitSpacing.gutter)) {
            Column {
                Spacer(Modifier.height(OvalitSpacing.lg))
                Row(verticalAlignment = Alignment.Bottom) {
                    SkeletonBlock(width = 120.dp, height = 18.dp)
                    Spacer(Modifier.weight(1f))
                    SkeletonBlock(width = 28.dp, height = 12.dp)
                }
                Spacer(Modifier.height(OvalitSpacing.md))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    SkeletonBlock(width = 120.dp, height = 12.dp)
                    Spacer(Modifier.weight(1f))
                    repeat(3) { SkeletonCell { SkeletonBlock(width = 30.dp, height = 12.dp) } }
                }
                repeat(3) {
                    Spacer(Modifier.height(OvalitSpacing.lg))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        SkeletonBlock(width = 48.dp, height = 28.dp)
                        Spacer(Modifier.width(OvalitSpacing.md))
                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            SkeletonBlock(width = 52.dp, height = 15.dp)
                            SkeletonBlock(width = 40.dp, height = 12.dp)
                        }
                        repeat(3) {
                            SkeletonCell {
                                Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    SkeletonBlock(width = 36.dp, height = 16.dp)
                                    SkeletonBlock(width = 24.dp, height = 10.dp)
                                }
                            }
                        }
                    }
                }
                Spacer(Modifier.height(OvalitSpacing.xl))
                Box(Modifier.fillMaxWidth().height(1.dp).background(OvalitTheme.colors.fill))
                Spacer(Modifier.height(18.dp))
                Row(verticalAlignment = Alignment.Bottom) {
                    SkeletonBlock(width = 56.dp, height = 18.dp)
                    Spacer(Modifier.weight(1f))
                    SkeletonBlock(width = 40.dp, height = 12.dp)
                }
                Spacer(Modifier.height(OvalitSpacing.sm))
                // 계열별은 가장 많은 계열이 처음부터 펼쳐져 있어서 첫 줄 밑에 무기 줄 넷을 같이 잡는다
                repeat(5) { index ->
                    Row(modifier = Modifier.fillMaxWidth().height(48.dp), verticalAlignment = Alignment.CenterVertically) {
                        SkeletonBlock(width = 44.dp, height = 16.dp)
                        Spacer(Modifier.width(OvalitSpacing.lg))
                        SkeletonBlock(width = 0.dp, height = 6.dp, modifier = Modifier.weight(1f), radius = 3.dp)
                        Spacer(Modifier.width(OvalitSpacing.sm))
                        SkeletonBlock(width = 44.dp, height = 14.dp)
                        Spacer(Modifier.width(22.dp))
                    }
                    if (index == 0) {
                        Column(modifier = Modifier.padding(bottom = 10.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Spacer(Modifier.width(44.dp + OvalitSpacing.md))
                                SkeletonBlock(width = 28.dp, height = 12.dp)
                                Spacer(Modifier.weight(1f))
                                CategoryCells { SkeletonBlock(width = 26.dp, height = 12.dp) }
                            }
                            repeat(4) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    SkeletonBlock(width = 44.dp, height = 24.dp)
                                    Spacer(Modifier.width(OvalitSpacing.md))
                                    // 실제 줄은 이름(본문)과 KDA 줄(작은 글자) 두 줄이라 글자 줄 높이만큼 자리를 잡는다
                                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                        Box(Modifier.height(18.dp), contentAlignment = Alignment.CenterStart) { SkeletonBlock(width = 44.dp, height = 14.dp) }
                                        Box(Modifier.height(14.dp), contentAlignment = Alignment.CenterStart) { SkeletonBlock(width = 110.dp, height = 11.dp) }
                                    }
                                    CategoryCells { SkeletonBlock(width = 30.dp, height = 15.dp) }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// 펼친 계열의 숫자 세 칸이다. 실제 표의 K/D, 피해량, 헤드샷 열 폭(48, 52, 56dp)과 같다.
@Composable
private fun CategoryCells(block: @Composable () -> Unit) {
    Row {
        listOf(48.dp, 52.dp, 56.dp).forEach { width ->
            Box(modifier = Modifier.width(width), contentAlignment = Alignment.CenterEnd) { block() }
        }
    }
}

// 무기 표의 숫자 칸 하나다. 실제 표처럼 54dp 폭에 오른쪽으로 붙인다.
@Composable
private fun SkeletonCell(content: @Composable () -> Unit) {
    Box(modifier = Modifier.width(54.dp), contentAlignment = Alignment.CenterEnd) { content() }
}

// 오른쪽 "이번 액트 50경기" 자리도 잡는다. 없으면 나타날 때 그 글자만 툭 튄다.
@Composable
private fun SkeletonTopBar(title: StringResource?, onBack: () -> Unit, description: String) {
    val caption: @Composable RowScope.() -> Unit = { OvalitSkeleton(description) { SkeletonBlock(width = 84.dp, height = 12.dp) } }
    if (title != null) {
        OvalitBackTopBar(onBack = onBack, title = stringResource(title), actions = caption)
    } else {
        OvalitBackTopBar(
            onBack = onBack,
            title = { OvalitSkeleton(description) { SkeletonBlock(width = 120.dp, height = 24.dp) } },
            actions = caption,
        )
    }
}
