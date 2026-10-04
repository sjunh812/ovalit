package com.ovalit.core.data

import com.ovalit.core.model.ContentCatalog
import com.ovalit.core.model.WeaponInfo
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

/**
 * 서버가 붙기 전까지 쓰는 카탈로그입니다. [language]가 `"ja"`면 일본어 이름을, 아니면 한국어 이름을 냅니다. 실제 저장소는 서버에
 * `locale`을 넘겨 그 언어의 카탈로그를 받습니다.
 *
 * @param language 화면 언어입니다. 앱이 켜진 채로 앱 언어를 바꾸면 새 값이 흘러 카탈로그도 그 언어로 다시 냅니다.
 */
class FakeContentRepository(language: Flow<String> = flowOf("ko")) : ContentRepository {

    override val catalog: Flow<ContentCatalog> = language.distinctUntilChanged().map(::catalogIn)

    private fun catalogIn(language: String): ContentCatalog {
        val japanese = language == "ja"
        fun name(korean: String): String = if (japanese) FakeNamesJa[korean] ?: korean else korean
        return ContentCatalog(
            agents = AgentPool.associate { it.id to name(it.name) },
            weapons = (FakeRifles + FakePistols).associate { it.id to WeaponInfo(name(it.name), it.category) },
            maps = FakeMaps.associate { it.id to name(it.name) },
            tiers = if (japanese) FakeTiersJa else FakeTiers,
        )
    }
}
