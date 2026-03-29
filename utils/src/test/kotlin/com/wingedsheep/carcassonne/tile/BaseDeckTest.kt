package com.wingedsheep.carcassonne.tile

import kotlin.test.Test
import kotlin.test.assertEquals

class BaseDeckTest {

    @Test
    fun `base deck has 72 tiles total`() {
        val total = BaseDeck.tiles().sumOf { it.count }
        assertEquals(72, total)
    }

    @Test
    fun `all tiles register without errors`() {
        TileRegistry.clear()
        for (tc in BaseDeck.tiles()) {
            TileRegistry.register(tc.definition)
        }
        // 32 unique tile types
        assertEquals(32, TileRegistry.size)
    }

    @Test
    fun `all tiles can be rotated`() {
        TileRegistry.clear()
        for (tc in BaseDeck.tiles()) {
            val id = TileRegistry.register(tc.definition)
            for (rot in 0..3) {
                val rotated = TileRegistry.getRotated(id, rot)
                // Just verify no exceptions
                rotated.terrainAt(com.wingedsheep.carcassonne.model.Side.TOP)
                rotated.terrainAt(com.wingedsheep.carcassonne.model.Side.RIGHT)
                rotated.terrainAt(com.wingedsheep.carcassonne.model.Side.BOTTOM)
                rotated.terrainAt(com.wingedsheep.carcassonne.model.Side.LEFT)
            }
        }
    }
}
