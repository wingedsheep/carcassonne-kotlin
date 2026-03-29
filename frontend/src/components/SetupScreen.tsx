import { useState } from 'react'
import type { CreateGameRequest } from '../types/game'

interface SetupScreenProps {
  onStart: (options: CreateGameRequest) => void
}

export function SetupScreen({ onStart }: SetupScreenProps) {
  const [players, setPlayers] = useState(2)
  const [aiCount, setAiCount] = useState(1)
  const [aiDepth, setAiDepth] = useState(2)

  const allAI = aiCount >= players

  const handleStart = () => {
    // AI players are the last N players (human is player 0 when not all-AI)
    const aiPlayers = Array.from(
      { length: Math.min(aiCount, players) },
      (_, i) => players - 1 - i
    )
    onStart({ players, aiPlayers, aiDepth })
  }

  return (
    <div style={{
      display: 'flex',
      flexDirection: 'column',
      alignItems: 'center',
      justifyContent: 'center',
      height: '100vh',
      gap: 32,
      background: '#0a0a14',
    }}>
      <div style={{ textAlign: 'center' }}>
        <h1 style={{
          fontSize: 56,
          fontWeight: 800,
          color: '#e94560',
          margin: 0,
          letterSpacing: -2,
        }}>
          Carcassonne
        </h1>
        <p style={{ color: '#6b7280', fontSize: 14, marginTop: 8 }}>
          A tile-placement board game
        </p>
      </div>

      <div style={{
        display: 'flex',
        flexDirection: 'column',
        gap: 16,
        width: 340,
        padding: 24,
        background: '#111827',
        borderRadius: 12,
        border: '1px solid #1f2937',
      }}>
        <SettingRow label="Players">
          <select style={selectStyle} value={players} onChange={e => {
            const n = Number(e.target.value)
            setPlayers(n)
            setAiCount(Math.min(aiCount, n))
          }}>
            {[2, 3, 4, 5].map(n => (
              <option key={n} value={n}>{n} players</option>
            ))}
          </select>
        </SettingRow>

        <SettingRow label="AI opponents">
          <select style={selectStyle} value={aiCount} onChange={e => setAiCount(Number(e.target.value))}>
            {Array.from({ length: players + 1 }, (_, i) => (
              <option key={i} value={i}>
                {i === 0 ? 'None (PvP)' : i === players ? `${i} AI (watch)` : `${i} AI`}
              </option>
            ))}
          </select>
        </SettingRow>

        {aiCount > 0 && (
          <SettingRow label="AI strength">
            <select style={selectStyle} value={aiDepth} onChange={e => setAiDepth(Number(e.target.value))}>
              <option value={1}>Easy (0.5s)</option>
              <option value={2}>Normal (2s)</option>
              <option value={3}>Hard (5s)</option>
            </select>
          </SettingRow>
        )}

        <button
          onClick={handleStart}
          style={{
            padding: '12px 24px',
            background: '#e94560',
            border: 'none',
            color: '#fff',
            fontSize: 18,
            fontWeight: 600,
            borderRadius: 8,
            cursor: 'pointer',
            marginTop: 8,
            transition: 'background 0.15s',
          }}
          onMouseEnter={e => (e.currentTarget.style.background = '#d63050')}
          onMouseLeave={e => (e.currentTarget.style.background = '#e94560')}
        >
          {allAI ? 'Watch AI Game' : aiCount > 0 ? 'Play vs AI' : 'Start Game'}
        </button>
      </div>
    </div>
  )
}

const selectStyle: React.CSSProperties = {
  padding: '6px 12px',
  background: '#1f2937',
  color: '#e5e7eb',
  border: '1px solid #374151',
  borderRadius: 6,
  fontSize: 14,
  cursor: 'pointer',
}

function SettingRow({ label, children }: { label: string; children: React.ReactNode }) {
  return (
    <label style={{
      display: 'flex',
      justifyContent: 'space-between',
      alignItems: 'center',
      fontSize: 14,
      color: '#e5e7eb',
    }}>
      {label}
      {children}
    </label>
  )
}

export { selectStyle }
