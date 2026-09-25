package com.ovalit.core.data.di

import com.ovalit.core.data.FakeMatchRepository
import com.ovalit.core.data.MatchRepository
import org.koin.dsl.module

// 프로덕션 키가 나오면 FakeMatchRepository를 실제 구현으로 바꾼다.
val dataModule = module {
    single<MatchRepository> { FakeMatchRepository() }
}
