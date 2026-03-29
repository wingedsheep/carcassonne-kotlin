export type TerrainType = 'CITY' | 'ROAD' | 'FIELD' | 'RIVER'
export type GamePhase = 'TILE_PLACEMENT' | 'MEEPLE_PLACEMENT'
export type SideName = 'TOP' | 'RIGHT' | 'BOTTOM' | 'LEFT'

export interface Edges {
  top: TerrainType
  right: TerrainType
  bottom: TerrainType
  left: TerrainType
}

export interface BoardTile {
  row: number
  col: number
  tileId: number
  rotation: number
  name: string
  edges: Edges
  monastery: boolean
  shield: boolean
  flowers: boolean
  cityZones: string[][]
  roads: string[][]
}

export interface TileInfo {
  tileId: number
  rotation: number
  name: string
  edges: Edges
}

export interface ActionResponse {
  type: 'place_tile' | 'place_meeple' | 'pass'
  tileId?: number
  rotation?: number
  row?: number
  col?: number
  meepleType?: string
  side?: string
  farmerSide?: string
  remove?: boolean
  index: number
}

export interface MeepleOnBoard {
  player: number
  type: string
  row: number
  col: number
  side: string | null
  farmerSide: string | null
}

export interface MeeplePoolInfo {
  normal: number
  big: number
  abbot: number
}

export interface GameResponse {
  id: string
  currentPlayer: number
  phase: GamePhase
  isFinished: boolean
  scores: number[]
  board: BoardTile[]
  currentTileId: number | null
  currentTileRotations: TileInfo[] | null
  validActions: ActionResponse[]
  meeplesOnBoard: MeepleOnBoard[]
  playerMeeples: MeeplePoolInfo[]
  aiPlayerIndices: number[]
  isAllAI: boolean
  lastPlacedRow: number | null
  lastPlacedCol: number | null
}

export interface CreateGameRequest {
  players: number
  withFarmers?: boolean
  withAbbots?: boolean
  withBigMeeples?: boolean
  aiPlayers?: number[]
  aiDepth?: number
  aiDepths?: Record<number, number>
}
