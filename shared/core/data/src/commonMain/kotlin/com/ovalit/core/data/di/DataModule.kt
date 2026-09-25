package com.ovalit.core.data.di

import com.ovalit.core.data.AccountRepository
import com.ovalit.core.data.ContentRepository
import com.ovalit.core.data.DataStoreUserPreferencesRepository
import com.ovalit.core.data.FriendRepository
import com.ovalit.core.data.FakeAccountRepository
import com.ovalit.core.data.FakeContentRepository
import com.ovalit.core.data.FakeFriendRepository
import com.ovalit.core.data.FakeMatchRepository
import com.ovalit.core.data.MatchRepository
import com.ovalit.core.data.UserPreferencesRepository
import com.ovalit.core.data.createPreferencesDataStore
import org.koin.dsl.bind
import org.koin.dsl.module

/** @param preferencesPath 설정 파일을 둘 절대 경로입니다. 플랫폼마다 앱 전용 폴더가 달라서 밖에서 받습니다. */
fun dataModule(preferencesPath: () -> String) = module {
    // 프로덕션 키가 나오면 Fake로 시작하는 저장소를 실제 구현으로 바꾼다.
    single { FakeMatchRepository() } bind MatchRepository::class
    single { FakeFriendRepository() } bind FriendRepository::class
    single { FakeAccountRepository(get(), friendRepository = get()) } bind AccountRepository::class
    single<ContentRepository> { FakeContentRepository() }
    single<UserPreferencesRepository> {
        DataStoreUserPreferencesRepository(createPreferencesDataStore(preferencesPath()))
    }
}
