package com.ovalit.core.network.dto

import kotlinx.serialization.Serializable

// VAL-MATCH-V1 `/val/match/v1/matches/{matchId}`의 응답이다.
// 우리 서버가 Riot 응답을 그대로 넘겨주고, 내가 안 뛴 친구 경기만 다른 사람을 가려서 준다(docs/backend.md).
// 필드 이름과 타입은 Riot 개발자 문서를 따랐다.
//
// 실제 응답을 아직 못 봤다.
// 문서가 필수라고 적은 필드도 모드에 따라 빠지거나 null로 올 수 있고, 가린 사람의 줄은 서버가 남긴 필드만 있다.
// 그래서 거의 모든 필드를 null이나 빈 목록으로 받아 두고, 무엇이 빠졌을 때 어떻게 할지는 옮겨 담는 쪽(MatchMapper)이 정한다.
// 쓰지 않는 필드도 교전 거리처럼 나중에 셀 지표가 쓰니 문서에 있는 것은 적어 둔다.

@Serializable
data class MatchDto(
    val matchInfo: MatchInfoDto? = null,
    val players: List<PlayerDto> = emptyList(),
    val coaches: List<CoachDto> = emptyList(),
    val teams: List<TeamDto> = emptyList(),
    val roundResults: List<RoundResultDto> = emptyList(),
)

/**
 * 문서의 `premierMatchInfo`는 두지 않습니다. 안의 모양이 문서에 없고, 서버가 가린 친구 경기에서는 빠져 옵니다.
 *
 * @property mapId UUID로 올지 `/Game/Maps/Ascent/Ascent` 같은 경로로 올지 모릅니다.
 * @property queueId 커스텀 게임이면 빈 값입니다.
 * @property provisioningFlowId 커스텀 게임이면 `CustomGame`입니다.
 * @property seasonId 액트 UUID입니다.
 */
@Serializable
data class MatchInfoDto(
    val matchId: String? = null,
    val mapId: String? = null,
    val gameVersion: String? = null,
    val gameLengthMillis: Long? = null,
    val region: String? = null,
    val gameStartMillis: Long? = null,
    val provisioningFlowId: String? = null,
    val isCompleted: Boolean? = null,
    val customGameName: String? = null,
    val queueId: String? = null,
    val gameMode: String? = null,
    val isRanked: Boolean? = null,
    val seasonId: String? = null,
)

/**
 * 가린 사람의 줄은 [puuid]가 `anon-N`이고 [gameName]과 [tagLine]이 빈 값이며, [playerCard], [playerTitle], [accountLevel]이 빠져 있습니다.
 *
 * @property teamId 라운드제 모드는 `Red`나 `Blue`이고, 데스매치는 그 사람의 PUUID입니다.
 * @property competitiveTier 0이면 티어가 없습니다.
 * @property isObserver 관전자면 `true`이고 [characterId]와 [stats]가 빠져 있을 수 있습니다.
 */
@Serializable
data class PlayerDto(
    val puuid: String? = null,
    val gameName: String? = null,
    val tagLine: String? = null,
    val teamId: String? = null,
    val partyId: String? = null,
    val characterId: String? = null,
    val stats: PlayerStatsDto? = null,
    val competitiveTier: Int? = null,
    val isObserver: Boolean? = null,
    val playerCard: String? = null,
    val playerTitle: String? = null,
    val accountLevel: Int? = null,
)

/**
 * 스코어보드 숫자입니다. 스킬로 자기나 우리 팀을 죽인 것을 Riot이 킬에서 빼는지 모릅니다.
 *
 * @property score 경기 전체 전투점수 합입니다. 라운드당 값이 아닙니다.
 */
@Serializable
data class PlayerStatsDto(
    val score: Int? = null,
    val roundsPlayed: Int? = null,
    val kills: Int? = null,
    val deaths: Int? = null,
    val assists: Int? = null,
    val playtimeMillis: Long? = null,
    val abilityCasts: AbilityCastsDto? = null,
)

@Serializable
data class AbilityCastsDto(
    val grenadeCasts: Int? = null,
    val ability1Casts: Int? = null,
    val ability2Casts: Int? = null,
    val ultimateCasts: Int? = null,
)

@Serializable
data class CoachDto(
    val puuid: String? = null,
    val teamId: String? = null,
)

/**
 * @property teamId 라운드제 모드는 `Red`나 `Blue`이고, 데스매치는 그 사람의 PUUID입니다.
 * @property numPoints 팀 점수입니다. 데스매치는 그 사람의 킬입니다.
 */
@Serializable
data class TeamDto(
    val teamId: String? = null,
    val won: Boolean? = null,
    val roundsPlayed: Int? = null,
    val roundsWon: Int? = null,
    val numPoints: Int? = null,
)

/**
 * @property roundNum 0부터인지 1부터인지 모릅니다. 옮겨 담을 때 가립니다.
 * @property winningTeamRole 이긴 팀의 진영입니다. 내 진영을 가리는 데 씁니다. 값이 `Attacker`, `Defender`로 오는지 모릅니다.
 * @property roundResultCode 항복한 라운드가 여기에 `Surrendered`로 오는지 모릅니다.
 * @property playerStats 그 라운드를 뛴 사람마다 한 줄로 봅니다. 튕긴 사람이 빠지는지 0으로 채워 오는지 모릅니다.
 */
@Serializable
data class RoundResultDto(
    val roundNum: Int? = null,
    val roundResult: String? = null,
    val roundCeremony: String? = null,
    val winningTeam: String? = null,
    val winningTeamRole: String? = null,
    val bombPlanter: String? = null,
    val bombDefuser: String? = null,
    val plantRoundTime: Long? = null,
    val plantPlayerLocations: List<PlayerLocationsDto> = emptyList(),
    val plantLocation: LocationDto? = null,
    val plantSite: String? = null,
    val defuseRoundTime: Long? = null,
    val defusePlayerLocations: List<PlayerLocationsDto> = emptyList(),
    val defuseLocation: LocationDto? = null,
    val playerStats: List<PlayerRoundStatsDto> = emptyList(),
    val roundResultCode: String? = null,
)

/**
 * @property kills 이 사람이 낸 킬입니다. 스킬로 자기나 우리 팀을 죽인 것도 들어 있습니다.
 * @property damage 이 사람이 받는 사람마다 입힌 피해와 맞힌 부위입니다.
 * @property economy 라운드를 시작할 때의 장비입니다.
 */
@Serializable
data class PlayerRoundStatsDto(
    val puuid: String? = null,
    val kills: List<KillDto> = emptyList(),
    val damage: List<DamageDto> = emptyList(),
    val score: Int? = null,
    val economy: EconomyDto? = null,
    val ability: AbilityDto? = null,
)

/**
 * @property killer 스파이크나 낙사로 죽었을 때 무엇이 오는지 모릅니다.
 * @property finishingDamage 마지막 피해를 준 것입니다. 무기별 킬은 이걸로 셉니다.
 */
@Serializable
data class KillDto(
    val timeSinceGameStartMillis: Long? = null,
    val timeSinceRoundStartMillis: Long? = null,
    val killer: String? = null,
    val victim: String? = null,
    val victimLocation: LocationDto? = null,
    val assistants: List<String> = emptyList(),
    val playerLocations: List<PlayerLocationsDto> = emptyList(),
    val finishingDamage: FinishingDamageDto? = null,
)

/**
 * @property damageType `Weapon`, `Ability`, `Bomb` 같은 값입니다.
 * @property damageItem 총이면 무기 UUID이고 스킬이면 `Ultimate` 같은 슬롯 이름입니다.
 */
@Serializable
data class FinishingDamageDto(
    val damageType: String? = null,
    val damageItem: String? = null,
    val isSecondaryFireMode: Boolean? = null,
)

/** 맞힌 횟수는 킬이 아니라 맞힌 탄 수입니다. */
@Serializable
data class DamageDto(
    val receiver: String? = null,
    val damage: Int? = null,
    val legshots: Int? = null,
    val bodyshots: Int? = null,
    val headshots: Int? = null,
)

/**
 * @property loadoutValue 라운드를 시작할 때 든 장비 가치입니다. 이코·포스바이·풀바이를 이걸로 가립니다.
 * @property weapon 라운드를 시작할 때 든 무기 UUID입니다. 주워 쓴 총은 안 잡힙니다.
 */
@Serializable
data class EconomyDto(
    val loadoutValue: Int? = null,
    val weapon: String? = null,
    val armor: String? = null,
    val remaining: Int? = null,
    val spent: Int? = null,
)

@Serializable
data class AbilityDto(
    val grenadeEffects: String? = null,
    val ability1Effects: String? = null,
    val ability2Effects: String? = null,
    val ultimateEffects: String? = null,
)

@Serializable
data class PlayerLocationsDto(
    val puuid: String? = null,
    val viewRadians: Double? = null,
    val location: LocationDto? = null,
)

// 문서는 정수라고 적었지만 좌표라 소수로 와도 읽게 둔다. 한 필드 타입이 어긋나면 경기 하나를 통째로 못 읽는다.
@Serializable
data class LocationDto(
    val x: Double? = null,
    val y: Double? = null,
)
