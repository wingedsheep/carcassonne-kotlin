package com.wingedsheep.carcassonne.model

import kotlinx.serialization.Serializable

/**
 * Describes one farm zone on a tile.
 *
 * @param farmerPositions Sides (using corner notation: TOP_LEFT etc.) where a farmer meeple can be placed for this zone.
 * @param tileConnections FarmerSide edge segments this farm zone touches (used for cross-tile connectivity).
 * @param citySides Cardinal sides of cities adjacent to this farm zone (used for end-game scoring).
 */
@Serializable
data class FarmerConnection(
    val farmerPositions: List<Side>,
    val tileConnections: List<FarmerSide>,
    val citySides: List<Side> = emptyList()
) {
    fun rotatedCW(times: Int): FarmerConnection = FarmerConnection(
        farmerPositions = farmerPositions.map { it.rotatedCW(times) },
        tileConnections = tileConnections.map { it.rotatedCW(times) },
        citySides = citySides.map { it.rotatedCW(times) }
    )
}
