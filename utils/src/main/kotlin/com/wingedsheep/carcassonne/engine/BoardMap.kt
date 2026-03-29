package com.wingedsheep.carcassonne.engine

import com.wingedsheep.carcassonne.model.Coordinate
import com.wingedsheep.carcassonne.model.PlacedTile
import com.wingedsheep.carcassonne.model.RotatedTile
import com.wingedsheep.carcassonne.model.Side
import com.wingedsheep.carcassonne.model.TerrainType
import com.wingedsheep.carcassonne.tile.TileRegistry
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@Serializable
data class BoardMap(
    private val tiles: HashMap<Coordinate, PlacedTile> = HashMap(),
    private val frontier: HashSet<Coordinate> = HashSet(),
) {
    /**
     * Per-frontier-coordinate edge constraints, packed into a single Long.
     * Low 8 bits = required terrain (EdgeProfile layout: TOP bits 0-1, RIGHT 2-3, BOTTOM 4-5, LEFT 6-7).
     * Bits 8-15 = mask (0x3 in each 2-bit slot that has a placed neighbor).
     *
     * A tile with EdgeProfile `e` fits at frontier coord `c` iff:
     *   `e.packed and mask == required`
     *
     * Rebuilt from tiles on deserialization.
     */
    @Transient
    private var constraints: HashMap<Coordinate, Long> = HashMap()

    /** Whether constraints have been initialized (false after deserialization). */
    @Transient
    private var constraintsReady: Boolean = false

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
        ensureConstraints()
        constraints.remove(coord)

        val rotated = TileRegistry.getRotated(tile.tileId, tile.rotation)

        for (side in Side.CARDINAL) {
            val neighbor = coord.neighbor(side)
            if (neighbor !in tiles) {
                frontier.add(neighbor)
                // The neighbor's side facing us is side.opposite().
                // The terrain we require on that side is our terrain on `side`.
                val terrain = rotated.terrainAt(side)
                addConstraint(neighbor, side.opposite(), terrain)
            }
        }
    }

    fun frontierCoordinates(): Set<Coordinate> = frontier

    fun allCoordinates(): Set<Coordinate> = tiles.keys

    /**
     * Get the packed constraint for a frontier coordinate.
     * Returns required (low 8 bits) and mask (bits 8-15) packed into a Long.
     */
    fun getConstraint(coord: Coordinate): Long {
        ensureConstraints()
        return constraints[coord] ?: 0L
    }

    fun copy(): BoardMap {
        val copy = BoardMap(HashMap(tiles), HashSet(frontier))
        copy.constraints = HashMap(this.constraints)
        copy.constraintsReady = this.constraintsReady
        return copy
    }

    /** Add a constraint for one side of a frontier coordinate. */
    private fun addConstraint(frontierCoord: Coordinate, side: Side, terrain: TerrainType) {
        val shift = sideShift(side)
        val existing = constraints[frontierCoord] ?: 0L
        val terrainBits = terrain.ordinal.toLong() shl shift
        val maskBits = 0x3L shl shift
        // Set the required terrain and mask for this side (OR in, preserving other sides)
        constraints[frontierCoord] = existing or terrainBits or (maskBits shl 8)
    }

    /** Rebuild constraints from tile data (after deserialization). */
    private fun ensureConstraints() {
        if (constraintsReady) return
        constraintsReady = true
        constraints = HashMap()
        for (coord in frontier) {
            for (side in Side.CARDINAL) {
                val neighbor = coord.neighbor(side)
                val neighborTile = getRotated(neighbor) ?: continue
                // Our `side` faces the neighbor. The neighbor's terrain on the opposite side
                // is what we require on our `side`.
                val terrain = neighborTile.terrainAt(side.opposite())
                addConstraint(coord, side, terrain)
            }
        }
    }

    companion object {
        /** Bit shift for each side in the EdgeProfile layout. */
        private fun sideShift(side: Side): Int = when (side) {
            Side.TOP -> 0
            Side.RIGHT -> 2
            Side.BOTTOM -> 4
            Side.LEFT -> 6
            else -> error("Only cardinal sides, got $side")
        }
    }
}
