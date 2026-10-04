package com.ovalit.feature.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
 * S6과 S7을 세는 동안의 모양입니다. 제목 밑에 맨 위 묶음 자리와 줄 다섯을 잡습니다. 빈 바탕으로 밀려 들어오다 전환 한가운데서
 * 표가 튀어나와 번쩍였습니다.
 *
 * @param title 내 기록이면 제목을 그대로 둡니다. 친구 기록은 이름을 받기 전이라 `null`이고 제목 자리만 잡습니다.
 */
@Composable
internal fun RecordsSkeleton(title: StringResource?, onBack: () -> Unit) {
    val description = stringResource(CoreUiRes.string.loading)
    Column(modifier = Modifier.fillMaxSize().safeDrawingPadding()) {
        if (title != null) {
            OvalitBackTopBar(onBack = onBack, title = stringResource(title))
        } else {
            OvalitBackTopBar(onBack = onBack, title = { OvalitSkeleton(description) { SkeletonBlock(width = 120.dp, height = 24.dp) } })
        }
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
