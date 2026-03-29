package com.wingedsheep.carcassonne.model

/**
 * Pre-computed rotated variant of a [TileDefinition].
 * All 4 rotations are created at tile registration time so rotation during gameplay is free.
 */
class RotatedTile(
    val definition: TileDefinition,
    val rotation: Int,
    val edges: EdgeProfile,
    val roads: List<Connection>,
    val rivers: List<Connection>,
    val cityZones: List<List<Side>>,
    val fieldSides: List<Side>,
    val farms: List<FarmerConnection>,
    val inn: List<Side>,
) {
    val shield: Boolean get() = definition.shield
    val monastery: Boolean get() = definition.monastery
    val flowers: Boolean get() = definition.flowers
    val cathedral: Boolean get() = definition.cathedral
    val name: String get() = definition.name

    fun terrainAt(side: Side): TerrainType = edges.terrain(side)

    fun roadEnds(): Set<Side> = roads.flatMapTo(mutableSetOf()) { it.sides() }

    fun riverEnds(): Set<Side> = rivers.flatMapTo(mutableSetOf()) { it.sides() }

    fun citySides(): Set<Side> = cityZones.flatMapTo(mutableSetOf()) { it }

    /** Which city zone (index) does this side belong to, or -1 if not a city side. */
    fun cityZoneIndex(side: Side): Int = cityZones.indexOfFirst { side in it }

    companion object {
        fun fromDefinition(definition: TileDefinition, rotation: Int): RotatedTile {
            return RotatedTile(
                definition = definition,
                rotation = rotation,
                edges = definition.edges.rotatedCW(rotation),
                roads = definition.roads.map { it.rotatedCW(rotation) },
                rivers = definition.rivers.map { it.rotatedCW(rotation) },
                cityZones = definition.cityZones.map { zone -> zone.map { it.rotatedCW(rotation) } },
                fieldSides = definition.fieldSides.map { it.rotatedCW(rotation) },
                farms = definition.farms.map { it.rotatedCW(rotation) },
                inn = definition.inn.map { it.rotatedCW(rotation) },
            )
        }

        fun allRotations(definition: TileDefinition): Array<RotatedTile> {
            return Array(4) { fromDefinition(definition, it) }
        }
    }
}
