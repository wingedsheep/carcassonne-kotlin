package com.wingedsheep.carcassonne.engine

import com.wingedsheep.carcassonne.model.*
import com.wingedsheep.carcassonne.tile.BaseDeck
import com.wingedsheep.carcassonne.tile.TileRegistry
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertTrue
import kotlin.test.assertFalse

class TileFitterTest {

    @BeforeTest
    fun setup() {
        TileRegistry.clear()
        for (tc in BaseDeck.tiles()) {
            TileRegistry.register(tc.definition)
        }
    }

    @Test
    fun `tile fits when edges match`() {
        val board = BoardMap()
        // Place starting tile (city_top_straight_road) at origin
        // edges: top=CITY, right=ROAD, bottom=FIELD, left=ROAD
        val startId = (0 until TileRegistry.size).first {
            TileRegistry.get(it).name == "city_top_straight_road"
        }
        board.place(Coordinate(0, 0), PlacedTile(startId, 0))

        // Try placing a tile with FIELD on top to the south (0,0 BOTTOM=FIELD, neighbor TOP must=FIELD)
        // chapel has all FIELD edges
        val chapelId = (0 until TileRegistry.size).first {
            TileRegistry.get(it).name == "chapel"
        }
        val chapelRotated = TileRegistry.getRotated(chapelId, 0)
        assertTrue(TileFitter.fits(board, chapelRotated, Coordinate(1, 0)))
    }

    @Test
    fun `tile does not fit when edges mismatch`() {
        val board = BoardMap()
        val startId = (0 until TileRegistry.size).first {
            TileRegistry.get(it).name == "city_top_straight_road"
        }
        board.place(Coordinate(0, 0), PlacedTile(startId, 0))

        // Try placing a city tile to the south - BOTTOM of start is FIELD, so neighbor TOP must be FIELD
        // full_city has CITY on all sides - should NOT fit
        val fullCityId = (0 until TileRegistry.size).first {
            TileRegistry.get(it).name == "full_city_with_shield"
        }
        val fullCity = TileRegistry.getRotated(fullCityId, 0)
        assertFalse(TileFitter.fits(board, fullCity, Coordinate(1, 0)))
    }

    @Test
    fun `tile requires at least one neighbor`() {
        val board = BoardMap()
        val startId = (0 until TileRegistry.size).first {
            TileRegistry.get(it).name == "city_top_straight_road"
        }
        board.place(Coordinate(0, 0), PlacedTile(startId, 0))

        val chapelId = (0 until TileRegistry.size).first {
            TileRegistry.get(it).name == "chapel"
        }
        val chapel = TileRegistry.getRotated(chapelId, 0)
        // Far away - no neighbors
        assertFalse(TileFitter.fits(board, chapel, Coordinate(5, 5)))
    }
}
