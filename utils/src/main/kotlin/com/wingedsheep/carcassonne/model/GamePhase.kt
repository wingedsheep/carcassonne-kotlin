package com.wingedsheep.carcassonne.model

import kotlinx.serialization.Serializable

@Serializable
enum class GamePhase {
    TILE_PLACEMENT,
    MEEPLE_PLACEMENT
}
