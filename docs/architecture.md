# Carcassonne Engine Architecture

Kotlin reimplementation of the [Python Carcassonne engine](https://github.com/wingedsheep/carcassonne)
with a React web frontend. Designed for both human play and reinforcement learning simulation.

## Design goals

| Priority | Goal |
|----------|------|
| 1 | **Speed** - millions of forward-play simulations per second for RL |
| 2 | **Serializable state** - snapshot/restore for branching, undo/redo, persistence |
| 3 | **Extensibility** - new expansions (tiles, rules, meeple types) without core changes |
| 4 | **Correctness** - faithful to official Carcassonne rules |
| 5 | **Playability** - usable React UI for human vs human / human vs AI |

---

## Module layout

```
kotlin-carcassonne/
├── utils/          Game engine library (zero UI dependencies)
│   └── src/main/kotlin/com/wingedsheep/carcassonne/
│       ├── model/          Value types, enums, data classes
│       ├── tile/           Tile definitions & deck building
│       ├── engine/         State transitions, move generation, validation
│       └── scoring/        Points calculation, structure detection
│
├── app/            Web application (Ktor + React SPA)
│   ├── src/main/kotlin/com/wingedsheep/app/
│   │   ├── App.kt          Ktor server entry point
│   │   ├── GameRoutes.kt   REST API endpoints
│   │   └── GameSession.kt  Server-side game session management
│   └── src/main/resources/
│       └── static/         React frontend (HTML + JS + CSS)
│
└── docs/           This document and other design notes
```

The `utils` module has **no framework dependencies** beyond kotlinx-serialization.
It can be embedded in any JVM application, test harness, or RL training loop
without pulling in web server or UI code.

---

## Core model (`utils/model/`)

### Enums

```
Side            TOP, RIGHT, BOTTOM, LEFT
                (4 cardinal directions - used for tile edges and meeple positions)

CenterSide      CENTER
                (separate type - only for monasteries/flowers meeple placement)

FarmerSide      TLL, TLT, TRT, TRR, BRR, BRB, BLB, BLL
                (8 positions around tile edges for farm zone connectivity)

TerrainType     CITY, ROAD, FIELD, RIVER
                (what a tile edge presents to its neighbor)

MeepleType      NORMAL, BIG, FARMER, BIG_FARMER, ABBOT

GamePhase       TILE_PLACEMENT, MEEPLE_PLACEMENT
```

### Value types

All model types are **immutable data classes** annotated with `@Serializable`.

```kotlin
data class Coordinate(val row: Int, val col: Int)

data class Connection(val a: Side, val b: Side)       // road/river segment

data class FarmerConnection(
    val farmerPositions: List<Side>,       // where farmer meeple can stand
    val tileConnections: List<FarmerSide>, // which edge segments this farm touches
    val citySides: List<Side>             // adjacent city zones (for end-game scoring)
)
```

### TileDefinition (flyweight)

A `TileDefinition` describes a tile **type** - its terrain, connections, and features.
It is created once per unique tile and shared across all rotations and instances.

```kotlin
@Serializable
data class TileDefinition(
    val name: String,
    val edges: EdgeProfile,                     // terrain type per side (packed)
    val roads: List<Connection>,
    val rivers: List<Connection>,
    val cityZones: List<List<Side>>,            // groups of sides forming each city
    val fieldSides: List<Side>,
    val farms: List<FarmerConnection>,
    val shield: Boolean,
    val monastery: Boolean,                     // "chapel" in Python impl
    val flowers: Boolean,
    val inn: List<Side>,
    val cathedral: Boolean,
)
```

**`EdgeProfile`** packs the 4 edge terrain types into a single `Int` (2 bits each).
This enables O(1) tile-fitting checks via bitwise comparison.

```kotlin
@JvmInline
value class EdgeProfile(val packed: Int) {
    fun terrain(side: Side): TerrainType = ...
    fun rotated(times: Int): EdgeProfile = ...
    fun matchesSide(other: EdgeProfile, mySide: Side): Boolean = ...
}
```

### RotatedTile

Pre-computed rotated variants of a tile definition. Created once at startup.

```kotlin
class RotatedTile(
    val definition: TileDefinition,
    val rotation: Int,                   // 0..3
    val edges: EdgeProfile,              // pre-rotated
    val roads: List<Connection>,         // pre-rotated
    val rivers: List<Connection>,        // pre-rotated
    val cityZones: List<List<Side>>,     // pre-rotated
    val fieldSides: List<Side>,          // pre-rotated
    val farms: List<FarmerConnection>,   // pre-rotated
    val inn: List<Side>,                 // pre-rotated
)
```

All 4 rotations are pre-computed at tile registration time, so rotation during
gameplay is a lookup, not a computation.

### PlacedTile

What actually sits on the board - just a reference to a pre-computed rotation.

```kotlin
data class PlacedTile(
    val tileId: Int,       // index into tile registry
    val rotation: Int,     // 0..3
)
```

This is **8 bytes** per board cell. The full tile data is looked up from the
global tile registry when needed.

---

## Game state (`utils/engine/`)

### GameState

The complete, serializable game state. Designed for fast cloning.

```kotlin
@Serializable
data class GameState(
    val board: BoardMap,                                // sparse tile map
    val deck: List<Int>,                                // remaining tile IDs (shuffled)
    val deckIndex: Int,                                 // next tile to draw
    val currentPlayer: Int,
    val phase: GamePhase,
    val players: Int,
    val scores: IntArray,
    val meeples: Array<MeeplePool>,                     // available meeples per player
    val placedMeeples: Array<List<PlacedMeeple>>,       // meeples on board per player
    val lastPlacedCoordinate: Coordinate?,              // for meeple phase
    val lastPlacedTile: PlacedTile?,                    // for meeple phase
)
```

**`BoardMap`**: A `HashMap<Coordinate, PlacedTile>` wrapper. Sparse representation -
only stores occupied cells. Average game has ~72 tiles on a theoretical 143x143 grid.

**`MeeplePool`**: Tracks available counts per meeple type.

```kotlin
@Serializable
data class MeeplePool(
    var normal: Int = 7,
    var big: Int = 0,
    var abbot: Int = 0,
)
```

**`PlacedMeeple`**:

```kotlin
@Serializable
data class PlacedMeeple(
    val type: MeepleType,
    val coordinate: Coordinate,
    val side: Side?,            // null for farmers
    val farmerSide: FarmerSide?, // non-null only for farmers
)
```

### State cloning strategy

For RL branching, `GameState.copy()` must be fast:

- `board` (HashMap): shallow-copy the map, entries are immutable `PlacedTile` values
- `deck`: shared (immutable list, only `deckIndex` changes)
- `scores`, `meeples`: array copy (tiny - max ~6 players)
- `placedMeeples`: shallow-copy outer array, inner lists are small

Estimated clone cost: **~200ns** for a mid-game state. No deep recursion, no
serialization overhead.

For **serialization** (checkpointing, network transfer), kotlinx-serialization
to CBOR or ProtoBuf is available. JSON for debugging.

---

## Tile registry (`utils/tile/`)

### TileRegistry

Global registry of all tile definitions and their pre-computed rotations.

```kotlin
object TileRegistry {
    private val definitions: MutableList<TileDefinition>
    private val rotations: MutableList<Array<RotatedTile>>  // [tileId][0..3]

    fun register(definition: TileDefinition): Int   // returns tileId
    fun get(tileId: Int): TileDefinition
    fun getRotated(tileId: Int, rotation: Int): RotatedTile
}
```

### Deck building

```kotlin
object BaseDeck {
    fun tiles(): List<TileCount>    // (TileDefinition, count) pairs
}

object RiverDeck { ... }
object InnsAndCathedralsDeck { ... }

data class TileCount(val definition: TileDefinition, val count: Int)
```

Decks are just lists of `(definition, count)` pairs. The `GameState` builder
registers all definitions, expands counts, shuffles, and stores as `List<Int>` (tile IDs).

### Adding an expansion

1. Create a new `object MyExpansionDeck` with `fun tiles(): List<TileCount>`
2. Define tile definitions using the same `TileDefinition` constructor
3. Register the deck when building the game: `GameBuilder.withDeck(MyExpansionDeck)`
4. If the expansion adds new rules, implement a `ScoringRule` (see below)

No existing code needs to change.

---

## Engine (`utils/engine/`)

### Game lifecycle

```
GameBuilder
  .players(2)
  .withDeck(BaseDeck)
  .withDeck(InnsAndCathedralsDeck)     // optional
  .withRule(FarmersRule)               // optional
  .withRule(AbbotsRule)                // optional
  .build()
    → Game

Game.getState()            → GameState (read-only snapshot)
Game.getValidActions()     → List<Action>
Game.apply(action: Action) → Game (new state)
Game.isFinished()          → Boolean
```

### Actions (sealed hierarchy)

```kotlin
@Serializable
sealed interface Action

data class PlaceTile(
    val tileId: Int,
    val rotation: Int,
    val coordinate: Coordinate,
) : Action

data class PlaceMeeple(
    val meepleType: MeepleType,
    val coordinate: Coordinate,
    val side: Side?,
    val farmerSide: FarmerSide?,
) : Action

data object Pass : Action
```

### Turn flow

```
┌─────────────────┐
│  TILE_PLACEMENT  │  ← Game draws next tile, computes valid placements
│                  │  ← Player chooses PlaceTile or Pass (if no valid spot)
└────────┬────────┘
         │ tile placed on board
         ▼
┌─────────────────┐
│ MEEPLE_PLACEMENT │  ← Engine computes valid meeple spots on placed tile
│                  │  ← Player chooses PlaceMeeple or Pass
└────────┬────────┘
         │ score completed structures, return meeples
         │ advance to next player
         ▼
┌─────────────────┐
│  TILE_PLACEMENT  │  ← next player's turn (or game end if deck empty)
└─────────────────┘
```

### Tile fitting (`TileFitter`)

Determines whether a `RotatedTile` can legally be placed at a `Coordinate`.

```kotlin
object TileFitter {
    fun fits(board: BoardMap, tile: RotatedTile, coord: Coordinate): Boolean
}
```

**Algorithm**: For each of the 4 neighbors:
1. If neighbor is empty → skip (open edge is always legal)
2. If neighbor exists → compare edge terrains: `tile.edges.terrain(side)` must equal
   `neighbor.edges.terrain(side.opposite())`

With `EdgeProfile`, this is **4 integer comparisons** - no allocation, no iteration.

### Valid placement generation (`MoveGenerator`)

```kotlin
object MoveGenerator {
    fun validTilePlacements(state: GameState, tileId: Int): List<PlaceTile>
    fun validMeeplePlacements(state: GameState): List<Action>  // PlaceMeeple + Pass
}
```

**Tile placements**: Maintain a **frontier set** - coordinates adjacent to any placed
tile. For each frontier coordinate, try all 4 rotations of the current tile.
The frontier is incrementally updated when tiles are placed (add new neighbors,
remove filled coordinate).

**Meeple placements**: After a tile is placed, check each feature (city zone, road
segment, field side, monastery center) on that tile. For non-farmer meeples, use
structure detection to verify no existing meeple claims that structure. For farmers,
check the connected farm.

### State transitions (`StateUpdater`)

```kotlin
object StateUpdater {
    fun applyTilePlacement(state: GameState, action: PlaceTile): GameState
    fun applyMeeplePlacement(state: GameState, action: PlaceMeeple): GameState
    fun applyPass(state: GameState): GameState
}
```

Each method returns a **new** `GameState`. The old state is untouched.
This enables branching without explicit clone:

```kotlin
val state = game.getState()
for (action in game.getValidActions()) {
    val nextState = StateUpdater.apply(state, action)
    // evaluate nextState for RL without affecting original
}
```

---

## Structure detection (`utils/scoring/`)

### Approach: BFS flood fill (on demand)

Structures (cities, roads, farms) are detected by **flood fill from a starting
position** when needed - primarily during scoring and meeple validation.

```kotlin
object CityDetector {
    fun findCity(board: BoardMap, start: CoordinateWithSide): City
    fun findCitiesAt(board: BoardMap, coord: Coordinate): List<City>
}

object RoadDetector {
    fun findRoad(board: BoardMap, start: CoordinateWithSide): Road
    fun findRoadsAt(board: BoardMap, coord: Coordinate): List<Road>
}

object FarmDetector {
    fun findFarm(board: BoardMap, start: CoordinateWithFarmerSide): Farm
}
```

**City detection**: BFS starting from a city side. Follow city zones across tile
boundaries (city on Side.TOP connects to neighbor's Side.BOTTOM). A city is
**finished** when all edges connect to another city side (no open edges).

**Road detection**: BFS following road connections. A road is **finished** when both
endpoints either loop back or terminate at a junction/city.

**Farm detection**: BFS following `FarmerSide` connections across tile boundaries.
Farms are never "finished" - they're only scored at game end.

**Why BFS over Union-Find**: Union-Find is faster for repeated queries but doesn't
support the `GameState` immutability model (un-merge on undo is expensive). BFS
is simpler, correct, and fast enough - structure sizes are small (typically <20 tiles).
For RL hot paths, structure detection results can be cached per state if profiling
shows it's a bottleneck.

### Result types

```kotlin
data class City(
    val positions: Set<CoordinateWithSide>,
    val finished: Boolean,
    val shieldCount: Int,
    val hasCathedral: Boolean,
    val tileCount: Int,
)

data class Road(
    val positions: Set<CoordinateWithSide>,
    val finished: Boolean,
    val hasInn: Boolean,
    val tileCount: Int,
)

data class Farm(
    val connections: Set<CoordinateWithFarmerSide>,
    val adjacentCities: Set<City>,
)
```

### Scoring (`PointsCalculator`)

```kotlin
object PointsCalculator {
    fun scoreCompletedStructures(state: GameState, placedAt: Coordinate): ScoreResult
    fun scoreFinalState(state: GameState): IntArray
}

data class ScoreResult(
    val pointsPerPlayer: IntArray,
    val returnedMeeples: List<PlacedMeeple>,
)
```

**Scoring rules by structure type**:

| Structure | Finished | Unfinished | With Inn/Cathedral |
|-----------|----------|------------|-------------------|
| City | 2/tile + 2/shield | 1/tile + 1/shield | 3/tile + 3/shield (finished), 0 (unfinished) |
| Road | 1/tile | 1/tile | 2/tile (finished), 0 (unfinished) |
| Monastery | 1 + surrounding tiles (max 9) | 1 + surrounding tiles | N/A |
| Farm | 3 per adjacent finished city | (end-game only) | N/A |

**Meeple majority**: The player(s) with the most meeples in a structure score points.
Big meeples count as 2. If tied, nobody scores (following the Python implementation).

---

## Undo / redo & RL support

### Snapshot-based undo

```kotlin
class GameHistory {
    private val snapshots: ArrayDeque<GameState>

    fun push(state: GameState)     // before applying action
    fun undo(): GameState?         // pop and return previous state
    fun redo(): GameState?         // if available
}
```

For human play: push a snapshot before each action. Undo restores the previous snapshot.

For RL: no `GameHistory` needed. Just hold a reference to the state before branching.
The immutable `GameState` design means the old reference stays valid.

### Serialization

```kotlin
// Snapshot to bytes (fast, compact)
val bytes: ByteArray = GameStateSerializer.toBytes(state)
val restored: GameState = GameStateSerializer.fromBytes(bytes)

// Snapshot to JSON (debugging, network)
val json: String = GameStateSerializer.toJson(state)
```

### RL integration surface

```kotlin
// Minimal API for RL agents
val game = GameBuilder.players(2).withDeck(BaseDeck).build()

while (!game.isFinished()) {
    val actions = game.getValidActions()
    val chosen = agent.selectAction(game.getState(), actions)
    game.apply(chosen)
}

val scores = game.getState().scores
```

The engine allocates minimally during simulation. Key optimizations:
- `PlacedTile` is 8 bytes (2 ints)
- `EdgeProfile` matching is bitwise
- Pre-computed rotations avoid runtime tile transformation
- Frontier set avoids scanning the whole board for valid placements
- No logging or print statements in engine code (the Python impl prints during scoring)

---

## Web layer (`app/`)

### Server (Ktor)

```
POST /api/games                    → create game session
GET  /api/games/{id}               → current game state
GET  /api/games/{id}/actions       → valid actions for current player
POST /api/games/{id}/actions       → apply action
POST /api/games/{id}/undo          → undo last action
GET  /api/games/{id}/history       → action history
```

Game sessions are held in memory (server-side `Game` instances).

### React frontend

Single-page app served as static files from Ktor's `resources/static/`.

Components:
- **GameBoard** - renders placed tiles on a scrollable/zoomable grid
- **TilePreview** - shows current tile with rotation controls
- **MeepleOverlay** - highlights valid meeple positions on the placed tile
- **ScoreBoard** - player scores, meeple counts, turn indicator
- **ActionBar** - pass, undo, new game controls

The frontend communicates with the Ktor server via REST. No WebSocket needed
for local play (request-response is sufficient for turn-based games).

---

## Expansion points

The architecture supports future expansions through:

1. **New tile definitions**: Add a deck object, no engine changes
2. **New meeple types**: Add to `MeepleType` enum, extend `MeeplePool`, add placement validation in `MoveGenerator`
3. **New scoring rules**: Add a `ScoringRule` interface implementation, register with `GameBuilder`
4. **New game phases**: Extend `GamePhase` enum and turn flow in `StateUpdater` (e.g., for the dragon, fairy, tower)
5. **Board variants**: `BoardMap` is an interface - can be swapped for fixed-size array if needed

### Supported expansions (matching Python impl)

- **Base game** (72 tiles)
- **The River** (12 tiles, river placement rules)
- **Inns & Cathedrals** (18 tiles, big meeple, inn/cathedral scoring)
- **Abbots** (supplementary rule for monastery/flowers)
- **Farmers** (supplementary rule for farm scoring)

---

## Testing strategy

- **Unit tests per module**: tile rotation, edge matching, structure detection, scoring
- **Property-based tests**: random game simulation should never crash or produce invalid states
- **Regression tests**: replay known games from the Python implementation and verify identical scores
- **Performance benchmarks**: measure simulations/second for RL viability
