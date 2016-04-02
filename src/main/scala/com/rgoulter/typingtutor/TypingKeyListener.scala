package com.rgoulter.typingtutor

import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea
import java.awt.event.KeyEvent
import java.awt.event.KeyListener

class TypedStats(val numTotal: Int,
                 val numCorrect: Int,
                 val numIncorrect: Int,
                 val entries: Array[(Char, Char, Long)]) {
  def print(): Unit = {
    println(s"Total: $numTotal")
    println(s"Correct: $numCorrect")
    println(s"Incorrect: $numIncorrect")
  }

  val duration: Long = {
    val start = entries.head._3
    val end   = entries.last._3
    end - start
  }

  val durationInMins: Double =
    (duration / 1000).toDouble / 60

  val durationStr: String =
    s"${(duration / 1000) / 60}:${(duration / 1000) % 60}"

  val accuracy = numCorrect.toDouble / numTotal

  val accuracyPercent: Int = (accuracy * 100).toInt

  // wpm = (# chars / 5) / (time in mins)
  // Rounded to int is close enough
  val wpm = ((numCorrect / 5) / durationInMins).toInt
}

// callback: (position, numIncorrect) => ()
class TypingKeyListener(var text: String,
                        callback: (Int, Int) => Unit,
                        endGame: TypedStats => Unit) extends KeyListener {
  private var lastCorrectPos = 0
  private var numIncorrect = 0

  private def pos: Int = lastCorrectPos + numIncorrect

  private var numTypedTotal: Int = 0
  private var numTypedIncorrect: Int = 0
  
  private def numTypedCorrect: Int =
    numTypedTotal - numTypedIncorrect

  // collect list-of (exp, actual, time)
  private val mutKeyEntries = new scala.collection.mutable.ArrayBuffer[(Char, Char, Long)](1000)

  def stats: TypedStats = {
    new TypedStats(numTypedTotal, numTypedCorrect, numTypedIncorrect, mutKeyEntries.toArray)
  }

  override def keyPressed(ke: KeyEvent): Unit = {}

  override def keyReleased(ke: KeyEvent): Unit = {}

  override def keyTyped(ke: KeyEvent): Unit = {
    val expectedChar = text.charAt(pos)
    val pressedChar = ke.getKeyChar

    pressedChar match {
      case KeyEvent.VK_ESCAPE => {
        endGame(stats)
      }

      case '\b' => {
        if (numIncorrect > 0) {
          numIncorrect -= 1
        } else if (pos > 0) {
          lastCorrectPos -= 1
        }
      }

      // newlines.. are considered as just 'incorrect' characters.
      case c if !ke.isControlDown() => { // what about characters like 'Home'?
//          println(s"Pressed Key '$charAtPos':$caretPosition <= '$pressedChar'")

        if (pos + 1 < text.length()) {
          // Keep track of entered things
          // (Slight discrepancy if we include these all).
          val timeMillis = System.currentTimeMillis()
          val entry = (expectedChar, pressedChar, timeMillis)
          mutKeyEntries += entry


          numTypedTotal += 1

          if (numIncorrect == 0 &&
              pressedChar == expectedChar) {
            lastCorrectPos += 1
          } else if (numIncorrect < 5) { // **MAGIC** MaxIncorrectRule = 5
            numIncorrect += 1
            numTypedIncorrect += 1
          }

          if (numTypedCorrect > 1000) { // **MAGIC** MaxCorrectTypedRule = 1000
            endGame(stats)
          }
        } else {
          // Got to the end.
          endGame(stats)
        }

        // The '}' still inserts a character, even if `editable` is false!
        ke.consume()
      }
      case _ => {}
    }

    callback(lastCorrectPos, numIncorrect)
  }
}