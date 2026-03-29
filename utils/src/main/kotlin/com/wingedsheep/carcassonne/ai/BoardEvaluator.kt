package com.wingedsheep.carcassonne.ai

import com.wingedsheep.carcassonne.engine.BoardMap
import com.wingedsheep.carcassonne.engine.GameState
import com.wingedsheep.carcassonne.model.*
import com.wingedsheep.carcassonne.scoring.CityDetector
import com.wingedsheep.carcassonne.scoring.PointsCalculator
import com.wingedsheep.carcassonne.scoring.RoadDetector
import com.wingedsheep.carcassonne.tile.TileRegistry

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
    private const val MEEPLE_AVAILABILITY_WEIGHT = 0.4
    private const val CITY_COMPLETION_BONUS = 0.3 // bonus per % of city that's close to finishing
    private const val MONASTERY_PROGRESS_WEIGHT = 0.8

    fun evaluate(state: GameState, player: Int): Double {
        var score = 0.0

        // 1. Realized score differential
        val myScore = state.scores[player]
        val bestOpponentScore = (0 until state.playerCount)
            .filter { it != player }
            .maxOfOrNull { state.scores[it] } ?: 0
        score += (myScore - bestOpponentScore) * SCORE_WEIGHT

        // 2. Potential points from own placed meeples
        score += meeplePotential(state, player) * OWN_POTENTIAL_WEIGHT

        // 3. Subtract opponent potential
        for (opp in 0 until state.playerCount) {
            if (opp == player) continue
            score -= meeplePotential(state, opp) * OPPONENT_POTENTIAL_WEIGHT
        }

        // 4. Meeple availability bonus
        val meeplesInHand = state.meeples[player].normal
        score += meeplesInHand * MEEPLE_AVAILABILITY_WEIGHT

        return score
    }

    /**
     * Estimate the expected points a player's placed meeples will earn.
     * Accounts for structure size, completion progress, and position.
     */
    private fun meeplePotential(state: GameState, player: Int): Double {
        var potential = 0.0
        val evaluated = mutableSetOf<PlacedMeeple>()

        for (meeple in state.placedMeeples[player]) {
            if (meeple in evaluated) continue
            evaluated.add(meeple)

            when {
                meeple.type.isFarmer -> {
                    // Farmers are hard to evaluate mid-game; give small fixed value
                    potential += 3.0
                }
                meeple.side != null -> {
                    val tile = state.board.getRotated(meeple.coordinate) ?: continue
                    val cws = CoordinateWithSide(meeple.coordinate, meeple.side)

                    if (meeple.side in tile.citySides()) {
                        potential += evaluateCity(state.board, cws, player, state)
                    } else if (tile.terrainAt(meeple.side) == TerrainType.ROAD) {
                        potential += evaluateRoad(state.board, cws)
                    }
                }
                else -> {
                    // Monastery
                    val points = PointsCalculator.monasteryPoints(state.board, meeple.coordinate)
                    // Scale by how close to completion (9 = complete)
                    potential += points * MONASTERY_PROGRESS_WEIGHT + (if (points >= 7) 2.0 else 0.0)
                }
            }
        }

        return potential
    }

    private fun evaluateCity(board: BoardMap, cws: CoordinateWithSide, player: Int, state: GameState): Double {
        val city = CityDetector.findCity(board, cws)
        val basePoints = PointsCalculator.cityPoints(city).toDouble()

        if (city.finished) return basePoints // already scored or about to be scored

        // Estimate completion likelihood based on open edges
        val totalPositions = city.positions.size
        val openEdges = city.positions.count { pos ->
            val neighbor = pos.coordinate.neighbor(pos.side)
            !board.contains(neighbor)
        }

        // Fewer open edges = more likely to complete
        // Completion bonus: if only 1-2 open edges, city is likely to complete
        val completionFactor = when (openEdges) {
            0 -> 2.0 // finished - full value (shouldn't happen since we check finished above)
            1 -> 1.4 // very likely to complete -> value at near-finished rate
            2 -> 0.9
            3 -> 0.6
            else -> 0.4
        }

        // Check if we're the sole claimer (no opponent meeples)
        val contested = isContested(state, city.positions, player)
        val contestPenalty = if (contested) 0.3 else 1.0

        return basePoints * completionFactor * contestPenalty * CITY_COMPLETION_BONUS + basePoints * 0.5
    }

    private fun evaluateRoad(board: BoardMap, cws: CoordinateWithSide): Double {
        val road = RoadDetector.findRoad(board, cws)
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
}
