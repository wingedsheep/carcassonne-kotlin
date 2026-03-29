package com.wingedsheep.carcassonne.scoring

import com.wingedsheep.carcassonne.engine.BoardMap
import com.wingedsheep.carcassonne.model.*
import com.wingedsheep.carcassonne.tile.BaseDeck
import com.wingedsheep.carcassonne.tile.TileRegistry
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.test.assertEquals

class CityDetectorTest {

    @BeforeTest
    fun setup() {
        TileRegistry.clear()
        for (tc in BaseDeck.tiles()) {
            TileRegistry.register(tc.definition)
        }
    }

    private fun findTileId(name: String): Int =
        (0 until TileRegistry.size).first { TileRegistry.get(it).name == name }

    @Test
    fun `single tile city is unfinished`() {
        val board = BoardMap()
        val id = findTileId("city_top")
        board.place(Coordinate(0, 0), PlacedTile(id, 0))

        val city = CityDetector.findCity(board, CoordinateWithSide(Coordinate(0, 0), Side.TOP))
        assertFalse(city.finished)
        assertEquals(1, city.tileCoordinates.size)
    }

    @Test
    fun `two matching city tiles form a finished city`() {
        val board = BoardMap()
        // Place city_top at (1,0) - city on TOP
        val cityTopId = findTileId("city_top")
        board.place(Coordinate(1, 0), PlacedTile(cityTopId, 0))

        // Place city_top rotated 180 at (0,0) - city on BOTTOM (facing the first tile)
        board.place(Coordinate(0, 0), PlacedTile(cityTopId, 2)) // 180 rotation: city on BOTTOM

        val city = CityDetector.findCity(board, CoordinateWithSide(Coordinate(1, 0), Side.TOP))
        assertTrue(city.finished)
        assertEquals(2, city.tileCoordinates.size)
    }

    @Test
    fun `city points for finished 2-tile city`() {
        val city = City(
            positions = setOf(
                CoordinateWithSide(Coordinate(0, 0), Side.TOP),
                CoordinateWithSide(Coordinate(1, 0), Side.BOTTOM),
            ),
            finished = true,
            tileCoordinates = setOf(Coordinate(0, 0), Coordinate(1, 0)),
        )
        assertEquals(4, PointsCalculator.cityPoints(city)) // 2 tiles * 2 points
    }

    @Test
    fun `city points for unfinished city`() {
        val city = City(
            positions = setOf(
                CoordinateWithSide(Coordinate(0, 0), Side.TOP),
            ),
            finished = false,
            tileCoordinates = setOf(Coordinate(0, 0)),
        )
        assertEquals(1, PointsCalculator.cityPoints(city)) // 1 tile * 1 point
    }

    @Test
    fun `city with shield scores extra`() {
        val city = City(
            positions = setOf(
                CoordinateWithSide(Coordinate(0, 0), Side.TOP),
                CoordinateWithSide(Coordinate(1, 0), Side.BOTTOM),
            ),
            finished = true,
            tileCoordinates = setOf(Coordinate(0, 0), Coordinate(1, 0)),
            shieldCount = 1,
        )
        assertEquals(6, PointsCalculator.cityPoints(city)) // 2*2 + 1*2 = 6
    }
}
