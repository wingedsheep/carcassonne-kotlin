package com.wingedsheep.carcassonne.engine

import com.wingedsheep.carcassonne.model.*
import com.wingedsheep.carcassonne.scoring.PointsCalculator
import com.wingedsheep.carcassonne.tile.BaseDeck
import com.wingedsheep.carcassonne.tile.TileCount
import com.wingedsheep.carcassonne.tile.TileRegistry

class Game internal constructor(
    private var state: GameState
) {
    fun getState(): GameState = state

    fun isFinished(): Boolean = state.isFinished

    fun getValidActions(): List<Action> {
        if (state.isFinished) return emptyList()
        return when (state.phase) {
            GamePhase.TILE_PLACEMENT -> {
                val tileId = state.currentTileId ?: return emptyList()
                val placements = MoveGenerator.validTilePlacements(state, tileId)
                placements.ifEmpty { listOf(Pass) }
            }
            GamePhase.MEEPLE_PLACEMENT -> {
                MoveGenerator.validMeeplePlacements(state)
            }
        }
    }

    fun apply(action: Action): Game {
        state = StateUpdater.applyAction(state, action)
        // Skip tiles that have no valid placement
        while (!state.isFinished && state.phase == GamePhase.TILE_PLACEMENT) {
            val tileId = state.currentTileId ?: break
            val placements = MoveGenerator.validTilePlacements(state, tileId)
            if (placements.isNotEmpty()) break
            // No valid placement for this tile, advance
            state = StateUpdater.applyAction(state, Pass)
        }
        return this
    }

    fun getFinalScores(): IntArray {
        if (!state.isFinished) error("Game is not finished")
        val finalState = state.copy()
        PointsCalculator.scoreFinalState(finalState)
        return finalState.scores.copyOf()
    }

    companion object {
        fun builder() = GameBuilder()
    }
}

class GameBuilder {
    private var playerCount = 2
    private val tileCounts = mutableListOf<TileCount>()
    private val rules = mutableSetOf<SupplementaryRule>()
    private var bigMeeples = false

    fun players(count: Int) = apply { playerCount = count }
    fun withBaseDeck() = apply { tileCounts.addAll(BaseDeck.tiles()) }
    fun withTiles(tiles: List<TileCount>) = apply { tileCounts.addAll(tiles) }
    fun withRule(rule: SupplementaryRule) = apply { rules.add(rule) }
    fun withBigMeeples() = apply { bigMeeples = true }

    fun build(): Game {
        if (tileCounts.isEmpty()) withBaseDeck()

        TileRegistry.clear()

        // Register all tile definitions and build deck
        val deckEntries = mutableListOf<Int>()
        for (tc in tileCounts) {
            val id = TileRegistry.register(tc.definition)
            repeat(tc.count) { deckEntries.add(id) }
        }

        // Shuffle deck
        deckEntries.shuffle()

        // Place starting tile
        val startingTileDef = BaseDeck.startingTile()
        val startingId = (0 until TileRegistry.size).first {
            TileRegistry.get(it) == startingTileDef
        }
        deckEntries.remove(startingId) // Remove one copy of the starting tile

        val board = BoardMap()
        val startCoord = Coordinate(0, 0)
        board.place(startCoord, PlacedTile(startingId, 0))

        val state = GameState(
            board = board,
            deck = deckEntries,
            deckIndex = 0,
            playerCount = playerCount,
            currentPlayer = 0,
            phase = GamePhase.TILE_PLACEMENT,
            scores = IntArray(playerCount),
            meeples = Array(playerCount) {
                MeeplePool(
                    normal = 7,
                    big = if (bigMeeples) 1 else 0,
                    abbot = if (SupplementaryRule.ABBOTS in rules) 1 else 0
                )
            },
            placedMeeples = Array(playerCount) { mutableListOf() },
            supplementaryRules = rules,
        )

        return Game(state)
    }
}
