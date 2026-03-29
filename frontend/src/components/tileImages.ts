// Maps Kotlin tile names to Python image filenames and rotation offsets.
// Most tiles share the same base orientation (offset 0).
// Garden variants of H and I have different base orientations in the Kotlin code.

interface TileImageInfo {
  file: string
  rotationOffset: number // extra 90° CW rotations to align image with Kotlin orientation
}

const TILE_IMAGE_MAP: Record<string, TileImageInfo> = {
  // A
  chapel_with_road: { file: 'Base_Game_C2_Tile_A.png', rotationOffset: 0 },
  // B
  chapel: { file: 'Base_Game_C2_Tile_B.png', rotationOffset: 0 },
  // C
  full_city_with_shield: { file: 'Base_Game_C2_Tile_C.png', rotationOffset: 0 },
  // D (starting tile)
  city_top_straight_road: { file: 'Base_Game_C2_Tile_D.png', rotationOffset: 0 },
  // E
  city_top: { file: 'Base_Game_C2_Tile_E.png', rotationOffset: 0 },
  // E (garden)
  city_top_flowers: { file: 'Abbot-Base_Game_C2_Tile_E_Garden.png', rotationOffset: 0 },
  // F
  city_narrow_shield: { file: 'Base_Game_C2_Tile_F.png', rotationOffset: 0 },
  // G
  city_narrow: { file: 'Base_Game_C2_Tile_G.png', rotationOffset: 0 },
  // H
  city_left_right: { file: 'Base_Game_C2_Tile_H.png', rotationOffset: 0 },
  // H (garden) - image has cities L/R, Kotlin defines T/B
  city_top_bottom_flowers: { file: 'Abbot-Base_Game_C2_Tile_H_Garden.png', rotationOffset: 1 },
  // I
  city_top_right: { file: 'Base_Game_C2_Tile_I.png', rotationOffset: 0 },
  // I (garden) - image has cities T/L, matching Kotlin definition
  city_top_left_flowers: { file: 'Abbot-Base_Game_C2_Tile_I_Garden.png', rotationOffset: 0 },
  // J
  city_top_road_bend_right: { file: 'Base_Game_C2_Tile_J.png', rotationOffset: 0 },
  // K
  city_top_road_bend_left: { file: 'Base_Game_C2_Tile_K.png', rotationOffset: 0 },
  // L
  city_top_crossroads: { file: 'Base_Game_C2_Tile_L.png', rotationOffset: 0 },
  // M
  city_diagonal_top_right_shield: { file: 'Base_Game_C2_Tile_M.png', rotationOffset: 0 },
  // M (garden)
  city_diagonal_top_right_shield_flowers: { file: 'Abbot-Base_Game_C2_Tile_M_Garden.png', rotationOffset: 0 },
  // N
  city_diagonal_top_right: { file: 'Base_Game_C2_Tile_N.png', rotationOffset: 0 },
  // N (garden)
  city_diagonal_top_right_flowers: { file: 'Abbot-Base_Game_C2_Tile_N_Garden.png', rotationOffset: 0 },
  // O
  city_diagonal_top_left_shield_road: { file: 'Base_Game_C2_Tile_O.png', rotationOffset: 0 },
  // P
  city_diagonal_top_left_road: { file: 'Base_Game_C2_Tile_P.png', rotationOffset: 0 },
  // Q
  city_bottom_grass_shield: { file: 'Base_Game_C2_Tile_Q.png', rotationOffset: 0 },
  // R
  city_bottom_grass: { file: 'Base_Game_C2_Tile_R.png', rotationOffset: 0 },
  // R (garden)
  city_bottom_grass_flowers: { file: 'Abbot-Base_Game_C2_Tile_R_Garden.png', rotationOffset: 0 },
  // S
  city_bottom_road_shield: { file: 'Base_Game_C2_Tile_S.png', rotationOffset: 0 },
  // T
  city_bottom_road: { file: 'Base_Game_C2_Tile_T.png', rotationOffset: 0 },
  // U
  straight_road: { file: 'Base_Game_C2_Tile_U.png', rotationOffset: 0 },
  // U (garden)
  straight_road_flowers: { file: 'Abbot-Base_Game_C2_Tile_U_Garden.png', rotationOffset: 0 },
  // V
  bent_road: { file: 'Base_Game_C2_Tile_V.png', rotationOffset: 0 },
  // V (garden)
  bent_road_flowers: { file: 'Abbot-Base_Game_C2_Tile_V_Garden.png', rotationOffset: 0 },
  // W
  three_split_road: { file: 'Base_Game_C2_Tile_W.png', rotationOffset: 0 },
  // X
  crossroads: { file: 'Base_Game_C2_Tile_X.png', rotationOffset: 0 },
}

export function getTileImageUrl(name: string): string | null {
  const info = TILE_IMAGE_MAP[name]
  return info ? `/tiles/${info.file}` : null
}

export function getTileRotationOffset(name: string): number {
  return TILE_IMAGE_MAP[name]?.rotationOffset ?? 0
}
