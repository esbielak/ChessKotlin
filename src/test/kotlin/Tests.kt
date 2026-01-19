import kotlin.test.*

class ChessLogicTest {

  @Test
  fun testPawnInitialMove() {
    val board = ChessBoard()
    // legal move
    val result = board.processMove("e2e4", Color.WHITE)
    assertNull(result, "Standard pawn opening should be valid")
    assertEquals(PieceType.PAWN, board.board[4][4].type)
    assertEquals(Color.BLACK, board.turn)
  }

  @Test
  fun testIllegalJumpBishop() {
    val board = ChessBoard()
    val result = board.processMove("f1c4", Color.WHITE)
    assertNotNull(result, "Bishop should not be able to jump over pieces")
  }
  @Test
  fun testIllegalJumpRook() {
    val board = ChessBoard()
    val result = board.processMove("a1a3", Color.WHITE)
    assertNotNull(result, "Rook should not be able to jump over pieces")
  }

  @Test
  fun testIllegalMovePawn() {
    val board = ChessBoard()
    val result = board.processMove("a2b3", Color.WHITE)
    assertNotNull(result, "pawn cannot move diagonally without capture")
  }
  @Test
  fun testPawnCapture() {
    val board = ChessBoard()
    board.processMove("e2e4", Color.WHITE)
    board.processMove("d7d5", Color.BLACK)
    val result = board.processMove("e4d5", Color.WHITE)
    assertNull(result, "Pawns can capture diagonally")
  }

  @Test
  fun testPawnPhase() {
    val board = ChessBoard()
    board.processMove("e2e4", Color.WHITE)
    board.processMove("e7e5", Color.BLACK)
    val result = board.processMove("e4e5", Color.WHITE)
    assertNotNull(result, "Pawns cannot capture vertically")
  }

  @Test
  fun testKnightMovement() {
    val board = ChessBoard()
    // knight g1 to f3 jumps over pawns
    val result = board.processMove("g1f3", Color.WHITE)
    assertNull(result, "Knight should be able to jump over pawns")
    assertEquals(PieceType.KNIGHT, board.board[5][5].type)
  }

  @Test
  fun testTurnOrder() {
    val board = ChessBoard()
    // black moves first
    val result = board.processMove("e7e5", Color.BLACK)
    assertEquals("not yet", result)
  }

  @Test
  fun testOutOfBounds() {
    val board = ChessBoard()
    // move not on the chessboard
    val result = board.processMove("e2e9", Color.WHITE)
    assertEquals("out of bounds", result)
  }
}