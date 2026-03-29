import type { CreateGameRequest, GameResponse } from '../types/game'

const API = '/api/games'

export async function createGame(options: CreateGameRequest): Promise<GameResponse> {
  const res = await fetch(API, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(options),
  })
  if (!res.ok) throw new Error(`Create game failed: ${res.status}`)
  return res.json()
}

export async function applyAction(gameId: string, actionIndex: number): Promise<GameResponse> {
  const res = await fetch(`${API}/${gameId}/action`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ actionIndex }),
  })
  if (!res.ok) throw new Error(`Apply action failed: ${res.status}`)
  return res.json()
}

export async function getGame(gameId: string): Promise<GameResponse> {
  const res = await fetch(`${API}/${gameId}`)
  if (!res.ok) throw new Error(`Get game failed: ${res.status}`)
  return res.json()
}

export async function stepAI(gameId: string): Promise<GameResponse> {
  const res = await fetch(`${API}/${gameId}/step`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
  })
  if (!res.ok) throw new Error(`Step AI failed: ${res.status}`)
  return res.json()
}
