enum class PieceType(val symbol: Char) {
  PAWN('P'), ROOK('R'), KNIGHT('N'), BISHOP('B'), QUEEN('Q'), KING('K'), EMPTY('.')
}

enum class Color {
  WHITE, BLACK, NONE;
  fun opposite(): Color = when (this) {
    WHITE -> BLACK
    BLACK -> WHITE
    NONE -> NONE
  }
}

data class Piece(val type: PieceType, val color: Color) {
  override fun toString(): String {
    return if (color == Color.WHITE) type.symbol.uppercase() else type.symbol.lowercase()
  }

  fun toVisualString(): String {
    return when (color) {
      Color.WHITE -> when (type) {
        PieceType.PAWN -> "♙"; PieceType.ROOK -> "♖"; PieceType.KNIGHT -> "♘"
        PieceType.BISHOP -> "♗"; PieceType.QUEEN -> "♕"; PieceType.KING -> "♔"; else -> " "
      }
      Color.BLACK -> when (type) {
        PieceType.PAWN -> "♟"; PieceType.ROOK -> "♜"; PieceType.KNIGHT -> "♞"
        PieceType.BISHOP -> "♝"; PieceType.QUEEN -> "♛"; PieceType.KING -> "♚"; else -> " "
      }
      else -> "·"
    }
  }
}