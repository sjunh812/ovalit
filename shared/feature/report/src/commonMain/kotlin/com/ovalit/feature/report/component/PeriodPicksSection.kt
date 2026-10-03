package com.ovalit.feature.report.component

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.ovalit.core.designsystem.component.OvalitCard
import com.ovalit.core.designsystem.theme.OvalitSpacing
import com.ovalit.core.model.ContentCatalog
import com.ovalit.core.model.WeeklyReport
import com.ovalit.core.ui.AgentTileRow
import com.ovalit.core.ui.ProfileSectionTitle
import com.ovalit.core.ui.WeaponTileRow
import com.ovalit.core.ui.periodLabel
import com.ovalit.feature.report.resources.Res
import com.ovalit.feature.report.resources.picks_agents
import com.ovalit.feature.report.resources.picks_weapons
import org.jetbrains.compose.resources.stringResource

/**
 * 리포트 기간에 많이 뛴 요원 셋과 킬을 많이 낸 무기 셋입니다. 내 프로필과 같은 칸이고 누르면 S7과 S6으로 갑니다. S6과
 * S7은 이번 액트 전체를 세서 숫자가 다르니 제목에 기간을 붙입니다("이번 주 요원"). 요원은 기간이 짧아 5판을 못
 * 넘기는 일이 많아서 판 수 대신 승패를 적습니다.
 */
@Composable
internal fun PeriodPicksSection(
    report: WeeklyReport.Ready,
    catalog: ContentCatalog,
    onOpenAgents: () -> Unit,
    onOpenWeapons: () -> Unit,
) {
    val period = periodLabel(report.period)
    // 카드 전체가 눌린다
    if (report.agents.isNotEmpty()) {
        OvalitCard(onClick = onOpenAgents) {
            PickSection(title = stringResource(Res.string.picks_agents, period)) {
                AgentTileRow(report.agents, catalog, showRecord = true)
            }
        }
    }
    if (report.weapons.isNotEmpty()) {
        OvalitCard(onClick = onOpenWeapons) {
            PickSection(title = stringResource(Res.string.picks_weapons, period)) {
                WeaponTileRow(report.weapons, catalog)
            }
        }
    }
}

@Composable
private fun PickSection(title: String, content: @Composable () -> Unit) {
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = OvalitSpacing.gutter)) {
        ProfileSectionTitle(title = title, chevron = true)
        Spacer(Modifier.height(SectionTitleGap))
        content()
    }
}
