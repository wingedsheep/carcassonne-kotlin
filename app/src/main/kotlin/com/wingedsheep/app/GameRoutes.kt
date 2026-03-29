package com.wingedsheep.app

import com.wingedsheep.carcassonne.ai.TreeSearchAI
import com.wingedsheep.carcassonne.engine.Game
import com.wingedsheep.carcassonne.engine.GameState
import com.wingedsheep.carcassonne.model.*
import com.wingedsheep.carcassonne.tile.TileRegistry
import io.ktor.http.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.Serializable

@Serializable
data class CreateGameRequest(
    val players: Int = 2,
    val withFarmers: Boolean = false,
    val withAbbots: Boolean = false,
    val withBigMeeples: Boolean = false,
    val aiPlayers: List<Int> = emptyList(),
    val aiDepth: Int = 1,
)

@Serializable
data class GameResponse(
    val id: String,
    val currentPlayer: Int,
    val phase: GamePhase,
    val isFinished: Boolean,
    val scores: List<Int>,
    val board: List<BoardTileResponse>,
    val currentTileId: Int?,
    val currentTileRotations: List<TileInfoResponse>?,
    val validActions: List<ActionResponse>,
    val meeplesOnBoard: List<MeepleOnBoardResponse>,
    val playerMeeples: List<MeeplePoolResponse>,
    val aiPlayerIndices: List<Int> = emptyList(),
    val isAllAI: Boolean = false,
)

@Serializable
data class BoardTileResponse(
    val row: Int,
    val col: Int,
    val tileId: Int,
    val rotation: Int,
    val name: String,
    val edges: EdgesResponse,
    val monastery: Boolean,
    val shield: Boolean,
    val flowers: Boolean,
    val cityZones: List<List<String>>,
    val roads: List<List<String>>,
)

@Serializable
data class EdgesResponse(val top: String, val right: String, val bottom: String, val left: String)

@Serializable
data class TileInfoResponse(
    val tileId: Int,
    val rotation: Int,
    val name: String,
    val edges: EdgesResponse,
)

@Serializable
data class ActionResponse(
    val type: String,
    val tileId: Int? = null,
    val rotation: Int? = null,
    val row: Int? = null,
    val col: Int? = null,
    val meepleType: String? = null,
    val side: String? = null,
    val farmerSide: String? = null,
    val remove: Boolean? = null,
    val index: Int = 0,
)

@Serializable
data class ApplyActionRequest(val actionIndex: Int)

@Serializable
data class MeepleOnBoardResponse(
    val player: Int,
    val type: String,
    val row: Int,
    val col: Int,
    val side: String?,
    val farmerSide: String?,
)

@Serializable
data class MeeplePoolResponse(
    val normal: Int,
    val big: Int,
    val abbot: Int,
)

fun Route.gameRoutes() {
    route("/api/games") {
        post {
            val request = call.receive<CreateGameRequest>()
            val builder = Game.builder().players(request.players).withBaseDeck()
            if (request.withFarmers) builder.withRule(SupplementaryRule.FARMERS)
            if (request.withAbbots) builder.withRule(SupplementaryRule.ABBOTS)
            if (request.withBigMeeples) builder.withBigMeeples()
            val game = builder.build()

            val aiPlayers = request.aiPlayers.associateWith { playerIdx ->
                TreeSearchAI(
                    player = playerIdx,
                    timeLimitMs = when (request.aiDepth) {
                        1 -> 500   // Easy: fast, shallow
                        2 -> 2000  // Normal: moderate thinking
                        else -> 5000  // Hard: deep search
                    },
                )
            }

            val id = GameSessionManager.create(game, aiPlayers)
            val session = GameSessionManager.get(id)!!
            session.runAiTurns()
            call.respond(buildGameResponse(id, session))
        }

        get("/{id}") {
            val id = call.parameters["id"] ?: return@get call.respond(HttpStatusCode.BadRequest)
            val session = GameSessionManager.get(id)
                ?: return@get call.respond(HttpStatusCode.NotFound)
            call.respond(buildGameResponse(id, session))
        }

        post("/{id}/step") {
            val id = call.parameters["id"] ?: return@post call.respond(HttpStatusCode.BadRequest)
            val session = GameSessionManager.get(id)
                ?: return@post call.respond(HttpStatusCode.NotFound)
            session.stepOneAiTurn()
            call.respond(buildGameResponse(id, session))
        }

        post("/{id}/action") {
            val id = call.parameters["id"] ?: return@post call.respond(HttpStatusCode.BadRequest)
            val session = GameSessionManager.get(id)
                ?: return@post call.respond(HttpStatusCode.NotFound)
            val request = call.receive<ApplyActionRequest>()
            val actions = session.game.getValidActions()
            if (request.actionIndex !in actions.indices) {
                return@post call.respond(HttpStatusCode.BadRequest, "Invalid action index")
            }
            session.pushState()
            session.game.apply(actions[request.actionIndex])
            call.respond(buildGameResponse(id, session))
        }
    }
}

private fun buildGameResponse(id: String, session: GameSession): GameResponse {
    val game = session.game
    val state = game.getState()
    val actions = if (state.isFinished) emptyList() else game.getValidActions()

    return GameResponse(
        id = id,
        currentPlayer = state.currentPlayer,
        phase = state.phase,
        isFinished = state.isFinished,
        scores = state.scores.toList(),
        board = buildBoardResponse(state),
        currentTileId = state.currentTileId,
        currentTileRotations = state.currentTileId?.let { tileId ->
            (0..3).map { rot ->
                val rt = TileRegistry.getRotated(tileId, rot)
                TileInfoResponse(
                    tileId = tileId,
                    rotation = rot,
                    name = rt.name,
                    edges = EdgesResponse(
                        top = rt.terrainAt(Side.TOP).name,
                        right = rt.terrainAt(Side.RIGHT).name,
                        bottom = rt.terrainAt(Side.BOTTOM).name,
                        left = rt.terrainAt(Side.LEFT).name,
                    ),
                )
            }
        },
        validActions = actions.mapIndexed { index, action -> actionToResponse(action, index) },
        meeplesOnBoard = buildMeeplesResponse(state),
        playerMeeples = (0 until state.playerCount).map { player ->
            MeeplePoolResponse(
                normal = state.meeples[player].normal,
                big = state.meeples[player].big,
                abbot = state.meeples[player].abbot,
            )
        },
        aiPlayerIndices = session.aiPlayers.keys.toList(),
        isAllAI = session.isAllAI,
    )
}

private fun buildBoardResponse(state: GameState): List<BoardTileResponse> {
    return state.board.allCoordinates().map { coord ->
        val placed = state.board[coord]!!
        val rt = TileRegistry.getRotated(placed.tileId, placed.rotation)
        BoardTileResponse(
            row = coord.row,
            col = coord.col,
            tileId = placed.tileId,
            rotation = placed.rotation,
            name = rt.name,
            edges = EdgesResponse(
                top = rt.terrainAt(Side.TOP).name,
                right = rt.terrainAt(Side.RIGHT).name,
                bottom = rt.terrainAt(Side.BOTTOM).name,
                left = rt.terrainAt(Side.LEFT).name,
            ),
            monastery = rt.monastery,
            shield = rt.shield,
            flowers = rt.flowers,
            cityZones = rt.cityZones.map { zone -> zone.map { it.name } },
            roads = rt.roads.map { conn -> listOf(conn.a.name, conn.b.name) },
        )
    }
}

private fun buildMeeplesResponse(state: GameState): List<MeepleOnBoardResponse> {
    val result = mutableListOf<MeepleOnBoardResponse>()
    for (player in 0 until state.playerCount) {
        for (meeple in state.placedMeeples[player]) {
            result.add(MeepleOnBoardResponse(
                player = player,
                type = meeple.type.name,
                row = meeple.coordinate.row,
                col = meeple.coordinate.col,
                side = meeple.side?.name,
                farmerSide = meeple.farmerSide?.name,
            ))
        }
    }
    return result
}

private fun actionToResponse(action: Action, index: Int): ActionResponse = when (action) {
    is PlaceTile -> ActionResponse(
        type = "place_tile",
        tileId = action.tileId,
        rotation = action.rotation,
        row = action.coordinate.row,
        col = action.coordinate.col,
        index = index,
    )
    is PlaceMeeple -> ActionResponse(
        type = "place_meeple",
        meepleType = action.meepleType.name,
        row = action.coordinate.row,
        col = action.coordinate.col,
        side = action.side?.name,
        farmerSide = action.farmerSide?.name,
        remove = action.remove,
        index = index,
    )
    is Pass -> ActionResponse(type = "pass", index = index)
}
