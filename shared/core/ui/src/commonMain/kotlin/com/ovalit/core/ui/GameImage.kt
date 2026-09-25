package com.ovalit.core.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import com.ovalit.core.designsystem.component.OvalitText
import com.ovalit.core.designsystem.theme.OvalitTheme
import com.ovalit.core.model.AgentId
import com.ovalit.core.model.MapId
import com.ovalit.core.model.Role
import com.ovalit.core.model.WeaponId
import com.ovalit.core.ui.resources.Res
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.resources.decodeToImageBitmap

// 게임 이미지는 콘텐츠 카탈로그에서 골라 `files/`에 넣어 둔다(tools/bundle_assets.py). 서버가 이미지를
// 내려주게 되면 그쪽을 먼저 보고, 못 받으면 여기서 찾는다. 여기에도 없으면 이름 첫 글자로 자리를 잡는다.
// ID에서 파일 이름으로 가는 표는 스크립트가 만든 GameAssetIndex에 있다.

enum class MapImageStyle { THUMBNAIL, BANNER }

/** 요원 얼굴입니다. 요원을 모르거나 카탈로그에 없는 새 요원이면 [name]의 첫 글자를 띄웁니다. */
@Composable
fun AgentImage(agent: AgentId?, name: String, modifier: Modifier = Modifier) {
    val file = agent?.let { GameAssetIndex.agents[it.value.uppercase()] }
    BundledImage(file?.let { "agents/$it.png" }, modifier, fallbackText = name)
}

/**
 * 사람을 나타내는 동그란 아바타입니다. 플레이어 카드 자리인데 카드는 서버에서 받으므로, 그때까지는 Riot ID
 * 첫 글자를 띄웁니다. 요원 얼굴은 경기 기록에만 씁니다. 그 판에 고른 요원이지 그 사람 얼굴이 아닙니다.
 */
@Composable
fun PlayerAvatar(riotId: String, modifier: Modifier = Modifier) {
    val colors = OvalitTheme.colors
    BoxWithConstraints(
        modifier = modifier.clip(CircleShape).background(colors.fill),
        contentAlignment = Alignment.Center,
    ) {
        // 글자를 아바타 크기에 맞춘다. 글꼴 배율을 따라 커지면 원 밖으로 넘친다.
        val fontSize = with(LocalDensity.current) { (maxHeight * 0.42f).toSp() }
        OvalitText(
            text = riotId.take(1).uppercase(),
            style = OvalitTheme.typography.label.copy(
                fontSize = fontSize,
                lineHeight = fontSize * 1.2f,
                fontWeight = FontWeight.SemiBold,
            ),
            color = colors.t2,
        )
    }
}

/** [map]은 UUID여야 합니다. 경기 응답이 경로로 오면 VAL-CONTENT에서 UUID를 찾아 담습니다. */
@Composable
fun MapImage(map: MapId, style: MapImageStyle, modifier: Modifier = Modifier) {
    val suffix = when (style) {
        MapImageStyle.THUMBNAIL -> "thumb"
        MapImageStyle.BANNER -> "banner"
    }
    BundledImage(GameAssetIndex.maps[map.value.uppercase()]?.let { "maps/${it}_$suffix.jpg" }, modifier)
}

/** 무기는 기본 스킨 그림입니다. 카탈로그의 무기 그림은 흰 선화라 밝은 바탕에서 안 보입니다. */
@Composable
fun WeaponImage(weapon: WeaponId, name: String, modifier: Modifier = Modifier) {
    BundledImage(
        path = GameAssetIndex.weapons[weapon.value.uppercase()]?.let { "weapons/$it.png" },
        modifier = modifier,
        contentScale = ContentScale.Fit,
        background = Color.Transparent,
        fallbackText = name,
    )
}

/** 티어 엠블럼입니다. 번호는 경기 응답의 `competitiveTier`입니다. */
@Composable
fun TierEmblem(tier: Int, modifier: Modifier = Modifier) {
    BundledImage(
        path = GameAssetIndex.tiers[tier]?.let { "tiers/$it.png" },
        modifier = modifier,
        contentScale = ContentScale.Fit,
        background = Color.Transparent,
    )
}

/** 역할 아이콘은 흰색 한 가지라 글자 색에 맞춰 칠합니다. */
@Composable
fun RoleIcon(role: Role, tint: Color, modifier: Modifier = Modifier) {
    val file = when (role) {
        Role.DUELIST -> "duelist"
        Role.INITIATOR -> "initiator"
        Role.CONTROLLER -> "controller"
        Role.SENTINEL -> "sentinel"
    }
    BundledImage(
        path = "roles/$file.png",
        modifier = modifier,
        contentScale = ContentScale.Fit,
        background = Color.Transparent,
        colorFilter = ColorFilter.tint(tint),
    )
}

@OptIn(ExperimentalResourceApi::class)
@Composable
private fun BundledImage(
    path: String?,
    modifier: Modifier,
    contentScale: ContentScale = ContentScale.Crop,
    background: Color = OvalitTheme.colors.fill,
    colorFilter: ColorFilter? = null,
    fallbackText: String? = null,
) {
    val state by produceState(ImageCache[path], path) {
        if (path == null || value != LoadState.Pending) return@produceState
        val loaded = withContext(Dispatchers.Default) {
            runCatching { Res.readBytes("files/$path").decodeToImageBitmap() }.getOrNull()
        }
        value = if (loaded != null) LoadState.Ready(loaded) else LoadState.Missing
        ImageCache[path] = value
    }

    Box(modifier = modifier.background(background), contentAlignment = Alignment.Center) {
        when (val current = state) {
            is LoadState.Ready -> Image(
                bitmap = current.image,
                contentDescription = null,
                contentScale = contentScale,
                colorFilter = colorFilter,
                modifier = Modifier.matchParentSize(),
            )
            LoadState.Missing -> if (fallbackText != null) {
                OvalitText(text = fallbackText.take(1), style = OvalitTheme.typography.label, color = OvalitTheme.colors.t3)
            }
            LoadState.Pending -> Unit
        }
    }
}

private sealed interface LoadState {
    data object Pending : LoadState

    data object Missing : LoadState

    data class Ready(val image: ImageBitmap) : LoadState
}

// 목록을 내릴 때마다 같은 그림을 다시 읽어 들이지 않도록 최근 것만 들고 있는다. 파일이 없었던 경로도 기억해서
// 없는 파일을 거듭 찾지 않는다. 컴포지션 스레드에서만 만진다.
private object ImageCache {
    private const val MAX_ENTRIES = 160
    private val entries = LinkedHashMap<String, LoadState>()

    operator fun get(path: String?): LoadState = if (path == null) LoadState.Missing else entries[path] ?: LoadState.Pending

    operator fun set(path: String, state: LoadState) {
        entries[path] = state
        if (entries.size > MAX_ENTRIES) entries.remove(entries.keys.first())
    }
}
