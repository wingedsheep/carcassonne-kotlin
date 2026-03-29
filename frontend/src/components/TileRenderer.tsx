import { getTileImageUrl, getTileRotationOffset } from './tileImages'

interface TileRendererProps {
  name?: string
  rotation?: number
  size?: number
  opacity?: number
}

export function TileRenderer({
  name,
  rotation = 0,
  size = 80,
  opacity = 1,
}: TileRendererProps) {
  const imageUrl = name ? getTileImageUrl(name) : null

  if (!imageUrl) {
    return (
      <div style={{
        width: size,
        height: size,
        opacity,
        background: '#4a8c3f',
        border: '1px solid #2a2a2a',
        display: 'block',
      }} />
    )
  }

  const rotationOffset = name ? getTileRotationOffset(name) : 0
  const totalRotation = (rotation + rotationOffset) * 90

  return (
    <div style={{
      width: size,
      height: size,
      opacity,
      overflow: 'hidden',
      lineHeight: 0,
    }}>
      <img
        src={imageUrl}
        alt={name ?? 'tile'}
        width={size}
        height={size}
        draggable={false}
        style={{
          display: 'block',
          transform: totalRotation ? `rotate(${totalRotation}deg)` : undefined,
          imageRendering: size <= 80 ? 'auto' : undefined,
        }}
      />
    </div>
  )
}
