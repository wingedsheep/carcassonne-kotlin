package com.wingedsheep.carcassonne.tile

import com.wingedsheep.carcassonne.model.RotatedTile
import com.wingedsheep.carcassonne.model.TileDefinition

object TileRegistry {
    private val definitions = mutableListOf<TileDefinition>()
    private val rotations = mutableListOf<Array<RotatedTile>>()

    val size: Int get() = definitions.size

    fun register(definition: TileDefinition): Int {
        val id = definitions.size
        definitions.add(definition)
        rotations.add(RotatedTile.allRotations(definition))
        return id
    }

    fun get(tileId: Int): TileDefinition = definitions[tileId]

    fun getRotated(tileId: Int, rotation: Int): RotatedTile = rotations[tileId][rotation % 4]

    fun clear() {
        definitions.clear()
        rotations.clear()
    }
}
