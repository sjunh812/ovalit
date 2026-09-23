package com.ovalit.core.designsystem.preview

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ovalit.core.designsystem.component.OvalitLogo
import com.ovalit.core.designsystem.component.OvalitPrimaryButton
import com.ovalit.core.designsystem.component.OvalitText
import com.ovalit.core.designsystem.component.OvalitTextButton
import com.ovalit.core.designsystem.theme.OvalitSpacing
import com.ovalit.core.designsystem.theme.OvalitTheme

private const val SAMPLE_GLYPHS = "가나다 Aa 24.7%"

@Preview
@Composable
private fun ColorTokensDarkPreview() {
    OvalitThemePreview(darkTheme = true) { ColorTokens() }
}

@Preview
@Composable
private fun ColorTokensLightPreview() {
    OvalitThemePreview(darkTheme = false) { ColorTokens() }
}

@Preview
@Composable
private fun TypographyDarkPreview() {
    OvalitThemePreview(darkTheme = true) { TypeScale() }
}

@Preview
@Composable
private fun TypographyLightPreview() {
    OvalitThemePreview(darkTheme = false) { TypeScale() }
}

@Preview
@Composable
private fun ComponentsDarkPreview() {
    OvalitThemePreview(darkTheme = true) { Components() }
}

@Preview
@Composable
private fun ComponentsLightPreview() {
    OvalitThemePreview(darkTheme = false) { Components() }
}

@Composable
private fun ColorTokens() {
    val colors = OvalitTheme.colors

    Column(
        modifier = Modifier.width(360.dp).padding(OvalitSpacing.lg),
        verticalArrangement = Arrangement.spacedBy(OvalitSpacing.md),
    ) {
        SectionTitle("면")
        SurfaceSwatch("bg", colors.bg)
        SurfaceSwatch("raised", colors.raised)
        SurfaceSwatch("fill", colors.fill)
        SurfaceSwatch("line", colors.line)
        SurfaceSwatch("lineWeak", colors.lineWeak)

        Spacer(Modifier.height(OvalitSpacing.sm))
        SectionTitle("글자")
        TextSwatch("t1", colors.t1)
        TextSwatch("t2", colors.t2)
        TextSwatch("t3", colors.t3)
        TextSwatch("t4", colors.t4)
        TextSwatch("t5", colors.t5)

        Spacer(Modifier.height(OvalitSpacing.sm))
        SectionTitle("의미")
        TextSwatch("pos", colors.pos)
        TextSwatch("neg", colors.neg)
        TextSwatch("accentInk", colors.accentInk)
        SurfaceSwatch("accent", colors.accent)
    }
}

@Composable
private fun TypeScale() {
    val type = OvalitTheme.typography

    Column(
        modifier = Modifier.width(360.dp).padding(OvalitSpacing.lg),
        verticalArrangement = Arrangement.spacedBy(OvalitSpacing.md),
    ) {
        TypeRow("metricXl", type.metricXl, "24.7")
        TypeRow("metricL", type.metricL, "1.42")
        TypeRow("metricM", type.metricM, "188")
        TypeRow("titleL", type.titleL, "이번 주 내 경기, 뭐가 달라졌을까")
        TypeRow("titleM", type.titleM, "이번 주 달라진 지표")
        TypeRow("body", type.body, "헤드샷 비율이 지난 4주보다 올랐어요")
        TypeRow("bodyStrong", type.bodyStrong, "헤드샷 비율이 올랐어요")
        TypeRow("label", type.label, "전투점수")
        TypeRow("caption", type.caption, "11경기 · 판당 16킬")
    }
}

@Composable
private fun Components() {
    Column(
        modifier = Modifier.width(360.dp).padding(OvalitSpacing.lg),
        verticalArrangement = Arrangement.spacedBy(OvalitSpacing.lg),
    ) {
        OvalitLogo(modifier = Modifier.width(140.dp))
        OvalitPrimaryButton(text = "Riot 계정으로 시작하기", onClick = {})
        OvalitPrimaryButton(text = "불러오는 중", onClick = {}, enabled = false)
        OvalitTextButton(text = "나중에 하기", onClick = {})
    }
}

@Composable
private fun SectionTitle(text: String) {
    OvalitText(
        text = text,
        style = OvalitTheme.typography.label,
        color = OvalitTheme.colors.t3,
    )
}

@Composable
private fun SurfaceSwatch(name: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Spacer(
            Modifier
                .size(40.dp)
                .background(color, RoundedCornerShape(6.dp))
                .border(1.dp, OvalitTheme.colors.line, RoundedCornerShape(6.dp)),
        )
        Spacer(Modifier.width(OvalitSpacing.md))
        OvalitText(text = name, style = OvalitTheme.typography.label)
    }
}

@Composable
private fun TextSwatch(name: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        OvalitText(
            text = name,
            modifier = Modifier.width(84.dp),
            style = OvalitTheme.typography.caption,
            color = OvalitTheme.colors.t3,
        )
        OvalitText(
            text = SAMPLE_GLYPHS,
            style = OvalitTheme.typography.bodyStrong,
            color = color,
        )
    }
}

@Composable
private fun TypeRow(name: String, style: TextStyle, sample: String) {
    Column(modifier = Modifier.fillMaxWidth()) {
        OvalitText(
            text = name,
            style = OvalitTheme.typography.caption,
            color = OvalitTheme.colors.t3,
        )
        Spacer(Modifier.height(OvalitSpacing.xs))
        OvalitText(text = sample, style = style)
    }
}
