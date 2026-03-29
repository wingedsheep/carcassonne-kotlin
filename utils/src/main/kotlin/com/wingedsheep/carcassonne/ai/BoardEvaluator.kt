package com.wingedsheep.carcassonne.ai

import com.wingedsheep.carcassonne.engine.BoardMap
import com.wingedsheep.carcassonne.engine.GameState
import com.wingedsheep.carcassonne.model.*
import com.wingedsheep.carcassonne.scoring.CityDetector
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
 */
object BoardEvaluator {

    /** Weight constants - tuned for reasonable play. */
    private const val SCORE_WEIGHT = 1.0
    private const val OWN_POTENTIAL_WEIGHT = 0.7
    private const val OPPONENT_POTENTIAL_WEIGHT = 0.5
    private const val MEEPLE_AVAILABILITY_WEIGHT = 0.5
    private const val CITY_COMPLETION_BONUS = 0.3 // bonus per % of city that's close to finishing
    private const val MONASTERY_PROGRESS_WEIGHT = 0.8
    /** Minimum expected return for a placed meeple to be "worth it". Below this, the meeple is wasted. */
    private const val MEEPLE_OPPORTUNITY_COST = 3.0

    /**
     * Default evaluation: uses full score differential (differentialWeight = 1.0).
     */
    fun evaluate(state: GameState, player: Int): Double = evaluate(state, player, 1.0)

    /**
     * Configurable evaluation.
     * @param differentialWeight 0.0 = maximize own score only, 1.0 = maximize score differential.
     *   Blends between `myScore` (weight 1-d) and `myScore - opponentScore` (weight d).
     *   Equivalent to: `myScore - d * opponentScore`.
     */
    fun evaluate(state: GameState, player: Int, differentialWeight: Double): Double {
        var score = 0.0

        // Reuse structure cache when the board object is the same (e.g. across meeple option evaluations)
        val cache = getOrCreateCache(state.board)

        // 1. Realized score: blend own score vs differential
        val myScore = state.scores[player]
        val bestOpponentScore = (0 until state.playerCount)
            .filter { it != player }
            .maxOfOrNull { state.scores[it] } ?: 0
        score += (myScore - differentialWeight * bestOpponentScore) * SCORE_WEIGHT

        // 2. Potential points from own placed meeples, net of opportunity cost
        score += netMeeplePotential(state, player, cache) * OWN_POTENTIAL_WEIGHT

        // 3. Subtract opponent potential (scaled by differentialWeight)
        for (opp in 0 until state.playerCount) {
            if (opp == player) continue
            score -= netMeeplePotential(state, opp, cache) * OPPONENT_POTENTIAL_WEIGHT * differentialWeight
        }

        // 4. Meeple availability bonus
        val meeplesInHand = state.meeples[player].normal
        score += meeplesInHand * MEEPLE_AVAILABILITY_WEIGHT

        return score
    }

    /**
     * Creates an evaluator function with a fixed differentialWeight.
     */
    fun withDifferentialWeight(differentialWeight: Double): (GameState, Int) -> Double =
        { state, player -> evaluate(state, player, differentialWeight) }

    /**
     * Net meeple potential: sum of each placed meeple's expected value minus
     * the opportunity cost of having it tied up. A meeple on a low-value
     * structure yields a negative contribution, discouraging wasteful placement.
     */
    private fun netMeeplePotential(state: GameState, player: Int, cache: StructureCache): Double {
        var total = 0.0
        val evaluated = mutableSetOf<PlacedMeeple>()

        for (meeple in state.placedMeeples[player]) {
            if (meeple in evaluated) continue
            evaluated.add(meeple)

            val rawPotential = singleMeeplePotential(state, meeple, player, cache)
            total += rawPotential - MEEPLE_OPPORTUNITY_COST
        }

        return total
    }

    /**
     * Estimate the expected points a single placed meeple will earn.
     */
    private fun singleMeeplePotential(state: GameState, meeple: PlacedMeeple, player: Int, cache: StructureCache): Double {
        return when {
            meeple.type.isFarmer -> {
                // Farmers are hard to evaluate mid-game; give small fixed value
                3.0
            }
            meeple.side != null -> {
                val tile = state.board.getRotated(meeple.coordinate) ?: return 0.0
                val cws = CoordinateWithSide(meeple.coordinate, meeple.side)

                if (meeple.side in tile.citySides()) {
                    evaluateCity(state.board, cws, player, state, cache)
                } else if (tile.terrainAt(meeple.side) == TerrainType.ROAD) {
                    evaluateRoad(state.board, cws, cache)
                } else {
                    0.0
                }
            }
            else -> {
                // Monastery
                val points = PointsCalculator.monasteryPoints(state.board, meeple.coordinate)
                points * MONASTERY_PROGRESS_WEIGHT + (if (points >= 7) 2.0 else 0.0)
            }
        }
    }

    private fun evaluateCity(board: BoardMap, cws: CoordinateWithSide, player: Int, state: GameState, cache: StructureCache): Double {
        val city = cache.getCity(cws)
        val basePoints = PointsCalculator.cityPoints(city).toDouble()

        if (city.finished) return basePoints

        val openEdges = city.positions.count { pos ->
            val neighbor = pos.coordinate.neighbor(pos.side)
            !board.contains(neighbor)
        }

        val completionFactor = when (openEdges) {
            0 -> 2.0
            1 -> 1.4
            2 -> 0.9
            3 -> 0.6
            else -> 0.4
        }

        val contested = isContested(state, city.positions, player)
        val contestPenalty = if (contested) 0.3 else 1.0

        return basePoints * completionFactor * contestPenalty * CITY_COMPLETION_BONUS + basePoints * 0.5
    }

    private fun evaluateRoad(board: BoardMap, cws: CoordinateWithSide, cache: StructureCache): Double {
        val road = cache.getRoad(cws)
        val basePoints = PointsCalculator.roadPoints(road).toDouble()
        if (road.finished) return basePoints

        val openEdges = road.positions.count { pos ->
            if (pos.side !in Side.CARDINAL) false
            else !board.contains(pos.coordinate.neighbor(pos.side))
        }

        val completionFactor = when (openEdges) {
            0 -> 1.5
            1 -> 1.0
            2 -> 0.7
            else -> 0.5
        }

        return basePoints * completionFactor * 0.5 + basePoints * 0.3
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

    // Single-entry cache: reuse BFS results when the board object hasn't changed.
    // Safe because applyMeeplePlacement shares the board reference, while
    // applyTilePlacement creates a new board via copy(). Single-threaded AI only.
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

        fun getCity(cws: CoordinateWithSide): City {
            cities[cws]?.let { return it }
            val city = CityDetector.findCity(board, cws)
            // Cache under all positions in this city so other meeples on the same city hit the cache
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
    }
}
