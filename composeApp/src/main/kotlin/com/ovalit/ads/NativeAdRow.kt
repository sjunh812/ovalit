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
import com.ovalit.core.designsystem.component.OvalitText
import com.ovalit.core.designsystem.icon.OvalitIcon
import com.ovalit.core.designsystem.icon.OvalitIcons
import com.ovalit.core.designsystem.theme.OvalitSpacing
import com.ovalit.core.designsystem.theme.OvalitTheme

/**
 * 네이티브 광고 한 줄입니다. 경기 줄처럼 왼쪽에 아이콘, 가운데 제목과 광고주, 오른쪽에 버튼을 둡니다. 제목 밑 줄 맨 앞에 "광고"를
 * 적어 경기 기록과 헷갈리지 않게 합니다. 오른쪽 끝에 광고를 닫는 ×를 두고, AdChoices 표시는 SDK가 왼쪽 위 여백에 얹습니다.
 *
 * AdMob은 광고 요소마다 안드로이드 뷰를 등록해야 눌림을 셉니다. 바탕까지 눌리게 하면 정책 위반이라 요소마다 따로 등록합니다.
 */
@Composable
internal fun NativeAdRow(ad: NativeAd, verticalPadding: Dp, onClose: () -> Unit) {
    val colors = OvalitTheme.colors
    NativeAdFrame(ad) { adView ->
        Row(
            modifier = Modifier
                .fillMaxWidth()
                // ×의 눌리는 영역(36dp) 안쪽 여백만큼 오른쪽을 덜 띄워, × 그림의 오른쪽 끝이 다른 줄의 스코어 끝선에 맞는다
                .padding(start = OvalitSpacing.gutter, end = OvalitSpacing.gutter - CloseInset, top = verticalPadding, bottom = verticalPadding),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ad.icon?.drawable?.let { drawable ->
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
            Column(modifier = Modifier.weight(1f)) {
                AdAsset(adView, register = { headlineView = it }) {
                    OvalitText(
                        text = ad.headline.orEmpty(),
                        style = OvalitTheme.typography.bodyStrong,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Spacer(Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    AdLabel()
                    val line = ad.advertiser ?: ad.body
                    if (line != null) {
                        Spacer(Modifier.width(6.dp))
                        AdAsset(
                            adView = adView,
                            register = { if (ad.advertiser != null) advertiserView = it else bodyView = it },
                            modifier = Modifier.weight(1f, fill = false),
                        ) {
                            OvalitText(
                                text = line,
                                style = OvalitTheme.typography.caption,
                                color = colors.t3,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                }
            }
            ad.callToAction?.let { action ->
                Spacer(Modifier.width(OvalitSpacing.sm))
                AdAsset(adView, register = { callToActionView = it }) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(colors.fill)
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
            // 광고 오른쪽 끝의 ×다(사용자 요청, 2026-10-04). 인스타그램의 "⋯"처럼 광고를 닫는 자리로 익숙한 곳이다. 둘째 줄 끝에
            // 작은 "숨기기" 글자로 두니 눈에 띄지 않았다. 광고 요소로 등록하지 않아 눌러도 광고가 열리지 않는다.
            Spacer(Modifier.width(2.dp))
            Box(
                modifier = Modifier
                    .size(CloseTouch)
                    .clickable(role = Role.Button, onClick = onClose),
                contentAlignment = Alignment.Center,
            ) {
                OvalitIcon(OvalitIcons.Close, contentDescription = stringResource(R.string.ad_close), tint = colors.t3, size = CloseIcon)
            }
        }
    }
}

private val CloseTouch = 36.dp
private val CloseIcon = 16.dp
private val CloseInset = (CloseTouch - CloseIcon) / 2

// 경기 줄의 등수 칩처럼 --fill 면에 흐린 글자로 "광고"를 적는다
@Composable
private fun AdLabel() {
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
 * [NativeAdView] 안에 Compose로 그린 광고를 담습니다. 요소가 모두 등록된 뒤에 광고를 붙여야 눌림이 이어져서, 안쪽이 두 프레임
 * 그려진 뒤에 붙입니다.
 */
@Composable
private fun NativeAdFrame(ad: NativeAd, content: @Composable (NativeAdView) -> Unit) {
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
private fun AdAsset(
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
