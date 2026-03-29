package com.wingedsheep.carcassonne.model

import kotlinx.serialization.Serializable

/** A road or river segment connecting two sides of a tile. CENTER means the segment terminates at the tile center (junction). */
@Serializable
data class Connection(val a: Side, val b: Side) {
    fun rotatedCW(times: Int): Connection = Connection(a.rotatedCW(times), b.rotatedCW(times))

    fun sides(): Set<Side> = setOf(a, b)
}
