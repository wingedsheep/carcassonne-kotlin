# Carcassonne - Kotlin/React

Kotlin reimplementation of the [Python Carcassonne engine](https://github.com/wingedsheep/carcassonne) with a React web frontend.

## Performance

The engine is designed for AI and reinforcement learning workloads where millions of games need to be simulated.

![Benchmark results](benchmark.png)

- **~750+ games/sec** on a single thread (~1.3ms per game)
- **~17 avg branching factor** (valid actions per step), peaking at 60+
- **~2400 moves considered** per game across ~142 steps
- **5 turns lookahead** in 5s with alpha-beta + PVS/LMR search (~29k nodes explored)

The immutable `GameState` design means branching during tree search requires no deep copy — just reference the previous state. This makes the engine particularly efficient for MCTS, AlphaZero-style self-play, and other search-based RL agents that require fast environment rollouts.

### Greedy vs Competitive: evaluation strategy comparison

The evaluator supports a `differentialWeight` parameter that blends between maximizing own score (d=0.0, "Greedy") and maximizing score difference (d=1.0, "Competitive"). Head-to-head results over 40 games per matchup:

![Evaluator comparison](evaluator-comparison.png)

Competitive agents that account for the opponent's score consistently beat the greedy point-maximizer — both in win rate (up to 80%) and in raw score. Playing to *win* outperforms playing to *score*.

## Architecture

See [docs/architecture.md](docs/architecture.md) for the full architecture document.

**Key design decisions:**
- Immutable `GameState` for safe RL branching (no deep copy needed)
- Pre-computed tile rotations (4 variants per tile definition, created at startup)
- `EdgeProfile` packs 4 edge terrain types into a single Int for O(1) tile fitting
- Sparse board (`HashMap<Coordinate, PlacedTile>`) - no fixed-size array
- Frontier set for valid placement generation (no full board scan)
- BFS flood fill for structure detection (cities, roads, farms)
- kotlinx-serialization for state snapshots (CBOR for speed, JSON for debugging)

## Project structure

- `utils/` - Game engine library. Zero UI dependencies. Package: `com.wingedsheep.carcassonne`
- `app/` - Ktor web server + React SPA. Package: `com.wingedsheep.app`
- `frontend/` - React SPA (Vite + TypeScript)
- `docs/` - Architecture and design documents

## Build & run

```bash
./gradlew build          # build everything
./gradlew :utils:test    # run engine tests
./gradlew run            # start web server
```

## Conventions

- All model types are `@Serializable` data classes
- Engine code must not print or log (no side effects)
- State transitions return new `GameState`, never mutate
- Tile definitions go in `utils/.../tile/` as deck objects
- Tests live next to source in `src/test/kotlin/`
