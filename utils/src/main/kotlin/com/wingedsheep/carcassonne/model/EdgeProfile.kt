package com.wingedsheep.carcassonne.model

import kotlinx.serialization.Serializable

/**
 * Packs the terrain type of all 4 edges into a single Int.
 * Bits 0-1: TOP, 2-3: RIGHT, 4-5: BOTTOM, 6-7: LEFT.
 *
 * Enables O(1) tile-fitting checks via bitwise comparison.
 */
@JvmInline
@Serializable
value class EdgeProfile(val packed: Int) {

    fun terrain(side: Side): TerrainType {
        val shift = sideIndex(side) * TerrainType.BITS
        return TerrainType.entries[(packed shr shift) and TerrainType.MASK]
    }

    fun rotatedCW(times: Int = 1): EdgeProfile {
        var p = packed
        repeat(times % 4) {
            val top = p and 0x3
            val right = (p shr 2) and 0x3
            val bottom = (p shr 4) and 0x3
            val left = (p shr 6) and 0x3
            p = (left) or (top shl 2) or (right shl 4) or (bottom shl 6)
        }
        return EdgeProfile(p)
    }

    fun matchesSide(other: EdgeProfile, mySide: Side): Boolean {
        return terrain(mySide) == other.terrain(mySide.opposite())
    }

    companion object {
        fun of(top: TerrainType, right: TerrainType, bottom: TerrainType, left: TerrainType): EdgeProfile {
            return EdgeProfile(
                (top.ordinal) or
                (right.ordinal shl 2) or
                (bottom.ordinal shl 4) or
                (left.ordinal shl 6)
            )
        }

        private fun sideIndex(side: Side): Int = when (side) {
            Side.TOP -> 0
            Side.RIGHT -> 1
            Side.BOTTOM -> 2
            Side.LEFT -> 3
            else -> error("EdgeProfile only supports cardinal sides, got $side")
        }
    }
}
