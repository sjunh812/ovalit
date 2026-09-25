package com.ovalit.core.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.text
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import com.ovalit.core.designsystem.component.OvalitText
import com.ovalit.core.designsystem.theme.OvalitSpacing
import com.ovalit.core.designsystem.theme.OvalitTheme
import com.ovalit.core.model.Role
import com.ovalit.core.ui.resources.Res
import com.ovalit.core.ui.resources.main_role
import org.jetbrains.compose.resources.stringResource

private val BannerHeight = 112.dp
private val AvatarSize = 64.dp

/**
 * 내 프로필과 S5 머리의 배너와 아바타입니다.
 *
 * 배너와 아바타 자리에는 플레이어 카드가 들어갈 예정입니다. 카드는 앱에 넣지 않고 서버에서 받으므로,
 * 그때까지 배너는 면만 칠하고 아바타는 Riot ID 첫 글자를 띄웁니다. 카드를 깔 때 목업처럼 아래쪽을 바탕색으로
 * 흐리게 잇습니다.
 */
@Composable
fun ProfileBanner(badge: PlayerBadge, modifier: Modifier = Modifier, topBar: @Composable BoxScope.() -> Unit) {
    val colors = OvalitTheme.colors
    Box(modifier = modifier.fillMaxWidth()) {
        Box(modifier = Modifier.fillMaxWidth().height(BannerHeight).background(colors.raised)) {
            Box(Modifier.safeDrawingPadding(), content = topBar)
        }
        PlayerAvatar(
            riotId = badge.riotId,
            modifier = Modifier
                .padding(start = OvalitSpacing.gutter, top = BannerHeight - AvatarSize / 2)
                .size(AvatarSize)
                .border(3.dp, colors.bg, CircleShape),
        )
    }
}

/**
 * Riot ID와 티어, 주로 하는 역할입니다. 목업 S5처럼 이름 옆에 엠블럼을 두고, 아래 줄에 "다이아몬드 2 ·
 * 주로 타격대"를 씁니다. 역할 이름만 한 단계 밝고 굵게 올립니다. [trailing]은 그 뒤에 붙는 말입니다.
 */
@Composable
fun ProfileIdentity(badge: PlayerBadge, mainRole: Role?, modifier: Modifier = Modifier, trailing: String? = null) {
    val colors = OvalitTheme.colors
    val caption = OvalitTheme.typography.caption
    val parts = buildList<@Composable () -> Unit> {
        badge.tierName?.let { add { OvalitText(text = it, style = caption, color = colors.t3) } }
        mainRole?.let { role -> add { MainRole(role) } }
        trailing?.let { add { OvalitText(text = it, style = caption, color = colors.t3) } }
    }

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(5.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(7.dp)) {
            OvalitText(text = badge.riotId, style = OvalitTheme.typography.titleL, modifier = Modifier.weight(1f, fill = false))
            badge.tier?.takeIf { badge.tierName != null }?.let { TierEmblem(it, Modifier.size(18.dp)) }
        }
        if (parts.isNotEmpty()) {
            SeparatedRow(
                items = parts,
                separator = { OvalitText(text = "\u00a0·\u00a0", style = caption, color = colors.t5) },
                modifier = Modifier.semantics(mergeDescendants = true) {},
            )
        }
    }
}

// 아이콘은 역할에 딸린 것이라 "주로" 뒤, 역할 이름 바로 앞에 둔다. 글자 안에 넣어야 줄이 넘어가도
// 아이콘과 이름이 떨어지지 않는다.
@Composable
private fun MainRole(role: Role) {
    val colors = OvalitTheme.colors
    val name = stringResource(role.label)
    val text = stringResource(Res.string.main_role, name)
    val start = text.indexOf(name)
    val icon = mapOf(
        RoleIconId to InlineTextContent(Placeholder(1.55.em, 1.2.em, PlaceholderVerticalAlign.TextCenter)) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.CenterStart) {
                RoleIcon(role, tint = colors.t3, modifier = Modifier.fillMaxHeight().aspectRatio(1f))
            }
        },
    )
    OvalitText(
        text = buildAnnotatedString {
            if (start < 0) {
                append(text)
                return@buildAnnotatedString
            }
            append(text.substring(0, start))
            appendInlineContent(RoleIconId)
            withStyle(SpanStyle(color = colors.t2, fontWeight = FontWeight.SemiBold)) { append(name) }
            append(text.substring(start + name.length))
        },
        // 아이콘 자리에 든 대체 글자를 읽지 않게 글자만 따로 알린다
        modifier = Modifier.clearAndSetSemantics { this.text = AnnotatedString(text) },
        style = OvalitTheme.typography.caption,
        color = colors.t3,
        inlineContent = icon,
    )
}

private const val RoleIconId = "role"
