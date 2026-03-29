package com.wingedsheep.carcassonne.engine

import com.wingedsheep.carcassonne.model.*
import com.wingedsheep.carcassonne.scoring.PointsCalculator
import com.wingedsheep.carcassonne.tile.TileRegistry

object StateUpdater {

    fun applyAction(state: GameState, action: Action): GameState {
        return when (action) {
            is PlaceTile -> applyTilePlacement(state, action)
            is PlaceMeeple -> applyMeeplePlacement(state, action)
            is Pass -> applyPass(state)
        }
    }

    private fun applyTilePlacement(state: GameState, action: PlaceTile): GameState {
        val newState = state.copy()
        val newBoard = newState.board
        newBoard.place(action.coordinate, PlacedTile(action.tileId, action.rotation))
        return GameState(
            board = newBoard,
            deck = state.deck,
            deckIndex = state.deckIndex,
            playerCount = state.playerCount,
            currentPlayer = state.currentPlayer,
            phase = GamePhase.MEEPLE_PLACEMENT,
            scores = newState.scores,
            meeples = newState.meeples,
            placedMeeples = newState.placedMeeples,
            lastPlacedCoordinate = action.coordinate,
            supplementaryRules = state.supplementaryRules,
        )
    }

    private fun applyMeeplePlacement(state: GameState, action: PlaceMeeple): GameState {
        val scores = state.scores.copyOf()
        val meeples = Array(state.playerCount) { state.meeples[it].copy() }
        val placedMeeples = Array(state.playerCount) { state.placedMeeples[it].toMutableList() }
        val player = state.currentPlayer

        if (action.remove) {
            // Remove abbot and score
            val meeple = placedMeeples[player].find {
                it.type == MeepleType.ABBOT && it.coordinate == action.coordinate
            }
            if (meeple != null) {
                placedMeeples[player].remove(meeple)
                meeples[player].returnMeeple(MeepleType.ABBOT)
                val points = PointsCalculator.monasteryPoints(state.board, action.coordinate)
                scores[player] += points
            }
        } else {
            // Place meeple
            meeples[player].take(action.meepleType)
            placedMeeples[player].add(
                PlacedMeeple(
                    type = action.meepleType,
                    coordinate = action.coordinate,
                    side = action.side,
                    farmerSide = action.farmerSide
                )
            )
        }

        val newState = GameState(
            board = state.board,
            deck = state.deck,
            deckIndex = state.deckIndex,
            playerCount = state.playerCount,
            currentPlayer = state.currentPlayer,
            phase = state.phase,
            scores = scores,
            meeples = meeples,
            placedMeeples = placedMeeples,
            lastPlacedCoordinate = state.lastPlacedCoordinate,
            supplementaryRules = state.supplementaryRules,
        )
        return finishTurn(newState)
    }

    private fun applyPass(state: GameState): GameState {
        val newState = state.copy()
        return when (state.phase) {
            GamePhase.TILE_PLACEMENT -> {
                // No valid tile placement - skip to next tile/player
                advanceToNextPlayer(newState)
            }
            GamePhase.MEEPLE_PLACEMENT -> {
                finishTurn(newState)
            }
        }
    }

    private fun finishTurn(state: GameState): GameState {
        // Score completed structures around the last placed tile
        val coord = state.lastPlacedCoordinate
        if (coord != null) {
            PointsCalculator.scoreCompletedStructures(state, coord)
        }

        return advanceToNextPlayer(state)
    }

    private fun advanceToNextPlayer(state: GameState): GameState {
        val nextPlayer = (state.currentPlayer + 1) % state.playerCount
        val nextDeckIndex = state.deckIndex + 1

        return GameState(
            board = state.board,
            deck = state.deck,
            deckIndex = nextDeckIndex,
            playerCount = state.playerCount,
            currentPlayer = nextPlayer,
            phase = GamePhase.TILE_PLACEMENT,
            scores = state.scores,
            meeples = state.meeples,
            placedMeeples = state.placedMeeples,
            lastPlacedCoordinate = null,
            supplementaryRules = state.supplementaryRules,
        )
    }
}
