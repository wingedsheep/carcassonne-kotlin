import { useEffect, useState } from 'react'

const PLAYER_COLORS = ['#e94560', '#2563eb', '#16a34a', '#eab308', '#9333ea', '#f97316']

export interface ScoringEvent {
  id: number
  player: number
  points: number
  row: number
  col: number
}

interface ScoreAnimationProps {
  event: ScoringEvent
  tileSize: number
  onDone: (id: number) => void
}

export function ScoreAnimation({ event, tileSize, onDone }: ScoreAnimationProps) {
  const [visible, setVisible] = useState(false)

  useEffect(() => {
    // Trigger enter animation on next frame
    requestAnimationFrame(() => setVisible(true))
    const timer = setTimeout(() => onDone(event.id), 1500)
    return () => clearTimeout(timer)
  }, [event.id, onDone])

  const color = PLAYER_COLORS[event.player % PLAYER_COLORS.length]
  const x = event.col * tileSize + tileSize / 2
  const y = event.row * tileSize + tileSize / 2

  return (
    <div
      style={{
        position: 'absolute',
        left: x,
        top: y,
        transform: visible
          ? 'translate(-50%, -120%) scale(1)'
          : 'translate(-50%, -50%) scale(0.5)',
        opacity: visible ? 0 : 1,
        color,
        fontSize: tileSize * 0.45,
        fontWeight: 800,
        textShadow: `0 0 8px ${color}, 0 0 16px ${color}, 0 2px 4px rgba(0,0,0,0.8)`,
        pointerEvents: 'none',
        zIndex: 100,
        transition: 'transform 1.5s cubic-bezier(0.16, 1, 0.3, 1), opacity 1.5s ease-in',
        whiteSpace: 'nowrap',
        fontFamily: '-apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif',
      }}
    >
      +{event.points}
    </div>
  )
}
