package com.ovalit.core.data

/**
 * 첫 수집을 시작하고 멈춥니다. 안드로이드는 앱을 닫아도 이어 받도록 WorkManager에 맡기고, 다 받으면 알림을 보냅니다.
 * 주기적으로 다시 받는 동기화는 두지 않습니다.
 */
interface ImportScheduler {
    fun start()

    /** 연동을 해제할 때 부릅니다. 돌고 있거나 기다리는 수집을 멈춰, 해제한 뒤에 경기를 다시 채우거나 알림을 보내지 않습니다. */
    fun cancel()
}
