package com.wingedsheep.carcassonne.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.assertFalse

class EdgeProfileTest {

    @Test
    fun `terrain retrieval matches construction`() {
        val profile = EdgeProfile.of(
            top = TerrainType.CITY,
            right = TerrainType.ROAD,
            bottom = TerrainType.FIELD,
            left = TerrainType.ROAD
        )
        assertEquals(TerrainType.CITY, profile.terrain(Side.TOP))
        assertEquals(TerrainType.ROAD, profile.terrain(Side.RIGHT))
        assertEquals(TerrainType.FIELD, profile.terrain(Side.BOTTOM))
        assertEquals(TerrainType.ROAD, profile.terrain(Side.LEFT))
    }

    @Test
    fun `rotation clockwise works correctly`() {
        val profile = EdgeProfile.of(
            top = TerrainType.CITY,
            right = TerrainType.ROAD,
            bottom = TerrainType.FIELD,
            left = TerrainType.FIELD
        )
        val rotated = profile.rotatedCW(1)
        // After CW rotation: TOP gets LEFT's value, RIGHT gets TOP's value, etc.
        assertEquals(TerrainType.FIELD, rotated.terrain(Side.TOP))  // was LEFT
        assertEquals(TerrainType.CITY, rotated.terrain(Side.RIGHT))  // was TOP
        assertEquals(TerrainType.ROAD, rotated.terrain(Side.BOTTOM))  // was RIGHT
        assertEquals(TerrainType.FIELD, rotated.terrain(Side.LEFT))  // was BOTTOM
    }

    @Test
    fun `four rotations returns to original`() {
        val profile = EdgeProfile.of(
            top = TerrainType.CITY,
            right = TerrainType.ROAD,
            bottom = TerrainType.FIELD,
            left = TerrainType.RIVER
        )
        assertEquals(profile, profile.rotatedCW(4))
    }

    @Test
    fun `matchesSide detects matching edges`() {
        val cityTop = EdgeProfile.of(TerrainType.CITY, TerrainType.FIELD, TerrainType.FIELD, TerrainType.FIELD)
        val cityBottom = EdgeProfile.of(TerrainType.FIELD, TerrainType.FIELD, TerrainType.CITY, TerrainType.FIELD)

        // cityTop's TOP should match cityBottom's BOTTOM (opposite of TOP)
        assertTrue(cityTop.matchesSide(cityBottom, Side.TOP))
    }

    @Test
    fun `matchesSide detects mismatching edges`() {
        val cityTop = EdgeProfile.of(TerrainType.CITY, TerrainType.FIELD, TerrainType.FIELD, TerrainType.FIELD)
        val roadBottom = EdgeProfile.of(TerrainType.FIELD, TerrainType.FIELD, TerrainType.ROAD, TerrainType.FIELD)

        assertFalse(cityTop.matchesSide(roadBottom, Side.TOP))
    }
}
