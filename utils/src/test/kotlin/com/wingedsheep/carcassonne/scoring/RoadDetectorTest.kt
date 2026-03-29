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

class RoadDetectorTest {

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
    fun `single straight road tile is unfinished`() {
        val board = BoardMap()
        val id = findTileId("straight_road")
        board.place(Coordinate(0, 0), PlacedTile(id, 0))

        val road = RoadDetector.findRoad(board, CoordinateWithSide(Coordinate(0, 0), Side.TOP))
        assertFalse(road.finished)
    }

    @Test
    fun `road points for unfinished road`() {
        val road = Road(
            positions = setOf(
                CoordinateWithSide(Coordinate(0, 0), Side.TOP),
                CoordinateWithSide(Coordinate(0, 0), Side.BOTTOM),
            ),
            finished = false,
            tileCoordinates = setOf(Coordinate(0, 0)),
        )
        assertEquals(1, PointsCalculator.roadPoints(road))
    }

    @Test
    fun `road points for finished road`() {
        val road = Road(
            positions = setOf(
                CoordinateWithSide(Coordinate(0, 0), Side.TOP),
            ),
            finished = true,
            tileCoordinates = setOf(Coordinate(0, 0), Coordinate(1, 0)),
        )
        assertEquals(2, PointsCalculator.roadPoints(road))
    }
}
