import { useState } from 'react'
import type { CreateGameRequest } from '../types/game'

interface SetupScreenProps {
  onStart: (options: CreateGameRequest) => void
}

export function SetupScreen({ onStart }: SetupScreenProps) {
  const [players, setPlayers] = useState(2)
  const [aiCount, setAiCount] = useState(1)
  const [aiDepths, setAiDepths] = useState<number[]>([2, 2, 2, 2, 2])
  const [withFarmers, setWithFarmers] = useState(false)
  const [withAbbots, setWithAbbots] = useState(false)
  const [withBigMeeples, setWithBigMeeples] = useState(false)

  const allAI = aiCount >= players

  const handleStart = () => {
    // AI players are the last N players (human is player 0 when not all-AI)
    const aiPlayers = Array.from(
      { length: Math.min(aiCount, players) },
      (_, i) => players - 1 - i
    )
    // Build per-player depth map
    const depthMap: Record<number, number> = {}
    aiPlayers.forEach((playerIdx, i) => {
      depthMap[playerIdx] = aiDepths[i]
    })
    onStart({ players, aiPlayers, aiDepths: depthMap, withFarmers, withAbbots, withBigMeeples })
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
      backgroundImage: 'url(/background.jpeg)',
      backgroundSize: 'cover',
      backgroundPosition: 'center',
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
        <p style={{ color: '#ffffff', fontSize: 14, marginTop: 8, textShadow: '0 0 8px rgba(0,0,0,1), 0 0 16px rgba(0,0,0,0.8)' }}>
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
          <div style={{ display: 'flex', flexDirection: 'column', gap: 8 }}>
            {Array.from({ length: aiCount }, (_, i) => (
              <SettingRow key={i} label={aiCount === 1 ? 'AI strength' : `AI ${i + 1} strength`}>
                <select style={selectStyle} value={aiDepths[i]} onChange={e => {
                  const next = [...aiDepths]
                  next[i] = Number(e.target.value)
                  setAiDepths(next)
                }}>
                  <option value={1}>Easy</option>
                  <option value={2}>Normal</option>
                  <option value={3}>Hard</option>
                </select>
              </SettingRow>
            ))}
          </div>
        )}

        <div style={{ borderTop: '1px solid #1f2937', paddingTop: 12 }}>
          <div style={{ color: '#9ca3af', fontSize: 12, marginBottom: 8, textTransform: 'uppercase', letterSpacing: 1 }}>
            Rules
          </div>
          <div style={{ display: 'flex', flexDirection: 'column', gap: 10 }}>
            <Toggle label="Farmers" checked={withFarmers} onChange={setWithFarmers} />
            <Toggle label="Abbots" checked={withAbbots} onChange={setWithAbbots} />
            <Toggle label="Big meeples" checked={withBigMeeples} onChange={setWithBigMeeples} />
          </div>
        </div>

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

function Toggle({ label, checked, onChange }: { label: string; checked: boolean; onChange: (v: boolean) => void }) {
  return (
    <label style={{
      display: 'flex',
      justifyContent: 'space-between',
      alignItems: 'center',
      fontSize: 14,
      color: '#e5e7eb',
      cursor: 'pointer',
    }}>
      {label}
      <div
        onClick={() => onChange(!checked)}
        style={{
          width: 40,
          height: 22,
          borderRadius: 11,
          background: checked ? '#e94560' : '#374151',
          position: 'relative',
          transition: 'background 0.15s',
        }}
      >
        <div style={{
          width: 18,
          height: 18,
          borderRadius: '50%',
          background: '#fff',
          position: 'absolute',
          top: 2,
          left: checked ? 20 : 2,
          transition: 'left 0.15s',
        }} />
      </div>
    </label>
  )
}

export { selectStyle }
