package com.wingedsheep.carcassonne.model

import kotlin.test.Test
import kotlin.test.assertEquals

class SideTest {

    @Test
    fun `opposite returns correct side`() {
        assertEquals(Side.BOTTOM, Side.TOP.opposite())
        assertEquals(Side.TOP, Side.BOTTOM.opposite())
        assertEquals(Side.LEFT, Side.RIGHT.opposite())
        assertEquals(Side.RIGHT, Side.LEFT.opposite())
    }

    @Test
    fun `rotatedCW single rotation`() {
        assertEquals(Side.RIGHT, Side.TOP.rotatedCW(1))
        assertEquals(Side.BOTTOM, Side.RIGHT.rotatedCW(1))
        assertEquals(Side.LEFT, Side.BOTTOM.rotatedCW(1))
        assertEquals(Side.TOP, Side.LEFT.rotatedCW(1))
    }

    @Test
    fun `rotatedCW four returns to original`() {
        for (side in Side.CARDINAL) {
            assertEquals(side, side.rotatedCW(4))
        }
    }

    @Test
    fun `diagonal rotation`() {
        assertEquals(Side.TOP_RIGHT, Side.TOP_LEFT.rotatedCW(1))
        assertEquals(Side.BOTTOM_RIGHT, Side.TOP_RIGHT.rotatedCW(1))
        assertEquals(Side.BOTTOM_LEFT, Side.BOTTOM_RIGHT.rotatedCW(1))
        assertEquals(Side.TOP_LEFT, Side.BOTTOM_LEFT.rotatedCW(1))
    }
}
