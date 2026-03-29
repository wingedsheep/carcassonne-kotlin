package com.wingedsheep.carcassonne.scoring

import com.wingedsheep.carcassonne.engine.BoardMap
import com.wingedsheep.carcassonne.model.*

object RoadDetector {

    fun findRoad(board: BoardMap, start: CoordinateWithSide): Road {
        val tile = board.getRotated(start.coordinate) ?: return emptyRoad()

        val positions = mutableSetOf<CoordinateWithSide>()
        val explored = mutableSetOf<CoordinateWithSide>()
        val queue = ArrayDeque<CoordinateWithSide>()
        val tileCoords = mutableSetOf<Coordinate>()
        var hasInn = false
        var finished = true

        // Find the road connection containing the start side
        val connectedSides = findConnectedRoadSides(tile, start.side)
        for (side in connectedSides) {
            val cws = CoordinateWithSide(start.coordinate, side)
            queue.add(cws)
            positions.add(cws)
        }

        while (queue.isNotEmpty()) {
            val current = queue.removeFirst()
            if (current in explored) continue
            explored.add(current)

            val coord = current.coordinate
            val currentTile = board.getRotated(coord) ?: continue

            tileCoords.add(coord)
            if (currentTile.inn.isNotEmpty()) hasInn = true

            // If the side is CENTER-connected (junction), don't cross to neighbor
            // A road ending at CENTER doesn't connect to a neighbor
            // Only cross when the side is a cardinal direction
            if (current.side !in Side.CARDINAL) continue

            val neighborCoord = coord.neighbor(current.side)
            val neighborTile = board.getRotated(neighborCoord)

            if (neighborTile == null) {
                finished = false
                continue
            }

            val oppositeSide = current.side.opposite()
            if (neighborTile.terrainAt(oppositeSide) != TerrainType.ROAD) continue

            // Find connected road sides on the neighbor
            val neighborConnected = findConnectedRoadSides(neighborTile, oppositeSide)
            for (side in neighborConnected) {
                val cws = CoordinateWithSide(neighborCoord, side)
                if (cws !in explored) {
                    positions.add(cws)
                    queue.add(cws)
                }
            }
        }

        return Road(
            positions = positions,
            finished = finished,
            tileCoordinates = tileCoords,
            hasInn = hasInn,
        )
    }

    fun findRoadsAt(board: BoardMap, coord: Coordinate): List<Road> {
        val tile = board.getRotated(coord) ?: return emptyList()
        val roads = mutableListOf<Road>()
        val seenSides = mutableSetOf<Side>()

        for (connection in tile.roads) {
            val side = connection.a
            if (side in seenSides) continue
            val cws = CoordinateWithSide(coord, side)
            val road = findRoad(board, cws)
            for (pos in road.positions) {
                if (pos.coordinate == coord) seenSides.add(pos.side)
            }
            roads.add(road)
        }
        return roads
    }

    /**
     * Given a starting side on a tile, find all sides connected via road connections on the same tile.
     * For example, on a straight road (TOP-BOTTOM), starting from TOP gives {TOP, BOTTOM}.
     * On a crossroads, each side only connects to CENTER (itself), so the result is just {side}.
     */
    private fun findConnectedRoadSides(tile: RotatedTile, startSide: Side): Set<Side> {
        val result = mutableSetOf(startSide)
        for (conn in tile.roads) {
            if (conn.a == startSide && conn.b != conn.a) result.add(conn.b)
            if (conn.b == startSide && conn.a != conn.b) result.add(conn.a)
        }
        // Remove CENTER - it's not a real side to traverse to neighbors
        // but keep cardinal sides that connect through a road
        return result.filter { it in Side.CARDINAL || it == startSide }.toSet()
    }

    private fun emptyRoad() = Road(emptySet(), false, emptySet())
}
