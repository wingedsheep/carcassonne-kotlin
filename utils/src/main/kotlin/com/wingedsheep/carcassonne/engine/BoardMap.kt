package com.wingedsheep.carcassonne.engine

import com.wingedsheep.carcassonne.model.Coordinate
import com.wingedsheep.carcassonne.model.PlacedTile
import com.wingedsheep.carcassonne.model.RotatedTile
import com.wingedsheep.carcassonne.model.Side
import com.wingedsheep.carcassonne.tile.TileRegistry
import kotlinx.serialization.Serializable

@Serializable
data class BoardMap(
    private val tiles: HashMap<Coordinate, PlacedTile> = HashMap(),
    private val frontier: HashSet<Coordinate> = HashSet()
) {
    val placedCount: Int get() = tiles.size

    operator fun get(coord: Coordinate): PlacedTile? = tiles[coord]

    fun getRotated(coord: Coordinate): RotatedTile? {
        val placed = tiles[coord] ?: return null
        return TileRegistry.getRotated(placed.tileId, placed.rotation)
    }

    fun contains(coord: Coordinate): Boolean = coord in tiles

    fun place(coord: Coordinate, tile: PlacedTile) {
        tiles[coord] = tile
        frontier.remove(coord)
        for (side in Side.CARDINAL) {
            val neighbor = coord.neighbor(side)
            if (neighbor !in tiles) {
                frontier.add(neighbor)
            }
        }
    }

    fun frontierCoordinates(): Set<Coordinate> = frontier

    fun allCoordinates(): Set<Coordinate> = tiles.keys

    fun copy(): BoardMap = BoardMap(HashMap(tiles), HashSet(frontier))
}
