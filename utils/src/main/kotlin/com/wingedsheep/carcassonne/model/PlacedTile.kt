package com.wingedsheep.carcassonne.model

import kotlinx.serialization.Serializable

/** Lightweight reference to a tile on the board: tile ID + rotation. */
@Serializable
data class PlacedTile(
    val tileId: Int,
    val rotation: Int
)
