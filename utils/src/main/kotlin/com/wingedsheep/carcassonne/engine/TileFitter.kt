package com.wingedsheep.carcassonne.engine

import com.wingedsheep.carcassonne.model.Coordinate
import com.wingedsheep.carcassonne.model.RotatedTile
import com.wingedsheep.carcassonne.model.Side

object TileFitter {

    fun fits(board: BoardMap, tile: RotatedTile, coord: Coordinate): Boolean {
        var hasNeighbor = false
        for (side in Side.CARDINAL) {
            val neighborCoord = coord.neighbor(side)
            val neighborTile = board.getRotated(neighborCoord) ?: continue
            hasNeighbor = true
            if (tile.terrainAt(side) != neighborTile.terrainAt(side.opposite())) {
                return false
            }
        }
        return hasNeighbor
    }
}
