const PLAYER_COLORS = ['#e94560', '#2563eb', '#16a34a', '#eab308', '#9333ea', '#f97316']

const MEEPLE_COLOR_NAMES = ['red', 'blue', 'green', 'yellow', 'pink', 'black']

interface MeepleProps {
  player: number
  x: number
  y: number
  size?: number
  title?: string
  type?: string
}

export function Meeple({ player, x, y, size = 24, title, type }: MeepleProps) {
  const colorName = MEEPLE_COLOR_NAMES[player % MEEPLE_COLOR_NAMES.length]
  const isAbbot = type === 'ABBOT'
  const isFarmer = type === 'FARMER' || type === 'BIG_FARMER'
  const isBig = type === 'BIG' || type === 'BIG_FARMER'
  const src = `/meeples/${colorName}_${isAbbot ? 'abbot' : 'meeple'}.png`
  const displaySize = isBig ? size * 1.4 : size

  return (
    <img
      src={src}
      alt={title ?? `Player ${player + 1} meeple`}
      title={title}
      draggable={false}
      style={{
        position: 'absolute',
        left: x,
        top: y,
        width: displaySize,
        height: displaySize,
        transform: `translate(-50%, -50%)${isFarmer ? ' rotate(90deg)' : ''}`,
        zIndex: 5,
        pointerEvents: 'none',
        filter:
          'drop-shadow(0 0 1.5px white) drop-shadow(0 0 1.5px white) drop-shadow(0 1px 3px rgba(0,0,0,0.6))',
      }}
    />
  )
}

// Garden positions in base image coordinates (fractions of tile size).
// These override the default center position for abbot/garden placements.
const GARDEN_POSITIONS: Record<string, { x: number; y: number }> = {
  city_top_flowers:                        { x: 0.32, y: 0.65 },
  city_top_bottom_flowers:                 { x: 0.69, y: 0.48 },
  city_top_left_flowers:                   { x: 0.65, y: 0.65 },
  city_diagonal_top_right_shield_flowers:  { x: 0.31, y: 0.69 },
  city_diagonal_top_right_flowers:         { x: 0.31, y: 0.69 },
  city_bottom_grass_flowers:               { x: 0.44, y: 0.65 },
  straight_road_flowers:                   { x: 0.27, y: 0.60 },
  bent_road_flowers:                       { x: 0.25, y: 0.29 },
}

/**
 * Rotate a point in base image coords by R * 90° CW around the tile center,
 * matching CSS rotate() behavior.
 */
function rotatePoint(x: number, y: number, rotation: number, size: number): { x: number; y: number } {
  const r = ((rotation % 4) + 4) % 4
  switch (r) {
    case 0: return { x, y }
    case 1: return { x: size - y, y: x }
    case 2: return { x: size - x, y: size - y }
    case 3: return { x: y, y: size - x }
    default: return { x, y }
  }
}

export function getMeeplePosition(
  side: string | null,
  farmerSide: string | null,
  tileSize: number,
  tileName?: string,
  tileRotation?: number,
): { x: number; y: number } {
  const mid = tileSize / 2
  const edge = tileSize * 0.18

  if (farmerSide) {
    const positions: Record<string, { x: number; y: number }> = {
      TLL: { x: edge * 0.6, y: mid - edge },
      TLT: { x: mid - edge, y: edge * 0.6 },
      TRT: { x: mid + edge, y: edge * 0.6 },
      TRR: { x: tileSize - edge * 0.6, y: mid - edge },
      BRR: { x: tileSize - edge * 0.6, y: mid + edge },
      BRB: { x: mid + edge, y: tileSize - edge * 0.6 },
      BLB: { x: mid - edge, y: tileSize - edge * 0.6 },
      BLL: { x: edge * 0.6, y: mid + edge },
    }
    return positions[farmerSide] ?? { x: mid, y: mid }
  }

  // Garden/monastery: no side
  if (!side) {
    if (tileName && tileRotation != null) {
      const gardenFrac = GARDEN_POSITIONS[tileName]
      if (gardenFrac) {
        return rotatePoint(
          gardenFrac.x * tileSize,
          gardenFrac.y * tileSize,
          tileRotation,
          tileSize,
        )
      }
    }
    return { x: mid, y: mid }
  }

  const positions: Record<string, { x: number; y: number }> = {
    TOP: { x: mid, y: edge },
    RIGHT: { x: tileSize - edge, y: mid },
    BOTTOM: { x: mid, y: tileSize - edge },
    LEFT: { x: edge, y: mid },
    TOP_LEFT: { x: edge, y: edge },
    TOP_RIGHT: { x: tileSize - edge, y: edge },
    BOTTOM_LEFT: { x: edge, y: tileSize - edge },
    BOTTOM_RIGHT: { x: tileSize - edge, y: tileSize - edge },
  }
  return positions[side] ?? { x: mid, y: mid }
}

export { PLAYER_COLORS }
