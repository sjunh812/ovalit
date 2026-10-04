package com.ovalit.core.data.di

import com.ovalit.core.data.AccountRepository
import com.ovalit.core.data.ContentRepository
import com.ovalit.core.data.DataStoreUserPreferencesRepository
import com.ovalit.core.data.FakeAccountRepository
import com.ovalit.core.data.FakeContentRepository
import com.ovalit.core.data.FakeFriendRepository
import com.ovalit.core.data.FakeMatchRepository
import com.ovalit.core.data.FakePingRepository
import com.ovalit.core.data.FakePushRepository
import com.ovalit.core.data.FriendRepository
import com.ovalit.core.data.MatchRepository
import com.ovalit.core.data.NewMatchesWatcher
import com.ovalit.core.data.PingRepository
import com.ovalit.core.data.PushRepository
import com.ovalit.core.data.UserPreferencesRepository
import com.ovalit.core.data.createPreferencesDataStore
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import org.koin.core.qualifier.named
import org.koin.dsl.bind
import org.koin.dsl.module

/** 앱이 사는 동안 도는 [CoroutineScope]입니다. 화면이 사라져도 이어져야 하는 일(새 경기 받기)을 여기서 합니다. */
val ApplicationScope = named("application")

/**
 * @param preferencesPath 설정 파일을 둘 절대 경로입니다. 플랫폼마다 앱 전용 폴더가 달라서 밖에서 받습니다.
 * `.preferences_pb`로 끝나야 합니다.
 */
fun dataModule(preferencesPath: () -> String) = module {
    // 여기서 도는 일은 실패를 스스로 다룬다. 놓친 실패가 있어도 앱을 죽이지 않는다.
    single(ApplicationScope) { CoroutineScope(SupervisorJob() + Dispatchers.Default + CoroutineExceptionHandler { _, _ -> }) }
    // 프로덕션 키가 나오면 Fake로 시작하는 저장소를 실제 구현으로 바꾼다.
    single { FakeMatchRepository(scope = get(ApplicationScope)) } bind MatchRepository::class
    single { FakeFriendRepository() } bind FriendRepository::class
    single { FakePingRepository(get()) } bind PingRepository::class
    single<PushRepository> { FakePushRepository() }
    single { FakeAccountRepository(get(), friendRepository = get(), pingRepository = get()) } bind AccountRepository::class
    single<ContentRepository> { FakeContentRepository() }
    // ImportScheduler는 앱 모듈이 넣는다
    single { NewMatchesWatcher(get(), get(), get(), get(ApplicationScope)) }
    single<UserPreferencesRepository> {
        DataStoreUserPreferencesRepository(createPreferencesDataStore(preferencesPath()))
    }
}
