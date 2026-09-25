package com.ovalit.feature.onboarding.consent

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.ovalit.core.designsystem.component.OvalitDivider
import com.ovalit.core.designsystem.component.OvalitPrimaryButton
import com.ovalit.core.designsystem.component.OvalitText
import com.ovalit.core.designsystem.icon.OvalitIcon
import com.ovalit.core.designsystem.icon.OvalitIcons
import com.ovalit.core.designsystem.theme.OvalitSpacing
import com.ovalit.core.designsystem.theme.OvalitTheme
import com.ovalit.feature.onboarding.resources.Res
import com.ovalit.feature.onboarding.resources.back
import com.ovalit.feature.onboarding.resources.consent_continue
import com.ovalit.feature.onboarding.resources.consent_matches_body
import com.ovalit.feature.onboarding.resources.consent_matches_title
import com.ovalit.feature.onboarding.resources.consent_note
import com.ovalit.feature.onboarding.resources.consent_password
import com.ovalit.feature.onboarding.resources.consent_public_body
import com.ovalit.feature.onboarding.resources.consent_public_title
import com.ovalit.feature.onboarding.resources.consent_title
import com.ovalit.feature.onboarding.resources.consent_unlink_body
import com.ovalit.feature.onboarding.resources.consent_unlink_title
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

/**
 * S0-2 연동 동의입니다. RSO 인증 전이라 여기서도 아무 데이터를 요청하지 않습니다.
 *
 * @param onContinue 다음은 S0-3 Riot 로그인입니다. Custom Tabs로 띄우고 앱은 비밀번호를 만지지 않습니다.
 */
@Composable
fun ConsentScreen(onBack: () -> Unit, onContinue: () -> Unit, modifier: Modifier = Modifier) {
    val colors = OvalitTheme.colors
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.bg)
            .safeDrawingPadding(),
    ) {
        Box(
            modifier = Modifier
                .padding(start = OvalitSpacing.sm, top = OvalitSpacing.sm)
                .size(44.dp)
                .clip(CircleShape)
                .clickable(role = Role.Button, onClick = onBack),
            contentAlignment = Alignment.Center,
        ) {
            OvalitIcon(OvalitIcons.Back, contentDescription = stringResource(Res.string.back), tint = colors.t2)
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = OvalitSpacing.xl),
        ) {
            Spacer(Modifier.height(36.dp))
            OvalitText(
                text = stringResource(Res.string.consent_title),
                modifier = Modifier.semantics { heading() },
                style = OvalitTheme.typography.titleL,
            )
            Spacer(Modifier.height(40.dp))
            OvalitDivider()
            ConsentItem(OvalitIcons.Chart, Res.string.consent_matches_title, Res.string.consent_matches_body)
            OvalitDivider()
            ConsentItem(OvalitIcons.Friends, Res.string.consent_public_title, Res.string.consent_public_body)
            OvalitDivider()
            ConsentItem(OvalitIcons.Restore, Res.string.consent_unlink_title, Res.string.consent_unlink_body)
            OvalitDivider()
            Spacer(Modifier.height(26.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OvalitIcon(OvalitIcons.Lock, contentDescription = null, tint = colors.t4, size = 15.dp, modifier = Modifier.padding(top = 1.dp))
                OvalitText(text = stringResource(Res.string.consent_password), style = OvalitTheme.typography.caption, color = colors.t3)
            }
            Spacer(Modifier.height(OvalitSpacing.xl))
        }

        Column(modifier = Modifier.padding(start = OvalitSpacing.xl, end = OvalitSpacing.xl, bottom = OvalitSpacing.xl)) {
            OvalitPrimaryButton(
                text = stringResource(Res.string.consent_continue),
                onClick = onContinue,
                trailingIcon = OvalitIcons.ArrowRight,
            )
            Spacer(Modifier.height(14.dp))
            OvalitText(
                text = stringResource(Res.string.consent_note),
                modifier = Modifier.fillMaxWidth(),
                style = OvalitTheme.typography.caption,
                color = colors.t4,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun ConsentItem(icon: ImageVector, title: StringResource, body: StringResource) {
    Row(
        modifier = Modifier.fillMaxWidth().semantics(mergeDescendants = true) {}.padding(vertical = 17.dp),
        horizontalArrangement = Arrangement.spacedBy(13.dp),
    ) {
        OvalitIcon(icon, contentDescription = null, tint = OvalitTheme.colors.t3, size = 18.dp, modifier = Modifier.padding(top = 2.dp))
        Column(modifier = Modifier.weight(1f)) {
            OvalitText(text = stringResource(title), style = OvalitTheme.typography.bodyStrong)
            Spacer(Modifier.height(5.dp))
            OvalitText(text = stringResource(body), style = OvalitTheme.typography.caption, color = OvalitTheme.colors.t2)
        }
    }
}
