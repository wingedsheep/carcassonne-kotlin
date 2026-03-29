package com.wingedsheep.carcassonne.scoring

import com.wingedsheep.carcassonne.engine.BoardMap
import com.wingedsheep.carcassonne.model.*

object FarmDetector {

    fun findFarm(board: BoardMap, start: CoordinateWithFarmerSide): Farm {
        val tile = board.getRotated(start.coordinate) ?: return emptyFarm()

        val connections = mutableSetOf<CoordinateWithFarmerSide>()
        val explored = mutableSetOf<CoordinateWithFarmerSide>()
        val queue = ArrayDeque<CoordinateWithFarmerSide>()
        val farmerConnsWithCoord = mutableListOf<Pair<Coordinate, FarmerConnection>>()

        // Find the farmer connection containing the start farmer side
        val startFarmConn = tile.farms.find { farm ->
            start.farmerSide in farm.tileConnections
        } ?: return emptyFarm()

        // Seed with all tile connections of this farm zone
        for (fs in startFarmConn.tileConnections) {
            val cwfs = CoordinateWithFarmerSide(start.coordinate, fs)
            queue.add(cwfs)
            connections.add(cwfs)
        }
        farmerConnsWithCoord.add(start.coordinate to startFarmConn)

        while (queue.isNotEmpty()) {
            val current = queue.removeFirst()
            if (current in explored) continue
            explored.add(current)

            val coord = current.coordinate
            val farmerSide = current.farmerSide

            // Cross to the neighbor tile through this farmer side
            val adjacentSide = farmerSide.adjacentSide()
            val neighborCoord = coord.neighbor(adjacentSide)
            val neighborTile = board.getRotated(neighborCoord) ?: continue

            val oppositeFarmerSide = farmerSide.opposite()

            // Find the farm zone on the neighbor that contains the opposite farmer side
            val neighborFarmConn = neighborTile.farms.find { farm ->
                oppositeFarmerSide in farm.tileConnections
            } ?: continue

            // Add all farmer sides of that zone
            val isNew = farmerConnsWithCoord.none {
                it.first == neighborCoord && it.second == neighborFarmConn
            }
            if (isNew) {
                farmerConnsWithCoord.add(neighborCoord to neighborFarmConn)
            }

            for (fs in neighborFarmConn.tileConnections) {
                val cwfs = CoordinateWithFarmerSide(neighborCoord, fs)
                if (cwfs !in explored) {
                    connections.add(cwfs)
                    queue.add(cwfs)
                }
            }
        }

        return Farm(
            connections = connections,
            farmerConnectionsWithCoordinate = farmerConnsWithCoord,
        )
    }

    private fun emptyFarm() = Farm(emptySet(), emptyList())
}
