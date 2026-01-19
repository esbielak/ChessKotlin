import java.io.PrintWriter
import java.net.ServerSocket
import java.util.concurrent.atomic.AtomicInteger


const val DEFAULT_PORT = 5000
const val DEFAULT_HOST = "localhost"

fun main(args: Array<String>) {
  if (args.isEmpty()) {
    println("Usage:")
    println("  Server: java -jar chess.jar server <port>")
    println("  Client: java -jar chess.jar client <host> <port>")
    return
  }

  val mode = args[0].lowercase()
  try {
    when (mode) {
      "server" -> {
        val port = if (args.size > 1) args[1].toInt() else DEFAULT_PORT
        runServer(port)
      }
      "client" -> {
        val host = if (args.size > 1) args[1] else DEFAULT_HOST
        val port = if (args.size > 2) args[2].toInt() else DEFAULT_PORT
        Client().runClient(host, port)
      }
      else -> println("use either server or client mode")
    }
  } catch (e: Exception) {
    println("Error: ${e.message}")
    e.printStackTrace()
  }
}

object Protocol {
  const val WELCOME = "WELCOME" // Args: Color
  const val MESSAGE = "MSG"     // Args: Text
  const val BOARD = "BOARD"     // Args: Serialized Board
  const val YOUR_TURN = "TURN"  // No args
  const val WAIT = "WAIT"       // No args
  const val GAME_OVER = "OVER"  // Args: Reason
  const val MOVE = "MOVE"       // Args: e2e4
}


fun runServer(port: Int) {
  println("Starting server on port $port")
  val serverSocket = ServerSocket(port)
  val gameCounter = AtomicInteger(0)
  println("waiting")

  while (true) {
    val socket1 = serverSocket.accept()
    println("white connected, waiting for black")
    val out1 = PrintWriter(socket1.getOutputStream(), true)
    out1.println("${Protocol.MESSAGE} waiting for opponent")
    val socket2 = serverSocket.accept()
    println("black connected - game started")

    val session = GameSession(socket1, socket2)
    val id = gameCounter.getAndIncrement()
    println("starting game #$id")
    session.start()
  }

}


fun printBoard(serialized: String, flipped: Boolean) {
  // cls
  print("\u001B[H\u001B[2J")
  System.out.flush()
  val parts = serialized.split(",")
  val turnColor = parts.last()

  println("      A   B   C   D   E   F   G   H")
  println("    +---+---+---+---+---+---+---+---+")
  val range = if (flipped) 7 downTo 0 else 0..7

  for (i in range) {
    val rowStr = if (flipped) parts[i].reversed() else parts[i]
    print("  ${8 - i} |")
    for (char in rowStr) {
      val visual = printPiece(char)
      print(" $visual |")
    }
    println(" ${8 - i}")
    println("    +---+---+---+---+---+---+---+---+")
  }
  if (flipped)
    println("      H   G   F   E   D   C   B   A")
  else
    println("      A   B   C   D   E   F   G   H")
  println("Turn: $turnColor")
  println("")
}

fun printPiece(char: Char): String {
  return when (char) {
    'P' -> "\u001B[37m♙\u001B[0m"
    'R' -> "\u001B[37m♖\u001B[0m"
    'N' -> "\u001B[37m♘\u001B[0m"
    'B' -> "\u001B[37m♗\u001B[0m"
    'Q' -> "\u001B[37m♕\u001B[0m"
    'K' -> "\u001B[37m♔\u001B[0m"

    'p' -> "\u001B[31m♟\u001B[0m"
    'r' -> "\u001B[31m♜\u001B[0m"
    'n' -> "\u001B[31m♞\u001B[0m"
    'b' -> "\u001B[31m♝\u001B[0m"
    'q' -> "\u001B[31m♛\u001B[0m"
    'k' -> "\u001B[31m♚\u001B[0m"

    '.' -> " "
    else -> "?"
  }
}