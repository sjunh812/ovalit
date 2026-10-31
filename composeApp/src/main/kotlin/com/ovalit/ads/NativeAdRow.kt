package com.ovalit.ads

import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.graphics.drawable.toBitmap
import com.google.android.gms.ads.nativead.NativeAd
import com.google.android.gms.ads.nativead.NativeAdView
import com.ovalit.R
import com.ovalit.core.designsystem.component.OvalitSideOrBelow
import com.ovalit.core.designsystem.component.OvalitText
import com.ovalit.core.designsystem.icon.OvalitIcon
import com.ovalit.core.designsystem.icon.OvalitIcons
import com.ovalit.core.designsystem.theme.OvalitSpacing
import com.ovalit.core.designsystem.theme.OvalitTheme

/**
 * 네이티브 광고 한 줄입니다.
 * 제목 밑에 "광고"를 적어 경기 기록과 헷갈리지 않게 합니다.
 *
 * AdMob은 광고 요소마다 안드로이드 뷰를 등록해야 눌림을 셉니다.
 * 바탕까지 눌리게 하면 정책 위반이라 요소마다 따로 등록합니다.
 *
 * AdMob은 제목의 앞 25자(한국어는 그 절반)는 자르지 말라고 합니다.
 * 한 줄로 자르면 384dp 폰에서 한국어 열 자 안팎만 남아서 제목은 두 줄까지 두고 본문과 광고주는 띄우지 않습니다.
 * 본문과 광고주는 꼭 보여야 하는 요소가 아닙니다.
 */
@Composable
internal fun NativeAdRow(ad: NativeAd, verticalPadding: Dp, horizontalPadding: Dp, onClose: () -> Unit) {
    val colors = OvalitTheme.colors
    NativeAdFrame(ad) { adView ->
        Row(
            modifier = Modifier
                .fillMaxWidth()
                // ×의 눌리는 영역 안쪽 여백만큼 오른쪽을 덜 띄워 × 그림의 오른쪽 끝이 다른 줄의 스코어 끝선에 맞는다
                .padding(start = horizontalPadding, end = horizontalPadding - CloseInset, top = verticalPadding, bottom = verticalPadding),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // 크기를 모르는 아이콘은 toBitmap()이 예외를 던져서 두지 않는다
            ad.icon?.drawable?.takeIf { it.intrinsicWidth > 0 && it.intrinsicHeight > 0 }?.let { drawable ->
                val bitmap = remember(drawable) { drawable.toBitmap().asImageBitmap() }
                AdAsset(adView, register = { iconView = it }) {
                    Image(
                        bitmap = bitmap,
                        contentDescription = null,
                        modifier = Modifier.size(38.dp).clip(RoundedCornerShape(9.dp)),
                        contentScale = ContentScale.Crop,
                    )
                }
                Spacer(Modifier.width(13.dp))
            }
            AdText(ad, adView, Modifier.weight(1f))
            AdCloseButton(onClose)
        }
    }
}

/**
 * 제목과 "광고" 표시, 그 옆의 광고 버튼입니다.
 * 큰 글꼴에서 버튼이 줄의 절반을 넘으면 "광고" 밑으로 내립니다.
 * 옆에 두면 제목이 한 자씩 꺾여 25자를 보여야 한다는 AdMob 규칙도 못 지킵니다.
 */
@Composable
internal fun AdText(ad: NativeAd, adView: NativeAdView, modifier: Modifier = Modifier) {
    OvalitSideOrBelow(
        start = {
            Column {
                AdHeadline(ad, adView)
                Spacer(Modifier.height(4.dp))
                AdLabel()
            }
        },
        end = { AdCallToAction(ad, adView) },
        modifier = modifier,
        gap = OvalitSpacing.sm,
        belowGap = OvalitSpacing.sm,
    )
}

/** 광고 제목입니다. 경기 줄 이름처럼 Medium이고 두 줄까지 둡니다. */
@Composable
internal fun AdHeadline(ad: NativeAd, adView: NativeAdView) {
    AdAsset(adView, register = { headlineView = it }) {
        OvalitText(
            text = ad.headline.orEmpty(),
            style = OvalitTheme.typography.body.copy(fontWeight = FontWeight.Medium),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/** 광고 버튼("설치하기")입니다. 광고에 없으면 두지 않습니다. */
@Composable
internal fun AdCallToAction(ad: NativeAd, adView: NativeAdView) {
    val action = ad.callToAction ?: return
    AdAsset(adView, register = { callToActionView = it }) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(OvalitTheme.colors.fill)
                .padding(horizontal = 12.dp, vertical = 7.dp),
        ) {
            OvalitText(
                text = action,
                style = OvalitTheme.typography.label.copy(fontWeight = FontWeight.SemiBold),
                maxLines = 1,
            )
        }
    }
}

// 광고 요소로 등록하지 않아서 ×를 눌러도 광고가 열리지 않는다.
// 광고 버튼과 붙어 있으면 ×를 노리다 광고를 누르기 쉬워서 8dp 띄운다(AdMob 정책은 닫기와 광고 요소가 겹치면 무효 클릭으로 본다).
@Composable
internal fun AdCloseButton(onClose: () -> Unit, modifier: Modifier = Modifier) {
    Spacer(Modifier.width(OvalitSpacing.sm))
    Box(
        modifier = modifier
            .size(CloseTouch)
            .clickable(role = Role.Button, onClick = onClose),
        contentAlignment = Alignment.Center,
    ) {
        OvalitIcon(OvalitIcons.Close, contentDescription = stringResource(R.string.ad_close), tint = OvalitTheme.colors.t3, size = CloseIcon)
    }
}

// 안드로이드 눌림 영역 기준(48dp)이다
internal val CloseTouch = 48.dp
private val CloseIcon = 16.dp

/**
 * × 그림 바깥의 눌림 여백입니다.
 * 광고 오른쪽 여백을 이만큼 덜 띄워 × 그림 끝을 다른 줄 끝선에 맞춥니다.
 */
internal val CloseInset = (CloseTouch - CloseIcon) / 2

@Composable
internal fun AdLabel() {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(5.dp))
            .background(OvalitTheme.colors.fill)
            .padding(horizontal = 5.dp, vertical = 2.dp),
    ) {
        OvalitText(
            text = stringResource(R.string.ad_label),
            style = OvalitTheme.typography.caption.copy(fontSize = 11.sp, lineHeight = 14.sp, fontWeight = FontWeight.Medium),
            color = OvalitTheme.colors.t2,
            maxLines = 1,
        )
    }
}

/**
 * [NativeAdView] 안에 Compose로 그린 광고를 담습니다.
 * 요소가 모두 등록된 뒤에 광고를 붙여야 눌림이 이어져서 안쪽이 두 프레임 그려진 뒤에 붙입니다.
 */
@Composable
internal fun NativeAdFrame(ad: NativeAd, content: @Composable (NativeAdView) -> Unit) {
    val context = LocalContext.current
    val adView = remember { NativeAdView(context) }
    AndroidView(
        factory = {
            adView.apply {
                addView(
                    ComposeView(context).apply { setContent { content(adView) } },
                    ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT),
                )
            }
        },
        modifier = Modifier.fillMaxWidth(),
    )
    LaunchedEffect(ad) {
        withFrameNanos {}
        withFrameNanos {}
        adView.setNativeAd(ad)
    }
    DisposableEffect(adView) { onDispose { adView.destroy() } }
}

/** 광고 요소 하나를 안드로이드 뷰로 감싸 [register]로 [NativeAdView]에 등록합니다. */
@Composable
internal fun AdAsset(
    adView: NativeAdView,
    register: NativeAdView.(View) -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    AndroidView(
        modifier = modifier,
        factory = { context ->
            ComposeView(context).apply {
                setContent(content)
                adView.register(this)
            }
        },
    )
}
