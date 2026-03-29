package com.wingedsheep.carcassonne.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
sealed interface Action

@Serializable
@SerialName("place_tile")
data class PlaceTile(
    val tileId: Int,
    val rotation: Int,
    val coordinate: Coordinate
) : Action

@Serializable
@SerialName("place_meeple")
data class PlaceMeeple(
    val meepleType: MeepleType,
    val coordinate: Coordinate,
    val side: Side? = null,
    val farmerSide: FarmerSide? = null,
    val remove: Boolean = false
) : Action

@Serializable
@SerialName("pass")
data object Pass : Action
