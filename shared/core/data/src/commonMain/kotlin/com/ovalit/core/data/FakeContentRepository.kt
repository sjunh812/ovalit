package com.ovalit.core.data

import com.ovalit.core.model.ContentCatalog
import com.ovalit.core.model.WeaponInfo
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

/**
 * 서버가 붙기 전까지 쓰는 카탈로그입니다. [language]가 `"ja"`면 일본어 이름을, 아니면 한국어 이름을 냅니다. 실제 저장소는 서버에
 * `locale`을 넘겨 그 언어의 카탈로그를 받습니다.
 */
class FakeContentRepository(language: String = "ko") : ContentRepository {
    private val japanese = language == "ja"

    private fun name(korean: String): String = if (japanese) FakeNamesJa[korean] ?: korean else korean

    override val catalog: Flow<ContentCatalog> = flowOf(
        ContentCatalog(
            agents = AgentPool.associate { it.id to name(it.name) },
            weapons = (FakeRifles + FakePistols).associate { it.id to WeaponInfo(name(it.name), it.category) },
            maps = FakeMaps.associate { it.id to name(it.name) },
            tiers = if (japanese) FakeTiersJa else FakeTiers,
        ),
    )
}
