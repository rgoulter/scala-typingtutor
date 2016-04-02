package com.rgoulter.typingtutor

import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea
import java.awt.event.KeyEvent
import java.awt.event.KeyListener

class TypedStats(val numTotal: Int,
                 val numCorrect: Int,
                 val numIncorrect: Int) {
  def print(): Unit = {
    println(s"Total: $numTotal")
    println(s"Correct: $numCorrect")
    println(s"Incorrect: $numIncorrect")
  }
}

// callback: (position, numIncorrect) => ()
class TypingKeyListener(text: String,
                        callback: (Int, Int) => Unit,
                        endGame: TypedStats => Unit) extends KeyListener {
  private var lastCorrectPos = 0
  private var numIncorrect = 0

  private def pos: Int = lastCorrectPos + numIncorrect

  private var numTypedTotal: Int = 0
  private var numTypedIncorrect: Int = 0
  
  private def numTypedCorrect: Int =
    numTypedTotal - numTypedIncorrect

  // TODO: Could also correct list-of (exp, actual, time)

  def stats: TypedStats = {
    new TypedStats(numTypedTotal, numTypedCorrect, numTypedIncorrect)
  }

  override def keyPressed(ke: KeyEvent): Unit = {}

  override def keyReleased(ke: KeyEvent): Unit = {}

  // TODO: There should be an "Endgame" condition;
  // e.g. typed for 1-2 mins on a page; typed 300 chars, or something.
  // and/or reached end of document.

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
          // TODO: Here is where we'd want to do any statistic-tracking.
          // e.g. which key-pairs go well together, which don't.,
          //      most "mistyped", etc.

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