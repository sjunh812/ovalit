package com.ovalit.ads

import android.app.Activity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import com.ovalit.R
import com.ovalit.core.designsystem.component.OvalitBottomSheet
import com.ovalit.core.designsystem.component.OvalitPrimaryButton
import com.ovalit.core.designsystem.component.OvalitText
import com.ovalit.core.designsystem.component.OvalitTextButton
import com.ovalit.core.designsystem.theme.OvalitSpacing
import com.ovalit.core.designsystem.theme.OvalitTheme

private enum class AdFreeStep { ASK, LOADING, FAILED }

/**
 * 광고를 없애는 시트입니다. 광고 줄의 ×나 설정 줄을 눌렀을 때만 띄우고 저절로 띄우지 않습니다.
 *
 * [onCloseOne]이 있으면 광고 줄의 ×에서 연 것이라 그 광고만 바로 닫는 길도 둡니다.
 * ×를 눌렀는데 광고를 봐야만 닫히면 닫기 버튼으로 속이는 셈입니다.
 *
 * AdMob 보상형 광고 정책을 따릅니다.
 * 무엇을 보고 무엇을 받는지 먼저 적고, "광고 보기"를 눌러야 광고를 받아 띄웁니다.
 * 끝까지 봐서 보상이 났을 때만 [onEarned]를 부릅니다.
 */
@Composable
internal fun AdFreeSheet(
    activity: Activity,
    unitId: String,
    onEarned: () -> Unit,
    onDismiss: () -> Unit,
    onCloseOne: (() -> Unit)? = null,
) {
    var step by remember { mutableStateOf(AdFreeStep.ASK) }

    OvalitBottomSheet(
        title = stringResource(if (onCloseOne != null) R.string.ad_close_title else R.string.ad_free_title),
        body = stringResource(if (onCloseOne != null) R.string.ad_close_body else R.string.ad_free_body),
        onDismiss = onDismiss,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(OvalitSpacing.xs),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            if (step == AdFreeStep.FAILED) {
                OvalitText(
                    text = stringResource(R.string.ad_free_failed),
                    modifier = Modifier.fillMaxWidth(),
                    style = OvalitTheme.typography.caption,
                    color = OvalitTheme.colors.t3,
                )
            }
            OvalitPrimaryButton(
                text = stringResource(
                    when {
                        step == AdFreeStep.LOADING -> R.string.ad_free_loading
                        onCloseOne != null -> R.string.ad_close_watch
                        else -> R.string.ad_free_watch
                    },
                ),
                enabled = step != AdFreeStep.LOADING,
                onClick = {
                    step = AdFreeStep.LOADING
                    RewardedAd.load(
                        activity,
                        unitId,
                        AdRequest.Builder().build(),
                        object : RewardedAdLoadCallback() {
                            override fun onAdLoaded(ad: RewardedAd) {
                                // 광고를 닫으면 보상과 상관없이 시트도 닫는다. 보상은 끝까지 봤을 때만 난다.
                                ad.fullScreenContentCallback = object : FullScreenContentCallback() {
                                    override fun onAdDismissedFullScreenContent() = onDismiss()

                                    override fun onAdFailedToShowFullScreenContent(error: AdError) {
                                        step = AdFreeStep.FAILED
                                    }
                                }
                                ad.show(activity) { onEarned() }
                            }

                            override fun onAdFailedToLoad(error: LoadAdError) {
                                step = AdFreeStep.FAILED
                            }
                        },
                    )
                },
            )
            if (onCloseOne != null) {
                OvalitTextButton(
                    text = stringResource(R.string.ad_close_one),
                    onClick = {
                        onCloseOne()
                        onDismiss()
                    },
                )
            } else {
                OvalitTextButton(text = stringResource(R.string.ad_free_cancel), onClick = onDismiss)
            }
        }
    }
}
