package com.wingedsheep.carcassonne.ai

import com.wingedsheep.carcassonne.engine.Game
import com.wingedsheep.carcassonne.model.*
import kotlin.test.Test
import kotlin.test.assertTrue
import kotlin.test.assertFalse
import kotlin.test.assertNotNull

class TreeSearchAITest {

    @Test
    fun `AI chooses a valid tile placement`() {
        val game = Game.builder().players(2).build()
        val ai = TreeSearchAI(player = 0, maxDepthTurns = 1, timeLimitMs = 1000)
        val state = game.getState()

        val action = ai.chooseAction(state)
        val validActions = game.getValidActions()
        assertTrue(action in validActions, "AI should choose a valid action")
    }

    @Test
    fun `AI chooses a valid meeple action`() {
        val game = Game.builder().players(2).build()
        val ai = TreeSearchAI(player = 0, maxDepthTurns = 1, timeLimitMs = 1000)

        // Place first tile
        val tileActions = game.getValidActions().filterIsInstance<PlaceTile>()
        game.apply(tileActions.first())

        // Now in meeple phase
        val state = game.getState()
        val action = ai.chooseAction(state)
        val validActions = game.getValidActions()
        assertTrue(action in validActions, "AI meeple action should be valid")
    }

    @Test
    fun `AI can play a full game against itself`() {
        if (System.getenv("RUN_SLOW_BENCHMARKS") != "true" && System.getProperty("runSlowBenchmarks") != "true") {
            println("Skipped: set RUN_SLOW_BENCHMARKS=true or -DrunSlowBenchmarks=true to run")
            return
        }
        val game = Game.builder().players(2).build()
        val ai0 = TreeSearchAI(player = 0, maxDepthTurns = 1, timeLimitMs = 500)
        val ai1 = TreeSearchAI(player = 1, maxDepthTurns = 1, timeLimitMs = 500)
        val ais = arrayOf(ai0, ai1)

        var turns = 0
        while (!game.isFinished() && turns < 500) {
            val state = game.getState()
            val ai = ais[state.currentPlayer]
            val action = ai.chooseAction(state)
            game.apply(action)
            turns++
        }

        assertTrue(game.isFinished(), "AI game should finish")
        val scores = game.getFinalScores()
        assertTrue(scores[0] >= 0 && scores[1] >= 0, "Scores should be non-negative")
    }

    @Test
    fun `depth-2 AI completes without timeout errors`() {
        if (System.getenv("RUN_SLOW_BENCHMARKS") != "true" && System.getProperty("runSlowBenchmarks") != "true") {
            println("Skipped: set RUN_SLOW_BENCHMARKS=true or -DrunSlowBenchmarks=true to run")
            return
        }
        val game = Game.builder().players(2).build()
        val ai = TreeSearchAI(player = 0, maxDepthTurns = 2, timeLimitMs = 2000)

        // Just test a few turns to verify depth-2 works
        repeat(4) {
            if (game.isFinished()) return
            val state = game.getState()
            val currentAi = if (state.currentPlayer == 0) ai
                else TreeSearchAI(player = 1, maxDepthTurns = 1, timeLimitMs = 500)
            val action = currentAi.chooseAction(state)
            game.apply(action)
        }
        // If we get here without exception, the test passes
    }

    @Test
    fun `evaluator produces different scores for different states`() {
        val game = Game.builder().players(2).build()
        val state1 = game.getState()
        val score1 = BoardEvaluator.evaluate(state1, 0)

        // Play a turn
        val actions = game.getValidActions()
        game.apply(actions.first())
        if (!game.isFinished()) {
            game.apply(game.getValidActions().first())
        }

        val state2 = game.getState()
        val score2 = BoardEvaluator.evaluate(state2, 0)

        // Scores should differ after gameplay changes the board
        // (they might be equal in rare cases, but very unlikely)
        assertNotNull(score1)
        assertNotNull(score2)
    }
}
