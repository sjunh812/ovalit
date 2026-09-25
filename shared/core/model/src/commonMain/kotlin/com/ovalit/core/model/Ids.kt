package com.ovalit.core.model

import kotlin.jvm.JvmInline

@JvmInline
value class MatchId(val value: String)

/** Riot의 PUUID입니다. 화면에 띄우지 않습니다. */
@JvmInline
value class PlayerId(val value: String)

/** 경기 응답의 `characterId`입니다. 요원 이름은 콘텐츠 API에서 따로 받습니다. */
@JvmInline
value class AgentId(val value: String)

/**
 * 경기 응답의 `seasonId`입니다.
 *
 * 액트마다 랭크가 초기화되고 매칭 난이도가 달라지므로, 액트 경계를 넘는 평균은 만들지
 * 않습니다. 이 값이 그 경계를 가릅니다.
 */
@JvmInline
value class ActId(val value: String)

/** `kills[].finishingDamage.damageItem`입니다. 총기 종류까지만 가리키고 스킨은 모릅니다. */
@JvmInline
value class WeaponId(val value: String)

/**
 * 경기 응답의 `matchInfo.mapId`입니다. UUID로 올지 `/Game/Maps/Ascent/Ascent` 같은 경로로 올지 아직
 * 확인하지 못했습니다. 어느 쪽이든 VAL-CONTENT의 맵 목록으로 이름과 UUID를 찾을 수 있습니다.
 */
@JvmInline
value class MapId(val value: String)

/** 경기 응답의 `players[].playerCard`입니다. 아바타와 프로필 배너에 씁니다. */
@JvmInline
value class PlayerCardId(val value: String)
