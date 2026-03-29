import { useState, useEffect, useCallback, useRef } from 'react'
import type { GameResponse, CreateGameRequest } from './types/game'
import { createGame, applyAction, stepAI } from './api/client'
import { SetupScreen } from './components/SetupScreen'
import { GameBoard } from './components/GameBoard'
import { ScoreBar } from './components/ScoreBar'
import { Sidebar } from './components/Sidebar'
import { GameOverOverlay } from './components/GameOverOverlay'
import type { ScoringEvent } from './components/ScoreAnimation'

let scoringEventId = 0

export default function App() {
  const [game, setGame] = useState<GameResponse | null>(null)
  const [selectedRotation, setSelectedRotation] = useState(0)
  const [loading, setLoading] = useState(false)
  const [autoPlay, setAutoPlay] = useState(false)
  const autoPlayRef = useRef(false)
  const [scoringEvents, setScoringEvents] = useState<ScoringEvent[]>([])
  const prevScoresRef = useRef<number[] | null>(null)
  const prevLastPlacedRef = useRef<{ row: number | null; col: number | null }>({ row: null, col: null })
  const [selectedMeepleType, setSelectedMeepleType] = useState<string>('NORMAL')
  const [gameConfig, setGameConfig] = useState<{ withBigMeeples: boolean; withAbbots: boolean; withFarmers: boolean }>({ withBigMeeples: false, withAbbots: false, withFarmers: false })

  const detectScoringEvents = useCallback((newGame: GameResponse) => {
    const prevScores = prevScoresRef.current
    // Use the new game's lastPlaced if available, otherwise fall back to what we saved from before the action
    const row = newGame.lastPlacedRow ?? prevLastPlacedRef.current.row
    const col = newGame.lastPlacedCol ?? prevLastPlacedRef.current.col
    if (prevScores && row != null && col != null) {
      const newEvents: ScoringEvent[] = []
      for (let p = 0; p < newGame.scores.length; p++) {
        const delta = newGame.scores[p] - (prevScores[p] ?? 0)
        if (delta > 0) {
          newEvents.push({ id: ++scoringEventId, player: p, points: delta, row, col })
        }
      }
      if (newEvents.length > 0) {
        setScoringEvents(prev => [...prev, ...newEvents])
      }
    }
    prevScoresRef.current = [...newGame.scores]
    prevLastPlacedRef.current = { row: newGame.lastPlacedRow, col: newGame.lastPlacedCol }
  }, [])

  const handleScoringEventDone = useCallback((id: number) => {
    setScoringEvents(prev => prev.filter(e => e.id !== id))
  }, [])

  const handleStart = async (options: CreateGameRequest) => {
    setLoading(true)
    try {
      const data = await createGame(options)
      prevScoresRef.current = [...data.scores]
      prevLastPlacedRef.current = { row: data.lastPlacedRow, col: data.lastPlacedCol }
      setScoringEvents([])
      setGame(data)
      setSelectedRotation(0)
      setSelectedMeepleType('NORMAL')
      setGameConfig({ withBigMeeples: !!options.withBigMeeples, withAbbots: !!options.withAbbots, withFarmers: !!options.withFarmers })
    } finally {
      setLoading(false)
    }
  }

  const handleAction = useCallback(async (actionIndex: number) => {
    if (!game || loading) return
    setLoading(true)
    try {
      const data = await applyAction(game.id, actionIndex)
      detectScoringEvents(data)
      setGame(data)
      if (data.phase === 'TILE_PLACEMENT') {
        setSelectedRotation(0)
        setSelectedMeepleType('NORMAL')
      }
    } finally {
      setLoading(false)
    }
  }, [game, loading, detectScoringEvents])

  const handleStep = useCallback(async () => {
    if (!game || loading) return
    setLoading(true)
    try {
      const data = await stepAI(game.id)
      detectScoringEvents(data)
      setGame(data)
    } finally {
      setLoading(false)
    }
  }, [game, loading, detectScoringEvents])

  const handleNewGame = () => {
    setAutoPlay(false)
    autoPlayRef.current = false
    setGame(null)
    setSelectedRotation(0)
  }

  const toggleAutoPlay = useCallback(() => {
    setAutoPlay(prev => {
      autoPlayRef.current = !prev
      return !prev
    })
  }, [])

  // Auto-step AI turns: triggers when it's an AI player's turn
  // For mixed games, runs immediately after the human's move is shown.
  // For all-AI auto-play, uses a delay for watchability.
  useEffect(() => {
    if (!game || game.isFinished || loading) return
    const isAiTurn = game.aiPlayerIndices?.includes(game.currentPlayer)
    if (!isAiTurn) return
    if (game.isAllAI && !autoPlay) return // all-AI needs auto-play enabled

    const delay = game.isAllAI ? 800 : 400
    const timer = setTimeout(async () => {
      if (game.isAllAI && !autoPlayRef.current) return
      try {
        setLoading(true)
        const data = await stepAI(game.id)
        detectScoringEvents(data)
        setGame(data)
      } finally {
        setLoading(false)
      }
    }, delay)

    return () => clearTimeout(timer)
  }, [game, autoPlay, loading])

  // Keyboard shortcuts
  useEffect(() => {
    if (!game) return

    const handleKey = (e: KeyboardEvent) => {
      if (e.key === 'q' || e.key === 'Q') {
        setSelectedRotation(r => (r + 3) % 4)
      } else if (e.key === 'e' || e.key === 'E') {
        setSelectedRotation(r => (r + 1) % 4)
      } else if (e.key === ' ') {
        e.preventDefault()
        if (game.isAllAI) {
          if (game.isFinished) return
          handleStep()
        } else {
          const passAction = game.validActions.find(a => a.type === 'pass')
          if (passAction) handleAction(passAction.index)
        }
      } else if (e.key === 'a' || e.key === 'A') {
        if (game.isAllAI) toggleAutoPlay()
      } else if (e.key === '1') {
        setSelectedMeepleType('NORMAL')
      } else if (e.key === '2') {
        setSelectedMeepleType('BIG')
      } else if (e.key === '3') {
        setSelectedMeepleType('ABBOT')
      }
    }

    window.addEventListener('keydown', handleKey)
    return () => window.removeEventListener('keydown', handleKey)
  }, [game, handleAction, handleStep, toggleAutoPlay])

  if (!game) {
    return <SetupScreen onStart={handleStart} />
  }

  return (
    <div style={{
      display: 'flex',
      flexDirection: 'column',
      height: '100vh',
      background: '#0a0a14',
      color: '#e5e7eb',
      fontFamily: '-apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif',
    }}>
      <ScoreBar game={game} />

      <div style={{ display: 'flex', flex: 1, overflow: 'hidden' }}>
        <GameBoard
          game={game}
          selectedRotation={selectedRotation}
          selectedMeepleType={selectedMeepleType}
          onAction={handleAction}
          scoringEvents={scoringEvents}
          onScoringEventDone={handleScoringEventDone}
        />
        <Sidebar
          game={game}
          selectedRotation={selectedRotation}
          onRotationChange={setSelectedRotation}
          onAction={handleAction}
          onStep={handleStep}
          autoPlay={autoPlay}
          onToggleAutoPlay={toggleAutoPlay}
          onConcede={handleNewGame}
          selectedMeepleType={selectedMeepleType}
          onMeepleTypeChange={setSelectedMeepleType}
          gameConfig={gameConfig}
        />
      </div>

      {game.isFinished && (
        <GameOverOverlay scores={game.scores} onNewGame={handleNewGame} />
      )}

      {loading && (
        <div style={{
          position: 'fixed',
          top: 0, left: 0, right: 0,
          height: 2,
          background: '#e94560',
          animation: 'pulse 0.8s infinite',
          zIndex: 1000,
        }} />
      )}
    </div>
  )
}
