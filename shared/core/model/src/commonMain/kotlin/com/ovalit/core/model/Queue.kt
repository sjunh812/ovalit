package com.ovalit.core.model

enum class Queue {
    COMPETITIVE,

    UNRATED,

    /** 스파이크 돌격입니다. 무기를 무작위로 주고 4라운드 선취라 이코노미 지표를 셀 수 없습니다. */
    SPIKE_RUSH,

    /** 신속 플레이입니다. 크레드 규칙이 달라 이코노미 지표를 같이 셀 수 없습니다. */
    SWIFTPLAY,

    /** 데스매치, 팀 데스매치, 에스컬레이션 같은 그 밖의 모드입니다. */
    OTHER,
    ;

    /** 전반 라운드 수입니다. 공수가 바뀌는 시점이 모드마다 다릅니다. 라운드제가 아니면 `null`입니다. */
    val halfRounds: Int?
        get() = when (this) {
            COMPETITIVE, UNRATED -> 12
            SWIFTPLAY -> 4
            SPIKE_RUSH -> 3
            OTHER -> null
        }

    /** 크레드로 장비를 사는 규칙이 경쟁전과 같은지입니다. 아니면 이코·포스바이를 가르지 않습니다. */
    val hasEconomy: Boolean
        get() = this == COMPETITIVE || this == UNRATED
}
