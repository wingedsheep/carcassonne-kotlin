package com.wingedsheep.carcassonne.engine

import kotlin.test.Test
import kotlin.random.Random
import java.io.File

class BenchmarkTest {

    private fun playRandomGame(seed: Int): Int {
        val game = Game.builder().players(2).build()
        val rng = Random(seed)
        var actions = 0
        while (!game.isFinished()) {
            val valid = game.getValidActions()
            if (valid.isEmpty()) break
            game.apply(valid[rng.nextInt(valid.size)])
            actions++
        }
        return actions
    }

    @Test
    fun benchmark() {
        // Warmup
        repeat(50) { playRandomGame(it) }

        val numGames = 1000
        val start = System.nanoTime()
        var totalActions = 0
        for (i in 0 until numGames) {
            totalActions += playRandomGame(i + 1000)
        }
        val elapsed = System.nanoTime() - start
        val ms = elapsed / 1_000_000.0
        val gamesPerSec = numGames / (ms / 1000.0)
        val actionsPerGame = totalActions.toDouble() / numGames

        val report = buildString {
            appendLine("=== Carcassonne Engine Benchmark ===")
            appendLine("Games played:      $numGames")
            appendLine("Total time:        ${"%.1f".format(ms)} ms")
            appendLine("Games/second:      ${"%.0f".format(gamesPerSec)}")
            appendLine("Avg actions/game:  ${"%.1f".format(actionsPerGame)}")
            appendLine("Avg time/game:     ${"%.2f".format(ms / numGames)} ms")
        }
        File("/tmp/carcassonne-benchmark.txt").writeText(report)
        println(report)
    }
}
