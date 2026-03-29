package com.wingedsheep.carcassonne.model

import kotlinx.serialization.Serializable

@Serializable
data class Coordinate(val row: Int, val col: Int) {
    fun neighbor(side: Side): Coordinate = when (side) {
        Side.TOP -> Coordinate(row - 1, col)
        Side.RIGHT -> Coordinate(row, col + 1)
        Side.BOTTOM -> Coordinate(row + 1, col)
        Side.LEFT -> Coordinate(row, col - 1)
        else -> error("neighbor() only supports cardinal sides, got $side")
    }

    fun neighbors(): List<Pair<Side, Coordinate>> = Side.CARDINAL.map { it to neighbor(it) }
}

@Serializable
data class CoordinateWithSide(val coordinate: Coordinate, val side: Side)

@Serializable
data class CoordinateWithFarmerSide(val coordinate: Coordinate, val farmerSide: FarmerSide)
