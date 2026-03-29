package com.wingedsheep.carcassonne.ai

import com.wingedsheep.carcassonne.engine.GameState
import com.wingedsheep.carcassonne.engine.MoveGenerator
import com.wingedsheep.carcassonne.engine.StateUpdater
import com.wingedsheep.carcassonne.model.*

/**
 * Iterative-deepening alpha-beta search AI.
 *
 * Key techniques:
 * - **Iterative deepening**: starts at depth 1, increases until time runs out.
 *   Always has a best move from the previous completed iteration.
 * - **Alpha-beta pruning**: prunes branches that can't affect the result.
 * - **Move ordering**: pre-scores moves with a cheap heuristic so the best moves
 *   are searched first, dramatically improving alpha-beta cutoff rates.
 * - **Dynamic branching**: at deeper levels, only the top-K moves are searched.
 *   When few moves exist, the search naturally goes deeper within the time budget.
 * - **Quiescence extension**: positions where a structure is about to complete
 *   get +1 depth to avoid misevaluation at the horizon.
 *
 * A "ply" here = one full turn (tile placement + meeple decision).
 */
data class SearchStats(
    val action: Action,
    val depthCompleted: Int,
    val nodesSearched: Long,
)

class TreeSearchAI(
    private val player: Int,
    private val maxDepthTurns: Int = 6,
    private val timeLimitMs: Long = 3000,
) {
    companion object {
        /** Number of top-ordered moves that always get full-depth search (no LMR). */
        private const val LMR_FULL_DEPTH_MOVES = 3
    }
    private var deadline: Long = 0
    private var nodesSearched: Long = 0
    private var depthCompleted: Int = 0

    fun chooseAction(state: GameState): Action = search(state).action

    fun search(state: GameState): SearchStats {
        deadline = System.currentTimeMillis() + timeLimitMs
        nodesSearched = 0
        depthCompleted = 0

        val action = when (state.phase) {
            GamePhase.TILE_PLACEMENT -> chooseTilePlacement(state)
            GamePhase.MEEPLE_PLACEMENT -> chooseMeeplePlacement(state)
        }
        return SearchStats(action, depthCompleted, nodesSearched)
    }

    // -- Top-level move selection --

    private fun chooseTilePlacement(state: GameState): Action {
        val tileId = state.currentTileId ?: return Pass
        val tilePlacements = MoveGenerator.validTilePlacements(state, tileId)
        if (tilePlacements.isEmpty()) return Pass
        if (tilePlacements.size == 1) return tilePlacements[0]

        // Pre-score and sort for move ordering
        val scored = tilePlacements.map { action ->
            val afterTile = StateUpdater.applyAction(state, action)
            action to quickEvalBestMeeple(afterTile)
        }.sortedByDescending { it.second }

        // Iterative deepening
        var bestAction: Action = scored[0].first
        for (depth in 1..maxDepthTurns) {
            if (isTimedOut()) break

            var alpha = Double.NEGATIVE_INFINITY
            val beta = Double.POSITIVE_INFINITY
            var depthBest: Action = bestAction
            var depthBestScore = Double.NEGATIVE_INFINITY
            val branchLimit = branchLimitForDepth(depth, scored.size)

            for ((action, _) in scored.take(branchLimit)) {
                if (isTimedOut()) break

                val afterTile = StateUpdater.applyAction(state, action)
                val score = searchMeepleThenDeeper(afterTile, depth, true, alpha, beta)

                if (score > depthBestScore) {
                    depthBestScore = score
                    depthBest = action
                }
                alpha = maxOf(alpha, score)
            }

            if (!isTimedOut()) {
                // Only commit result from a fully completed depth iteration
                bestAction = depthBest
                depthCompleted = depth
            }
        }

        return bestAction
    }

    private fun chooseMeeplePlacement(state: GameState): Action {
        val meepleActions = MoveGenerator.validMeeplePlacements(state)
        if (meepleActions.isEmpty()) return Pass
        if (meepleActions.size == 1) return meepleActions[0]

        val scored = meepleActions.map { action ->
            val after = StateUpdater.applyAction(state, action)
            action to BoardEvaluator.evaluate(after, player)
        }.sortedByDescending { it.second }

        var bestAction: Action = scored[0].first
        for (depth in 1..maxDepthTurns) {
            if (isTimedOut()) break

            var alpha = Double.NEGATIVE_INFINITY
            val beta = Double.POSITIVE_INFINITY
            var depthBest: Action = bestAction
            var depthBestScore = Double.NEGATIVE_INFINITY

            for ((action, _) in scored) {
                if (isTimedOut()) break
                val after = StateUpdater.applyAction(state, action)
                val score = alphabeta(after, depth, false, alpha, beta)
                if (score > depthBestScore) {
                    depthBestScore = score
                    depthBest = action
                }
                alpha = maxOf(alpha, score)
            }

            if (!isTimedOut()) {
                bestAction = depthBest
                depthCompleted = depth
            }
        }

        return bestAction
    }

    // -- Core search --

    /**
     * After a tile is placed (state is in MEEPLE_PLACEMENT), evaluate all meeple
     * options then continue deeper search.
     */
    private fun searchMeepleThenDeeper(
        state: GameState,
        depthRemaining: Int,
        maximizing: Boolean,
        alpha: Double,
        beta: Double,
    ): Double {
        val meepleActions = MoveGenerator.validMeeplePlacements(state)
        if (meepleActions.isEmpty()) {
            return if (depthRemaining > 1 && !isTimedOut()) {
                alphabeta(state, depthRemaining - 1, !maximizing, alpha, beta)
            } else {
                nodesSearched++
                BoardEvaluator.evaluate(state, player)
            }
        }

        // Pre-score meeple actions for ordering
        val sorted = meepleActions.map { action ->
            val after = StateUpdater.applyAction(state, action)
            action to BoardEvaluator.evaluate(after, player)
        }.let { list ->
            if (maximizing) list.sortedByDescending { it.second }
            else list.sortedBy { it.second }
        }

        var currentAlpha = alpha
        var currentBeta = beta
        var bestScore = if (maximizing) Double.NEGATIVE_INFINITY else Double.POSITIVE_INFINITY

        for ((action, _) in sorted) {
            if (isTimedOut()) break

            val after = StateUpdater.applyAction(state, action)
            val score = if (depthRemaining > 1 && !isTimedOut()) {
                alphabeta(after, depthRemaining - 1, !maximizing, currentAlpha, currentBeta)
            } else {
                nodesSearched++
                BoardEvaluator.evaluate(after, player)
            }

            if (maximizing) {
                bestScore = maxOf(bestScore, score)
                currentAlpha = maxOf(currentAlpha, score)
                if (currentAlpha >= currentBeta) break // beta cutoff
            } else {
                bestScore = minOf(bestScore, score)
                currentBeta = minOf(currentBeta, score)
                if (currentAlpha >= currentBeta) break // alpha cutoff
            }
        }

        return bestScore
    }

    /**
     * Alpha-beta search over full turns with PVS and LMR.
     * State should be at TILE_PLACEMENT phase (start of a player's turn).
     *
     * - **PVS (Principal Variation Search)**: the first move (expected best from ordering)
     *   is searched with a full window. Subsequent moves use a null/scout window; if
     *   the scout search fails high, a full-window re-search is done.
     * - **LMR (Late Move Reductions)**: moves ordered beyond the top few are searched
     *   at reduced depth. If the reduced search returns a score that beats alpha,
     *   a full-depth re-search is triggered. This lets promising branches consume more
     *   of the time budget while weak branches are cheaply dismissed.
     */
    private fun alphabeta(
        state: GameState,
        depthRemaining: Int,
        maximizing: Boolean,
        alpha: Double,
        beta: Double,
    ): Double {
        if (state.isFinished || depthRemaining <= 0 || isTimedOut()) {
            nodesSearched++
            return BoardEvaluator.evaluate(state, player)
        }

        val tileId = state.currentTileId
        if (tileId == null) {
            nodesSearched++
            return BoardEvaluator.evaluate(state, player)
        }

        val tilePlacements = MoveGenerator.validTilePlacements(state, tileId)
        if (tilePlacements.isEmpty()) {
            val afterPass = StateUpdater.applyAction(state, Pass)
            return alphabeta(afterPass, depthRemaining, maximizing, alpha, beta)
        }

        // Pre-score for move ordering (cheap greedy eval)
        val sorted = presortTilePlacements(state, tilePlacements, maximizing)

        // Dynamic branching: limit width at deeper levels
        val branchLimit = branchLimitForDepth(depthRemaining, sorted.size)

        var currentAlpha = alpha
        var currentBeta = beta
        var bestScore = if (maximizing) Double.NEGATIVE_INFINITY else Double.POSITIVE_INFINITY
        var isFirstMove = true

        for ((moveIndex, pair) in sorted.take(branchLimit).withIndex()) {
            if (isTimedOut()) break
            val (action, quickScore) = pair

            val afterTile = StateUpdater.applyAction(state, action)

            // Quiescence extension: if this move likely completes a structure, search +1 deeper
            val effectiveDepth = if (depthRemaining <= 2 && isHotMove(afterTile, quickScore)) {
                depthRemaining + 1
            } else {
                depthRemaining
            }

            // LMR: reduce depth for late moves at sufficient depth
            val reducedDepth = lmrDepth(effectiveDepth, moveIndex)

            val score: Double
            if (isFirstMove) {
                // PVS: search first move with full window
                score = searchMeepleThenDeeper(afterTile, effectiveDepth, maximizing, currentAlpha, currentBeta)
                isFirstMove = false
            } else {
                // LMR + PVS scout: search with reduced depth and null window
                val scoutBound = if (maximizing) currentAlpha + 0.01 else currentBeta - 0.01
                var lmrScore = if (maximizing) {
                    searchMeepleThenDeeper(afterTile, reducedDepth, maximizing, currentAlpha, scoutBound)
                } else {
                    searchMeepleThenDeeper(afterTile, reducedDepth, maximizing, scoutBound, currentBeta)
                }

                // Re-search at full depth + full window if scout/LMR found something interesting
                if (maximizing && lmrScore > currentAlpha && (reducedDepth < effectiveDepth || lmrScore < currentBeta)) {
                    lmrScore = searchMeepleThenDeeper(afterTile, effectiveDepth, maximizing, currentAlpha, currentBeta)
                } else if (!maximizing && lmrScore < currentBeta && (reducedDepth < effectiveDepth || lmrScore > currentAlpha)) {
                    lmrScore = searchMeepleThenDeeper(afterTile, effectiveDepth, maximizing, currentAlpha, currentBeta)
                }

                score = lmrScore
            }

            if (maximizing) {
                bestScore = maxOf(bestScore, score)
                currentAlpha = maxOf(currentAlpha, score)
                if (currentAlpha >= currentBeta) break
            } else {
                bestScore = minOf(bestScore, score)
                currentBeta = minOf(currentBeta, score)
                if (currentAlpha >= currentBeta) break
            }
        }

        return bestScore
    }

    // -- Move ordering --

    /**
     * Pre-score tile placements with a fast greedy evaluation (place tile + best meeple).
     * Returns sorted list: best-first for maximizing, worst-first for minimizing.
     */
    private fun presortTilePlacements(
        state: GameState,
        placements: List<PlaceTile>,
        maximizing: Boolean,
    ): List<Pair<PlaceTile, Double>> {
        val scored = placements.map { action ->
            val afterTile = StateUpdater.applyAction(state, action)
            action to quickEvalBestMeeple(afterTile)
        }
        return if (maximizing) scored.sortedByDescending { it.second }
        else scored.sortedBy { it.second }
    }

    /**
     * Quick evaluation: try all meeple options, return best eval.
     * Used for move ordering only - no deeper search.
     */
    private fun quickEvalBestMeeple(meeplePhaseState: GameState): Double {
        val meepleActions = MoveGenerator.validMeeplePlacements(meeplePhaseState)
        if (meepleActions.isEmpty()) return BoardEvaluator.evaluate(meeplePhaseState, player)

        var best = Double.NEGATIVE_INFINITY
        for (action in meepleActions) {
            val after = StateUpdater.applyAction(meeplePhaseState, action)
            val score = BoardEvaluator.evaluate(after, player)
            if (score > best) best = score
        }
        return best
    }

    // -- Late Move Reductions --

    /**
     * Compute reduced depth for late moves.
     * Top 3 moves are always searched at full depth.
     * Later moves get a reduction that grows with move index,
     * but never reduces below 1 (so we always do at least a shallow search).
     */
    private fun lmrDepth(effectiveDepth: Int, moveIndex: Int): Int {
        if (effectiveDepth <= 2 || moveIndex < LMR_FULL_DEPTH_MOVES) return effectiveDepth
        // Logarithmic reduction: grows slowly with move index
        val reduction = 1 + (moveIndex - LMR_FULL_DEPTH_MOVES) / 4
        return maxOf(1, effectiveDepth - reduction)
    }

    // -- Dynamic branching --

    /**
     * At depth 1 (deepest in remaining search), examine fewer moves.
     * At the root, examine everything. Scales between.
     */
    private fun branchLimitForDepth(depthRemaining: Int, totalMoves: Int): Int {
        if (totalMoves <= 6) return totalMoves // small enough, search all
        return when {
            depthRemaining >= 4 -> minOf(totalMoves, 8)
            depthRemaining == 3 -> minOf(totalMoves, 10)
            depthRemaining == 2 -> minOf(totalMoves, 14)
            else -> totalMoves // depth 1: full width (we're about to eval leaves)
        }
    }

    // -- Quiescence --

    /**
     * A move is "hot" if it likely completes a city or road (large score jump).
     * We detect this by checking if the quick-eval is significantly above average.
     */
    private fun isHotMove(afterTile: GameState, quickScore: Double): Boolean {
        // If placing this tile resulted in score changes, it's completing something
        val coord = afterTile.lastPlacedCoordinate ?: return false
        val tile = afterTile.board.getRotated(coord) ?: return false

        // Check for near-complete structures: any city zone with only 1 open edge
        for (zone in tile.cityZones) {
            val side = zone.firstOrNull() ?: continue
            val neighborCoord = coord.neighbor(side)
            if (!afterTile.board.contains(neighborCoord)) continue
            // This side is connected - check the other sides of the zone
            val openCount = zone.count { s ->
                !afterTile.board.contains(coord.neighbor(s))
            }
            if (openCount <= 1) return true // city nearly or fully closed
        }

        return false
    }

    private fun isTimedOut(): Boolean = System.currentTimeMillis() > deadline
}
