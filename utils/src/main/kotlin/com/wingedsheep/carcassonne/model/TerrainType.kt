package com.wingedsheep.carcassonne.model

import kotlinx.serialization.Serializable

@Serializable
enum class TerrainType {
    CITY, ROAD, FIELD, RIVER;

    companion object {
        const val BITS = 2
        const val MASK = 0x3
    }
}
