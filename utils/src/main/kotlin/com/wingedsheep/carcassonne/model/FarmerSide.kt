package com.wingedsheep.carcassonne.model

import kotlinx.serialization.Serializable

/**
 * 8 positions around a tile's edges for farm zone connectivity.
 *
 * Naming: first letter = vertical half (T=top, B=bottom),
 *         second letter = horizontal half (L=left, R=right),
 *         third letter = which cardinal side this position touches.
 *
 *     TLT  TRT
 *  TLL +------+ TRR
 *      |      |
 *  BLL +------+ BRR
 *     BLB  BRB
 */
@Serializable
enum class FarmerSide {
    TLL, TLT, TRT, TRR,
    BRR, BRB, BLB, BLL;

    fun rotatedCW(times: Int = 1): FarmerSide {
        var result = this
        repeat(times % 4) {
            result = when (result) {
                TLL -> TRT
                TLT -> TRR
                TRT -> BRR
                TRR -> BRB
                BRR -> BLB
                BRB -> BLL
                BLB -> TLL
                BLL -> TLT
            }
        }
        return result
    }

    fun opposite(): FarmerSide = when (this) {
        TLL -> TRR
        TLT -> BLB
        TRT -> BRB  // Note: TRT is actually on the neighbor's BRB when crossing top edge? No...
        TRR -> TLL
        BRR -> BLL
        BRB -> TRT
        BLB -> TLT
        BLL -> BRR
    }

    /** Which cardinal side this farmer position is adjacent to. */
    fun adjacentSide(): Side = when (this) {
        TLL -> Side.LEFT
        TLT -> Side.TOP
        TRT -> Side.TOP
        TRR -> Side.RIGHT
        BRR -> Side.RIGHT
        BRB -> Side.BOTTOM
        BLB -> Side.BOTTOM
        BLL -> Side.LEFT
    }
}
