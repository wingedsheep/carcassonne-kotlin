import { useRef, useState, useEffect, useCallback } from 'react'
import type { GameResponse } from '../types/game'
import { TileRenderer } from './TileRenderer'
import { Meeple, getMeeplePosition } from './Meeple'
import { ScoreAnimation } from './ScoreAnimation'
import type { ScoringEvent } from './ScoreAnimation'

const TILE_SIZE = 80

interface GameBoardProps {
  game: GameResponse
  selectedRotation: number
  onAction: (index: number) => void
  scoringEvents: ScoringEvent[]
  onScoringEventDone: (id: number) => void
}

export function GameBoard({ game, selectedRotation, onAction, scoringEvents, onScoringEventDone }: GameBoardProps) {
  const containerRef = useRef<HTMLDivElement>(null)
  const [offset, setOffset] = useState({ x: 0, y: 0 })
  const [zoom, setZoom] = useState(1)
  const [dragging, setDragging] = useState(false)
  const dragStart = useRef<{ x: number; y: number } | null>(null)
  const [centered, setCentered] = useState(false)
  const [mousePos, setMousePos] = useState<{ x: number; y: number } | null>(null)
  const [hoveredCell, setHoveredCell] = useState<{ row: number; col: number } | null>(null)

  const { board, phase, validActions, currentTileRotations, meeplesOnBoard, currentPlayer, aiPlayerIndices, lastPlacedRow, lastPlacedCol } = game
  const isHumanTurn = !aiPlayerIndices.includes(currentPlayer)

  // Center on initial render
  useEffect(() => {
    if (containerRef.current && board.length > 0 && !centered) {
      const rect = containerRef.current.getBoundingClientRect()
      const minRow = Math.min(...board.map(t => t.row))
      const maxRow = Math.max(...board.map(t => t.row))
      const minCol = Math.min(...board.map(t => t.col))
      const maxCol = Math.max(...board.map(t => t.col))
      const centerRow = (minRow + maxRow) / 2
      const centerCol = (minCol + maxCol) / 2
      setOffset({
        x: rect.width / 2 - centerCol * TILE_SIZE - TILE_SIZE / 2,
        y: rect.height / 2 - centerRow * TILE_SIZE - TILE_SIZE / 2,
      })
      setCentered(true)
    }
  }, [board, centered])

  // Pan
  const handleMouseDown = useCallback((e: React.MouseEvent) => {
    if (e.button !== 0) return
    const target = e.target as HTMLElement
    if (target.closest('[data-clickable]')) return
    setDragging(true)
    dragStart.current = { x: e.clientX - offset.x, y: e.clientY - offset.y }
  }, [offset])

  const handleMouseMove = useCallback((e: React.MouseEvent) => {
    if (!dragging || !dragStart.current) return
    setOffset({
      x: e.clientX - dragStart.current.x,
      y: e.clientY - dragStart.current.y,
    })
  }, [dragging])

  const handleMouseUp = useCallback(() => {
    setDragging(false)
    dragStart.current = null
  }, [])

  // Zoom
  const handleWheel = useCallback((e: React.WheelEvent) => {
    e.preventDefault()
    const delta = e.deltaY > 0 ? 0.92 : 1.08
    setZoom(z => Math.max(0.25, Math.min(3, z * delta)))
  }, [])

  // Valid placements for the selected rotation
  const tilePlacements = phase === 'TILE_PLACEMENT'
    ? validActions.filter(a => a.type === 'place_tile' && a.rotation === selectedRotation)
    : []

  // Build a lookup set for valid placement cells
  const validCellKeys = new Set(tilePlacements.map(a => `${a.row},${a.col}`))

  const meeplePlacements = phase === 'MEEPLE_PLACEMENT'
    ? validActions.filter(a => a.type === 'place_meeple' && !a.remove)
    : []

  // Deduplicate meeple placements by position (show only NORMAL type spots)
  const uniqueMeeplePlacements = meeplePlacements.filter(a => a.meepleType === 'NORMAL')

  const previewTile = currentTileRotations?.[selectedRotation]

  // Store values in refs so the mousemove handler doesn't need to be recreated
  const phaseRef = useRef(phase)
  phaseRef.current = phase
  const previewTileRef = useRef(previewTile)
  previewTileRef.current = previewTile
  const offsetRef = useRef(offset)
  offsetRef.current = offset
  const zoomRef = useRef(zoom)
  zoomRef.current = zoom
  const validCellKeysRef = useRef(validCellKeys)
  validCellKeysRef.current = validCellKeys
  const isHumanTurnRef = useRef(isHumanTurn)
  isHumanTurnRef.current = isHumanTurn

  // Track mouse for cursor tile preview
  const handleMouseMoveBoard = useCallback((e: React.MouseEvent) => {
    handleMouseMove(e)
    if (!isHumanTurnRef.current || phaseRef.current !== 'TILE_PLACEMENT' || !previewTileRef.current) {
      setMousePos(null)
      setHoveredCell(null)
      return
    }
    const rect = containerRef.current?.getBoundingClientRect()
    if (!rect) return
    const ox = offsetRef.current.x
    const oy = offsetRef.current.y
    const z = zoomRef.current
    const boardX = (e.clientX - rect.left - ox) / z
    const boardY = (e.clientY - rect.top - oy) / z
    const col = Math.floor(boardX / TILE_SIZE)
    const row = Math.floor(boardY / TILE_SIZE)
    const key = `${row},${col}`
    if (validCellKeysRef.current.has(key)) {
      setHoveredCell({ row, col })
      setMousePos(null)
    } else {
      setHoveredCell(null)
      setMousePos({ x: boardX, y: boardY })
    }
  }, [handleMouseMove])

  const handleMouseLeaveBoard = useCallback(() => {
    handleMouseUp()
    setMousePos(null)
    setHoveredCell(null)
  }, [handleMouseUp])

  // Determine cursor style
  const showTileCursor = isHumanTurn && phase === 'TILE_PLACEMENT' && !!previewTile
  let cursorStyle: string
  if (dragging) cursorStyle = 'grabbing'
  else if (hoveredCell) cursorStyle = 'pointer'
  else if (showTileCursor) cursorStyle = 'none'
  else cursorStyle = 'grab'

  return (
    <div
      ref={containerRef}
      style={{
        flex: 1,
        overflow: 'hidden',
        position: 'relative',
        cursor: cursorStyle,
        background: '#0a0a14',
      }}
      onMouseDown={handleMouseDown}
      onMouseMove={handleMouseMoveBoard}
      onMouseUp={handleMouseUp}
      onMouseLeave={handleMouseLeaveBoard}
      onWheel={handleWheel}
    >
      {/* Board layer */}
      <div style={{
        position: 'absolute',
        transformOrigin: '0 0',
        transform: `translate(${offset.x}px, ${offset.y}px) scale(${zoom})`,
      }}>
        {/* Placed tiles */}
        {board.map((tile, i) => {
          const isLastPlaced = tile.row === lastPlacedRow && tile.col === lastPlacedCol
          return (
            <div
              key={`tile-${i}`}
              style={{
                position: 'absolute',
                left: tile.col * TILE_SIZE,
                top: tile.row * TILE_SIZE,
                width: TILE_SIZE,
                height: TILE_SIZE,
              }}
            >
              <TileRenderer
                name={tile.name}
                rotation={tile.rotation}
                size={TILE_SIZE}
              />
              {isLastPlaced && (
                <div style={{
                  position: 'absolute',
                  inset: 0,
                  border: '2.5px solid rgba(255, 255, 255, 0.8)',
                  borderRadius: 2,
                  boxShadow: '0 0 8px rgba(255, 255, 255, 0.4), inset 0 0 8px rgba(255, 255, 255, 0.15)',
                  pointerEvents: 'none',
                }} />
              )}
            </div>
          )
        })}

        {/* Meeples on board */}
        {meeplesOnBoard.map((m, i) => {
          const pos = getMeeplePosition(m.side, m.farmerSide, TILE_SIZE)
          return (
            <Meeple
              key={`meeple-${i}`}
              player={m.player}
              x={m.col * TILE_SIZE + pos.x}
              y={m.row * TILE_SIZE + pos.y}
              type={m.type}
              title={`Player ${m.player + 1} - ${m.type}`}
            />
          )
        })}

        {/* Valid tile placement markers */}
        {tilePlacements.map((action, i) => {
          const isHovered = hoveredCell?.row === action.row && hoveredCell?.col === action.col
          return (
            <div
              key={`placement-${i}`}
              data-clickable
              style={{
                position: 'absolute',
                left: action.col! * TILE_SIZE,
                top: action.row! * TILE_SIZE,
                width: TILE_SIZE,
                height: TILE_SIZE,
                cursor: 'pointer',
                borderRadius: 3,
                overflow: 'hidden',
              }}
              onClick={() => onAction(action.index)}
            >
              {/* Show full tile preview when hovered */}
              {isHovered && previewTile && (
                <TileRenderer name={previewTile.name} rotation={previewTile.rotation} size={TILE_SIZE} opacity={0.85} />
              )}
              {/* Subtle marker */}
              <div style={{
                position: 'absolute',
                inset: 0,
                border: isHovered
                  ? '2px solid rgba(233, 69, 96, 0.9)'
                  : '1.5px dashed rgba(233, 69, 96, 0.35)',
                borderRadius: 3,
                background: isHovered
                  ? 'rgba(233, 69, 96, 0.1)'
                  : 'transparent',
              }} />
            </div>
          )
        })}

        {/* Valid meeple placements */}
        {uniqueMeeplePlacements.map((action, i) => {
          const pos = getMeeplePosition(action.side ?? null, action.farmerSide ?? null, TILE_SIZE)
          return (
            <div
              key={`mspot-${i}`}
              data-clickable
              style={{
                position: 'absolute',
                left: action.col! * TILE_SIZE + pos.x - 10,
                top: action.row! * TILE_SIZE + pos.y - 10,
                width: 20,
                height: 20,
                borderRadius: '50%',
                border: '2.5px solid #e94560',
                background: 'rgba(233, 69, 96, 0.25)',
                cursor: 'pointer',
                zIndex: 10,
                transition: 'background 0.15s, transform 0.15s',
              }}
              onClick={() => onAction(action.index)}
              onMouseEnter={e => {
                e.currentTarget.style.background = 'rgba(233, 69, 96, 0.55)'
                e.currentTarget.style.transform = 'scale(1.2)'
              }}
              onMouseLeave={e => {
                e.currentTarget.style.background = 'rgba(233, 69, 96, 0.25)'
                e.currentTarget.style.transform = 'scale(1)'
              }}
              title={action.side ?? action.farmerSide ?? 'center'}
            />
          )
        })}
        {/* Floating tile at cursor */}
        {previewTile && mousePos && !dragging && (
          <div
            style={{
              position: 'absolute',
              left: mousePos.x - TILE_SIZE / 2,
              top: mousePos.y - TILE_SIZE / 2,
              width: TILE_SIZE,
              height: TILE_SIZE,
              pointerEvents: 'none',
              opacity: 0.7,
              filter: 'drop-shadow(0 0 6px rgba(233, 69, 96, 0.5))',
            }}
          >
            <TileRenderer name={previewTile.name} rotation={previewTile.rotation} size={TILE_SIZE} />
          </div>
        )}

        {/* Scoring animations */}
        {scoringEvents.map(event => (
          <ScoreAnimation
            key={event.id}
            event={event}
            tileSize={TILE_SIZE}
            onDone={onScoringEventDone}
          />
        ))}
      </div>

      {/* Zoom controls */}
      <div style={{
        position: 'absolute', bottom: 12, right: 12,
        display: 'flex', gap: 4, zIndex: 20,
      }}>
        <ZoomButton onClick={() => setZoom(z => Math.min(3, z * 1.25))}>+</ZoomButton>
        <ZoomButton onClick={() => setZoom(z => Math.max(0.25, z / 1.25))}>-</ZoomButton>
        <ZoomButton onClick={() => {
          setZoom(1)
          setCentered(false) // re-trigger centering
        }}>&#x2302;</ZoomButton>
      </div>
    </div>
  )
}

function ZoomButton({ children, onClick }: { children: React.ReactNode; onClick: () => void }) {
  return (
    <button
      onClick={onClick}
      data-clickable
      style={{
        width: 32, height: 32,
        background: '#111827', border: '1px solid #374151',
        color: '#e5e7eb', borderRadius: 4, cursor: 'pointer',
        fontSize: 16, display: 'flex', alignItems: 'center', justifyContent: 'center',
      }}
    >
      {children}
    </button>
  )
}
