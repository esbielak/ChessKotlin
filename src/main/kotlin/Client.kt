import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.PrintWriter
import java.net.Socket
import java.util.Scanner
import kotlin.system.exitProcess

class Client {
  var color = Color.WHITE;
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
  private fun handleServerMessage(message: String) : Boolean {
    val parts = message.split(" ", limit = 2)
    val command = parts[0]
    val payload = if (parts.size > 1) parts[1] else ""

    when (command) {
      Protocol.WELCOME -> {
        color = if (payload == "BLACK") Color.BLACK else Color.WHITE
        println("you are playing as $payload.")
      }
      Protocol.MESSAGE -> println(">> $payload")
      Protocol.BOARD -> printBoard(payload, color == Color.BLACK)
      Protocol.YOUR_TURN -> {
        print("\u001B[32mEnter Move > \u001B[0m")
      }
      Protocol.WAIT -> {
      }
      Protocol.GAME_OVER -> return false
    }
    return true
  }
}