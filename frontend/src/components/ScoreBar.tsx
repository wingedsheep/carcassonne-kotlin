import type { GameResponse } from '../types/game'
import { PLAYER_COLORS } from './Meeple'

interface ScoreBarProps {
  game: GameResponse
}

export function ScoreBar({ game }: ScoreBarProps) {
  const { scores, currentPlayer, phase, playerMeeples, aiPlayerIndices } = game
  const isAI = (i: number) => aiPlayerIndices?.includes(i)
  return (
    <div style={{
      display: 'flex',
      justifyContent: 'space-between',
      alignItems: 'center',
      padding: '8px 16px',
      background: '#111827',
      borderBottom: '1px solid #1f2937',
      flexShrink: 0,
      zIndex: 10,
    }}>
      <h1 style={{ fontSize: 18, fontWeight: 700, color: '#e94560', margin: 0 }}>
        Carcassonne
      </h1>

      <div style={{ display: 'flex', gap: 12, alignItems: 'center' }}>
        {scores.map((score, i) => (
          <div
            key={i}
            style={{
              padding: '4px 14px',
              borderRadius: 6,
              fontWeight: 600,
              fontSize: 14,
              background: PLAYER_COLORS[i % PLAYER_COLORS.length],
              color: '#fff',
              outline: i === currentPlayer ? '2px solid #fff' : 'none',
              outlineOffset: 2,
              display: 'flex',
              alignItems: 'center',
              gap: 6,
            }}
          >
            <span>{isAI(i) ? 'AI' : `P${i + 1}`}: {score}</span>
            {playerMeeples[i] && (
              <span style={{ fontSize: 11, opacity: 0.8 }}>
                ({playerMeeples[i].normal}m)
              </span>
            )}
          </div>
        ))}
      </div>

      <div style={{ display: 'flex', gap: 8, alignItems: 'center', fontSize: 13 }}>
        <span style={{
          padding: '3px 10px',
          borderRadius: 4,
          background: phase === 'TILE_PLACEMENT' ? '#7c3aed' : '#0891b2',
          fontSize: 12,
          fontWeight: 500,
        }}>
          {phase === 'TILE_PLACEMENT' ? 'Place Tile' : 'Place Meeple'}
        </span>
      </div>
    </div>
  )
}
