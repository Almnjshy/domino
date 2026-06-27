package com.agon.app.domain.model

/**
 * Represents the current state of the game board
 */
data class BoardState(
    val tiles: List<PlacedTile> = emptyList(),
    val leftEnd: Int? = null,
    val rightEnd: Int? = null,
    val isEmpty: Boolean = true
) {
    data class PlacedTile(
        val tile: DominoTile,
        val side: BoardSide,
        val orientation: TileOrientation
    )

    fun canPlace(tile: DominoTile): Boolean {
        if (isEmpty) return true
        return tile.matches(leftEnd ?: 0) || tile.matches(rightEnd ?: 0)
    }

    fun getLegalSides(tile: DominoTile): Set<BoardSide> = buildSet {
        if (isEmpty) {
            add(BoardSide.LEFT)
            add(BoardSide.RIGHT)
            return@buildSet
        }
        if (tile.matches(leftEnd ?: 0)) add(BoardSide.LEFT)
        if (tile.matches(rightEnd ?: 0)) add(BoardSide.RIGHT)
    }

    fun place(tile: DominoTile, side: BoardSide): BoardState {
        val newTiles = tiles + PlacedTile(tile, side, TileOrientation.HORIZONTAL)
        return when {
            isEmpty -> BoardState(
                tiles = newTiles,
                leftEnd = tile.top,
                rightEnd = tile.bottom,
                isEmpty = false
            )
            side == BoardSide.LEFT -> {
                val newLeft = tile.getMatchingEnd(leftEnd ?: tile.top)
                BoardState(newTiles, newLeft, rightEnd, false)
            }
            else -> {
                val newRight = tile.getMatchingEnd(rightEnd ?: tile.bottom)
                BoardState(newTiles, leftEnd, newRight, false)
            }
        }
    }
}

enum class BoardSide { LEFT, RIGHT }
enum class TileOrientation { HORIZONTAL, VERTICAL }
