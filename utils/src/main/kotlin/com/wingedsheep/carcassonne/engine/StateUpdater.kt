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
        newState.board.place(action.coordinate, PlacedTile(action.tileId, action.rotation))
        return newState.copy(
            phase = GamePhase.MEEPLE_PLACEMENT,
            lastPlacedCoordinate = action.coordinate,
        )
    }

    private fun applyMeeplePlacement(state: GameState, action: PlaceMeeple): GameState {
        val newState = state.copy()

        if (action.remove) {
            // Remove abbot and score
            val meeple = newState.placedMeeples[newState.currentPlayer].find {
                it.type == MeepleType.ABBOT && it.coordinate == action.coordinate
            }
            if (meeple != null) {
                newState.placedMeeples[newState.currentPlayer].remove(meeple)
                newState.meeples[newState.currentPlayer].returnMeeple(MeepleType.ABBOT)
                val points = PointsCalculator.monasteryPoints(newState.board, action.coordinate)
                newState.scores[newState.currentPlayer] += points
            }
        } else {
            // Place meeple
            newState.meeples[newState.currentPlayer].take(action.meepleType)
            newState.placedMeeples[newState.currentPlayer].add(
                PlacedMeeple(
                    type = action.meepleType,
                    coordinate = action.coordinate,
                    side = action.side,
                    farmerSide = action.farmerSide
                )
            )
        }

        return finishTurn(newState)
    }

    private fun applyPass(state: GameState): GameState {
        return when (state.phase) {
            GamePhase.TILE_PLACEMENT -> {
                // No valid tile placement - skip to next tile/player
                advanceToNextPlayer(state.copy())
            }
            GamePhase.MEEPLE_PLACEMENT -> {
                finishTurn(state.copy())
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

        return state.copy(
            currentPlayer = nextPlayer,
            phase = GamePhase.TILE_PLACEMENT,
            deckIndex = nextDeckIndex,
            lastPlacedCoordinate = null,
        )
    }
}
