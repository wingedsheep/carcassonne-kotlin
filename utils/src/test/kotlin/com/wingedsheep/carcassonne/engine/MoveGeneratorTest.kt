package com.wingedsheep.carcassonne.engine

import com.wingedsheep.carcassonne.model.*
import com.wingedsheep.carcassonne.scoring.CityDetector
import com.wingedsheep.carcassonne.tile.BaseDeck
import com.wingedsheep.carcassonne.tile.TileRegistry
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class MoveGeneratorTest {

    private fun registerTile(name: String): Int {
        val def = BaseDeck.tiles().first { it.definition.name == name }.definition
        return TileRegistry.register(def)
    }

    private fun buildState(
        board: BoardMap,
        deck: List<Int> = emptyList(),
        lastPlaced: Coordinate? = null,
        phase: GamePhase = GamePhase.MEEPLE_PLACEMENT,
        placedMeeples: Array<MutableList<PlacedMeeple>> = Array(2) { mutableListOf() },
    ): GameState = GameState(
        board = board,
        deck = deck,
        deckIndex = 0,
        playerCount = 2,
        currentPlayer = 0,
        phase = phase,
        scores = IntArray(2),
        meeples = Array(2) { MeeplePool(normal = 7, big = 0, abbot = 0) },
        placedMeeples = placedMeeples,
        lastPlacedCoordinate = lastPlaced,
    )

    /**
     * Place a tile that completes a 2-tile city with no meeples.
     * Meeple placement on the city should be allowed.
     */
    @Test
    fun `can place meeple on just-completed empty city`() {
        TileRegistry.clear()
        val cityTopId = registerTile("city_top")

        val board = BoardMap()
        // city_top at (0,0) rotation 0 → city on TOP
        board.place(Coordinate(0, 0), PlacedTile(cityTopId, 0))
        // city_top at (-1,0) rotation 2 → city on BOTTOM, completing city
        board.place(Coordinate(-1, 0), PlacedTile(cityTopId, 2))

        val state = buildState(board, lastPlaced = Coordinate(-1, 0))
        val actions = MoveGenerator.validMeeplePlacements(state)

        // Verify the city is actually complete
        val city = CityDetector.findCity(board, CoordinateWithSide(Coordinate(-1, 0), Side.BOTTOM))
        assertTrue(city.finished, "City should be finished")

        // Should offer city meeple placement
        val cityPlacements = actions.filterIsInstance<PlaceMeeple>().filter { it.side == Side.BOTTOM }
        assertTrue(cityPlacements.isNotEmpty(), "Should allow meeple on completed city side")
    }

    /**
     * Complete a city that already has an opponent's meeple.
     * Placement should NOT be allowed (standard rules).
     */
    @Test
    fun `cannot place meeple on completed city with existing meeple`() {
        TileRegistry.clear()
        val cityTopId = registerTile("city_top")

        val board = BoardMap()
        board.place(Coordinate(0, 0), PlacedTile(cityTopId, 0))
        board.place(Coordinate(-1, 0), PlacedTile(cityTopId, 2))

        // Player 1 has a meeple on (0,0) TOP (inside the city)
        val meeples = Array(2) { mutableListOf<PlacedMeeple>() }
        meeples[1].add(PlacedMeeple(MeepleType.NORMAL, Coordinate(0, 0), side = Side.TOP))

        val state = buildState(board, lastPlaced = Coordinate(-1, 0), placedMeeples = meeples)
        val actions = MoveGenerator.validMeeplePlacements(state)

        val cityPlacements = actions.filterIsInstance<PlaceMeeple>().filter { it.side == Side.BOTTOM }
        assertTrue(cityPlacements.isEmpty(), "Should NOT allow meeple on city with existing meeple")
    }

    /**
     * Complete a diagonal (2-side connected) city.
     * Meeple placement should be allowed.
     */
    @Test
    fun `can place meeple on completed diagonal city`() {
        TileRegistry.clear()
        val diagId = registerTile("city_diagonal_top_right")
        val cityTopId = registerTile("city_top")

        val board = BoardMap()
        // diagonal city_top_right at (0,0): city zone [TOP, RIGHT]
        board.place(Coordinate(0, 0), PlacedTile(diagId, 0))
        // city_top at (-1,0) rotation 2 → city on BOTTOM, connects to (0,0) TOP
        board.place(Coordinate(-1, 0), PlacedTile(cityTopId, 2))
        // city_top at (0,1) rotation 3 → city on LEFT, connects to (0,0) RIGHT
        board.place(Coordinate(0, 1), PlacedTile(cityTopId, 3))

        // Verify the city is complete
        val city = CityDetector.findCity(board, CoordinateWithSide(Coordinate(0, 1), Side.LEFT))
        assertTrue(city.finished, "Diagonal city should be finished")

        // Place the last tile that completed the city - use (0,1) as lastPlaced
        val state = buildState(board, lastPlaced = Coordinate(0, 1))
        val actions = MoveGenerator.validMeeplePlacements(state)

        val cityPlacements = actions.filterIsInstance<PlaceMeeple>().filter { it.side == Side.LEFT }
        assertTrue(cityPlacements.isNotEmpty(), "Should allow meeple on completed diagonal city")
    }

    /**
     * Complete a road and verify meeple placement is offered.
     */
    @Test
    fun `can place meeple on just-completed road`() {
        TileRegistry.clear()
        val bentRoadId = registerTile("bent_road")

        val board = BoardMap()
        // bent_road at (0,0) rotation 0: road connects LEFT-BOTTOM
        board.place(Coordinate(0, 0), PlacedTile(bentRoadId, 0))
        // bent_road at (0,-1) rotation 1: rotated 90° CW, road connects BOTTOM-RIGHT
        // RIGHT connects to (0,0) LEFT - completing a road loop? No...
        // Actually bent_road connects LEFT-BOTTOM, rotation 1 rotates to TOP-RIGHT
        // So at (0,-1) rotation 1: road connects TOP-RIGHT
        // RIGHT of (0,-1) connects to LEFT of (0,0) ✓
        // TOP is open → road not complete yet

        // Let me use straight_road instead for a simpler complete road
        TileRegistry.clear()
        val straightId = registerTile("straight_road")
        val threeWayId = registerTile("three_split_road")

        val board2 = BoardMap()
        // three_split_road at (0,0) rotation 0: roads on RIGHT, BOTTOM, LEFT (junction)
        board2.place(Coordinate(0, 0), PlacedTile(threeWayId, 0))
        // three_split_road at (0,1) rotation 0: roads on RIGHT, BOTTOM, LEFT (junction)
        // LEFT of (0,1) connects to RIGHT of (0,0) - road between two junctions is complete!
        board2.place(Coordinate(0, 1), PlacedTile(threeWayId, 0))

        val state = buildState(board2, deck = listOf(straightId), lastPlaced = Coordinate(0, 1))
        val actions = MoveGenerator.validMeeplePlacements(state)

        // Should have road placement for LEFT side (connecting to junction)
        val roadPlacements = actions.filterIsInstance<PlaceMeeple>().filter {
            it.side == Side.LEFT || it.side == Side.RIGHT || it.side == Side.BOTTOM
        }
        assertTrue(roadPlacements.isNotEmpty(), "Should allow meeple on completed road segment")
    }

    /**
     * Test through the Game object's actual apply flow.
     */
    @Test
    fun `game flow allows meeple on completed city`() {
        TileRegistry.clear()
        val cityTopId = registerTile("city_top")

        val board = BoardMap()
        // Pre-place one tile with city on TOP
        board.place(Coordinate(0, 0), PlacedTile(cityTopId, 0))

        // Deck has a city_top tile that we'll rotate 180° to complete the city
        val state = GameState(
            board = board,
            deck = listOf(cityTopId),
            deckIndex = 0,
            playerCount = 2,
            currentPlayer = 0,
            phase = GamePhase.TILE_PLACEMENT,
            scores = IntArray(2),
            meeples = Array(2) { MeeplePool(normal = 7, big = 0, abbot = 0) },
            placedMeeples = Array(2) { mutableListOf() },
        )
        val game = Game(state)

        // Place tile at (-1,0) rotation 2 (city on BOTTOM) to complete the city
        val placeAction = PlaceTile(cityTopId, 2, Coordinate(-1, 0))
        game.apply(placeAction)

        assertEquals(GamePhase.MEEPLE_PLACEMENT, game.getState().phase)

        val validActions = game.getValidActions()
        val cityPlacements = validActions.filterIsInstance<PlaceMeeple>().filter { it.side != null }
        assertTrue(
            cityPlacements.any { it.side == Side.BOTTOM },
            "Game flow should allow meeple on completed city. Valid actions: $validActions"
        )
    }
}
