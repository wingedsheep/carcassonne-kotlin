package com.wingedsheep.app

import com.wingedsheep.carcassonne.ai.TreeSearchAI
import com.wingedsheep.carcassonne.engine.Game
import com.wingedsheep.carcassonne.engine.GameState
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

class GameSession(
    val game: Game,
    val aiPlayers: Map<Int, TreeSearchAI> = emptyMap(),
    val history: MutableList<GameState> = mutableListOf()
) {
    fun pushState() {
        history.add(game.getState().copy())
    }

    /** If the current player is AI, auto-play until it's a human's turn or the game ends. */
    fun runAiTurns() {
        if (isAllAI) return // don't auto-play all-AI games; use step instead
        var safety = 0
        while (!game.isFinished() && safety < 500) {
            val state = game.getState()
            val ai = aiPlayers[state.currentPlayer] ?: break // human's turn
            val action = ai.chooseAction(state)
            pushState()
            game.apply(action)
            safety++
        }
    }

    /** Play a single AI full turn (tile + meeple). Returns true if an AI turn was played. */
    fun stepOneAiTurn(): Boolean {
        if (game.isFinished()) return false
        val state = game.getState()
        val ai = aiPlayers[state.currentPlayer] ?: return false

        // Play tile phase
        val tileAction = ai.chooseAction(state)
        pushState()
        game.apply(tileAction)

        // Play meeple phase if still same player's turn
        if (!game.isFinished() && game.getState().currentPlayer == state.currentPlayer) {
            val meepleAction = ai.chooseAction(game.getState())
            pushState()
            game.apply(meepleAction)
        }

        return true
    }

    val isAllAI: Boolean get() = aiPlayers.size >= game.getState().playerCount
}

object GameSessionManager {
    private val sessions = ConcurrentHashMap<String, GameSession>()
    private val counter = AtomicInteger(0)

    fun create(game: Game, aiPlayers: Map<Int, TreeSearchAI> = emptyMap()): String {
        val id = "game-${counter.incrementAndGet()}"
        sessions[id] = GameSession(game, aiPlayers)
        return id
    }

    fun get(id: String): GameSession? = sessions[id]

    fun remove(id: String) { sessions.remove(id) }
}
