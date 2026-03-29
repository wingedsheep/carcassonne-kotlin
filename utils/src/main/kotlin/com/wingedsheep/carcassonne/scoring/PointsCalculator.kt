package com.wingedsheep.carcassonne.scoring

import com.wingedsheep.carcassonne.engine.BoardMap
import com.wingedsheep.carcassonne.engine.GameState
import com.wingedsheep.carcassonne.model.*

object PointsCalculator {

    fun scoreCompletedStructures(state: GameState, placedAt: Coordinate) {
        // Score completed cities
        val cities = CityDetector.findCitiesAt(state.board, placedAt)
        for (city in cities) {
            if (!city.finished) continue
            val meeples = findMeeplesInStructure(state, city.positions)
            val counts = meepleCounts(state, meeples)
            if (counts.all { it == 0 }) continue
            val points = cityPoints(city)
            val winners = findWinners(counts)
            for (winner in winners) {
                state.scores[winner] += points
            }
            removeMeeples(state, meeples)
        }

        // Score completed roads
        val roads = RoadDetector.findRoadsAt(state.board, placedAt)
        for (road in roads) {
            if (!road.finished) continue
            val meeples = findMeeplesInStructure(state, road.positions)
            val counts = meepleCounts(state, meeples)
            if (counts.all { it == 0 }) continue
            val points = roadPoints(road)
            val winners = findWinners(counts)
            for (winner in winners) {
                state.scores[winner] += points
            }
            removeMeeples(state, meeples)
        }

        // Score completed monasteries (check 3x3 area around placed tile)
        for (row in (placedAt.row - 1)..(placedAt.row + 1)) {
            for (col in (placedAt.col - 1)..(placedAt.col + 1)) {
                val coord = Coordinate(row, col)
                val tile = state.board.getRotated(coord) ?: continue
                if (!tile.monastery && !tile.flowers) continue

                val points = monasteryPoints(state.board, coord)
                if (points < 9) continue // Not complete

                // Find meeple on this monastery
                val meepleOwner = findMonasteryMeepleOwner(state, coord)
                if (meepleOwner != null) {
                    state.scores[meepleOwner] += points
                    removeMonasteryMeeple(state, coord, meepleOwner)
                }
            }
        }
    }

    fun scoreFinalState(state: GameState) {
        val processed = mutableSetOf<PlacedMeeple>()

        for (player in 0 until state.playerCount) {
            val meeplesToProcess = state.placedMeeples[player].toList()
            for (meeple in meeplesToProcess) {
                if (meeple in processed) continue

                when {
                    meeple.type.isFarmer -> {
                        val fs = meeple.farmerSide ?: continue
                        val cwfs = CoordinateWithFarmerSide(meeple.coordinate, fs)
                        val farm = FarmDetector.findFarm(state.board, cwfs)
                        val farmMeeples = findFarmMeeples(state, farm)
                        val counts = meepleCounts(state, farmMeeples)
                        val points = farmPoints(state.board, farm)
                        for (winner in findWinners(counts)) {
                            state.scores[winner] += points
                        }
                        for (list in farmMeeples) for (ms in list) processed.add(ms)
                        removeMeeples(state, farmMeeples)
                    }
                    meeple.side != null -> {
                        val cws = CoordinateWithSide(meeple.coordinate, meeple.side)
                        val tile = state.board.getRotated(meeple.coordinate) ?: continue

                        if (meeple.side in tile.citySides()) {
                            val city = CityDetector.findCity(state.board, cws)
                            val cityMeeples = findMeeplesInStructure(state, city.positions)
                            val counts = meepleCounts(state, cityMeeples)
                            val points = cityPoints(city)
                            for (winner in findWinners(counts)) {
                                state.scores[winner] += points
                            }
                            for (list in cityMeeples) for (ms in list) processed.add(ms)
                            removeMeeples(state, cityMeeples)
                        } else if (tile.terrainAt(meeple.side) == TerrainType.ROAD) {
                            val road = RoadDetector.findRoad(state.board, cws)
                            val roadMeeples = findMeeplesInStructure(state, road.positions)
                            val counts = meepleCounts(state, roadMeeples)
                            val points = roadPoints(road)
                            for (winner in findWinners(counts)) {
                                state.scores[winner] += points
                            }
                            for (list in roadMeeples) for (ms in list) processed.add(ms)
                            removeMeeples(state, roadMeeples)
                        }
                    }
                    else -> {
                        // Monastery/flowers (side == null, not farmer)
                        val points = monasteryPoints(state.board, meeple.coordinate)
                        state.scores[player] += points
                        removeMonasteryMeeple(state, meeple.coordinate, player)
                        processed.add(meeple)
                    }
                }
            }
        }
    }

    fun monasteryPoints(board: BoardMap, coord: Coordinate): Int {
        var points = 0
        for (row in (coord.row - 1)..(coord.row + 1)) {
            for (col in (coord.col - 1)..(coord.col + 1)) {
                if (board.contains(Coordinate(row, col))) points++
            }
        }
        return points
    }

    fun cityPoints(city: City): Int {
        val tileCount = city.tileCoordinates.size
        return if (city.finished) {
            if (city.hasCathedral) {
                tileCount * 3 + city.shieldCount * 3
            } else {
                tileCount * 2 + city.shieldCount * 2
            }
        } else {
            if (city.hasCathedral) {
                0
            } else {
                tileCount + city.shieldCount
            }
        }
    }

    fun roadPoints(road: Road): Int {
        val tileCount = road.tileCoordinates.size
        return if (road.finished) {
            if (road.hasInn) tileCount * 2 else tileCount
        } else {
            if (road.hasInn) 0 else tileCount
        }
    }

    fun farmPoints(board: BoardMap, farm: Farm): Int {
        val adjacentCities = mutableSetOf<Set<CoordinateWithSide>>()
        var points = 0

        for ((coord, farmConn) in farm.farmerConnectionsWithCoordinate) {
            for (citySide in farmConn.citySides) {
                val cws = CoordinateWithSide(coord, citySide)
                val city = CityDetector.findCity(board, cws)
                if (city.finished && city.positions !in adjacentCities) {
                    adjacentCities.add(city.positions)
                    points += 3
                }
            }
        }

        return points
    }

    private fun findMeeplesInStructure(
        state: GameState,
        positions: Set<CoordinateWithSide>
    ): Array<List<PlacedMeeple>> {
        return Array(state.playerCount) { player ->
            state.placedMeeples[player].filter { meeple ->
                !meeple.type.isFarmer &&
                    meeple.side != null &&
                    CoordinateWithSide(meeple.coordinate, meeple.side) in positions
            }
        }
    }

    private fun findFarmMeeples(
        state: GameState,
        farm: Farm
    ): Array<List<PlacedMeeple>> {
        return Array(state.playerCount) { player ->
            state.placedMeeples[player].filter { meeple ->
                meeple.type.isFarmer &&
                    meeple.farmerSide != null &&
                    CoordinateWithFarmerSide(meeple.coordinate, meeple.farmerSide) in farm.connections
            }
        }
    }

    private fun meepleCounts(state: GameState, meeples: Array<List<PlacedMeeple>>): IntArray {
        return IntArray(state.playerCount) { player ->
            meeples[player].sumOf { it.type.weight }
        }
    }

    private fun findWinners(counts: IntArray): List<Int> {
        val max = counts.max()
        if (max == 0) return emptyList()
        return counts.indices.filter { counts[it] == max }
    }

    private fun findWinner(counts: IntArray): Int? {
        val winners = findWinners(counts)
        return if (winners.size == 1) winners[0] else null
    }

    private fun removeMeeples(state: GameState, meeples: Array<List<PlacedMeeple>>) {
        for (player in 0 until state.playerCount) {
            for (meeple in meeples[player]) {
                state.placedMeeples[player].remove(meeple)
                state.meeples[player].returnMeeple(meeple.type)
            }
        }
    }

    private fun findMonasteryMeepleOwner(state: GameState, coord: Coordinate): Int? {
        for (player in 0 until state.playerCount) {
            if (state.placedMeeples[player].any {
                    it.coordinate == coord && it.side == null && !it.type.isFarmer
                }) return player
        }
        return null
    }

    private fun removeMonasteryMeeple(state: GameState, coord: Coordinate, player: Int) {
        val meeple = state.placedMeeples[player].find {
            it.coordinate == coord && it.side == null && !it.type.isFarmer
        } ?: return
        state.placedMeeples[player].remove(meeple)
        state.meeples[player].returnMeeple(meeple.type)
    }
}
