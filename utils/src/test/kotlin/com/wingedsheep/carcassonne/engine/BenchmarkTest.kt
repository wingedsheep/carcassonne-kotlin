package com.wingedsheep.carcassonne.engine

import com.wingedsheep.carcassonne.ai.BoardEvaluator
import com.wingedsheep.carcassonne.ai.TreeSearchAI
import kotlin.test.Test
import kotlin.random.Random
import java.io.File

data class GameStats(
    val timeMs: Double,
    val totalMovesConsidered: Int,
    val avgBranchingFactor: Double,
    val maxBranchingFactor: Int,
    val steps: Int,
)

data class SearchBenchmarkEntry(
    val timeBudgetMs: Long,
    val depthCompleted: Int,
    val nodesSearched: Long,
    val elapsedMs: Double,
)

class BenchmarkTest {

    private fun playRandomGame(seed: Int): GameStats {
        val game = Game.builder().players(2).build()
        val rng = Random(seed)
        var steps = 0
        var totalMoves = 0
        var maxBranching = 0
        val start = System.nanoTime()
        while (!game.isFinished()) {
            val valid = game.getValidActions()
            if (valid.isEmpty()) break
            totalMoves += valid.size
            if (valid.size > maxBranching) maxBranching = valid.size
            game.apply(valid[rng.nextInt(valid.size)])
            steps++
        }
        val elapsed = (System.nanoTime() - start) / 1_000_000.0
        return GameStats(
            timeMs = elapsed,
            totalMovesConsidered = totalMoves,
            avgBranchingFactor = if (steps > 0) totalMoves.toDouble() / steps else 0.0,
            maxBranchingFactor = maxBranching,
            steps = steps,
        )
    }

    @Test
    fun benchmark() {
        // Warmup
        repeat(50) { playRandomGame(it) }

        val numGames = 1000
        val stats = mutableListOf<GameStats>()
        val start = System.nanoTime()
        for (i in 0 until numGames) {
            stats.add(playRandomGame(i + 1000))
        }
        val elapsed = System.nanoTime() - start
        val ms = elapsed / 1_000_000.0
        val gamesPerSec = numGames / (ms / 1000.0)
        val avgBranching = stats.map { it.avgBranchingFactor }.average()
        val avgMovesConsidered = stats.map { it.totalMovesConsidered }.average()

        val report = buildString {
            appendLine("=== Carcassonne Engine Benchmark ===")
            appendLine("Games played:         $numGames")
            appendLine("Total time:           ${"%.1f".format(ms)} ms")
            appendLine("Games/second:         ${"%.0f".format(gamesPerSec)}")
            appendLine("Avg time/game:        ${"%.2f".format(ms / numGames)} ms")
            appendLine("Avg branching factor: ${"%.1f".format(avgBranching)}")
            appendLine("Avg moves considered: ${"%.0f".format(avgMovesConsidered)} per game")
        }
        File("/tmp/carcassonne-benchmark.txt").writeText(report)
        println(report)

        // Write CSV for chart generation
        File("/tmp/carcassonne-benchmark.csv").printWriter().use { out ->
            out.println("game,time_ms,moves_considered,avg_branching,max_branching,steps")
            for ((i, s) in stats.withIndex()) {
                out.println("${i + 1},${s.timeMs},${s.totalMovesConsidered},${s.avgBranchingFactor},${s.maxBranchingFactor},${s.steps}")
            }
        }
    }

    @Test
    fun searchDepthBenchmark() {
        // Play a game to mid-game state (~40 tiles placed) for a realistic position
        val game = Game.builder().players(2).build()
        val rng = Random(42)
        var steps = 0
        while (!game.isFinished() && steps < 80) { // ~40 tile placements (tile + meeple = 2 steps each)
            val valid = game.getValidActions()
            if (valid.isEmpty()) break
            game.apply(valid[rng.nextInt(valid.size)])
            steps++
        }
        val midGameState = game.getState()
        println("Mid-game position: ${midGameState.board.placedCount} tiles placed, step $steps")

        val timeBudgets = listOf(10L, 50L, 100L, 250L, 500L, 1000L, 2000L, 5000L)
        val results = mutableListOf<SearchBenchmarkEntry>()

        for (budget in timeBudgets) {
            // Run multiple trials and take the best completed depth
            val trials = 3
            val trialResults = mutableListOf<SearchBenchmarkEntry>()
            for (t in 0 until trials) {
                val ai = TreeSearchAI(player = 0, maxDepthTurns = 20, timeLimitMs = budget)
                val start = System.nanoTime()
                val stats = ai.search(midGameState)
                val elapsed = (System.nanoTime() - start) / 1_000_000.0
                trialResults.add(SearchBenchmarkEntry(budget, stats.depthCompleted, stats.nodesSearched, elapsed))
            }
            // Use median trial
            val median = trialResults.sortedBy { it.nodesSearched }[trials / 2]
            results.add(median)
            println("Budget: ${budget}ms -> depth ${median.depthCompleted}, ${median.nodesSearched} nodes in ${"%.0f".format(median.elapsedMs)}ms")
        }

        File("/tmp/carcassonne-search-benchmark.csv").printWriter().use { out ->
            out.println("time_budget_ms,depth_completed,nodes_searched,elapsed_ms")
            for (r in results) {
                out.println("${r.timeBudgetMs},${r.depthCompleted},${r.nodesSearched},${r.elapsedMs}")
            }
        }
    }

    /**
     * Play real AI games and measure where time is spent per component.
     * Wraps each hot function with nanoTime probes during actual search.
     */
    @Test
    fun profileRealGames() {
        val numGames = 3
        val timeBudget = 1000L

        // Accumulators (nanos)
        var totalBoardCopy = 0L
        var totalMoveGenTile = 0L
        var totalMoveGenMeeple = 0L
        var totalApplyAction = 0L
        var totalEvaluate = 0L
        var totalSearch = 0L
        var totalMoves = 0

        for (g in 0 until numGames) {
            val game = Game.builder().players(2).build()
            val ai0 = TreeSearchAI(player = 0, timeLimitMs = timeBudget)
            val ai1 = TreeSearchAI(player = 1, timeLimitMs = timeBudget)

            while (!game.isFinished()) {
                val state = game.getState()
                val ai = if (state.currentPlayer == 0) ai0 else ai1

                val t0 = System.nanoTime()
                val action = ai.chooseAction(state)
                totalSearch += System.nanoTime() - t0
                totalMoves++

                game.apply(action)
            }
            val scores = game.getFinalScores()
            println("Game ${g + 1}: ${scores.joinToString(" - ")}")
        }

        // Now measure component cost with targeted microbenchmarks in representative positions
        // Play to various game stages and measure
        var evalTime = 0L
        var moveGenTileTime = 0L
        var moveGenMeepleTime = 0L
        var boardCopyTime = 0L
        var applyTileTime = 0L
        var applyMeepleTime = 0L
        var evalCalls = 0L
        var moveGenTileCalls = 0L
        var moveGenMeepleCalls = 0L
        var boardCopyCalls = 0L
        var applyTileCalls = 0L
        var applyMeepleCalls = 0L

        // Sample at multiple game stages
        for (seed in 0 until 10) {
            val game = Game.builder().players(2).build()
            val rng = Random(seed + 100)
            var steps = 0
            // Play to random mid-game point
            val target = 40 + seed * 10
            while (!game.isFinished() && steps < target) {
                val valid = game.getValidActions()
                if (valid.isEmpty()) break
                game.apply(valid[rng.nextInt(valid.size)])
                steps++
            }
            if (game.isFinished()) continue
            val state = game.getState()
            val tileId = state.currentTileId ?: continue

            val iters = 1000

            // Tile move gen
            var t = System.nanoTime()
            var placements: List<com.wingedsheep.carcassonne.model.PlaceTile> = emptyList()
            repeat(iters) { placements = MoveGenerator.validTilePlacements(state, tileId) }
            moveGenTileTime += System.nanoTime() - t
            moveGenTileCalls += iters

            if (placements.isEmpty()) continue
            val placement = placements[0]

            // Apply tile
            t = System.nanoTime()
            var afterTile: GameState = state
            repeat(iters) { afterTile = StateUpdater.applyAction(state, placement) }
            applyTileTime += System.nanoTime() - t
            applyTileCalls += iters

            // Board copy (isolated)
            t = System.nanoTime()
            repeat(iters) { state.board.copy() }
            boardCopyTime += System.nanoTime() - t
            boardCopyCalls += iters

            // Meeple move gen
            t = System.nanoTime()
            var meepleActions: List<com.wingedsheep.carcassonne.model.Action> = emptyList()
            repeat(iters) { meepleActions = MoveGenerator.validMeeplePlacements(afterTile) }
            moveGenMeepleTime += System.nanoTime() - t
            moveGenMeepleCalls += iters

            // Evaluate
            t = System.nanoTime()
            repeat(iters) { BoardEvaluator.evaluate(afterTile, 0) }
            evalTime += System.nanoTime() - t
            evalCalls += iters

            // Apply meeple (if possible)
            val meeple = meepleActions.firstOrNull { it is com.wingedsheep.carcassonne.model.PlaceMeeple }
            if (meeple != null) {
                t = System.nanoTime()
                repeat(iters) { StateUpdater.applyAction(afterTile, meeple) }
                applyMeepleTime += System.nanoTime() - t
                applyMeepleCalls += iters
            }
        }

        println("\n=== Component Cost (avg across game stages) ===")
        fun avg(total: Long, calls: Long) = if (calls > 0) total / calls else 0
        val components = listOf(
            "Board evaluation" to (avg(evalTime, evalCalls) to evalCalls),
            "Tile move gen" to (avg(moveGenTileTime, moveGenTileCalls) to moveGenTileCalls),
            "Meeple move gen" to (avg(moveGenMeepleTime, moveGenMeepleCalls) to moveGenMeepleCalls),
            "Apply tile action" to (avg(applyTileTime, applyTileCalls) to applyTileCalls),
            "Apply meeple action" to (avg(applyMeepleTime, applyMeepleCalls) to applyMeepleCalls),
            "Board copy (isolated)" to (avg(boardCopyTime, boardCopyCalls) to boardCopyCalls),
        )
        val totalComponent = components.sumOf { it.second.first }
        for ((name, data) in components.sortedByDescending { it.second.first }) {
            val (ns, _) = data
            val pct = if (totalComponent > 0) ns * 100 / totalComponent else 0
            println("  %-25s %6d µs  (%2d%%)".format(name, ns / 1000, pct))
        }

        println("\nAI search total: ${"%.1f".format(totalSearch / 1_000_000_000.0)}s across $totalMoves moves ($numGames games)")
        println("Avg search time per move: ${"%.0f".format(totalSearch.toDouble() / totalMoves / 1_000_000)}ms")
    }

    /**
     * Profile where the AI spends its time during search.
     * Measures: board copy, move generation, state updates, evaluation (incl. BFS).
     */
    @Test
    fun profileSearchBottlenecks() {
        // Play to mid-game
        val game = Game.builder().players(2).build()
        val rng = Random(42)
        var steps = 0
        while (!game.isFinished() && steps < 80) {
            val valid = game.getValidActions()
            if (valid.isEmpty()) break
            game.apply(valid[rng.nextInt(valid.size)])
            steps++
        }
        val state = game.getState()
        println("Position: ${state.board.placedCount} tiles, step $steps")

        // Measure individual operations
        val iterations = 5000

        // 1. Board copy
        var t = System.nanoTime()
        repeat(iterations) { state.board.copy() }
        val boardCopyNs = (System.nanoTime() - t) / iterations
        println("Board copy:         ${boardCopyNs / 1000} µs")

        // 2. Full state copy
        t = System.nanoTime()
        repeat(iterations) { state.copy() }
        val stateCopyNs = (System.nanoTime() - t) / iterations
        println("State copy:         ${stateCopyNs / 1000} µs")

        // 3. Move generation (tile placements)
        val tileId = state.currentTileId!!
        t = System.nanoTime()
        repeat(iterations) { MoveGenerator.validTilePlacements(state, tileId) }
        val moveGenNs = (System.nanoTime() - t) / iterations
        val placements = MoveGenerator.validTilePlacements(state, tileId)
        println("Tile move gen:      ${moveGenNs / 1000} µs (${placements.size} moves)")

        // 4. Apply tile placement + meeple generation
        val placement = placements[0]
        t = System.nanoTime()
        repeat(iterations) {
            val after = StateUpdater.applyAction(state, placement)
            MoveGenerator.validMeeplePlacements(after)
        }
        val applyTileNs = (System.nanoTime() - t) / iterations
        println("Apply tile + mgen:  ${applyTileNs / 1000} µs")

        // 5. Apply tile only
        t = System.nanoTime()
        repeat(iterations) { StateUpdater.applyAction(state, placement) }
        val applyTileOnlyNs = (System.nanoTime() - t) / iterations
        println("Apply tile only:    ${applyTileOnlyNs / 1000} µs")

        // 6. Meeple move gen only
        val afterTile = StateUpdater.applyAction(state, placement)
        t = System.nanoTime()
        repeat(iterations) { MoveGenerator.validMeeplePlacements(afterTile) }
        val meepleGenNs = (System.nanoTime() - t) / iterations
        val meepleActions = MoveGenerator.validMeeplePlacements(afterTile)
        println("Meeple move gen:    ${meepleGenNs / 1000} µs (${meepleActions.size} moves)")

        // 7. Board evaluation
        t = System.nanoTime()
        repeat(iterations) { BoardEvaluator.evaluate(afterTile, 0) }
        val evalNs = (System.nanoTime() - t) / iterations
        println("Board evaluation:   ${evalNs / 1000} µs")

        // 8. Apply meeple placement (if available)
        if (meepleActions.isNotEmpty()) {
            val meepleAction = meepleActions[0]
            t = System.nanoTime()
            repeat(iterations) { StateUpdater.applyAction(afterTile, meepleAction) }
            val applyMeepleNs = (System.nanoTime() - t) / iterations
            println("Apply meeple:       ${applyMeepleNs / 1000} µs")
        }

        // 9. Full search node cost estimate (tile + eval best meeple)
        t = System.nanoTime()
        repeat(iterations) {
            val after = StateUpdater.applyAction(state, placement)
            val ma = MoveGenerator.validMeeplePlacements(after)
            for (a in ma) {
                val s = StateUpdater.applyAction(after, a)
                BoardEvaluator.evaluate(s, 0)
            }
        }
        val fullNodeNs = (System.nanoTime() - t) / iterations
        println("Full node (tile+all meeple evals): ${fullNodeNs / 1000} µs")

        println("\nBreakdown per search node:")
        println("  Board copy ~${boardCopyNs * 100 / fullNodeNs}%% of node cost")
        println("  Evaluation ~${evalNs * meepleActions.size.toLong() * 100 / fullNodeNs}%% of node cost")
    }

    /**
     * Compare agents with different differentialWeight settings.
     * differentialWeight=0.0 → maximize own score (greedy)
     * differentialWeight=1.0 → maximize score difference (competitive)
     *
     * Each pair plays multiple games, alternating who goes first.
     *
     * Disabled by default (slow). Run with: RUN_SLOW_BENCHMARKS=true ./gradlew :utils:test --tests "*.BenchmarkTest.evaluatorComparisonBenchmark"
     */
    @Test
    fun evaluatorComparisonBenchmark() {
        if (System.getenv("RUN_SLOW_BENCHMARKS") != "true" && System.getProperty("runSlowBenchmarks") != "true") {
            println("Skipped: set RUN_SLOW_BENCHMARKS=true or -DrunSlowBenchmarks=true to run")
            return
        }
        val weights = listOf(0.0, 0.25, 0.5, 0.75, 1.0)
        val gamesPerMatchup = 10 // per side, so 20 total per pair
        val timeBudget = 100L

        data class MatchResult(
            val w1: Double, val w2: Double,
            val wins1: Int, val wins2: Int, val draws: Int,
            val avgScore1: Double, val avgScore2: Double,
        )

        val results = mutableListOf<MatchResult>()

        for (i in weights.indices) {
            for (j in i + 1 until weights.size) {
                val w1 = weights[i]
                val w2 = weights[j]
                var wins1 = 0; var wins2 = 0; var draws = 0
                var totalScore1 = 0L; var totalScore2 = 0L
                var gamesPlayed = 0

                for (side in 0..1) {
                    // side 0: w1 plays as player 0, side 1: w1 plays as player 1
                    val p1Weight = if (side == 0) w1 else w2
                    val p2Weight = if (side == 0) w2 else w1

                    for (g in 0 until gamesPerMatchup) {
                        val game = Game.builder().players(2).build()
                        val ai0 = TreeSearchAI(
                            player = 0, timeLimitMs = timeBudget,
                            evaluate = BoardEvaluator.withDifferentialWeight(p1Weight),
                        )
                        val ai1 = TreeSearchAI(
                            player = 1, timeLimitMs = timeBudget,
                            evaluate = BoardEvaluator.withDifferentialWeight(p2Weight),
                        )

                        while (!game.isFinished()) {
                            val state = game.getState()
                            val ai = if (state.currentPlayer == 0) ai0 else ai1
                            val action = ai.chooseAction(state)
                            game.apply(action)
                        }

                        val scores = game.getFinalScores()
                        val s1 = if (side == 0) scores[0] else scores[1]
                        val s2 = if (side == 0) scores[1] else scores[0]
                        totalScore1 += s1; totalScore2 += s2
                        when {
                            s1 > s2 -> wins1++
                            s2 > s1 -> wins2++
                            else -> draws++
                        }
                        gamesPlayed++
                    }
                }

                val result = MatchResult(
                    w1, w2, wins1, wins2, draws,
                    totalScore1.toDouble() / gamesPlayed,
                    totalScore2.toDouble() / gamesPlayed,
                )
                results.add(result)
                println("d=%.2f vs d=%.2f: %d-%d-%d (avg %.1f vs %.1f)".format(
                    w1, w2, wins1, wins2, draws, result.avgScore1, result.avgScore2,
                ))
            }
        }

        // Summary table
        println("\n=== Evaluator Comparison (${gamesPerMatchup * 2} games per matchup, ${timeBudget}ms/move) ===")
        println("%-12s %-12s %6s %6s %6s %10s %10s".format(
            "Agent A", "Agent B", "WinsA", "WinsB", "Draws", "AvgScoreA", "AvgScoreB"))
        for (r in results) {
            println("d=%-10.2f d=%-10.2f %6d %6d %6d %10.1f %10.1f".format(
                r.w1, r.w2, r.wins1, r.wins2, r.draws, r.avgScore1, r.avgScore2))
        }

        File("/tmp/carcassonne-evaluator-comparison.csv").printWriter().use { out ->
            out.println("weight_a,weight_b,wins_a,wins_b,draws,avg_score_a,avg_score_b")
            for (r in results) {
                out.println("%.2f,%.2f,%d,%d,%d,%.1f,%.1f".format(
                    r.w1, r.w2, r.wins1, r.wins2, r.draws, r.avgScore1, r.avgScore2))
            }
        }
    }
}
