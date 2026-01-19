import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.PrintWriter
import java.net.Socket

class GameSession(private val whiteSocket: Socket, private val blackSocket: Socket) : Thread() {
  private val board = ChessBoard()
  private val whiteIn = BufferedReader(InputStreamReader(whiteSocket.getInputStream()))
  private val whiteOut = PrintWriter(whiteSocket.getOutputStream(), true)
  private val blackIn = BufferedReader(InputStreamReader(blackSocket.getInputStream()))
  private val blackOut = PrintWriter(blackSocket.getOutputStream(), true)

  override fun run() {
    try {
      whiteOut.println("${Protocol.WELCOME} WHITE")
      blackOut.println("${Protocol.WELCOME} BLACK")

      sendState()
      var gameRunning = true
      while (gameRunning) {
        val currentPlayerIn = if (board.turn == Color.WHITE) whiteIn else blackIn
        val currentPlayerOut = if (board.turn == Color.WHITE) whiteOut else blackOut

        val inputLine = currentPlayerIn.readLine() ?: break

        if (inputLine.startsWith(Protocol.MOVE)) {
          val move = inputLine.substringAfter(" ").trim()
          println("move: $move from ${board.turn}")

          val error = board.processMove(move, board.turn)
          if (board.findKing(Color.WHITE) == null) {
            endGame("BLACK")
            break
          } else if (board.findKing(Color.BLACK) == null) {
            endGame("WHITE")
            break
          }

          if (error == null) {
            sendState()
          } else {
            currentPlayerOut.println("${Protocol.MESSAGE} invalid move: $error")
            currentPlayerOut.println(Protocol.YOUR_TURN)
          }
        } else if (inputLine == "QUIT") {
          gameRunning = false
        }
      }
    } catch (e: Exception) {
      println("connection err ${e.message}")
    } finally {
      close()
    }
  }
  private fun endGame(winner: String) {
    whiteOut.println("${Protocol.GAME_OVER} $winner WINS")
    blackOut.println("${Protocol.GAME_OVER} $winner WINS")
    close()
  }

  private fun sendState() {
    val serialized = board.serialize()
    whiteOut.println("${Protocol.BOARD} $serialized")
    blackOut.println("${Protocol.BOARD} $serialized")

    if (board.turn == Color.WHITE) {
      whiteOut.println(Protocol.YOUR_TURN)
      whiteOut.println("${Protocol.MESSAGE} white to move, format: e2e4")
      blackOut.println(Protocol.WAIT)
      blackOut.println("${Protocol.MESSAGE} waiting for white")
    } else {
      blackOut.println(Protocol.YOUR_TURN)
      blackOut.println("${Protocol.MESSAGE} black to move,  format: e7e5")
      whiteOut.println(Protocol.WAIT)
      whiteOut.println("${Protocol.MESSAGE} waiting for black")
    }
  }

  private fun close() {
    try { whiteSocket.close() } catch (e: Exception) {}
    try { blackSocket.close() } catch (e: Exception) {}
    println("game ended")
  }
}