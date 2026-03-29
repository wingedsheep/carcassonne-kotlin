package com.wingedsheep.carcassonne.engine

import com.wingedsheep.carcassonne.model.*
import com.wingedsheep.carcassonne.scoring.CityDetector
import com.wingedsheep.carcassonne.scoring.FarmDetector
import com.wingedsheep.carcassonne.scoring.RoadDetector
import com.wingedsheep.carcassonne.tile.TileRegistry

object MoveGenerator {

    fun validTilePlacements(state: GameState, tileId: Int): List<PlaceTile> {
        val actions = mutableListOf<PlaceTile>()
        for (coord in state.board.frontierCoordinates()) {
            val constraint = state.board.getConstraint(coord)
            val required = (constraint and 0xFF).toInt()
            val mask = ((constraint shr 8) and 0xFF).toInt()
            for (rotation in 0..3) {
                val rotated = TileRegistry.getRotated(tileId, rotation)
                if (rotated.edges.packed and mask == required) {
                    actions.add(PlaceTile(tileId, rotation, coord))
                }
            }
        }
        return actions
    }

    fun validMeeplePlacements(state: GameState): List<Action> {
        val actions = mutableListOf<Action>()
        val coord = state.lastPlacedCoordinate ?: return listOf(Pass)
        val placedTile = state.board[coord] ?: return listOf(Pass)
        val tile = TileRegistry.getRotated(placedTile.tileId, placedTile.rotation)
        val player = state.currentPlayer
        val pool = state.meeples[player]

        // Pre-build lookup sets for O(1) meeple presence checks
        val nonFarmerPositions = HashSet<CoordinateWithSide>()
        val farmerPositions = HashSet<CoordinateWithFarmerSide>()
        for (p in 0 until state.playerCount) {
            for (meeple in state.placedMeeples[p]) {
                if (meeple.type.isFarmer) {
                    val fs = meeple.farmerSide
                    if (fs != null) farmerPositions.add(CoordinateWithFarmerSide(meeple.coordinate, fs))
                } else {
                    val s = meeple.side
                    if (s != null) nonFarmerPositions.add(CoordinateWithSide(meeple.coordinate, s))
                }
            }
        }

        val hasNormal = pool.available(MeepleType.NORMAL) > 0
        val hasBig = pool.available(MeepleType.BIG) > 0

        if (hasNormal || hasBig) {
            // City placements
            for ((zoneIndex, zone) in tile.cityZones.withIndex()) {
                val representative = zone.first()
                val cws = CoordinateWithSide(coord, representative)
                val city = CityDetector.findCity(state.board, cws)
                if (!structureHasMeeple(nonFarmerPositions, city.positions)) {
                    if (hasNormal) actions.add(PlaceMeeple(MeepleType.NORMAL, coord, side = representative))
                    if (hasBig) actions.add(PlaceMeeple(MeepleType.BIG, coord, side = representative))
                }
            }

            // Road placements
            val visitedRoadSides = mutableSetOf<Side>()
            for (road in tile.roads) {
                val side = road.a
                if (side in visitedRoadSides) continue
                val cws = CoordinateWithSide(coord, side)
                val roadStructure = RoadDetector.findRoad(state.board, cws)
                for (pos in roadStructure.positions) {
                    if (pos.coordinate == coord) visitedRoadSides.add(pos.side)
                }
                if (!structureHasMeeple(nonFarmerPositions, roadStructure.positions)) {
                    if (hasNormal) actions.add(PlaceMeeple(MeepleType.NORMAL, coord, side = side))
                    if (hasBig) actions.add(PlaceMeeple(MeepleType.BIG, coord, side = side))
                }
            }

            // Monastery/flowers placement
            if (tile.monastery || tile.flowers) {
                if (hasNormal) actions.add(PlaceMeeple(MeepleType.NORMAL, coord, side = null))
                if (hasBig) actions.add(PlaceMeeple(MeepleType.BIG, coord, side = null))
            }
        }

        // Farmer placements
        if (SupplementaryRule.FARMERS in state.supplementaryRules) {
            val hasFarmer = pool.available(MeepleType.FARMER) > 0
            val hasBigFarmer = pool.available(MeepleType.BIG_FARMER) > 0
            if (hasFarmer || hasBigFarmer) {
                val visitedFarms = mutableSetOf<Set<CoordinateWithFarmerSide>>()
                for (farm in tile.farms) {
                    if (farm.tileConnections.isEmpty()) continue
                    val firstFarmerSide = farm.tileConnections.first()
                    val cwfs = CoordinateWithFarmerSide(coord, firstFarmerSide)
                    val farmStructure = FarmDetector.findFarm(state.board, cwfs)
                    if (farmStructure.connections in visitedFarms) continue
                    visitedFarms.add(farmStructure.connections)
                    if (!farmHasMeeple(farmerPositions, farmStructure)) {
                        if (hasFarmer) actions.add(PlaceMeeple(MeepleType.FARMER, coord, farmerSide = firstFarmerSide))
                        if (hasBigFarmer) actions.add(PlaceMeeple(MeepleType.BIG_FARMER, coord, farmerSide = firstFarmerSide))
                    }
                }
            }
        }

        // Abbot placements
        if (SupplementaryRule.ABBOTS in state.supplementaryRules) {
            if (pool.available(MeepleType.ABBOT) > 0 && (tile.monastery || tile.flowers)) {
                actions.add(PlaceMeeple(MeepleType.ABBOT, coord, side = null))
            }

            // Check if player has abbots on board that can be removed
            for (meeple in state.placedMeeples[player]) {
                if (meeple.type == MeepleType.ABBOT) {
                    actions.add(PlaceMeeple(MeepleType.ABBOT, meeple.coordinate, side = null, remove = true))
                }
            }
        }

        actions.add(Pass)
        return actions
    }

    private fun structureHasMeeple(meeplePositions: Set<CoordinateWithSide>, structurePositions: Set<CoordinateWithSide>): Boolean {
        // Iterate the smaller set for efficiency
        if (meeplePositions.size <= structurePositions.size) {
            for (pos in meeplePositions) {
                if (pos in structurePositions) return true
            }
        } else {
            for (pos in structurePositions) {
                if (pos in meeplePositions) return true
            }
        }
        return false
    }

    private fun farmHasMeeple(farmerPositions: Set<CoordinateWithFarmerSide>, farm: Farm): Boolean {
        if (farmerPositions.size <= farm.connections.size) {
            for (pos in farmerPositions) {
                if (pos in farm.connections) return true
            }
        } else {
            for (pos in farm.connections) {
                if (pos in farmerPositions) return true
            }
        }
        return false
    }
}
