package com.wingedsheep.carcassonne.model

import kotlinx.serialization.Serializable

@Serializable
enum class MeepleType {
    NORMAL, BIG, FARMER, BIG_FARMER, ABBOT;

    val weight: Int get() = when (this) {
        BIG, BIG_FARMER -> 2
        else -> 1
    }

    val isFarmer: Boolean get() = this == FARMER || this == BIG_FARMER
}
