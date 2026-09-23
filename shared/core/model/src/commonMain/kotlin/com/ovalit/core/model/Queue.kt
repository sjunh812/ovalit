package com.ovalit.core.model

/**
 * 큐 종류입니다.
 *
 * 식별자는 영어로 둡니다. 화면에 뜨는 이름은 문자열 리소스에서 가져옵니다. 코드에 한글
 * 식별자를 두면 스택 트레이스와 도구에서 다루기 어렵습니다.
 */
enum class Queue {
    /** 경쟁. */
    COMPETITIVE,

    /** 일반. */
    UNRATED,

    /** 스파이크 돌격. 무기를 랜덤으로 주고 4라운드 선취라 이코노미 지표가 성립하지 않는다. */
    SPIKE_RUSH,

    /** 신속 플레이. 크레드 규칙이 달라 이코노미 지표를 같이 셀 수 없다. */
    SWIFTPLAY,

    /** 그 밖의 모드. 데스매치, 팀 데스매치, 에스컬레이션 등. */
    OTHER,
    ;

    /**
     * 기본 집계에 들어가는 큐인지.
     *
     * 경쟁과 일반은 13라운드 선취에 이코노미 규칙이 같아 합산해도 통계가 깨지지 않습니다.
     * 나머지는 규칙이 달라 섞으면 숫자가 의미를 잃습니다.
     */
    val countsTowardWeeklyReport: Boolean
        get() = this == COMPETITIVE || this == UNRATED
}
