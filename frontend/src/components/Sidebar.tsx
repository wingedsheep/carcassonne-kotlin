import type { GameResponse } from '../types/game'
import { TileRenderer } from './TileRenderer'
import { PLAYER_COLORS } from './Meeple'

interface SidebarProps {
  game: GameResponse
  selectedRotation: number
  onRotationChange: (rotation: number) => void
  onAction: (index: number) => void
  onStep?: () => void
  autoPlay?: boolean
  onToggleAutoPlay?: () => void
  onConcede?: () => void
}

export function Sidebar({ game, selectedRotation, onRotationChange, onAction, onStep, autoPlay, onToggleAutoPlay, onConcede }: SidebarProps) {
  const { phase, currentPlayer, currentTileRotations, validActions, playerMeeples, aiPlayerIndices } = game
  const passAction = validActions.find(a => a.type === 'pass')
  const previewTile = currentTileRotations?.[selectedRotation]
  const isCurrentAI = aiPlayerIndices?.includes(currentPlayer)

  return (
    <div style={{
      width: 250,
      background: '#111827',
      borderLeft: '1px solid #1f2937',
      padding: 16,
      overflowY: 'auto',
      flexShrink: 0,
      display: 'flex',
      flexDirection: 'column',
      gap: 16,
    }}>
      {/* Current player */}
      <div>
        <SectionTitle>Current Turn</SectionTitle>
        <div style={{
          display: 'flex', alignItems: 'center', gap: 8, marginTop: 6,
        }}>
          <div style={{
            width: 12, height: 12, borderRadius: '50%',
            background: PLAYER_COLORS[currentPlayer],
          }} />
          <span style={{ fontSize: 14, fontWeight: 500 }}>
            {isCurrentAI ? `AI (Player ${currentPlayer + 1})` : `Player ${currentPlayer + 1}`}
          </span>
        </div>
        {playerMeeples[currentPlayer] && (
          <div style={{ fontSize: 12, color: '#9ca3af', marginTop: 4 }}>
            Meeples: {playerMeeples[currentPlayer].normal} normal
            {playerMeeples[currentPlayer].big > 0 && `, ${playerMeeples[currentPlayer].big} big`}
            {playerMeeples[currentPlayer].abbot > 0 && `, ${playerMeeples[currentPlayer].abbot} abbot`}
          </div>
        )}
      </div>

      {/* Tile preview */}
      {phase === 'TILE_PLACEMENT' && previewTile && (
        <div>
          <SectionTitle>Current Tile</SectionTitle>
          <div style={{ display: 'flex', flexDirection: 'column', alignItems: 'center', gap: 10, marginTop: 8 }}>
            <div style={{
              border: '2px solid #374151',
              borderRadius: 6,
              overflow: 'hidden',
              lineHeight: 0,
            }}>
              <TileRenderer name={previewTile.name} rotation={previewTile.rotation} size={120} />
            </div>
            <div style={{ display: 'flex', gap: 8, alignItems: 'center' }}>
              <RotateButton onClick={() => onRotationChange((selectedRotation + 3) % 4)}>
                &#x21ba;
              </RotateButton>
              <span style={{ fontSize: 13, color: '#9ca3af', minWidth: 32, textAlign: 'center' }}>
                {selectedRotation * 90}&deg;
              </span>
              <RotateButton onClick={() => onRotationChange((selectedRotation + 1) % 4)}>
                &#x21bb;
              </RotateButton>
            </div>
            <div style={{ fontSize: 11, color: '#6b7280' }}>
              {previewTile.name.replace(/_/g, ' ')}
            </div>
          </div>
        </div>
      )}

      {/* Meeple phase instructions */}
      {phase === 'MEEPLE_PLACEMENT' && (
        <div>
          <SectionTitle>Place Meeple</SectionTitle>
          <p style={{ fontSize: 12, color: '#9ca3af', marginTop: 6, lineHeight: 1.5 }}>
            Click a highlighted spot on the board to place a meeple, or pass.
          </p>
        </div>
      )}

      {/* Pass button */}
      {passAction && (
        <button
          onClick={() => onAction(passAction.index)}
          style={{
            padding: '10px 16px',
            background: '#7c3aed',
            border: 'none',
            color: '#fff',
            borderRadius: 6,
            cursor: 'pointer',
            fontSize: 14,
            fontWeight: 500,
            transition: 'background 0.15s',
          }}
          onMouseEnter={e => (e.currentTarget.style.background = '#6d28d9')}
          onMouseLeave={e => (e.currentTarget.style.background = '#7c3aed')}
        >
          {phase === 'MEEPLE_PLACEMENT' ? 'Skip Meeple' : 'Pass'}
        </button>
      )}

      {/* AI watch controls */}
      {game.isAllAI && !game.isFinished && (
        <div style={{ display: 'flex', flexDirection: 'column', gap: 8 }}>
          <SectionTitle>AI Watch Mode</SectionTitle>
          <div style={{ display: 'flex', gap: 8 }}>
            <button
              onClick={onStep}
              style={{
                flex: 1, padding: '10px 12px',
                background: '#7c3aed', border: 'none', color: '#fff',
                borderRadius: 6, cursor: 'pointer', fontSize: 13, fontWeight: 500,
              }}
            >
              Step
            </button>
            <button
              onClick={onToggleAutoPlay}
              style={{
                flex: 1, padding: '10px 12px',
                background: autoPlay ? '#dc2626' : '#059669',
                border: 'none', color: '#fff',
                borderRadius: 6, cursor: 'pointer', fontSize: 13, fontWeight: 500,
              }}
            >
              {autoPlay ? 'Pause' : 'Auto'}
            </button>
          </div>
        </div>
      )}

      {/* Concede */}
      {!game.isFinished && (
        <button
          onClick={onConcede}
          style={{
            padding: '10px 12px',
            background: 'transparent', border: '1px solid #4b5563', color: '#9ca3af',
            borderRadius: 6, cursor: 'pointer', fontSize: 13, fontWeight: 500,
          }}
          onMouseEnter={e => { e.currentTarget.style.background = '#7f1d1d'; e.currentTarget.style.borderColor = '#dc2626'; e.currentTarget.style.color = '#fca5a5' }}
          onMouseLeave={e => { e.currentTarget.style.background = 'transparent'; e.currentTarget.style.borderColor = '#4b5563'; e.currentTarget.style.color = '#9ca3af' }}
        >
          Concede
        </button>
      )}

      {/* Keyboard shortcuts */}
      <div style={{ marginTop: 'auto' }}>
        <SectionTitle>Controls</SectionTitle>
        <div style={{ fontSize: 11, color: '#6b7280', lineHeight: 1.8, marginTop: 4 }}>
          {game.isAllAI ? (
            <>
              <div><Kbd>Space</Kbd> &mdash; Step one turn</div>
              <div><Kbd>A</Kbd> &mdash; Toggle auto-play</div>
            </>
          ) : (
            <>
              <div><Kbd>Q</Kbd> / <Kbd>E</Kbd> &mdash; Rotate tile</div>
              <div><Kbd>Space</Kbd> &mdash; Pass</div>
            </>
          )}
          <div>Scroll &mdash; Zoom</div>
          <div>Drag &mdash; Pan</div>
        </div>
      </div>
    </div>
  )
}

function SectionTitle({ children }: { children: React.ReactNode }) {
  return (
    <h3 style={{
      fontSize: 11, color: '#6b7280', textTransform: 'uppercase',
      letterSpacing: 1.5, fontWeight: 600, margin: 0,
    }}>
      {children}
    </h3>
  )
}

function RotateButton({ children, onClick }: { children: React.ReactNode; onClick: () => void }) {
  return (
    <button
      onClick={onClick}
      style={{
        padding: '4px 14px',
        background: '#1f2937',
        border: '1px solid #374151',
        color: '#e5e7eb',
        borderRadius: 4,
        cursor: 'pointer',
        fontSize: 16,
        lineHeight: 1,
      }}
    >
      {children}
    </button>
  )
}

function Kbd({ children }: { children: React.ReactNode }) {
  return (
    <kbd style={{
      padding: '1px 5px', background: '#1f2937', border: '1px solid #374151',
      borderRadius: 3, fontSize: 10, fontFamily: 'inherit',
    }}>
      {children}
    </kbd>
  )
}
