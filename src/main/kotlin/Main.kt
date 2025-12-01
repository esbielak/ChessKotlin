
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.PrintWriter
import java.net.ServerSocket
import java.net.Socket
import java.util.Scanner
import kotlin.concurrent.atomics.AtomicBoolean
import kotlin.math.abs
import kotlin.system.exitProcess

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
        runClient(host, port)
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

enum class PieceType(val symbol: Char) {
  PAWN('P'), ROOK('R'), KNIGHT('N'), BISHOP('B'), QUEEN('Q'), KING('K'), EMPTY('.'), KINGMOVED('M'),
  ROOKMOVED('W')
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
      //if (piece.type == PieceType.ROOK) newPiece = Piece(PieceType.ROOKMOVED, piece.color)
      //if (piece.type == PieceType.KING) newPiece = Piece(PieceType.KINGMOVED, piece.color)
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
        if (board[i][j].color == color && (board[i][j].type == PieceType.KINGMOVED || board[i][j].type == PieceType.KING)) return Pair(i, j)
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

fun runServer(port: Int) {
  println("Starting server on port $port")
  val serverSocket = ServerSocket(port)
  println("waiting")

  val socket1 = serverSocket.accept()
  println("white connected, waiting for balck")
  val out1 = PrintWriter(socket1.getOutputStream(), true)
  out1.println("${Protocol.MESSAGE} waiting for opponent")
  val socket2 = serverSocket.accept()
  println("black connected - game started")

  val session = GameSession(socket1, socket2)
  session.start()
}

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

fun runClient(host: String, port: Int) {
  println("connecting to $host:$port")
  try {
    val socket = Socket(host, port)
    val input = BufferedReader(InputStreamReader(socket.getInputStream()))
    val output = PrintWriter(socket.getOutputStream(), true)
    val scanner = Scanner(System.`in`)

    val lock = Any()
    var ended = false
    val listener = Thread {
      try {
        var line: String?
        while (input.readLine().also { line = it } != null) {
          if (!handleServerMessage(line!!)) {
            println("Game ended")
            synchronized(lock) {
              ended = true
              scanner.close()
            }
            break
          }
        }
      } catch (e: Exception) {
        println("connection error")
        exitProcess(0)
      }
    }
    listener.start()

    while (true) {
      synchronized(lock) {
        if (ended) {
          break
        }
      }
      if (scanner.hasNextLine()) {
        val command = scanner.nextLine()
        output.println("${Protocol.MOVE} $command")
      }
    }

  } catch (e: Exception) {
    println("Could not connect: ${e.message}")
  }
}

fun handleServerMessage(message: String) : Boolean {
  val parts = message.split(" ", limit = 2)
  val command = parts[0]
  val payload = if (parts.size > 1) parts[1] else ""

  when (command) {
    Protocol.WELCOME -> println("you are playing as $payload.")
    Protocol.MESSAGE -> println(">> $payload")
    Protocol.BOARD -> prntBoard(payload)
    Protocol.YOUR_TURN -> {
      print("\u001B[32mEnter Move > \u001B[0m")
    }
    Protocol.WAIT -> {
    }
    Protocol.GAME_OVER -> return false
  }
  return true
}

fun prntBoard(serialized: String) {
  // cls
  print("\u001B[H\u001B[2J")
  System.out.flush()
  val parts = serialized.split(",")
  val turnColor = parts.last()

  println("      A   B   C   D   E   F   G   H")
  println("    +---+---+---+---+---+---+---+---+")

  for (i in 0 until 8) {
    val rowStr = parts[i]
    print("  ${8 - i} |")
    for (char in rowStr) {
      val visual = prntPiece(char)
      print(" $visual |")
    }
    println(" ${8 - i}")
    println("    +---+---+---+---+---+---+---+---+")
  }
  println("      A   B   C   D   E   F   G   H")
  println("Turn: $turnColor")
  println("")
}

fun prntPiece(char: Char): String {
  return when (char) {
    'P' -> "\u001B[37m♙\u001B[0m"

    'R' -> "\u001B[37m♖\u001B[0m"
    'W' -> "\u001B[37m♖\u001B[0m" // rookmoved

    'N' -> "\u001B[37m♘\u001B[0m"
    'B' -> "\u001B[37m♗\u001B[0m"
    'Q' -> "\u001B[37m♕\u001B[0m"

    'K' -> "\u001B[37m♔\u001B[0m"
    'M' -> "\u001B[37m♔\u001B[0m" // kingmoved

    'p' -> "\u001B[31m♟\u001B[0m"

    'r' -> "\u001B[31m♜\u001B[0m"
    'w' -> "\u001B[31m♜\u001B[0m" // rookmoved

    'n' -> "\u001B[31m♞\u001B[0m"
    'b' -> "\u001B[31m♝\u001B[0m"
    'q' -> "\u001B[31m♛\u001B[0m"

    'k' -> "\u001B[31m♚\u001B[0m"
    'm' -> "\u001B[31m♚\u001B[0m" // kingmoved
    '.' -> " "
    else -> "?"
  }
}