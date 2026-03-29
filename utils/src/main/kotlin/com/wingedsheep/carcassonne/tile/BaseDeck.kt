package com.wingedsheep.carcassonne.tile

import com.wingedsheep.carcassonne.model.*
import com.wingedsheep.carcassonne.model.FarmerSide.*
import com.wingedsheep.carcassonne.model.Side.*
import com.wingedsheep.carcassonne.model.TerrainType.*

object BaseDeck {

    fun tiles(): List<TileCount> = baseTiles.map { (name, count) ->
        TileCount(definitions.getValue(name), count)
    }

    /** The "city_top_straight_road" tile is used as the starting tile. */
    fun startingTile(): TileDefinition = definitions.getValue("city_top_straight_road")

    private val baseTiles = listOf(
        "chapel_with_road" to 2,
        "chapel" to 4,
        "full_city_with_shield" to 1,
        "city_top_straight_road" to 4,
        "city_top" to 4,
        "city_top_flowers" to 1,
        "city_narrow_shield" to 2,
        "city_narrow" to 1,
        "city_left_right" to 2,
        "city_top_bottom_flowers" to 1,
        "city_top_right" to 1,
        "city_top_left_flowers" to 1,
        "city_top_road_bend_right" to 3,
        "city_top_road_bend_left" to 3,
        "city_top_crossroads" to 3,
        "city_diagonal_top_right_shield" to 1,
        "city_diagonal_top_right_shield_flowers" to 1,
        "city_diagonal_top_right" to 2,
        "city_diagonal_top_right_flowers" to 1,
        "city_diagonal_top_left_shield_road" to 2,
        "city_diagonal_top_left_road" to 3,
        "city_bottom_grass_shield" to 1,
        "city_bottom_grass" to 2,
        "city_bottom_grass_flowers" to 1,
        "city_bottom_road_shield" to 2,
        "city_bottom_road" to 1,
        "straight_road" to 7,
        "straight_road_flowers" to 1,
        "bent_road" to 8,
        "bent_road_flowers" to 1,
        "three_split_road" to 4,
        "crossroads" to 1,
    )

    private val definitions: Map<String, TileDefinition> = buildMap {
        // A - Chapel with road
        put("chapel_with_road", TileDefinition(
            name = "chapel_with_road",
            edges = EdgeProfile.of(top = FIELD, right = FIELD, bottom = ROAD, left = FIELD),
            roads = listOf(Connection(BOTTOM, BOTTOM)), // road to center
            monastery = true,
            farms = listOf(
                FarmerConnection(
                    farmerPositions = listOf(TOP_LEFT, TOP_RIGHT, BOTTOM_LEFT, BOTTOM_RIGHT),
                    tileConnections = listOf(TLL, TLT, TRT, TRR, BRR, BRB, BLB, BLL)
                )
            ),
            fieldSides = listOf(LEFT, TOP, RIGHT)
        ))

        // B - Chapel
        put("chapel", TileDefinition(
            name = "chapel",
            edges = EdgeProfile.of(top = FIELD, right = FIELD, bottom = FIELD, left = FIELD),
            monastery = true,
            farms = listOf(
                FarmerConnection(
                    farmerPositions = listOf(TOP_LEFT, TOP_RIGHT, BOTTOM_LEFT, BOTTOM_RIGHT),
                    tileConnections = listOf(TLL, TLT, TRT, TRR, BRR, BRB, BLB, BLL)
                )
            ),
            fieldSides = listOf(TOP, RIGHT, BOTTOM, LEFT)
        ))

        // C - Full city with shield
        put("full_city_with_shield", TileDefinition(
            name = "full_city_with_shield",
            edges = EdgeProfile.of(top = CITY, right = CITY, bottom = CITY, left = CITY),
            cityZones = listOf(listOf(TOP, RIGHT, BOTTOM, LEFT)),
            shield = true
        ))

        // D - City top with straight road (starting tile)
        put("city_top_straight_road", TileDefinition(
            name = "city_top_straight_road",
            edges = EdgeProfile.of(top = CITY, right = ROAD, bottom = FIELD, left = ROAD),
            roads = listOf(Connection(LEFT, RIGHT)),
            cityZones = listOf(listOf(TOP)),
            farms = listOf(
                FarmerConnection(
                    farmerPositions = listOf(TOP_LEFT, TOP_RIGHT),
                    tileConnections = listOf(TLL, TLT, TRT, TRR),
                    citySides = listOf(TOP)
                ),
                FarmerConnection(
                    farmerPositions = listOf(BOTTOM_LEFT, BOTTOM_RIGHT),
                    tileConnections = listOf(BRR, BRB, BLB, BLL)
                )
            ),
            fieldSides = listOf(BOTTOM)
        ))

        // E - City top
        put("city_top", TileDefinition(
            name = "city_top",
            edges = EdgeProfile.of(top = CITY, right = FIELD, bottom = FIELD, left = FIELD),
            cityZones = listOf(listOf(TOP)),
            farms = listOf(
                FarmerConnection(
                    farmerPositions = listOf(TOP_LEFT, TOP_RIGHT, BOTTOM_LEFT, BOTTOM_RIGHT),
                    tileConnections = listOf(TLL, TRR, BRR, BRB, BLB, BLL),
                    citySides = listOf(TOP)
                )
            ),
            fieldSides = listOf(RIGHT, BOTTOM, LEFT)
        ))

        // E (garden) - City top with flowers
        put("city_top_flowers", TileDefinition(
            name = "city_top_flowers",
            edges = EdgeProfile.of(top = CITY, right = FIELD, bottom = FIELD, left = FIELD),
            cityZones = listOf(listOf(TOP)),
            farms = listOf(
                FarmerConnection(
                    farmerPositions = listOf(TOP_LEFT, TOP_RIGHT, BOTTOM_LEFT, BOTTOM_RIGHT),
                    tileConnections = listOf(TLL, TRR, BRR, BRB, BLB, BLL),
                    citySides = listOf(TOP)
                )
            ),
            flowers = true,
            fieldSides = listOf(RIGHT, BOTTOM, LEFT)
        ))

        // F - City narrow with shield (left-right connected)
        put("city_narrow_shield", TileDefinition(
            name = "city_narrow_shield",
            edges = EdgeProfile.of(top = FIELD, right = CITY, bottom = FIELD, left = CITY),
            cityZones = listOf(listOf(LEFT, RIGHT)),
            farms = listOf(
                FarmerConnection(
                    farmerPositions = listOf(TOP_LEFT, TOP_RIGHT),
                    tileConnections = listOf(TLT, TRT),
                    citySides = listOf(LEFT, RIGHT)
                ),
                FarmerConnection(
                    farmerPositions = listOf(BOTTOM_LEFT, BOTTOM_RIGHT),
                    tileConnections = listOf(BRB, BLB),
                    citySides = listOf(LEFT, RIGHT)
                )
            ),
            shield = true,
            fieldSides = listOf(TOP, BOTTOM)
        ))

        // G - City narrow (left-right connected)
        put("city_narrow", TileDefinition(
            name = "city_narrow",
            edges = EdgeProfile.of(top = FIELD, right = CITY, bottom = FIELD, left = CITY),
            cityZones = listOf(listOf(LEFT, RIGHT)),
            farms = listOf(
                FarmerConnection(
                    farmerPositions = listOf(TOP_LEFT, TOP_RIGHT),
                    tileConnections = listOf(TLT, TRT),
                    citySides = listOf(LEFT, RIGHT)
                ),
                FarmerConnection(
                    farmerPositions = listOf(BOTTOM_LEFT, BOTTOM_RIGHT),
                    tileConnections = listOf(BRB, BLB),
                    citySides = listOf(LEFT, RIGHT)
                )
            ),
            fieldSides = listOf(TOP, BOTTOM)
        ))

        // H - City left + right (two separate cities)
        put("city_left_right", TileDefinition(
            name = "city_left_right",
            edges = EdgeProfile.of(top = FIELD, right = CITY, bottom = FIELD, left = CITY),
            cityZones = listOf(listOf(LEFT), listOf(RIGHT)),
            farms = listOf(
                FarmerConnection(
                    farmerPositions = listOf(TOP_LEFT, TOP_RIGHT, BOTTOM_LEFT, BOTTOM_RIGHT),
                    tileConnections = listOf(TLT, TRT, BRB, BLB),
                    citySides = listOf(LEFT, RIGHT)
                )
            ),
            fieldSides = listOf(TOP, BOTTOM)
        ))

        // H (garden) - City top + bottom with flowers
        put("city_top_bottom_flowers", TileDefinition(
            name = "city_top_bottom_flowers",
            edges = EdgeProfile.of(top = CITY, right = FIELD, bottom = CITY, left = FIELD),
            cityZones = listOf(listOf(TOP), listOf(BOTTOM)),
            farms = listOf(
                FarmerConnection(
                    farmerPositions = listOf(TOP_LEFT, TOP_RIGHT, BOTTOM_LEFT, BOTTOM_RIGHT),
                    tileConnections = listOf(TLL, TRR, BRR, BLL),
                    citySides = listOf(TOP, BOTTOM)
                )
            ),
            flowers = true,
            fieldSides = listOf(LEFT, RIGHT)
        ))

        // I - City top + right (two separate cities)
        put("city_top_right", TileDefinition(
            name = "city_top_right",
            edges = EdgeProfile.of(top = CITY, right = CITY, bottom = FIELD, left = FIELD),
            cityZones = listOf(listOf(TOP), listOf(RIGHT)),
            farms = listOf(
                FarmerConnection(
                    farmerPositions = listOf(TOP_LEFT, TOP_RIGHT, BOTTOM_LEFT, BOTTOM_RIGHT),
                    tileConnections = listOf(TLL, BRB, BLB, BLL),
                    citySides = listOf(TOP, RIGHT)
                )
            ),
            fieldSides = listOf(LEFT, BOTTOM)
        ))

        // I (garden) - City top + left with flowers
        put("city_top_left_flowers", TileDefinition(
            name = "city_top_left_flowers",
            edges = EdgeProfile.of(top = CITY, right = FIELD, bottom = FIELD, left = CITY),
            cityZones = listOf(listOf(TOP), listOf(LEFT)),
            farms = listOf(
                FarmerConnection(
                    farmerPositions = listOf(TOP_LEFT, TOP_RIGHT, BOTTOM_LEFT, BOTTOM_RIGHT),
                    tileConnections = listOf(TRR, BRR, BRB, BLB),
                    citySides = listOf(LEFT, TOP)
                )
            ),
            flowers = true,
            fieldSides = listOf(BOTTOM, RIGHT)
        ))

        // J - City top, road bend right (bottom-right)
        put("city_top_road_bend_right", TileDefinition(
            name = "city_top_road_bend_right",
            edges = EdgeProfile.of(top = CITY, right = ROAD, bottom = ROAD, left = FIELD),
            roads = listOf(Connection(BOTTOM, RIGHT)),
            cityZones = listOf(listOf(TOP)),
            farms = listOf(
                FarmerConnection(
                    farmerPositions = listOf(TOP_LEFT, TOP_RIGHT, BOTTOM_LEFT),
                    tileConnections = listOf(TLL, TRR, BLB, BLL),
                    citySides = listOf(TOP)
                ),
                FarmerConnection(
                    farmerPositions = listOf(BOTTOM_RIGHT),
                    tileConnections = listOf(BRR, BRB)
                )
            ),
            fieldSides = listOf(LEFT)
        ))

        // K - City top, road bend left (bottom-left)
        put("city_top_road_bend_left", TileDefinition(
            name = "city_top_road_bend_left",
            edges = EdgeProfile.of(top = CITY, right = FIELD, bottom = ROAD, left = ROAD),
            roads = listOf(Connection(BOTTOM, LEFT)),
            cityZones = listOf(listOf(TOP)),
            farms = listOf(
                FarmerConnection(
                    farmerPositions = listOf(TOP_LEFT, TOP_RIGHT, BOTTOM_RIGHT),
                    tileConnections = listOf(TLL, TRR, BRB, BRR),
                    citySides = listOf(TOP)
                ),
                FarmerConnection(
                    farmerPositions = listOf(BOTTOM_LEFT),
                    tileConnections = listOf(BLL, BLB)
                )
            ),
            fieldSides = listOf(RIGHT)
        ))

        // L - City top, crossroads
        put("city_top_crossroads", TileDefinition(
            name = "city_top_crossroads",
            edges = EdgeProfile.of(top = CITY, right = ROAD, bottom = ROAD, left = ROAD),
            roads = listOf(
                Connection(BOTTOM, BOTTOM),
                Connection(LEFT, LEFT),
                Connection(RIGHT, RIGHT)
            ),
            cityZones = listOf(listOf(TOP)),
            farms = listOf(
                FarmerConnection(
                    farmerPositions = listOf(TOP_LEFT, TOP_RIGHT),
                    tileConnections = listOf(TLL, TRR),
                    citySides = listOf(TOP)
                ),
                FarmerConnection(
                    farmerPositions = listOf(BOTTOM_LEFT),
                    tileConnections = listOf(BLL, BLB)
                ),
                FarmerConnection(
                    farmerPositions = listOf(BOTTOM_RIGHT),
                    tileConnections = listOf(BRB, BRR)
                )
            )
        ))

        // M - City diagonal top-right with shield
        put("city_diagonal_top_right_shield", TileDefinition(
            name = "city_diagonal_top_right_shield",
            edges = EdgeProfile.of(top = CITY, right = CITY, bottom = FIELD, left = FIELD),
            cityZones = listOf(listOf(TOP, RIGHT)),
            farms = listOf(
                FarmerConnection(
                    farmerPositions = listOf(TOP_LEFT, BOTTOM_LEFT, BOTTOM_RIGHT),
                    tileConnections = listOf(TLL, BLB, BLL, BRB),
                    citySides = listOf(TOP, RIGHT)
                )
            ),
            shield = true,
            fieldSides = listOf(LEFT, BOTTOM)
        ))

        // M (garden)
        put("city_diagonal_top_right_shield_flowers", TileDefinition(
            name = "city_diagonal_top_right_shield_flowers",
            edges = EdgeProfile.of(top = CITY, right = CITY, bottom = FIELD, left = FIELD),
            cityZones = listOf(listOf(TOP, RIGHT)),
            farms = listOf(
                FarmerConnection(
                    farmerPositions = listOf(TOP_LEFT, BOTTOM_LEFT, BOTTOM_RIGHT),
                    tileConnections = listOf(TLL, BLB, BLL, BRB),
                    citySides = listOf(TOP, RIGHT)
                )
            ),
            shield = true,
            flowers = true,
            fieldSides = listOf(LEFT, BOTTOM)
        ))

        // N - City diagonal top-right
        put("city_diagonal_top_right", TileDefinition(
            name = "city_diagonal_top_right",
            edges = EdgeProfile.of(top = CITY, right = CITY, bottom = FIELD, left = FIELD),
            cityZones = listOf(listOf(TOP, RIGHT)),
            farms = listOf(
                FarmerConnection(
                    farmerPositions = listOf(TOP_LEFT, BOTTOM_LEFT, BOTTOM_RIGHT),
                    tileConnections = listOf(TLL, BLB, BLL, BRB),
                    citySides = listOf(TOP, RIGHT)
                )
            ),
            fieldSides = listOf(LEFT, BOTTOM)
        ))

        // N (garden)
        put("city_diagonal_top_right_flowers", TileDefinition(
            name = "city_diagonal_top_right_flowers",
            edges = EdgeProfile.of(top = CITY, right = CITY, bottom = FIELD, left = FIELD),
            cityZones = listOf(listOf(TOP, RIGHT)),
            farms = listOf(
                FarmerConnection(
                    farmerPositions = listOf(TOP_LEFT, BOTTOM_LEFT, BOTTOM_RIGHT),
                    tileConnections = listOf(TLL, BLB, BLL, BRB),
                    citySides = listOf(TOP, RIGHT)
                )
            ),
            flowers = true,
            fieldSides = listOf(LEFT, BOTTOM)
        ))

        // O - City diagonal top-left with shield, road
        put("city_diagonal_top_left_shield_road", TileDefinition(
            name = "city_diagonal_top_left_shield_road",
            edges = EdgeProfile.of(top = CITY, right = ROAD, bottom = ROAD, left = CITY),
            roads = listOf(Connection(BOTTOM, RIGHT)),
            cityZones = listOf(listOf(TOP, LEFT)),
            farms = listOf(
                FarmerConnection(
                    farmerPositions = listOf(BOTTOM_LEFT, TOP_RIGHT),
                    tileConnections = listOf(BLB, TRR),
                    citySides = listOf(TOP, LEFT)
                ),
                FarmerConnection(
                    farmerPositions = listOf(BOTTOM_RIGHT),
                    tileConnections = listOf(BRR, BRB)
                )
            ),
            shield = true
        ))

        // P - City diagonal top-left, road
        put("city_diagonal_top_left_road", TileDefinition(
            name = "city_diagonal_top_left_road",
            edges = EdgeProfile.of(top = CITY, right = ROAD, bottom = ROAD, left = CITY),
            roads = listOf(Connection(BOTTOM, RIGHT)),
            cityZones = listOf(listOf(TOP, LEFT)),
            farms = listOf(
                FarmerConnection(
                    farmerPositions = listOf(BOTTOM_LEFT, TOP_RIGHT),
                    tileConnections = listOf(BLB, TRR),
                    citySides = listOf(TOP, LEFT)
                ),
                FarmerConnection(
                    farmerPositions = listOf(BOTTOM_RIGHT),
                    tileConnections = listOf(BRR, BRB)
                )
            )
        ))

        // Q - City 3-sides with shield, grass bottom
        put("city_bottom_grass_shield", TileDefinition(
            name = "city_bottom_grass_shield",
            edges = EdgeProfile.of(top = CITY, right = CITY, bottom = FIELD, left = CITY),
            cityZones = listOf(listOf(TOP, LEFT, RIGHT)),
            farms = listOf(
                FarmerConnection(
                    farmerPositions = listOf(BOTTOM_LEFT, BOTTOM_RIGHT),
                    tileConnections = listOf(BLB, BRB),
                    citySides = listOf(TOP, LEFT, RIGHT)
                )
            ),
            shield = true,
            fieldSides = listOf(BOTTOM)
        ))

        // R - City 3-sides, grass bottom
        put("city_bottom_grass", TileDefinition(
            name = "city_bottom_grass",
            edges = EdgeProfile.of(top = CITY, right = CITY, bottom = FIELD, left = CITY),
            cityZones = listOf(listOf(TOP, LEFT, RIGHT)),
            farms = listOf(
                FarmerConnection(
                    farmerPositions = listOf(BOTTOM_LEFT, BOTTOM_RIGHT),
                    tileConnections = listOf(BLB, BRB),
                    citySides = listOf(TOP, LEFT, RIGHT)
                )
            ),
            fieldSides = listOf(BOTTOM)
        ))

        // R (garden)
        put("city_bottom_grass_flowers", TileDefinition(
            name = "city_bottom_grass_flowers",
            edges = EdgeProfile.of(top = CITY, right = CITY, bottom = FIELD, left = CITY),
            cityZones = listOf(listOf(TOP, LEFT, RIGHT)),
            farms = listOf(
                FarmerConnection(
                    farmerPositions = listOf(BOTTOM_LEFT, BOTTOM_RIGHT),
                    tileConnections = listOf(BLB, BRB),
                    citySides = listOf(TOP, LEFT, RIGHT)
                )
            ),
            flowers = true,
            fieldSides = listOf(BOTTOM)
        ))

        // S - City 3-sides with shield, road bottom
        put("city_bottom_road_shield", TileDefinition(
            name = "city_bottom_road_shield",
            edges = EdgeProfile.of(top = CITY, right = CITY, bottom = ROAD, left = CITY),
            roads = listOf(Connection(BOTTOM, BOTTOM)),
            cityZones = listOf(listOf(TOP, LEFT, RIGHT)),
            farms = listOf(
                FarmerConnection(
                    farmerPositions = listOf(BOTTOM_LEFT),
                    tileConnections = listOf(BLB),
                    citySides = listOf(TOP, LEFT, RIGHT)
                ),
                FarmerConnection(
                    farmerPositions = listOf(BOTTOM_RIGHT),
                    tileConnections = listOf(BRB),
                    citySides = listOf(TOP, LEFT, RIGHT)
                )
            ),
            shield = true
        ))

        // T - City 3-sides, road bottom
        put("city_bottom_road", TileDefinition(
            name = "city_bottom_road",
            edges = EdgeProfile.of(top = CITY, right = CITY, bottom = ROAD, left = CITY),
            roads = listOf(Connection(BOTTOM, BOTTOM)),
            cityZones = listOf(listOf(TOP, LEFT, RIGHT)),
            farms = listOf(
                FarmerConnection(
                    farmerPositions = listOf(BOTTOM_LEFT),
                    tileConnections = listOf(BLB),
                    citySides = listOf(TOP, LEFT, RIGHT)
                ),
                FarmerConnection(
                    farmerPositions = listOf(BOTTOM_RIGHT),
                    tileConnections = listOf(BRB),
                    citySides = listOf(TOP, LEFT, RIGHT)
                )
            )
        ))

        // U - Straight road
        put("straight_road", TileDefinition(
            name = "straight_road",
            edges = EdgeProfile.of(top = ROAD, right = FIELD, bottom = ROAD, left = FIELD),
            roads = listOf(Connection(BOTTOM, TOP)),
            farms = listOf(
                FarmerConnection(
                    farmerPositions = listOf(TOP_LEFT, BOTTOM_LEFT),
                    tileConnections = listOf(TLL, TLT, BLB, BLL)
                ),
                FarmerConnection(
                    farmerPositions = listOf(TOP_RIGHT, BOTTOM_RIGHT),
                    tileConnections = listOf(TRR, TRT, BRR, BRB)
                )
            ),
            fieldSides = listOf(LEFT, RIGHT)
        ))

        // U (garden)
        put("straight_road_flowers", TileDefinition(
            name = "straight_road_flowers",
            edges = EdgeProfile.of(top = ROAD, right = FIELD, bottom = ROAD, left = FIELD),
            roads = listOf(Connection(BOTTOM, TOP)),
            farms = listOf(
                FarmerConnection(
                    farmerPositions = listOf(TOP_LEFT, BOTTOM_LEFT),
                    tileConnections = listOf(TLL, TLT, BLB, BLL)
                ),
                FarmerConnection(
                    farmerPositions = listOf(TOP_RIGHT, BOTTOM_RIGHT),
                    tileConnections = listOf(TRR, TRT, BRR, BRB)
                )
            ),
            flowers = true,
            fieldSides = listOf(LEFT, RIGHT)
        ))

        // V - Bent road
        put("bent_road", TileDefinition(
            name = "bent_road",
            edges = EdgeProfile.of(top = FIELD, right = FIELD, bottom = ROAD, left = ROAD),
            roads = listOf(Connection(LEFT, BOTTOM)),
            farms = listOf(
                FarmerConnection(
                    farmerPositions = listOf(BOTTOM_LEFT),
                    tileConnections = listOf(BLB, BLL)
                ),
                FarmerConnection(
                    farmerPositions = listOf(TOP_LEFT, TOP_RIGHT, BOTTOM_RIGHT),
                    tileConnections = listOf(TLL, TLT, TRR, TRT, BRB, BRR)
                )
            ),
            fieldSides = listOf(TOP, RIGHT)
        ))

        // V (garden)
        put("bent_road_flowers", TileDefinition(
            name = "bent_road_flowers",
            edges = EdgeProfile.of(top = FIELD, right = FIELD, bottom = ROAD, left = ROAD),
            roads = listOf(Connection(LEFT, BOTTOM)),
            farms = listOf(
                FarmerConnection(
                    farmerPositions = listOf(BOTTOM_LEFT),
                    tileConnections = listOf(BLB, BLL)
                ),
                FarmerConnection(
                    farmerPositions = listOf(TOP_LEFT, TOP_RIGHT, BOTTOM_RIGHT),
                    tileConnections = listOf(TLL, TLT, TRR, TRT, BRB, BRR)
                )
            ),
            flowers = true,
            fieldSides = listOf(TOP, RIGHT)
        ))

        // W - Three-way road split
        put("three_split_road", TileDefinition(
            name = "three_split_road",
            edges = EdgeProfile.of(top = FIELD, right = ROAD, bottom = ROAD, left = ROAD),
            roads = listOf(
                Connection(BOTTOM, BOTTOM),
                Connection(LEFT, LEFT),
                Connection(RIGHT, RIGHT)
            ),
            farms = listOf(
                FarmerConnection(
                    farmerPositions = listOf(BOTTOM_LEFT),
                    tileConnections = listOf(BLB, BLL)
                ),
                FarmerConnection(
                    farmerPositions = listOf(BOTTOM_RIGHT),
                    tileConnections = listOf(BRB, BRR)
                ),
                FarmerConnection(
                    farmerPositions = listOf(TOP_LEFT, TOP_RIGHT),
                    tileConnections = listOf(TLL, TLT, TRR, TRT)
                )
            ),
            fieldSides = listOf(TOP)
        ))

        // X - Crossroads
        put("crossroads", TileDefinition(
            name = "crossroads",
            edges = EdgeProfile.of(top = ROAD, right = ROAD, bottom = ROAD, left = ROAD),
            roads = listOf(
                Connection(BOTTOM, BOTTOM),
                Connection(LEFT, LEFT),
                Connection(RIGHT, RIGHT),
                Connection(TOP, TOP)
            ),
            farms = listOf(
                FarmerConnection(
                    farmerPositions = listOf(BOTTOM_LEFT),
                    tileConnections = listOf(BLB, BLL)
                ),
                FarmerConnection(
                    farmerPositions = listOf(BOTTOM_RIGHT),
                    tileConnections = listOf(BRB, BRR)
                ),
                FarmerConnection(
                    farmerPositions = listOf(TOP_LEFT),
                    tileConnections = listOf(TLL, TLT)
                ),
                FarmerConnection(
                    farmerPositions = listOf(TOP_RIGHT),
                    tileConnections = listOf(TRR, TRT)
                )
            )
        ))
    }
}
