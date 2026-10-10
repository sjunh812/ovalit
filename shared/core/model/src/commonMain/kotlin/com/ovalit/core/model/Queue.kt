package com.ovalit.core.model

/**
 * 경기 큐입니다. 응답의 `matchInfo.queueId`를 [fromRiot]로 옮깁니다.
 *
 * @property halfRounds 전반 라운드 수입니다. 공수가 바뀌는 시점이 모드마다 다릅니다. 라운드제가 아니면 `null`입니다.
 * @property hasEconomy 크레드로 장비를 사는 규칙이 경쟁전과 같은지입니다. 아니면 이코·포스바이를 가르지 않습니다.
 * @property countsInReports 리포트와 평균에 넣는지입니다.
 *   `false`인 모드는 다시 살아나거나 혼자 싸우는 등 규칙이 달라 K/D와 헤드샷도 다른 모드와 같은 잣대로 볼 수 없습니다.
 *   경기 탭 목록에만 둡니다.
 */
enum class Queue(
    val halfRounds: Int?,
    val hasEconomy: Boolean,
    val countsInReports: Boolean,
) {
    COMPETITIVE(halfRounds = 12, hasEconomy = true, countsInReports = true),

    UNRATED(halfRounds = 12, hasEconomy = true, countsInReports = true),

    /** 프리미어입니다. 규칙은 경쟁전과 같지만 티어가 아니라 팀 순위가 걸려서 경쟁 + 일반에 넣지 않습니다. */
    PREMIER(halfRounds = 12, hasEconomy = true, countsInReports = true),

    /** 스파이크 돌격입니다. 무기를 무작위로 주고 4라운드 선취라 이코노미 지표를 셀 수 없습니다. */
    SPIKE_RUSH(halfRounds = 3, hasEconomy = false, countsInReports = true),

    /** 신속 플레이입니다. 5라운드 선취이고 크레드 규칙이 달라 이코노미 지표를 같이 셀 수 없습니다. */
    SWIFTPLAY(halfRounds = 4, hasEconomy = false, countsInReports = true),

    /** 복제입니다. 한 팀이 모두 같은 요원으로 5라운드 선취를 다투고 크레드는 라운드마다 정해져 있습니다. */
    REPLICATION(halfRounds = 4, hasEconomy = false, countsInReports = true),

    /** 에스컬레이션입니다. 다시 살아나며 정해진 무기로 단계를 올리는 팀전이라 라운드가 없습니다. */
    ESCALATION(halfRounds = null, hasEconomy = false, countsInReports = false),

    /** 데스매치입니다. 모두가 서로 적이고 다시 살아납니다. */
    DEATHMATCH(halfRounds = null, hasEconomy = false, countsInReports = false),

    /** 팀 데스매치입니다. 다시 살아나며 팀 킬 수를 겨뤄서 라운드가 없습니다. */
    TEAM_DEATHMATCH(halfRounds = null, hasEconomy = false, countsInReports = false),

    /** 눈싸움입니다. 눈덩이 발사기만 쓰는 팀 데스매치입니다. */
    SNOWBALL_FIGHT(halfRounds = null, hasEconomy = false, countsInReports = false),

    /**
     * 큐 ID를 모르는 모드입니다.
     * 13.06의 건틀릿: 글리치처럼 새로 나온 모드가 여기로 옵니다.
     * 규칙을 몰라서 라운드제로 보지 않고 목록에만 둡니다.
     */
    OTHER(halfRounds = null, hasEconomy = false, countsInReports = false),
    ;

    companion object {
        /**
         * 응답의 `queueId`를 옮깁니다. 경기 ID 목록(`matchlists`)에도 같은 값이 있어서 상세를 받기 전에 거를 수 있습니다.
         *
         * 커스텀 게임이면 `null`이고, 커스텀 게임은 받지도 저장하지도 않습니다.
         * 모르는 값은 [OTHER]라서 새 모드가 나와도 기타 목록에 뜹니다.
         *
         * @param provisioningFlowId 경기 상세의 `matchInfo.provisioningFlowId`입니다. 경기 ID 목록에는 없어서 빼고 불러도 됩니다.
         */
        fun fromRiot(queueId: String, provisioningFlowId: String? = null): Queue? {
            if (provisioningFlowId.equals(CUSTOM_GAME_FLOW, ignoreCase = true)) return null
            return when (queueId.trim().lowercase()) {
                "", "custom" -> null
                "competitive" -> COMPETITIVE
                // 새 맵 큐는 새 맵에서만 도는 일반전이라 일반과 같이 센다
                "unrated", "newmap" -> UNRATED
                "premier" -> PREMIER
                "spikerush" -> SPIKE_RUSH
                "swiftplay" -> SWIFTPLAY
                "onefa" -> REPLICATION
                "ggteam" -> ESCALATION
                "deathmatch" -> DEATHMATCH
                "hurm" -> TEAM_DEATHMATCH
                "snowball" -> SNOWBALL_FIGHT
                else -> OTHER
            }
        }

        private const val CUSTOM_GAME_FLOW = "CustomGame"
    }
}
