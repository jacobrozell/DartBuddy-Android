package com.jacobrozell.dartbuddy.domain.bots

import com.jacobrozell.dartbuddy.domain.engine.CheckoutSuggester
import com.jacobrozell.dartbuddy.domain.engine.CricketState
import com.jacobrozell.dartbuddy.domain.engine.CricketTarget
import com.jacobrozell.dartbuddy.domain.model.CricketScoringMode
import com.jacobrozell.dartbuddy.domain.model.X01CheckInMode
import com.jacobrozell.dartbuddy.domain.model.X01CheckoutMode
import com.jacobrozell.dartbuddy.domain.scoring.DartInput
import com.jacobrozell.dartbuddy.domain.scoring.DartMultiplier
import com.jacobrozell.dartbuddy.domain.scoring.DartSegment
import kotlin.random.Random

object DartBotEngine {
    fun generateX01Turn(
        remaining: Int,
        difficulty: BotDifficulty,
        checkoutMode: X01CheckoutMode,
        checkInMode: X01CheckInMode,
        isCheckedIn: Boolean,
        rng: Random = Random,
    ): List<DartInput> = generateX01Turn(
        remaining = remaining,
        profile = difficulty.skillProfile,
        checkoutMode = checkoutMode,
        checkInMode = checkInMode,
        isCheckedIn = isCheckedIn,
        rng = rng,
    )

    fun generateX01Turn(
        remaining: Int,
        profile: BotSkillProfile,
        checkoutMode: X01CheckoutMode,
        checkInMode: X01CheckInMode,
        isCheckedIn: Boolean,
        rng: Random = Random,
    ): List<DartInput> {
        val darts = mutableListOf<DartInput>()
        var simulatedRemaining = remaining
        var checkedIn = isCheckedIn

        while (darts.size < 3 && simulatedRemaining > 0) {
            val dartsLeft = 3 - darts.size
            val intended = intendedX01Dart(
                remaining = simulatedRemaining,
                dartsLeft = dartsLeft,
                profile = profile,
                checkoutMode = checkoutMode,
                checkInMode = checkInMode,
                checkedIn = checkedIn,
                rng = rng,
            )

            val hitBoost = if (checkedIn) 0.0 else profile.x01.checkInHitBoost
            val resolved = resolveDart(
                intended = intended,
                profile = profile,
                hitBoost = hitBoost,
                rng = rng,
            )
            val points = scoredPoints(
                dart = resolved,
                checkInMode = checkInMode,
                checkedIn = checkedIn,
            )

            if (!checkedIn && points > 0) {
                checkedIn = true
            }

            if (checkedIn && wouldBust(
                    remaining = simulatedRemaining,
                    points = points,
                    checkoutMode = checkoutMode,
                )
            ) {
                val playsRisky = rng.nextDouble() < profile.x01.riskyBustChance
                if (playsRisky) {
                    darts.add(resolved)
                    if (checkedIn) {
                        simulatedRemaining -= points
                        if (turnEndsAfterSimulatedDart(
                                remaining = simulatedRemaining,
                                checkoutMode = checkoutMode,
                            )
                        ) {
                            break
                        }
                    }
                } else {
                    val safe = safeScoringDart(
                        remaining = simulatedRemaining,
                        checkoutMode = checkoutMode,
                        rng = rng,
                    )
                    darts.add(safe)
                    simulatedRemaining -= safe.points
                    if (turnEndsAfterSimulatedDart(
                            remaining = simulatedRemaining,
                            checkoutMode = checkoutMode,
                        )
                    ) {
                        break
                    }
                }
            } else {
                darts.add(resolved)
                if (checkedIn) {
                    simulatedRemaining -= points
                    if (turnEndsAfterSimulatedDart(
                            remaining = simulatedRemaining,
                            checkoutMode = checkoutMode,
                        )
                    ) {
                        break
                    }
                }
            }
        }

        return darts
    }

    fun generateCricketTurn(
        state: CricketState,
        playerIndex: Int,
        difficulty: BotDifficulty,
        rng: Random = Random,
    ): List<DartInput> = generateCricketTurn(
        state = state,
        playerIndex = playerIndex,
        profile = difficulty.skillProfile,
        rng = rng,
    )

    fun generateCricketTurn(
        state: CricketState,
        playerIndex: Int,
        profile: BotSkillProfile,
        rng: Random = Random,
    ): List<DartInput> {
        val darts = mutableListOf<DartInput>()
        val marksSnapshot = state.players[playerIndex].marks.toMutableMap()

        while (darts.size < 3) {
            val intended = intendedCricketDart(
                state = state,
                playerIndex = playerIndex,
                marksSnapshot = marksSnapshot,
                profile = profile,
                rng = rng,
            )
            val resolved = resolveCricketDart(intended = intended, profile = profile, rng = rng)
            darts.add(resolved)

            val targetRaw = resolved.segment.cricketTargetRaw
            if (targetRaw != null) {
                val incoming = cricketMarks(resolved)
                val before = marksSnapshot[targetRaw] ?: 0
                marksSnapshot[targetRaw] = minOf(3, before + incoming)
            }
        }

        return darts
    }

    fun dartFromCheckoutLabel(label: String): DartInput? {
        if (label == "Bull") {
            return DartInput(multiplier = DartMultiplier.Single, segment = DartSegment.InnerBull)
        }
        if (label == "25") {
            return DartInput(multiplier = DartMultiplier.Single, segment = DartSegment.OuterBull)
        }
        if (label.startsWith("T")) {
            val value = label.drop(1).toIntOrNull()
            if (value != null && value in 1..20) {
                return DartInput(
                    multiplier = DartMultiplier.Triple,
                    segment = DartSegment.OneToTwenty(value),
                )
            }
        }
        if (label.startsWith("D")) {
            val value = label.drop(1).toIntOrNull()
            if (value != null && value in 1..20) {
                return DartInput(
                    multiplier = DartMultiplier.Double,
                    segment = DartSegment.OneToTwenty(value),
                )
            }
        }
        val value = label.toIntOrNull()
        if (value != null && value in 1..20) {
            return DartInput(
                multiplier = DartMultiplier.Single,
                segment = DartSegment.OneToTwenty(value),
            )
        }
        return null
    }

    private fun intendedX01Dart(
        remaining: Int,
        dartsLeft: Int,
        profile: BotSkillProfile,
        checkoutMode: X01CheckoutMode,
        checkInMode: X01CheckInMode,
        checkedIn: Boolean,
        rng: Random,
    ): DartInput {
        if (checkedIn &&
            CheckoutSuggester.suggestion(remaining, checkoutMode, dartsLeft) != null &&
            rng.nextDouble() < profile.x01.checkoutAttemptChance
        ) {
            val route = CheckoutSuggester.suggestion(remaining, checkoutMode, dartsLeft)
            val first = route?.firstOrNull()
            val dart = first?.let(::dartFromCheckoutLabel)
            if (dart != null) {
                return dart
            }
        }

        if (!checkedIn) {
            return openerDart(checkInMode, profile, rng)
        }

        val visitRange = profile.x01.scoringVisitMin..profile.x01.scoringVisitMax
        val targetTotal = minOf(
            remaining - safeRemainingBuffer(checkoutMode, profile),
            rng.nextInt(visitRange.first, visitRange.last + 1),
        )
        val segment = preferredScoringSegment(profile, rng)
        val multiplier = preferredScoringMultiplier(
            targetTotal = maxOf(1, targetTotal),
            segment = segment,
            profile = profile,
            rng = rng,
        )
        return DartInput(
            multiplier = multiplier,
            segment = DartSegment.OneToTwenty(segment),
        )
    }

    private fun openerDart(
        mode: X01CheckInMode,
        profile: BotSkillProfile,
        rng: Random,
    ): DartInput = when (mode) {
        X01CheckInMode.StraightIn -> intendedX01Dart(
            remaining = 501,
            dartsLeft = 3,
            profile = profile,
            checkoutMode = X01CheckoutMode.DoubleOut,
            checkInMode = X01CheckInMode.StraightIn,
            checkedIn = true,
            rng = rng,
        )
        X01CheckInMode.DoubleIn -> {
            val faces = listOf(16, 8, 4, 20, 12, 18)
            val face = faces.random(rng)
            DartInput(multiplier = DartMultiplier.Double, segment = DartSegment.OneToTwenty(face))
        }
        X01CheckInMode.MasterIn -> {
            if (rng.nextDouble() < profile.x01.masterInTripleOpenerChance) {
                DartInput(multiplier = DartMultiplier.Triple, segment = DartSegment.OneToTwenty(20))
            } else {
                val faces = listOf(16, 8, 20, 12, 18, 4)
                val face = faces.random(rng)
                DartInput(multiplier = DartMultiplier.Double, segment = DartSegment.OneToTwenty(face))
            }
        }
    }

    private fun intendedCricketDart(
        state: CricketState,
        playerIndex: Int,
        marksSnapshot: Map<String, Int>,
        profile: BotSkillProfile,
        rng: Random,
    ): DartInput {
        val target = cricketAimTarget(state, playerIndex, marksSnapshot, rng)
            ?: return boardGlanceDart(
                near = DartInput(multiplier = DartMultiplier.Single, segment = DartSegment.OneToTwenty(20)),
                rng = rng,
            )
        return cricketDartInput(target, profile, rng)
    }

    private fun cricketAimTarget(
        state: CricketState,
        playerIndex: Int,
        marksSnapshot: Map<String, Int>,
        rng: Random,
    ): CricketTarget? {
        val ownOpen = CricketTarget.allCases.filter { (marksSnapshot[it.raw] ?: 0) < 3 }
        ownOpen.maxByOrNull { it.points }?.let { return it }

        if (state.config.pointsEnabled && state.config.scoringMode == CricketScoringMode.CutThroat) {
            cutThroatPunishTargets(state, playerIndex, marksSnapshot)
                .maxByOrNull { it.points }
                ?.let { return it }
        }

        if (state.config.pointsEnabled && state.config.scoringMode == CricketScoringMode.Standard) {
            standardScoringTargets(state, playerIndex, marksSnapshot)
                .maxByOrNull { it.points }
                ?.let { return it }
        }

        return CricketTarget.allCases.random(rng)
    }

    private fun standardScoringTargets(
        state: CricketState,
        playerIndex: Int,
        marksSnapshot: Map<String, Int>,
    ): List<CricketTarget> = CricketTarget.allCases.filter { target ->
        if ((marksSnapshot[target.raw] ?: 0) < 3) return@filter false
        state.players.withIndex().any { (index, player) ->
            index != playerIndex && (player.marks[target.raw] ?: 0) < 3
        }
    }

    private fun cutThroatPunishTargets(
        state: CricketState,
        playerIndex: Int,
        marksSnapshot: Map<String, Int>,
    ): List<CricketTarget> = CricketTarget.allCases.filter { target ->
        if ((marksSnapshot[target.raw] ?: 0) < 3) return@filter false
        state.players.withIndex().any { (index, player) ->
            index != playerIndex && (player.marks[target.raw] ?: 0) < 3
        }
    }

    private fun cricketDartInput(
        target: CricketTarget,
        profile: BotSkillProfile,
        rng: Random,
    ): DartInput {
        if (target == CricketTarget.Bull) {
            return if (rng.nextDouble() < profile.cricket.innerBullAimChance) {
                DartInput(multiplier = DartMultiplier.Single, segment = DartSegment.InnerBull)
            } else {
                DartInput(multiplier = DartMultiplier.Single, segment = DartSegment.OuterBull)
            }
        }
        val segment = target.points
        val tier = profile.x01.scoringBehaviorTier
        if (tier == BotDifficulty.VeryEasy || tier == BotDifficulty.Easy) {
            return DartInput(multiplier = DartMultiplier.Single, segment = DartSegment.OneToTwenty(segment))
        }
        if (rng.nextDouble() < profile.cricket.tripleOnOpenChance) {
            return DartInput(multiplier = DartMultiplier.Triple, segment = DartSegment.OneToTwenty(segment))
        }
        if (rng.nextDouble() < profile.cricket.doubleOnOpenChance) {
            return DartInput(multiplier = DartMultiplier.Double, segment = DartSegment.OneToTwenty(segment))
        }
        return DartInput(multiplier = DartMultiplier.Single, segment = DartSegment.OneToTwenty(segment))
    }

    private fun resolveCricketDart(
        intended: DartInput,
        profile: BotSkillProfile,
        rng: Random,
    ): DartInput {
        if (intended.isMiss) {
            return intended
        }

        val roll = rng.nextDouble()
        val hitChance = minOf(0.90, profile.cricketHitChance(intended.multiplier))
        if (roll < hitChance) {
            return intended
        }

        val missRoll = rng.nextDouble()
        if (missRoll < profile.cricket.offBoardMissChance) {
            return DartInput(multiplier = DartMultiplier.Single, segment = DartSegment.Miss, isMiss = true)
        }
        if (missRoll < profile.cricket.offBoardMissChance + profile.cricket.wrongBedChance) {
            return cricketWrongBedDart(rng)
        }

        return cricketPartialMissDart(intended, rng)
    }

    private fun cricketWrongBedDart(rng: Random): DartInput {
        val face = rng.nextInt(1, 15)
        return DartInput(multiplier = DartMultiplier.Single, segment = DartSegment.OneToTwenty(face))
    }

    private fun cricketPartialMissDart(
        intended: DartInput,
        rng: Random,
    ): DartInput {
        if (rng.nextDouble() < 0.55) {
            return cricketWrongBedDart(rng)
        }

        return when (val segment = intended.segment) {
            is DartSegment.OneToTwenty -> {
                val cricketNeighbors = (15..20).filter { it != segment.value }
                val neighbor = cricketNeighbors.randomOrNull(rng)
                if (neighbor != null) {
                    DartInput(multiplier = DartMultiplier.Single, segment = DartSegment.OneToTwenty(neighbor))
                } else {
                    DartInput(multiplier = DartMultiplier.Single, segment = DartSegment.OneToTwenty(segment.value))
                }
            }
            DartSegment.InnerBull, DartSegment.OuterBull -> {
                val face = rng.nextInt(1, 15)
                DartInput(multiplier = DartMultiplier.Single, segment = DartSegment.OneToTwenty(face))
            }
            DartSegment.Miss -> intended
        }
    }

    private fun resolveDart(
        intended: DartInput,
        profile: BotSkillProfile,
        hitBoost: Double = 0.0,
        rng: Random,
    ): DartInput {
        if (intended.isMiss) {
            return intended
        }

        val roll = rng.nextDouble()
        val hitChance = minOf(
            0.95,
            profile.x01HitChance(intended.multiplier) + hitBoost,
        )
        if (roll < hitChance) {
            return intended
        }

        val downgradeBand = 0.28
        if (roll < hitChance + downgradeBand) {
            return downgrade(intended, rng)
        }

        if (roll < hitChance + downgradeBand + profile.x01.offBoardMissChance) {
            return DartInput(multiplier = DartMultiplier.Single, segment = DartSegment.Miss, isMiss = true)
        }

        return boardGlanceDart(intended, rng)
    }

    private fun downgrade(
        intended: DartInput,
        rng: Random,
    ): DartInput = when (val segment = intended.segment) {
        is DartSegment.OneToTwenty -> when (intended.multiplier) {
            DartMultiplier.Triple -> {
                if (rng.nextDouble() < 0.55) {
                    DartInput(multiplier = DartMultiplier.Single, segment = DartSegment.OneToTwenty(segment.value))
                } else {
                    DartInput(multiplier = DartMultiplier.Double, segment = DartSegment.OneToTwenty(segment.value))
                }
            }
            DartMultiplier.Double -> {
                DartInput(multiplier = DartMultiplier.Single, segment = DartSegment.OneToTwenty(segment.value))
            }
            DartMultiplier.Single -> {
                val neighbor = (segment.value + rng.nextInt(-2, 3)).coerceIn(1, 20)
                DartInput(multiplier = DartMultiplier.Single, segment = DartSegment.OneToTwenty(neighbor))
            }
        }
        DartSegment.InnerBull -> DartInput(multiplier = DartMultiplier.Single, segment = DartSegment.OuterBull)
        DartSegment.OuterBull -> {
            val face = rng.nextInt(16, 21)
            DartInput(multiplier = DartMultiplier.Single, segment = DartSegment.OneToTwenty(face))
        }
        DartSegment.Miss -> intended
    }

    private fun boardGlanceDart(
        near: DartInput,
        rng: Random,
    ): DartInput = when (val segment = near.segment) {
        is DartSegment.OneToTwenty -> {
            val neighbor = (segment.value + rng.nextInt(-3, 4)).coerceIn(1, 20)
            DartInput(multiplier = DartMultiplier.Single, segment = DartSegment.OneToTwenty(neighbor))
        }
        DartSegment.InnerBull, DartSegment.OuterBull -> {
            val face = rng.nextInt(1, 21)
            DartInput(multiplier = DartMultiplier.Single, segment = DartSegment.OneToTwenty(face))
        }
        DartSegment.Miss -> {
            val face = rng.nextInt(1, 21)
            DartInput(multiplier = DartMultiplier.Single, segment = DartSegment.OneToTwenty(face))
        }
    }

    private fun safeScoringDart(
        remaining: Int,
        checkoutMode: X01CheckoutMode,
        rng: Random,
    ): DartInput {
        val candidates = (1..20).filter { value ->
            !wouldBust(remaining, value, checkoutMode)
        }
        val segment = candidates.randomOrNull(rng) ?: 1
        return DartInput(multiplier = DartMultiplier.Single, segment = DartSegment.OneToTwenty(segment))
    }

    private fun scoredPoints(
        dart: DartInput,
        checkInMode: X01CheckInMode,
        checkedIn: Boolean,
    ): Int {
        if (checkedIn) return dart.points
        return when (checkInMode) {
            X01CheckInMode.StraightIn -> dart.points
            X01CheckInMode.DoubleIn -> if (dart.multiplier == DartMultiplier.Double) dart.points else 0
            X01CheckInMode.MasterIn -> {
                if (dart.multiplier == DartMultiplier.Double || dart.multiplier == DartMultiplier.Triple) {
                    dart.points
                } else {
                    0
                }
            }
        }
    }

    private fun turnEndsAfterSimulatedDart(
        remaining: Int,
        checkoutMode: X01CheckoutMode,
    ): Boolean {
        if (remaining == 0) return true
        if (remaining < 0) return true
        if (remaining == 1 && checkoutMode != X01CheckoutMode.SingleOut) return true
        return false
    }

    private fun wouldBust(
        remaining: Int,
        points: Int,
        checkoutMode: X01CheckoutMode,
    ): Boolean {
        val next = remaining - points
        if (next < 0) return true
        if (next == 1 && checkoutMode != X01CheckoutMode.SingleOut) return true
        return false
    }

    private fun safeRemainingBuffer(
        checkoutMode: X01CheckoutMode,
        profile: BotSkillProfile,
    ): Int = when (checkoutMode) {
        X01CheckoutMode.SingleOut -> profile.x01.safeRemainingSingleOut
        X01CheckoutMode.DoubleOut -> profile.x01.safeRemainingDoubleOut
        X01CheckoutMode.MasterOut -> profile.x01.safeRemainingMasterOut
    }

    private fun preferredScoringSegment(
        profile: BotSkillProfile,
        rng: Random,
    ): Int = when (profile.x01.scoringBehaviorTier) {
        BotDifficulty.VeryEasy -> rng.nextInt(1, 17)
        BotDifficulty.Easy -> rng.nextInt(5, 21)
        BotDifficulty.Medium -> listOf(16, 17, 18, 19, 20).random(rng)
        BotDifficulty.Hard -> {
            if (rng.nextDouble() < 0.35) 20 else listOf(18, 19).random(rng)
        }
        BotDifficulty.Pro -> if (rng.nextDouble() < 0.46) 20 else 19
    }

    private fun preferredScoringMultiplier(
        targetTotal: Int,
        segment: Int,
        profile: BotSkillProfile,
        rng: Random,
    ): DartMultiplier = when (profile.x01.scoringBehaviorTier) {
        BotDifficulty.VeryEasy -> if (rng.nextDouble() < 0.92) DartMultiplier.Single else DartMultiplier.Double
        BotDifficulty.Easy -> if (rng.nextDouble() < 0.75) DartMultiplier.Single else DartMultiplier.Double
        BotDifficulty.Medium -> {
            when {
                targetTotal >= segment * 3 && rng.nextDouble() < 0.48 -> DartMultiplier.Triple
                rng.nextDouble() < 0.28 -> DartMultiplier.Triple
                else -> DartMultiplier.Double
            }
        }
        BotDifficulty.Hard, BotDifficulty.Pro -> {
            if (targetTotal >= segment * 3) {
                DartMultiplier.Triple
            } else if (rng.nextDouble() < profile.x01.triplePreference) {
                DartMultiplier.Triple
            } else {
                DartMultiplier.Double
            }
        }
    }

    private fun cricketMarks(dart: DartInput): Int {
        if (dart.isMiss) return 0
        return when (dart.segment) {
            DartSegment.InnerBull -> 2
            DartSegment.OuterBull -> 1
            is DartSegment.OneToTwenty -> dart.multiplier.markValue
            DartSegment.Miss -> 0
        }
    }
}

private fun <T> List<T>.randomOrNull(rng: Random): T? =
    if (isEmpty()) null else this[rng.nextInt(size)]
