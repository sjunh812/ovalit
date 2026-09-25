package com.ovalit.core.data

import com.ovalit.core.model.ContentCatalog
import kotlinx.coroutines.flow.Flow

/** VAL-CONTENT의 요원·무기 이름입니다. 패치 때마다 바뀌므로 한 번 받아 두고 계속 쓰면 안 됩니다. */
interface ContentRepository {
    val catalog: Flow<ContentCatalog>
}
