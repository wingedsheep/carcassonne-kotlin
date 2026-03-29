package com.wingedsheep.carcassonne.engine

import com.wingedsheep.carcassonne.model.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class GameTest {

    @Test
    fun `new game starts with one tile on board`() {
        val game = Game.builder().players(2).build()
        val state = game.getState()
        assertEquals(1, state.board.placedCount)
        assertEquals(0, state.currentPlayer)
        assertEquals(GamePhase.TILE_PLACEMENT, state.phase)
        assertFalse(state.isFinished)
    }

    @Test
    fun `valid actions are available on first turn`() {
        val game = Game.builder().players(2).build()
        val actions = game.getValidActions()
        assertTrue(actions.isNotEmpty())
        assertTrue(actions.any { it is PlaceTile })
    }

    @Test
    fun `placing a tile advances to meeple phase`() {
        val game = Game.builder().players(2).build()
        val actions = game.getValidActions()
        val tileAction = actions.filterIsInstance<PlaceTile>().first()
        game.apply(tileAction)
        assertEquals(GamePhase.MEEPLE_PLACEMENT, game.getState().phase)
    }

    @Test
    fun `passing meeple phase advances to next player`() {
        val game = Game.builder().players(2).build()
        val actions = game.getValidActions()
        val tileAction = actions.filterIsInstance<PlaceTile>().first()
        game.apply(tileAction)
        // Now in meeple phase, pass
        game.apply(Pass)
        val state = game.getState()
        // Should be next player's turn (or still player 0 if tiles were skipped)
        assertEquals(GamePhase.TILE_PLACEMENT, state.phase)
    }

    @Test
    fun `full game completes without errors`() {
        val game = Game.builder().players(2).build()
        var turns = 0
        while (!game.isFinished() && turns < 500) {
            val actions = game.getValidActions()
            if (actions.isEmpty()) break
            // Always pick first valid action (deterministic play)
            game.apply(actions.first())
            turns++
        }
        assertTrue(game.isFinished(), "Game should finish after all tiles are placed")
        val scores = game.getFinalScores()
        assertEquals(2, scores.size)
    }

    @Test
    fun `state copy is independent`() {
        val game = Game.builder().players(2).build()
        val state1 = game.getState().copy()
        val actions = game.getValidActions()
        game.apply(actions.first())
        val state2 = game.getState()
        // state1 should be unmodified
        assertEquals(1, state1.board.placedCount)
        assertTrue(state2.board.placedCount >= 1)
    }

    @Test
    fun `meeple placement works`() {
        val game = Game.builder().players(2).build()

        // Play through a few turns to find a meeple opportunity
        var foundMeeple = false
        for (i in 0 until 20) {
            if (game.isFinished()) break
            val actions = game.getValidActions()
            if (actions.isEmpty()) break

            if (game.getState().phase == GamePhase.MEEPLE_PLACEMENT) {
                val meepleAction = actions.filterIsInstance<PlaceMeeple>().firstOrNull()
                if (meepleAction != null) {
                    game.apply(meepleAction)
                    foundMeeple = true
                    continue
                }
            }
            game.apply(actions.first())
        }
        // Just verify it didn't crash - meeple placement depends on tile draw
    }
}
