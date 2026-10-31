package com.ovalit.ads

import android.widget.ImageView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.gms.ads.nativead.MediaView
import com.google.android.gms.ads.nativead.NativeAd
import com.google.android.gms.ads.nativead.NativeAdView
import com.ovalit.core.designsystem.theme.OvalitSpacing
import com.ovalit.core.designsystem.theme.OvalitTheme

/**
 * 왼쪽에 정사각 그림이나 영상을 둔 네이티브 광고 카드입니다.
 * 경기 상세 맨 아래, 프로필, 홈 맨 아래처럼 다 읽은 뒤에 오는 자리에 둡니다.
 * 당근 홈 피드의 광고처럼 목록 한 줄 모양이라 다 읽은 화면 끝을 그림 한 장이 덮지 않습니다.
 *
 * 그림 자리(MediaView)는 가로세로 모두 120dp를 넘어야 영상 광고가 들어와서 120dp에 둡니다.
 * 오른쪽에는 제목, "광고", 버튼을 차례로 쌓고 ×는 오른쪽 위에 둡니다.
 * × 그림의 위끝은 눌림 여백만큼 올려 그림 칸 위끝에 맞춥니다.
 * 큰 글꼴이나 좁은 폭에서 제목 자리가 모자라면 그림 밑으로 글을 내립니다.
 */
@Composable
internal fun NativeAdCard(ad: NativeAd, verticalPadding: Dp, horizontalPadding: Dp, onClose: () -> Unit) {
    // AdMob은 제목 앞 25자(한국어는 그 절반)를 자르지 말라고 해서 두 줄에 한 줄 일곱 자는 들어가야 한다.
    // 한글과 일본어는 한 자가 글자 크기만큼이라 글자 크기로 센다.
    val titleMinWidth = with(LocalDensity.current) { OvalitTheme.typography.body.fontSize.toDp() } * TITLE_MIN_CHARS
    NativeAdFrame(ad) { adView ->
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                // ×의 눌림 여백만큼 오른쪽을 덜 띄워 × 그림 끝이 다른 줄 끝선에 맞는다
                .padding(start = horizontalPadding, end = horizontalPadding - CloseInset, top = verticalPadding, bottom = verticalPadding),
        ) {
            val textWidth = maxWidth - MediaSize - MediaGap - CloseSlot
            if (textWidth >= titleMinWidth) {
                Row(verticalAlignment = Alignment.Top) {
                    AdMedia(adView)
                    Spacer(Modifier.width(MediaGap))
                    AdCardText(ad, adView, Modifier.weight(1f))
                    AdCloseButton(onClose, Modifier.offset(y = -CloseInset))
                }
            } else {
                Column {
                    Row(verticalAlignment = Alignment.Top) {
                        AdMedia(adView)
                        Spacer(Modifier.weight(1f))
                        AdCloseButton(onClose, Modifier.offset(y = -CloseInset))
                    }
                    Spacer(Modifier.height(OvalitSpacing.md))
                    AdCardText(ad, adView)
                }
            }
        }
    }
}

// 정사각이 아닌 그림은 자르지 않고 칸 안에 맞춰 넣는다(AdMob은 그림 높이를 자르지 말라고 한다).
// 남는 위아래는 바탕보다 한 단계 짙게 칠해 그림 칸이 보이게 한다.
@Composable
private fun AdMedia(adView: NativeAdView) {
    AndroidView(
        factory = { context ->
            MediaView(context).also {
                it.setImageScaleType(ImageView.ScaleType.FIT_CENTER)
                adView.mediaView = it
            }
        },
        modifier = Modifier
            .size(MediaSize)
            .clip(RoundedCornerShape(12.dp))
            .background(OvalitTheme.colors.fill),
    )
}

// 버튼을 제목 옆에 두면 좁은 칸에서 제목이 한 줄에 몇 자 못 들어가서 늘 "광고" 밑에 둔다
@Composable
private fun AdCardText(ad: NativeAd, adView: NativeAdView, modifier: Modifier = Modifier) {
    Column(modifier) {
        AdHeadline(ad, adView)
        Spacer(Modifier.height(4.dp))
        AdLabel()
        Spacer(Modifier.height(OvalitSpacing.sm))
        AdCallToAction(ad, adView)
    }
}

private val MediaSize = 120.dp
private val MediaGap = 12.dp
private const val TITLE_MIN_CHARS = 7

// × 앞 띄움과 눌림 영역을 합친 폭이다
private val CloseSlot = OvalitSpacing.sm + CloseTouch
