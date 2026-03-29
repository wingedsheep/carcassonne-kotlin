package com.wingedsheep.carcassonne.engine

import com.wingedsheep.carcassonne.model.*
import kotlinx.serialization.Serializable

@Serializable
data class GameState(
    val board: BoardMap,
    val deck: List<Int>,
    val deckIndex: Int,
    val playerCount: Int,
    val currentPlayer: Int,
    val phase: GamePhase,
    val scores: IntArray,
    val meeples: Array<MeeplePool>,
    val placedMeeples: Array<MutableList<PlacedMeeple>>,
    val lastPlacedCoordinate: Coordinate? = null,
    val supplementaryRules: Set<SupplementaryRule> = emptySet(),
) {
    val currentTileId: Int? get() = if (deckIndex < deck.size) deck[deckIndex] else null
    val isFinished: Boolean get() = currentTileId == null && phase == GamePhase.TILE_PLACEMENT

    fun copy(): GameState = GameState(
        board = board.copy(),
        deck = deck, // immutable, shared
        deckIndex = deckIndex,
        playerCount = playerCount,
        currentPlayer = currentPlayer,
        phase = phase,
        scores = scores.copyOf(),
        meeples = Array(playerCount) { meeples[it].copy() },
        placedMeeples = Array(playerCount) { playerIdx ->
            placedMeeples[playerIdx].toMutableList()
        },
        lastPlacedCoordinate = lastPlacedCoordinate,
        supplementaryRules = supplementaryRules,
    )

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is GameState) return false
        return board == other.board &&
            deckIndex == other.deckIndex &&
            playerCount == other.playerCount &&
            currentPlayer == other.currentPlayer &&
            phase == other.phase &&
            scores.contentEquals(other.scores) &&
            meeples.contentEquals(other.meeples) &&
            lastPlacedCoordinate == other.lastPlacedCoordinate &&
            supplementaryRules == other.supplementaryRules &&
            placedMeeplesEqual(other)
    }

    private fun placedMeeplesEqual(other: GameState): Boolean {
        for (i in 0 until playerCount) {
            if (placedMeeples[i] != other.placedMeeples[i]) return false
        }
        return true
    }

    override fun hashCode(): Int {
        var result = board.hashCode()
        result = 31 * result + deckIndex
        result = 31 * result + playerCount
        result = 31 * result + currentPlayer
        result = 31 * result + phase.hashCode()
        result = 31 * result + scores.contentHashCode()
        result = 31 * result + meeples.contentHashCode()
        result = 31 * result + (lastPlacedCoordinate?.hashCode() ?: 0)
        result = 31 * result + supplementaryRules.hashCode()
        for (i in 0 until playerCount) {
            result = 31 * result + placedMeeples[i].hashCode()
        }
        return result
    }
}
