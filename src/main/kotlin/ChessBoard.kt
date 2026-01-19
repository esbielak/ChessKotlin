import kotlin.math.abs

class ChessBoard {
  val board = Array(8) { Array(8) { Piece(PieceType.EMPTY, Color.NONE) } }
  var turn = Color.WHITE

  init {
    setupBoard()
  }

  private fun setupBoard() {
    for (i in 0..7) {
      board[1][i] = Piece(PieceType.PAWN, Color.BLACK)
      board[6][i] = Piece(PieceType.PAWN, Color.WHITE)
    }
    val order = listOf(PieceType.ROOK, PieceType.KNIGHT, PieceType.BISHOP, PieceType.QUEEN, PieceType.KING, PieceType.BISHOP, PieceType.KNIGHT, PieceType.ROOK)
    for (i in 0..7) {
      board[0][i] = Piece(order[i], Color.BLACK)
      board[7][i] = Piece(order[i], Color.WHITE)
    }
  }

  fun processMove(moveStr: String, playerColor: Color): String? {
    if (playerColor != turn) return "not yet"

    try {
      if (moveStr.length != 4) return "invalid format. algebraic notation is used (e2e4)"

      val c1 = moveStr[0] - 'a'
      val r1 = 8 - (moveStr[1] - '0')
      val c2 = moveStr[2] - 'a'
      val r2 = 8 - (moveStr[3] - '0')

      if (r1 !in 0..7 || c1 !in 0..7 || r2 !in 0..7 || c2 !in 0..7) return "out of bounds"

      val piece = board[r1][c1]
      if (piece.color != playerColor) return "invalid piece"
      if (board[r2][c2].color == playerColor) return "wrong target piece color"

      val validMove = when(piece.type) {
        PieceType.PAWN -> validatePawn(r1, c1, r2, c2, piece.color, board[r2][c2].type != PieceType.EMPTY)
        PieceType.ROOK -> validateRook(r1, c1, r2, c2)
        PieceType.KNIGHT -> validateKnight(r1, c1, r2, c2)
        PieceType.BISHOP -> validateBishop(r1, c1, r2, c2)
        PieceType.QUEEN -> validateRook(r1, c1, r2, c2) || validateBishop(r1, c1, r2, c2)
        PieceType.KING -> abs(r1 - r2) <= 1 && abs(c1 - c2) <= 1
        else -> false
      }
      var newPiece = piece
      if (!validMove) return "illegal move for ${piece.type}."
      board[r2][c2] = newPiece
      board[r1][c1] = Piece(PieceType.EMPTY, Color.NONE)
      turn = turn.opposite()

      return null
    } catch (e: Exception) {
      return "Error"
    }
  }
  fun findKing(color: Color) : Pair<Int, Int>? {
    for (i in 0..7) {
      for (j in 0..7) {
        if (board[i][j].color == color && board[i][j].type == PieceType.KING) return Pair(i, j)
      }
    }
    return null
  }

  private fun validatePawn(r1: Int, c1: Int, r2: Int, c2: Int, color: Color, isCapture: Boolean): Boolean {
    val direction = if (color == Color.WHITE) -1 else 1
    val startRow = if (color == Color.WHITE) 6 else 1
    if (c1 == c2 && r2 == r1 + direction && !isCapture) return true
    if (c1 == c2 && r1 == startRow && r2 == r1 + (direction * 2) && !isCapture && board[r1+direction][c1].type == PieceType.EMPTY) return true
    if (abs(c1 - c2) == 1 && r2 == r1 + direction && isCapture) return true

    return false
  }

  private fun validateRook(r1: Int, c1: Int, r2: Int, c2: Int): Boolean {
    if (r1 != r2 && c1 != c2) return false
    return isPathClear(r1, c1, r2, c2)
  }

  private fun validateBishop(r1: Int, c1: Int, r2: Int, c2: Int): Boolean {
    if (abs(r1 - r2) != abs(c1 - c2)) return false
    return isPathClear(r1, c1, r2, c2)
  }

  private fun validateKnight(r1: Int, c1: Int, r2: Int, c2: Int): Boolean {
    val dr = abs(r1 - r2)
    val dc = abs(c1 - c2)
    return (dr == 2 && dc == 1) || (dr == 1 && dc == 2)
  }

  private fun isPathClear(r1: Int, c1: Int, r2: Int, c2: Int): Boolean {
    val dr = Integer.signum(r2 - r1)
    val dc = Integer.signum(c2 - c1)
    var cr = r1 + dr
    var cc = c1 + dc
    while (cr != r2 || cc != c2) {
      if (board[cr][cc].type != PieceType.EMPTY) return false
      cr += dr
      cc += dc
    }
    return true
  }

  fun serialize(): String {
    return board.joinToString(",") { row ->
      row.joinToString("") { it.toString() }
    } + ",$turn"
  }
}