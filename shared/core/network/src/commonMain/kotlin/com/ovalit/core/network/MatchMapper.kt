package com.ovalit.core.network

import com.ovalit.core.model.ActId
import com.ovalit.core.model.AgentId
import com.ovalit.core.model.KillEvent
import com.ovalit.core.model.MapId
import com.ovalit.core.model.Match
import com.ovalit.core.model.MatchFormat
import com.ovalit.core.model.MatchId
import com.ovalit.core.model.MatchTeam
import com.ovalit.core.model.PlayerCardId
import com.ovalit.core.model.PlayerId
import com.ovalit.core.model.Queue
import com.ovalit.core.model.Role
import com.ovalit.core.model.Round
import com.ovalit.core.model.RoundEconomy
import com.ovalit.core.model.RoundEnding
import com.ovalit.core.model.Scoreline
import com.ovalit.core.model.Shots
import com.ovalit.core.model.Side
import com.ovalit.core.model.WeaponId
import com.ovalit.core.network.dto.DamageDto
import com.ovalit.core.network.dto.FinishingDamageDto
import com.ovalit.core.network.dto.KillDto
import com.ovalit.core.network.dto.MatchDto
import com.ovalit.core.network.dto.PlayerDto
import com.ovalit.core.network.dto.PlayerRoundStatsDto
import com.ovalit.core.network.dto.RoundResultDto
import com.ovalit.core.network.dto.TeamDto
import kotlin.math.roundToInt
import kotlin.time.Instant

/**
 * Riot 경기 상세를 [Match]로 옮깁니다. 지표는 [Match]에서 `core/model`이 세고, 여기서는 그 계산이 믿는 값을 맞춰 담습니다.
 *
 * - 라운드는 [me]가 뛴 라운드만 담습니다. 그 라운드 `playerStats`에 [me]가 없으면 튕겨서 못 뛴 것으로 봅니다. 스코어를 그리는
 *   [Match.roundOutcomes]에는 못 뛴 라운드도 넣습니다.
 * - 킬은 그 라운드 사람마다의 `kills[]`를 모두 모아 시각순으로 둡니다. 스킬로 자기나 우리 팀을 죽인 것도 그대로 담고, 킬로
 *   세지 않는 일은 `core/model`이 [Match.allies]로 가립니다.
 * - 피해량과 맞힌 부위는 상대에게 준 것만 셉니다. 스킬로 자기나 우리 팀에 준 피해는 뺍니다.
 * - 라운드가 없는 모드(데스매치, 팀 데스매치, 에스컬레이션, 눈싸움, 큐를 모르는 모드)는 라운드를 0으로 둡니다. 스코어보드의
 *   K/D/A와 `damage[]`에서 모은 맞힌 부위만 씁니다.
 * - 관전자와 코치는 스코어보드와 팀에 넣지 않습니다.
 * - 요원·무기·맵·액트·카드 UUID는 소문자로 옮깁니다. Riot은 같은 UUID를 응답마다 대소문자를 달리 주고, 서버의 역할 표와
 *   카탈로그는 소문자 키입니다.
 *
 * @param me 누구 눈으로 볼지입니다. 내 경기는 내 PUUID, 친구 경기는 친구 PUUID입니다. 서버가 가린 친구 경기에서도 친구는
 * 가리지 않아 PUUID 그대로 옵니다.
 * @param roleOf 서버의 요원 역할 표(`/content/roles`)입니다. 표에 없는 새 요원이면 `null`을 돌려줍니다.
 * @return 커스텀 게임, 끝나지 않은 경기, [me]가 뛰지 않았거나 관전한 경기면 `null`입니다. 끝나지 않은 경기를 저장하면 틀린
 * 결과가 그대로 남아서 받지 않습니다.
 * @throws RiotResponseFormatException 경기 ID나 시작 시각처럼 없으면 저장할 수 없는 값이 빠졌을 때 던집니다.
 */
fun MatchDto.toMatch(me: PlayerId, roleOf: (AgentId) -> Role?): Match? {
    val info = matchInfo ?: throw RiotResponseFormatException("$.matchInfo")
    if (info.isCompleted == false) return null
    val queue = Queue.fromRiot(info.queueId.orEmpty(), info.provisioningFlowId) ?: return null
    val id = info.matchId?.takeIf { it.isNotBlank() } ?: throw RiotResponseFormatException("$.matchInfo.matchId")
    val startedAt = info.gameStartMillis ?: throw RiotResponseFormatException("$.matchInfo.gameStartMillis")

    // 관전자는 stats 없이 올 수 있어 isObserver가 빠져도 stats가 없으면 뛴 사람으로 보지 않는다
    val lineup = players.mapNotNull { player ->
        val puuid = player.puuid?.takeIf { it.isNotBlank() && player.isObserver != true && player.stats != null }
        puuid?.let { Participant(PlayerId(it), player) }
    }
    val mine = lineup.firstOrNull { it.id == me } ?: return null
    val myAgent = AgentId(mine.dto.characterId.contentId())
    val skeleton = Match(
        id = MatchId(id),
        queue = queue,
        act = ActId(info.seasonId.contentId()),
        map = MapId(info.mapId.contentId()),
        startedAt = Instant.fromEpochMilliseconds(startedAt),
        lengthMillis = info.gameLengthMillis ?: 0,
        me = me,
        myAgent = myAgent,
        myRole = roleOf(myAgent),
        allies = emptySet(),
        myCombatScore = mine.dto.stats?.score ?: 0,
        myTeamWon = null,
        roundOutcomes = emptyList(),
        rounds = emptyList(),
        players = emptyList(),
        teams = teams.mapNotNull { it.toMatchTeam(lineup) },
    )
    val sides = Sides(me = me, myTeam = mine.dto.teamId, format = skeleton.format, lineup = lineup)
    val hasRounds = skeleton.format == MatchFormat.ROUNDS
    val numbered = roundResults.numbered()

    val roundOutcomes = if (hasRounds) {
        val surrenderInScore = teams.sumOf { it.roundsWon ?: 0 } == numbered.size
        numbered
            .filter { (_, round) -> !round.isSurrender || surrenderInScore }
            .map { (_, round) -> sides.won(round) }
    } else {
        emptyList()
    }
    val rounds = if (hasRounds) {
        numbered.mapNotNull { (number, round) ->
            // 항복한 라운드는 아무도 안 죽은 라운드라 넣으면 생존율과 관여율이 오르고 ACS·ADR의 분모가 는다
            if (round.isSurrender) return@mapNotNull null
            val myStats = round.playerStats.firstOrNull { it.puuid == me.value } ?: return@mapNotNull null
            round.toRound(number, myStats, sides)
        }
    } else {
        emptyList()
    }

    return skeleton.copy(
        allies = sides.allies,
        myTeamWon = if (skeleton.teams.isEmpty()) roundOutcomes.majority() else skeleton.teams.myTeamWon(me),
        roundOutcomes = roundOutcomes,
        rounds = rounds,
        players = lineup.map { it.toScoreline(sides, hasRounds, roundResults) },
    )
}

/**
 * 응답의 `roundNum`을 [Round.number]로 옮길 때 더하는 값입니다. 앱은 1부터 세어 피스톨 라운드(1, 13)와 S3 라운드 줄이 이 번호를
 * 믿습니다.
 *
 * Riot 문서는 0부터인지 1부터인지 적지 않았고 실제 응답도 아직 못 봤습니다. 커뮤니티 자료는 0부터라고 합니다. 경기는 늘 첫
 * 라운드부터 기록되니 0번 라운드가 있으면 0부터 센 것으로 보고 1을 더합니다. 실제 응답을 보고 한쪽으로 정해지면 여기만
 * 고칩니다.
 */
internal fun roundNumberOffset(roundNums: List<Int>): Int = if (0 in roundNums) 1 else 0

// roundNum이 빠진 라운드는 응답 순서를 0부터 센 번호로 본다
private fun List<RoundResultDto>.numbered(): List<Pair<Int, RoundResultDto>> {
    val nums = mapIndexed { index, round -> round.roundNum ?: index }
    val offset = roundNumberOffset(nums)
    return nums.zip(this) { num, round -> num + offset to round }.sortedBy { it.first }
}

/**
 * 항복한 라운드가 응답에 어떻게 오는지 아직 못 봤습니다. `roundResultCode`나 `roundResult`에 `Surrendered`가 온다고 보고 가립니다.
 * 스코어에는 `teams[].roundsWon`이 그 라운드까지 셌을 때만 넣고, 내 라운드에는 넣지 않습니다.
 */
internal val RoundResultDto.isSurrender: Boolean
    get() = ending() == RoundEnding.SURRENDERED

private class Participant(val id: PlayerId, val dto: PlayerDto)

/**
 * 누가 누구 편인지입니다. 데스매치는 모두가 서로 적이라 팀 ID와 상관없이 나 말고는 모두 상대입니다.
 *
 * 스코어보드에 없는 사람(관전자, 코치, 응답에 없는 PUUID)은 누구 편도 아니라 피해량과 구매 유형을 셀 때 빠집니다.
 */
private class Sides(
    val me: PlayerId,
    private val myTeam: String?,
    private val format: MatchFormat,
    lineup: List<Participant>,
) {
    private val teamOf: Map<PlayerId, String?> = lineup.associate { it.id to it.dto.teamId }

    val allies: Set<PlayerId> = teamOf.keys.filterTo(mutableSetOf()) { it != me && sameTeam(it, me) }

    fun sameTeam(a: PlayerId, b: PlayerId): Boolean = when {
        a == b -> true
        format == MatchFormat.FREE_FOR_ALL -> false
        else -> teamOf[a] != null && teamOf[a] == teamOf[b]
    }

    fun opposed(a: PlayerId, b: PlayerId): Boolean = a in teamOf && b in teamOf && !sameTeam(a, b)

    fun isKnown(player: PlayerId): Boolean = player in teamOf

    fun won(round: RoundResultDto): Boolean = myTeam != null && round.winningTeam == myTeam

    /**
     * 그 라운드에 내가 공격이었는지 수비였는지입니다. 이긴 팀의 진영(`winningTeamRole`)이 오면 그걸로 가리고, 없으면 스파이크를
     * 설치하거나 해체한 사람의 편으로 가립니다. 둘 다 없으면 `null`입니다.
     */
    fun mySide(round: RoundResultDto): Side? {
        val winnerSide = round.winningTeamRole.toSide()
        if (winnerSide != null && myTeam != null && round.winningTeam != null) {
            return if (round.winningTeam == myTeam) winnerSide else winnerSide.opposite()
        }
        round.bombPlanter.knownPlayer()?.let { planter -> return if (sameTeam(planter, me)) Side.ATTACK else Side.DEFENSE }
        round.bombDefuser.knownPlayer()?.let { defuser -> return if (sameTeam(defuser, me)) Side.DEFENSE else Side.ATTACK }
        return null
    }

    private fun String?.knownPlayer(): PlayerId? = this?.takeIf { it.isNotBlank() }?.let(::PlayerId)?.takeIf(::isKnown)
}

// 값이 Attacker, Defender로 온다고 짐작한다. 실제 응답에서 다른 값이 오면 null이라 진영별 지표에서 빠질 뿐이다.
private fun String?.toSide(): Side? = when {
    this == null -> null
    startsWith("attack", ignoreCase = true) -> Side.ATTACK
    startsWith("defen", ignoreCase = true) -> Side.DEFENSE
    else -> null
}

private fun Side.opposite(): Side = if (this == Side.ATTACK) Side.DEFENSE else Side.ATTACK

private fun RoundResultDto.toRound(number: Int, mine: PlayerRoundStatsDto, sides: Sides): Round {
    val me = sides.me
    val dealt = mine.damage.hitsOnOpponentsOf(me, sides)
    return Round(
        number = number,
        won = sides.won(this),
        // 같은 킬이 두 사람의 kills[]에 함께 들어 있어도 한 번만 센다. 세이지 부활로 두 번 죽은 건 시각이 달라 둘 다 남는다.
        kills = playerStats.flatMap { it.kills }.mapNotNull { it.toKillEvent() }.distinct().sortedBy { it.atMillis },
        myDamage = dealt.sumOf { it.damage },
        myShots = dealt.shots(),
        mySide = sides.mySide(this),
        ending = ending(),
        economy = economy(mine, sides),
        myDamageTo = dealt
            .groupBy { it.receiver }
            .mapValues { (_, hits) -> hits.sumOf { it.damage } }
            .filterValues { it > 0 },
        myDamageFrom = playerStats
            .mapNotNull { stats -> stats.id?.takeIf { sides.opposed(me, it) }?.let { it to stats } }
            .associate { (dealer, stats) -> dealer to stats.damage.filter { it.receiverId == me }.sumOf { it.damage ?: 0 } }
            .filterValues { it > 0 },
    )
}

private val PlayerRoundStatsDto.id: PlayerId?
    get() = puuid?.takeIf { it.isNotBlank() }?.let(::PlayerId)

private val DamageDto.receiverId: PlayerId?
    get() = receiver?.takeIf { it.isNotBlank() }?.let(::PlayerId)

private class Hit(val receiver: PlayerId, val damage: Int, val shots: Shots)

// 스킬로 자기나 우리 팀에 준 피해는 피해량과 헤드샷 비율에 넣지 않는다
private fun List<DamageDto>.hitsOnOpponentsOf(dealer: PlayerId, sides: Sides): List<Hit> = mapNotNull { dto ->
    val receiver = dto.receiverId?.takeIf { sides.opposed(dealer, it) } ?: return@mapNotNull null
    Hit(
        receiver = receiver,
        damage = dto.damage ?: 0,
        shots = Shots(head = dto.headshots ?: 0, body = dto.bodyshots ?: 0, leg = dto.legshots ?: 0),
    )
}

private fun List<Hit>.shots(): Shots = fold(Shots.None) { total, hit -> total + hit.shots }

private fun KillDto.toKillEvent(): KillEvent? {
    val victim = victim?.takeIf { it.isNotBlank() } ?: return null
    // 스파이크 폭발과 낙사는 누구 킬로 오는지 모른다. 킬러가 비었거나 스파이크면 자기 스킬로 죽은 것처럼 데스로만 센다.
    val bySpike = finishingDamage?.damageType.equals("Bomb", ignoreCase = true)
    val killer = killer?.takeIf { it.isNotBlank() && !bySpike } ?: victim
    return KillEvent(
        // 시각이 없으면 맨 뒤로 보내 첫 킬이나 트레이드로 잡히지 않게 한다
        atMillis = timeSinceRoundStartMillis ?: Long.MAX_VALUE,
        killer = PlayerId(killer),
        victim = PlayerId(victim),
        assistants = assistants.filter { it.isNotBlank() && it != killer }.mapTo(mutableSetOf(), ::PlayerId),
        weapon = finishingDamage?.weapon(),
    )
}

// economy.weapon은 라운드 시작 로드아웃이라 주워 쓴 총이 안 잡힌다. 무기별 킬은 마지막 피해를 준 무기로 센다.
// 스킬은 damageItem이 Ultimate 같은 슬롯 이름이라 무기로 담지 않는다.
private fun FinishingDamageDto.weapon(): WeaponId? {
    val type = damageType?.trim().orEmpty()
    if (!type.equals("Weapon", ignoreCase = true) && !type.equals("Melee", ignoreCase = true)) return null
    return damageItem.contentIdOrNull()?.let(::WeaponId)
}

/**
 * 라운드를 시작할 때의 장비입니다. 팀 값은 그 라운드 `playerStats`에 있는 사람의 한 사람당 평균이라 누가 튕겨 네 명이 뛴
 * 라운드도 같은 기준으로 가릅니다. 내 장비 값이 없으면 `null`입니다.
 */
private fun RoundResultDto.economy(mine: PlayerRoundStatsDto, sides: Sides): RoundEconomy? {
    val myEconomy = mine.economy ?: return null
    val myLoadout = myEconomy.loadoutValue ?: return null
    val loadouts = playerStats.mapNotNull { stats ->
        val player = stats.id ?: return@mapNotNull null
        stats.economy?.loadoutValue?.let { player to it }
    }
    return RoundEconomy(
        myLoadout = myLoadout,
        teamLoadout = loadouts.filter { (player, _) -> sides.sameTeam(player, sides.me) }.averageLoadout(),
        enemyLoadout = loadouts.filter { (player, _) -> sides.opposed(sides.me, player) }.averageLoadout(),
        myWeapon = myEconomy.weapon.contentIdOrNull()?.let(::WeaponId),
    )
}

private fun List<Pair<PlayerId, Int>>.averageLoadout(): Int =
    if (isEmpty()) 0 else (sumOf { it.second }.toDouble() / size).roundToInt()

private fun RoundResultDto.ending(): RoundEnding? = endingOf(roundResultCode) ?: endingOf(roundResult)

// 문서에 값 목록이 없다. 커뮤니티 자료의 roundResultCode(Elimination, Detonate, Defuse, Surrendered)와
// roundResult(Eliminated, Bomb detonated, Bomb defused, Round timer expired)를 둘 다 받는다.
private fun endingOf(value: String?): RoundEnding? {
    val text = value?.lowercase() ?: return null
    return when {
        "surrender" in text -> RoundEnding.SURRENDERED
        "defuse" in text -> RoundEnding.SPIKE_DEFUSED
        "detonat" in text -> RoundEnding.SPIKE_DETONATED
        "eliminat" in text -> RoundEnding.ELIMINATION
        "timer" in text || "expire" in text -> RoundEnding.TIME_EXPIRED
        else -> null
    }
}

private fun TeamDto.toMatchTeam(lineup: List<Participant>): MatchTeam? {
    val id = teamId?.takeIf { it.isNotBlank() } ?: return null
    // 데스매치는 팀 ID가 그 사람의 PUUID다. 플레이어 줄의 teamId도 같은 값으로 오는지 몰라 PUUID로도 찾는다.
    val members = lineup.filter { it.dto.teamId == id }.ifEmpty { lineup.filter { it.id.value == id } }
    return MatchTeam(members = members.mapTo(mutableSetOf()) { it.id }, won = won, points = numPoints)
}

// 비기면 두 팀 모두 won이 false라 null이다
private fun List<MatchTeam>.myTeamWon(me: PlayerId): Boolean? {
    val mine = firstOrNull { me in it.members }
    return when {
        mine?.won == true -> true
        any { it !== mine && it.won == true } -> false
        else -> null
    }
}

// teams[]가 없을 때만 쓴다. 이긴 라운드가 더 많으면 이긴 경기다.
private fun List<Boolean>.majority(): Boolean? {
    val won = count { it }
    return when {
        won * 2 > size -> true
        won * 2 < size -> false
        else -> null
    }
}

/**
 * 스코어보드 한 줄입니다. 킬·데스·어시와 전투점수는 게임 스코어보드와 같게 `stats`를 그대로 옮기고, 응답에 라운드별로만 있는
 * 피해량과 맞힌 부위는 모든 라운드를 더합니다. 라운드가 없는 모드는 뛴 라운드를 0으로 두어 전투점수와 피해량을 나누지 않습니다.
 */
private fun Participant.toScoreline(sides: Sides, hasRounds: Boolean, roundResults: List<RoundResultDto>): Scoreline {
    val stats = dto.stats
    val dealt = roundResults
        .flatMap { round -> round.playerStats.filter { it.id == id } }
        .flatMap { it.damage }
        .hitsOnOpponentsOf(id, sides)
    return Scoreline(
        player = id,
        riotId = dto.riotId(),
        agent = AgentId(dto.characterId.contentId()),
        onMyTeam = sides.sameTeam(id, sides.me),
        tier = dto.competitiveTier?.takeIf { it > 0 },
        playerCard = dto.playerCard.contentIdOrNull()?.let(::PlayerCardId),
        kills = stats?.kills ?: 0,
        deaths = stats?.deaths ?: 0,
        assists = stats?.assists ?: 0,
        combatScore = stats?.score ?: 0,
        damage = dealt.sumOf { it.damage },
        roundsPlayed = if (hasRounds) stats?.roundsPlayed ?: 0 else 0,
        shots = if (roundResults.isEmpty()) null else dealt.shots(),
    )
}

// 서버가 가린 사람은 이름과 태그가 빈 값이라 빈 문자열이다
private fun PlayerDto.riotId(): String {
    val name = gameName?.trim().orEmpty()
    val tag = tagLine?.trim().orEmpty()
    return when {
        name.isEmpty() -> ""
        tag.isEmpty() -> name
        else -> "$name#$tag"
    }
}

private val Uuid = Regex("[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}")

// UUID만 소문자로 바꾼다. 맵은 /Game/Maps/Ascent/Ascent 같은 경로로 올 수도 있어 그대로 둔다.
private fun String?.contentIdOrNull(): String? {
    val value = this?.trim()?.takeIf { it.isNotEmpty() } ?: return null
    return if (Uuid.matches(value)) value.lowercase() else value
}

private fun String?.contentId(): String = contentIdOrNull().orEmpty()
