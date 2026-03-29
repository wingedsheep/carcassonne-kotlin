package com.wingedsheep.carcassonne.model

import kotlinx.serialization.Serializable

@Serializable
enum class Side {
    TOP, RIGHT, BOTTOM, LEFT,
    CENTER,
    TOP_LEFT, TOP_RIGHT, BOTTOM_LEFT, BOTTOM_RIGHT;

    fun opposite(): Side = when (this) {
        TOP -> BOTTOM
        RIGHT -> LEFT
        BOTTOM -> TOP
        LEFT -> RIGHT
        CENTER -> CENTER
        TOP_LEFT -> BOTTOM_RIGHT
        TOP_RIGHT -> BOTTOM_LEFT
        BOTTOM_LEFT -> TOP_RIGHT
        BOTTOM_RIGHT -> TOP_LEFT
    }

    fun rotatedCW(times: Int = 1): Side {
        var result = this
        repeat(times % 4) {
            result = when (result) {
                TOP -> RIGHT
                RIGHT -> BOTTOM
                BOTTOM -> LEFT
                LEFT -> TOP
                CENTER -> CENTER
                TOP_LEFT -> TOP_RIGHT
                TOP_RIGHT -> BOTTOM_RIGHT
                BOTTOM_RIGHT -> BOTTOM_LEFT
                BOTTOM_LEFT -> TOP_LEFT
            }
        }
        return result
    }

    companion object {
        val CARDINAL = listOf(TOP, RIGHT, BOTTOM, LEFT)
    }
}
