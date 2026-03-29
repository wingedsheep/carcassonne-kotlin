package com.wingedsheep.carcassonne.ai

import com.wingedsheep.carcassonne.engine.BoardMap
import com.wingedsheep.carcassonne.engine.GameState
import com.wingedsheep.carcassonne.model.*
import com.wingedsheep.carcassonne.scoring.CityDetector
import com.wingedsheep.carcassonne.scoring.FarmDetector
import com.wingedsheep.carcassonne.scoring.PointsCalculator
import com.wingedsheep.carcassonne.scoring.RoadDetector

/**
 * Heuristic evaluation of a game state from [player]'s perspective.
 * Returns a score where higher = better for [player].
 *
 * Components:
 * - Realized score differential (points already scored)
 * - Potential points from meeples on unfinished structures
 * - Meeple availability bonus (having meeples in hand = flexibility)
 * - Penalty for opponent potential
 * - Game-phase-aware weighting (early/mid/late game)
 * - Proper farm evaluation with adjacent city counting
 * - Blocking awareness: penalty for easy-to-complete opponent structures
 */
object BoardEvaluator {

    // -- Base weight constants --
    private const val SCORE_WEIGHT = 1.0
    private const val OWN_POTENTIAL_WEIGHT = 0.7
    private const val OPPONENT_POTENTIAL_WEIGHT = 0.5
    private const val MEEPLE_AVAILABILITY_WEIGHT = 0.5
    private const val MONASTERY_PROGRESS_WEIGHT = 0.9
    /** Minimum expected return for a placed meeple to be "worth it". */
    private const val MEEPLE_OPPORTUNITY_COST = 2.0

    // -- Near-completion bonus: structures close to finishing are more reliable --
    private const val NEAR_COMPLETE_BONUS = 1.5

    // -- Opponent trapping: bonus when opponent has meeple stuck on hard-to-complete structure --
    private const val OPPONENT_TRAPPED_MEEPLE_BONUS = 1.5

    /**
     * Default evaluation: uses full score differential (differentialWeight = 1.0).
     */
    fun evaluate(state: GameState, player: Int): Double = evaluate(state, player, 1.0)

    /**
     * Configurable evaluation.
     * @param differentialWeight 0.0 = maximize own score only, 1.0 = maximize score differential.
     */
    fun evaluate(state: GameState, player: Int, differentialWeight: Double): Double {
        var score = 0.0

        val cache = getOrCreateCache(state.board)
        val gameProgress = gameProgress(state)

        // 1. Realized score: blend own score vs differential
        val myScore = state.scores[player]
        val bestOpponentScore = (0 until state.playerCount)
            .filter { it != player }
            .maxOfOrNull { state.scores[it] } ?: 0
        score += (myScore - differentialWeight * bestOpponentScore) * SCORE_WEIGHT

        // 2. Potential points from own placed meeples
        score += netMeeplePotential(state, player, cache, gameProgress) * OWN_POTENTIAL_WEIGHT

        // 3. Subtract opponent potential + reward trapped opponent meeples
        for (opp in 0 until state.playerCount) {
            if (opp == player) continue
            score -= netMeeplePotential(state, opp, cache, gameProgress) * OPPONENT_POTENTIAL_WEIGHT * differentialWeight
            score += trappedMeepleBonus(state, opp, cache, gameProgress) * differentialWeight
        }

        // 4. Meeple availability bonus (scales down late game - you want meeples placed by then)
        val meeplesInHand = state.meeples[player].normal
        val availabilityWeight = MEEPLE_AVAILABILITY_WEIGHT * (1.0 - gameProgress * 0.5)
        score += meeplesInHand * availabilityWeight

        return score
    }

    /**
     * Creates an evaluator function with a fixed differentialWeight.
     */
    fun withDifferentialWeight(differentialWeight: Double): (GameState, Int) -> Double =
        { state, player -> evaluate(state, player, differentialWeight) }

    /**
     * Returns game progress as 0.0 (start) to 1.0 (end).
     */
    private fun gameProgress(state: GameState): Double {
        if (state.deck.isEmpty()) return 1.0
        return state.deckIndex.toDouble() / state.deck.size
    }

    /**
     * Net meeple potential: sum of each placed meeple's expected value minus
     * the opportunity cost of having it tied up.
     */
    private fun netMeeplePotential(state: GameState, player: Int, cache: StructureCache, gameProgress: Double): Double {
        var total = 0.0
        val evaluated = mutableSetOf<PlacedMeeple>()

        for (meeple in state.placedMeeples[player]) {
            if (meeple in evaluated) continue
            evaluated.add(meeple)

            val rawPotential = singleMeeplePotential(state, meeple, player, cache, gameProgress)
            total += rawPotential - MEEPLE_OPPORTUNITY_COST
        }

        return total
    }

    /**
     * Estimate expected points a single placed meeple will earn.
     */
    private fun singleMeeplePotential(
        state: GameState, meeple: PlacedMeeple, player: Int,
        cache: StructureCache, gameProgress: Double
    ): Double {
        return when {
            meeple.type.isFarmer -> evaluateFarmer(state, meeple, player, cache, gameProgress)
            meeple.side != null -> {
                val tile = state.board.getRotated(meeple.coordinate) ?: return 0.0
                val cws = CoordinateWithSide(meeple.coordinate, meeple.side)

                if (meeple.side in tile.citySides()) {
                    evaluateCity(state.board, cws, player, state, cache, gameProgress)
                } else if (tile.terrainAt(meeple.side) == TerrainType.ROAD) {
                    evaluateRoad(state.board, cws, player, state, cache, gameProgress)
                } else {
                    0.0
                }
            }
            else -> {
                // Monastery
                evaluateMonastery(state, meeple, gameProgress)
            }
        }
    }

    // -- Farm evaluation --

    /**
     * Evaluate a farmer meeple by counting adjacent completed and near-complete cities.
     * This is the most impactful improvement: farms are the #1 scoring mechanism
     * in competitive Carcassonne (3 pts per adjacent finished city at end of game).
     */
    private fun evaluateFarmer(
        state: GameState, meeple: PlacedMeeple, player: Int,
        cache: StructureCache, gameProgress: Double
    ): Double {
        val fs = meeple.farmerSide ?: return 0.0
        val cwfs = CoordinateWithFarmerSide(meeple.coordinate, fs)
        val farm = cache.getFarm(cwfs)

        // Count adjacent cities: finished ones are guaranteed 3 pts each
        val adjacentCities = mutableSetOf<Set<CoordinateWithSide>>()
        var finishedCityCount = 0
        var nearCompleteCityCount = 0

        for ((coord, farmConn) in farm.farmerConnectionsWithCoordinate) {
            for (citySide in farmConn.citySides) {
                val cws = CoordinateWithSide(coord, citySide)
                val city = cache.getCity(cws)
                if (city.positions in adjacentCities) continue
                adjacentCities.add(city.positions)

                if (city.finished) {
                    finishedCityCount++
                } else {
                    // Estimate chance this city will complete before game ends
                    val openEdges = city.positions.count { pos ->
                        val neighbor = pos.coordinate.neighbor(pos.side)
                        !state.board.contains(neighbor)
                    }
                    if (openEdges <= 2) {
                        nearCompleteCityCount++
                    }
                }
            }
        }

        val guaranteedPoints = finishedCityCount * 3.0
        // Near-complete cities have a reasonable chance of completing
        val nearCompleteProb = completionProbForOpenEdges(1, gameProgress) * 0.7
        val potentialPoints = nearCompleteCityCount * 3.0 * nearCompleteProb

        // Check if farm is contested by opponent
        val contested = isFarmContested(state, farm, player)
        val contestPenalty = if (contested) 0.3 else 1.0

        // Farms are more valuable as game progresses (their value becomes more certain)
        val phaseMultiplier = 0.6 + 0.4 * gameProgress

        return (guaranteedPoints + potentialPoints) * contestPenalty * phaseMultiplier
    }

    private fun isFarmContested(state: GameState, farm: Farm, player: Int): Boolean {
        for (opp in 0 until state.playerCount) {
            if (opp == player) continue
            for (meeple in state.placedMeeples[opp]) {
                if (!meeple.type.isFarmer || meeple.farmerSide == null) continue
                if (CoordinateWithFarmerSide(meeple.coordinate, meeple.farmerSide) in farm.connections) {
                    return true
                }
            }
        }
        return false
    }

    // -- City evaluation --

    private fun evaluateCity(
        board: BoardMap, cws: CoordinateWithSide, player: Int,
        state: GameState, cache: StructureCache, gameProgress: Double
    ): Double {
        val city = cache.getCity(cws)
        if (city.finished) return PointsCalculator.cityPoints(city).toDouble()

        val tileCount = city.tileCoordinates.size
        val shieldCount = city.shieldCount

        // Unfinished value (end-game scoring)
        val guaranteed = if (city.hasCathedral) 0.0 else (tileCount + shieldCount).toDouble()
        // Completed value
        val potential = if (city.hasCathedral) {
            (tileCount * 3 + shieldCount * 3).toDouble()
        } else {
            (tileCount * 2 + shieldCount * 2).toDouble()
        }

        val openEdges = city.positions.count { pos ->
            val neighbor = pos.coordinate.neighbor(pos.side)
            !board.contains(neighbor)
        }

        val completionProb = completionProbForOpenEdges(openEdges, gameProgress)

        // Cathedral cities that won't complete are worth 0 - penalize heavily
        val cathedralRisk = if (city.hasCathedral && completionProb < 0.5) 0.5 else 1.0

        val contested = isContested(state, city.positions, player)
        // Contested penalty: if we're losing the contest, it's very bad; if tied, moderate
        val contestPenalty = if (contested) 0.25 else 1.0

        // Near-completion bonus: 1-open-edge cities are very likely to close
        val nearCompleteBonus = if (openEdges == 1) NEAR_COMPLETE_BONUS else 0.0

        return ((guaranteed + (potential - guaranteed) * completionProb) * contestPenalty + nearCompleteBonus) * cathedralRisk
    }

    // -- Road evaluation --

    private fun evaluateRoad(
        board: BoardMap, cws: CoordinateWithSide, player: Int,
        state: GameState, cache: StructureCache, gameProgress: Double
    ): Double {
        val road = cache.getRoad(cws)
        if (road.finished) return PointsCalculator.roadPoints(road).toDouble()

        val tileCount = road.tileCoordinates.size
        val guaranteed = if (road.hasInn) 0.0 else tileCount.toDouble()
        val potential = if (road.hasInn) (tileCount * 2).toDouble() else tileCount.toDouble()

        val openEdges = road.positions.count { pos ->
            if (pos.side !in Side.CARDINAL) false
            else !board.contains(pos.coordinate.neighbor(pos.side))
        }

        val completionProb = when (openEdges) {
            0 -> 1.0
            1 -> 0.8 * (1.0 - gameProgress * 0.2) // slightly less likely late game
            2 -> 0.5 * (1.0 - gameProgress * 0.3)
            else -> 0.25 * (1.0 - gameProgress * 0.4)
        }

        // Contested roads: opponent meeple on same road
        val contested = isContested(state, road.positions, player)
        val contestPenalty = if (contested) 0.25 else 1.0

        return (guaranteed + (potential - guaranteed) * completionProb) * contestPenalty
    }

    // -- Monastery evaluation --

    private fun evaluateMonastery(state: GameState, meeple: PlacedMeeple, gameProgress: Double): Double {
        val points = PointsCalculator.monasteryPoints(state.board, meeple.coordinate)
        val remaining = 9 - points

        // Estimate completion chance based on how surrounded the monastery is
        // More surrounded = more tiles already placed = higher chance remaining slots fill
        val surroundedness = points / 9.0
        // Base fill rate decreases as game progresses (fewer tiles left)
        val baseFillRate = 0.55 * (1.0 - gameProgress * 0.3)
        // Already-surrounded monasteries are in denser areas, so fill rate is higher
        val adjustedFillRate = baseFillRate * (0.7 + 0.3 * surroundedness)

        val expectedExtra = remaining * adjustedFillRate
        return points + expectedExtra * MONASTERY_PROGRESS_WEIGHT
    }

    // -- Completion probability model --

    /**
     * Game-phase-aware completion probability.
     * Late game: fewer tiles remaining means lower chance of filling gaps,
     * especially for structures with many open edges.
     */
    private fun completionProbForOpenEdges(openEdges: Int, gameProgress: Double): Double {
        // Base probabilities (early/mid game)
        val baseProb = when (openEdges) {
            0 -> 1.0
            1 -> 0.80
            2 -> 0.50
            3 -> 0.28
            4 -> 0.14
            5 -> 0.07
            else -> 0.03
        }
        // Late-game decay: each open edge becomes harder to fill with fewer tiles
        // The decay is stronger for more open edges
        val latePenalty = if (openEdges > 0) {
            gameProgress * 0.15 * openEdges.coerceAtMost(4)
        } else 0.0

        return (baseProb - latePenalty).coerceIn(0.0, 1.0)
    }

    // -- Trapped meeple detection --

    /**
     * Bonus for when an opponent has a meeple stuck on a structure that's very
     * unlikely to complete. This effectively means they're playing with fewer meeples.
     */
    private fun trappedMeepleBonus(state: GameState, opponent: Int, cache: StructureCache, gameProgress: Double): Double {
        var bonus = 0.0

        for (meeple in state.placedMeeples[opponent]) {
            if (meeple.type.isFarmer) continue // farmers aren't "trapped"
            if (meeple.side == null) continue

            val tile = state.board.getRotated(meeple.coordinate) ?: continue
            val cws = CoordinateWithSide(meeple.coordinate, meeple.side)

            val openEdges: Int
            if (meeple.side in tile.citySides()) {
                val city = cache.getCity(cws)
                if (city.finished) continue
                openEdges = city.positions.count { pos ->
                    !state.board.contains(pos.coordinate.neighbor(pos.side))
                }
            } else if (tile.terrainAt(meeple.side) == TerrainType.ROAD) {
                val road = cache.getRoad(cws)
                if (road.finished) continue
                openEdges = road.positions.count { pos ->
                    pos.side in Side.CARDINAL && !state.board.contains(pos.coordinate.neighbor(pos.side))
                }
            } else {
                continue
            }

            // A meeple is "trapped" when the structure has many open edges, especially late game
            if (openEdges >= 3) {
                val trappedStrength = (openEdges - 2) * gameProgress
                bonus += OPPONENT_TRAPPED_MEEPLE_BONUS * trappedStrength
            }
        }

        return bonus
    }

    private fun isContested(state: GameState, positions: Set<CoordinateWithSide>, player: Int): Boolean {
        for (opp in 0 until state.playerCount) {
            if (opp == player) continue
            for (meeple in state.placedMeeples[opp]) {
                if (meeple.type.isFarmer || meeple.side == null) continue
                if (CoordinateWithSide(meeple.coordinate, meeple.side) in positions) return true
            }
        }
        return false
    }

    // -- Structure cache --

    private var cachedBoard: BoardMap? = null
    private var cachedStructures: StructureCache? = null

    private fun getOrCreateCache(board: BoardMap): StructureCache {
        if (board === cachedBoard) return cachedStructures!!
        val cache = StructureCache(board)
        cachedBoard = board
        cachedStructures = cache
        return cache
    }

    private class StructureCache(private val board: BoardMap) {
        private val cities = HashMap<CoordinateWithSide, City>()
        private val roads = HashMap<CoordinateWithSide, Road>()
        private val farms = HashMap<CoordinateWithFarmerSide, Farm>()

        fun getCity(cws: CoordinateWithSide): City {
            cities[cws]?.let { return it }
            val city = CityDetector.findCity(board, cws)
            for (pos in city.positions) {
                cities[pos] = city
            }
            return city
        }

        fun getRoad(cws: CoordinateWithSide): Road {
            roads[cws]?.let { return it }
            val road = RoadDetector.findRoad(board, cws)
            for (pos in road.positions) {
                roads[pos] = road
            }
            return road
        }

        fun getFarm(cwfs: CoordinateWithFarmerSide): Farm {
            farms[cwfs]?.let { return it }
            val farm = FarmDetector.findFarm(board, cwfs)
            for (pos in farm.connections) {
                farms[pos] = farm
            }
            return farm
        }
    }
}
