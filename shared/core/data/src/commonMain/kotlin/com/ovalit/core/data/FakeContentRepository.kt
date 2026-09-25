package com.ovalit.core.data

import com.ovalit.core.model.ContentCatalog
import com.ovalit.core.model.WeaponInfo
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

/** [FakeMatchRepository]에 나오는 요원과 무기 이름입니다. */
class FakeContentRepository : ContentRepository {
    override val catalog: Flow<ContentCatalog> = flowOf(
        ContentCatalog(
            agents = FakeAgents.associate { it.id to it.name },
            weapons = (FakeRifles + FakePistols).associate { it.id to WeaponInfo(it.name, it.category) },
        ),
    )
}
