import type { GameResponse, MeeplePoolInfo } from '../types/game'
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
  selectedMeepleType: string
  onMeepleTypeChange: (type: string) => void
  gameConfig: { withBigMeeples: boolean; withAbbots: boolean; withFarmers: boolean }
}

export function Sidebar({ game, selectedRotation, onRotationChange, onAction, onStep, autoPlay, onToggleAutoPlay, onConcede, selectedMeepleType, onMeepleTypeChange, gameConfig }: SidebarProps) {
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
          <MeeplePool pool={playerMeeples[currentPlayer]} gameConfig={gameConfig} />
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

      {/* Meeple type selector */}
      {phase === 'MEEPLE_PLACEMENT' && (
        <div>
          <SectionTitle>Place Meeple</SectionTitle>
          <p style={{ fontSize: 12, color: '#9ca3af', marginTop: 6, marginBottom: 10, lineHeight: 1.5 }}>
            Select a meeple type, then click a spot on the board.
          </p>
          <MeepleTypeSelector
            pool={playerMeeples[currentPlayer]}
            validActions={validActions}
            selectedType={selectedMeepleType}
            onSelect={onMeepleTypeChange}
            gameConfig={gameConfig}
          />
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
              <div><Kbd>1</Kbd> <Kbd>2</Kbd> <Kbd>3</Kbd> &mdash; Meeple type</div>
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

function MeeplePool({ pool, gameConfig }: { pool: MeeplePoolInfo; gameConfig: { withBigMeeples: boolean; withAbbots: boolean } }) {
  return (
    <div style={{ display: 'flex', gap: 12, marginTop: 8 }}>
      <MeeplePoolItem label="Normal" current={pool.normal} max={7} icon="/meeples/red_meeple.png" />
      {gameConfig.withBigMeeples && (
        <MeeplePoolItem label="Big" current={pool.big} max={1} icon="/meeples/red_meeple.png" big />
      )}
      {gameConfig.withAbbots && (
        <MeeplePoolItem label="Abbot" current={pool.abbot} max={1} icon="/meeples/red_abbot.png" />
      )}
    </div>
  )
}

function MeeplePoolItem({ label, current, max, icon, big }: { label: string; current: number; max: number; icon: string; big?: boolean }) {
  return (
    <div style={{ display: 'flex', flexDirection: 'column', alignItems: 'center', gap: 2 }}>
      <img
        src={icon}
        alt={label}
        style={{
          width: big ? 28 : 20,
          height: big ? 28 : 20,
          filter: 'drop-shadow(0 0 1px white) drop-shadow(0 0 1px white)',
          opacity: current > 0 ? 1 : 0.3,
        }}
        draggable={false}
      />
      <span style={{
        fontSize: 11,
        color: current > 0 ? '#e5e7eb' : '#4b5563',
        fontWeight: 600,
        fontVariantNumeric: 'tabular-nums',
      }}>
        {current}/{max}
      </span>
      <span style={{ fontSize: 9, color: '#6b7280', textTransform: 'uppercase', letterSpacing: 0.5 }}>
        {label}
      </span>
    </div>
  )
}

function MeepleTypeSelector({ pool, validActions, selectedType, onSelect, gameConfig }: {
  pool: MeeplePoolInfo
  validActions: GameResponse['validActions']
  selectedType: string
  onSelect: (type: string) => void
  gameConfig: { withBigMeeples: boolean; withAbbots: boolean; withFarmers: boolean }
}) {
  const meepleActions = validActions.filter(a => a.type === 'place_meeple' && !a.remove)
  const hasNormal = meepleActions.some(a => a.meepleType === 'NORMAL' || a.meepleType === 'FARMER')
  const hasBig = meepleActions.some(a => a.meepleType === 'BIG' || a.meepleType === 'BIG_FARMER')
  const hasAbbot = meepleActions.some(a => a.meepleType === 'ABBOT')

  const types: { key: string; label: string; available: boolean; enabled: boolean; icon: string; big?: boolean }[] = [
    { key: 'NORMAL', label: `Normal (${pool.normal})`, available: hasNormal, enabled: pool.normal > 0, icon: '/meeples/red_meeple.png' },
  ]
  if (gameConfig.withBigMeeples) {
    types.push({ key: 'BIG', label: `Big (${pool.big})`, available: hasBig, enabled: pool.big > 0, icon: '/meeples/red_meeple.png', big: true })
  }
  if (gameConfig.withAbbots) {
    types.push({ key: 'ABBOT', label: `Abbot (${pool.abbot})`, available: hasAbbot, enabled: pool.abbot > 0, icon: '/meeples/red_abbot.png' })
  }

  return (
    <div style={{ display: 'flex', gap: 6 }}>
      {types.map(t => {
        const isSelected = selectedType === t.key
        const canUse = t.available && t.enabled
        return (
          <button
            key={t.key}
            onClick={() => canUse && onSelect(t.key)}
            style={{
              flex: 1,
              display: 'flex',
              flexDirection: 'column',
              alignItems: 'center',
              gap: 4,
              padding: '8px 4px',
              background: isSelected ? 'rgba(233, 69, 96, 0.2)' : '#1f2937',
              border: isSelected ? '2px solid #e94560' : '2px solid #374151',
              borderRadius: 8,
              cursor: canUse ? 'pointer' : 'not-allowed',
              opacity: canUse ? 1 : 0.35,
              transition: 'all 0.15s',
            }}
          >
            <img
              src={t.icon}
              alt={t.key}
              style={{
                width: t.big ? 28 : 22,
                height: t.big ? 28 : 22,
                filter: 'drop-shadow(0 0 1px white) drop-shadow(0 0 1px white)',
              }}
              draggable={false}
            />
            <span style={{ fontSize: 10, color: '#e5e7eb', fontWeight: 500 }}>
              {t.label}
            </span>
          </button>
        )
      })}
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
