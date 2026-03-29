package com.wingedsheep.carcassonne.model

import kotlinx.serialization.Serializable

/**
 * Immutable description of a tile type. Created once per unique tile in the game.
 * Rotation is handled by [RotatedTile], not by mutating this.
 */
@Serializable
data class TileDefinition(
    val name: String,
    val edges: EdgeProfile,
    val roads: List<Connection> = emptyList(),
    val rivers: List<Connection> = emptyList(),
    val cityZones: List<List<Side>> = emptyList(),
    val fieldSides: List<Side> = emptyList(),
    val farms: List<FarmerConnection> = emptyList(),
    val shield: Boolean = false,
    val monastery: Boolean = false,
    val flowers: Boolean = false,
    val inn: List<Side> = emptyList(),
    val cathedral: Boolean = false,
)
