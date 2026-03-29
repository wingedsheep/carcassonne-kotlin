package com.wingedsheep.carcassonne.model

data class City(
    val positions: Set<CoordinateWithSide>,
    val finished: Boolean,
    val tileCoordinates: Set<Coordinate>,
    val shieldCount: Int = 0,
    val hasCathedral: Boolean = false,
)

data class Road(
    val positions: Set<CoordinateWithSide>,
    val finished: Boolean,
    val tileCoordinates: Set<Coordinate>,
    val hasInn: Boolean = false,
)

data class Farm(
    val connections: Set<CoordinateWithFarmerSide>,
    val farmerConnectionsWithCoordinate: List<Pair<Coordinate, FarmerConnection>>,
)
