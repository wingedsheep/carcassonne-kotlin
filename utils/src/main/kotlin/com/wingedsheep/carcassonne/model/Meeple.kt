package com.wingedsheep.carcassonne.model

import kotlinx.serialization.Serializable

@Serializable
data class PlacedMeeple(
    val type: MeepleType,
    val coordinate: Coordinate,
    val side: Side?,
    val farmerSide: FarmerSide? = null
)

@Serializable
data class MeeplePool(
    var normal: Int = 7,
    var big: Int = 0,
    var abbot: Int = 0,
) {
    fun available(type: MeepleType): Int = when (type) {
        MeepleType.NORMAL, MeepleType.FARMER -> normal
        MeepleType.BIG, MeepleType.BIG_FARMER -> big
        MeepleType.ABBOT -> abbot
    }

    fun take(type: MeepleType) {
        when (type) {
            MeepleType.NORMAL, MeepleType.FARMER -> normal--
            MeepleType.BIG, MeepleType.BIG_FARMER -> big--
            MeepleType.ABBOT -> abbot--
        }
    }

    fun returnMeeple(type: MeepleType) {
        when (type) {
            MeepleType.NORMAL, MeepleType.FARMER -> normal++
            MeepleType.BIG, MeepleType.BIG_FARMER -> big++
            MeepleType.ABBOT -> abbot++
        }
    }

    fun copy(): MeeplePool = MeeplePool(normal, big, abbot)
}
