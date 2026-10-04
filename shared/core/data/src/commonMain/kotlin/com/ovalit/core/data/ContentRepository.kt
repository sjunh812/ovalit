package com.ovalit.core.data

import com.ovalit.core.model.ContentCatalog
import kotlinx.coroutines.flow.Flow

/** 요원·무기·맵·티어 이름을 담은 [ContentCatalog]입니다. 패치 때마다 바뀌어서 한 번 받아 두고 계속 쓰면 안 됩니다. */
interface ContentRepository {
    val catalog: Flow<ContentCatalog>
}
