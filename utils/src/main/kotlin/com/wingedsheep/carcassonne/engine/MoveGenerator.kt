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
            for (rotation in 0..3) {
                val rotated = TileRegistry.getRotated(tileId, rotation)
                if (TileFitter.fits(state.board, rotated, coord)) {
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

        val hasNormal = pool.available(MeepleType.NORMAL) > 0
        val hasBig = pool.available(MeepleType.BIG) > 0

        if (hasNormal || hasBig) {
            // City placements
            for ((zoneIndex, zone) in tile.cityZones.withIndex()) {
                val representative = zone.first()
                val cws = CoordinateWithSide(coord, representative)
                val city = CityDetector.findCity(state.board, cws)
                if (!structureHasMeeple(state, city.positions)) {
                    val side = zone.first()
                    if (hasNormal) actions.add(PlaceMeeple(MeepleType.NORMAL, coord, side = side))
                    if (hasBig) actions.add(PlaceMeeple(MeepleType.BIG, coord, side = side))
                }
            }

            // Road placements
            val visitedRoadSides = mutableSetOf<Side>()
            for (road in tile.roads) {
                val side = road.a
                if (side in visitedRoadSides) continue
                // Find all sides of this road segment on this tile
                val roadEnds = tile.roadEnds()
                val cws = CoordinateWithSide(coord, side)
                val roadStructure = RoadDetector.findRoad(state.board, cws)
                for (pos in roadStructure.positions) {
                    if (pos.coordinate == coord) visitedRoadSides.add(pos.side)
                }
                if (!structureHasMeeple(state, roadStructure.positions)) {
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
                    if (!farmHasMeeple(state, farmStructure)) {
                        val farmerSide = farm.tileConnections.first()
                        if (hasFarmer) actions.add(PlaceMeeple(MeepleType.FARMER, coord, farmerSide = farmerSide))
                        if (hasBigFarmer) actions.add(PlaceMeeple(MeepleType.BIG_FARMER, coord, farmerSide = farmerSide))
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

    private fun structureHasMeeple(state: GameState, positions: Set<CoordinateWithSide>): Boolean {
        for (player in 0 until state.playerCount) {
            for (meeple in state.placedMeeples[player]) {
                if (meeple.type.isFarmer) continue
                if (meeple.side == null) continue
                val cws = CoordinateWithSide(meeple.coordinate, meeple.side)
                if (cws in positions) return true
            }
        }
        return false
    }

    private fun farmHasMeeple(state: GameState, farm: Farm): Boolean {
        for (player in 0 until state.playerCount) {
            for (meeple in state.placedMeeples[player]) {
                if (!meeple.type.isFarmer) continue
                val fs = meeple.farmerSide ?: continue
                val cwfs = CoordinateWithFarmerSide(meeple.coordinate, fs)
                if (cwfs in farm.connections) return true
            }
        }
        return false
    }
}
