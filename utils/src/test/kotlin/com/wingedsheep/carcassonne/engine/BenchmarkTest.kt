package com.wingedsheep.carcassonne.engine

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
}
