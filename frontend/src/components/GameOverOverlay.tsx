import { PLAYER_COLORS } from './Meeple'

interface GameOverOverlayProps {
  scores: number[]
  onNewGame: () => void
}

export function GameOverOverlay({ scores, onNewGame }: GameOverOverlayProps) {
  const maxScore = Math.max(...scores)
  const winners = scores
    .map((s, i) => ({ score: s, player: i }))
    .filter(p => p.score === maxScore)

  return (
    <div style={{
      position: 'fixed',
      inset: 0,
      background: 'rgba(0, 0, 0, 0.8)',
      display: 'flex',
      alignItems: 'center',
      justifyContent: 'center',
      zIndex: 100,
      backdropFilter: 'blur(4px)',
    }}>
      <div style={{
        background: '#111827',
        padding: '40px 48px',
        borderRadius: 16,
        textAlign: 'center',
        border: '2px solid #e94560',
        minWidth: 360,
      }}>
        <h2 style={{ fontSize: 32, color: '#e94560', margin: '0 0 8px' }}>
          Game Over!
        </h2>
        <p style={{ color: '#9ca3af', fontSize: 14, margin: '0 0 24px' }}>
          {winners.length === 1
            ? `Player ${winners[0].player + 1} wins!`
            : `Tie between ${winners.map(w => `P${w.player + 1}`).join(' & ')}!`}
        </p>

        <div style={{
          display: 'flex',
          gap: 12,
          justifyContent: 'center',
          marginBottom: 28,
        }}>
          {scores.map((score, i) => (
            <div
              key={i}
              style={{
                padding: '14px 22px',
                borderRadius: 8,
                fontSize: 20,
                fontWeight: 700,
                background: PLAYER_COLORS[i % PLAYER_COLORS.length],
                color: '#fff',
                border: score === maxScore ? '2px solid #ffd700' : '2px solid transparent',
              }}
            >
              P{i + 1}: {score}
            </div>
          ))}
        </div>

        <button
          onClick={onNewGame}
          style={{
            padding: '12px 32px',
            background: '#e94560',
            border: 'none',
            color: '#fff',
            fontSize: 16,
            fontWeight: 600,
            borderRadius: 8,
            cursor: 'pointer',
            transition: 'background 0.15s',
          }}
          onMouseEnter={e => (e.currentTarget.style.background = '#d63050')}
          onMouseLeave={e => (e.currentTarget.style.background = '#e94560')}
        >
          New Game
        </button>
      </div>
    </div>
  )
}
