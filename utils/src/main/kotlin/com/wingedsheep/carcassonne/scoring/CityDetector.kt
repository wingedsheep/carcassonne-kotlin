package com.wingedsheep.carcassonne.scoring

import com.wingedsheep.carcassonne.engine.BoardMap
import com.wingedsheep.carcassonne.model.*

object CityDetector {

    fun findCity(board: BoardMap, start: CoordinateWithSide): City {
        val tile = board.getRotated(start.coordinate) ?: return emptyCity()
        val zoneIndex = tile.cityZoneIndex(start.side)
        if (zoneIndex < 0) return emptyCity()

        val positions = mutableSetOf<CoordinateWithSide>()
        val explored = mutableSetOf<CoordinateWithSide>()
        val queue = ArrayDeque<CoordinateWithSide>()
        val tileCoords = mutableSetOf<Coordinate>()
        var shieldCount = 0
        var hasCathedral = false
        var finished = true

        // Seed with all sides of the city zone on the starting tile
        for (side in tile.cityZones[zoneIndex]) {
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
            if (currentTile.shield) shieldCount++
            if (currentTile.cathedral) hasCathedral = true

            // Look at the opposite side on the neighbor
            val neighborCoord = coord.neighbor(current.side)
            val neighborTile = board.getRotated(neighborCoord)

            if (neighborTile == null) {
                finished = false
                continue
            }

            val oppositeSide = current.side.opposite()
            val neighborZoneIndex = neighborTile.cityZoneIndex(oppositeSide)
            if (neighborZoneIndex < 0) continue

            // Add all sides of the neighbor's city zone
            for (side in neighborTile.cityZones[neighborZoneIndex]) {
                val cws = CoordinateWithSide(neighborCoord, side)
                if (cws !in explored) {
                    positions.add(cws)
                    queue.add(cws)
                }
            }
        }

        // Deduplicate shield counting: count per unique tile coordinate
        val shieldTiles = tileCoords.count { coord ->
            val t = board.getRotated(coord)
            t != null && t.shield
        }

        return City(
            positions = positions,
            finished = finished,
            tileCoordinates = tileCoords,
            shieldCount = shieldTiles,
            hasCathedral = hasCathedral,
        )
    }

    fun findCitiesAt(board: BoardMap, coord: Coordinate): List<City> {
        val tile = board.getRotated(coord) ?: return emptyList()
        val cities = mutableListOf<City>()
        val seenZones = mutableSetOf<Int>()

        for ((zoneIndex, zone) in tile.cityZones.withIndex()) {
            if (zoneIndex in seenZones) continue
            seenZones.add(zoneIndex)
            val cws = CoordinateWithSide(coord, zone.first())
            cities.add(findCity(board, cws))
        }
        return cities
    }

    private fun emptyCity() = City(emptySet(), false, emptySet())
}
