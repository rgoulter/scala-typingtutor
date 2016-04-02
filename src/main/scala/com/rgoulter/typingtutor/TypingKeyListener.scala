package com.rgoulter.typingtutor

import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea
import java.awt.event.KeyEvent
import java.awt.event.KeyListener

// callback: (position, numIncorrect) => ()
class TypingKeyListener(text: String, callback: (Int, Int) => Unit) extends KeyListener {
  private var lastCorrectPos = 0
  private var numIncorrect = 0

  private def pos: Int = lastCorrectPos + numIncorrect

  override def keyPressed(ke: KeyEvent): Unit = {
    ke.getKeyCode() match {
      case KeyEvent.VK_LEFT  => ke.consume()
      case KeyEvent.VK_RIGHT => ke.consume()
      case KeyEvent.VK_UP    => ke.consume()
      case KeyEvent.VK_DOWN  => ke.consume()
      case KeyEvent.VK_END   => ke.consume()
      case KeyEvent.VK_HOME  => ke.consume()
      case KeyEvent.VK_PAGE_UP   => ke.consume()
      case KeyEvent.VK_PAGE_DOWN => ke.consume()
      case _ => ()
    }
  }

  override def keyReleased(ke: KeyEvent): Unit = {}

  // TODO: There should be an "Endgame" condition;
  // e.g. typed for 1-2 mins on a page; typed 300 chars, or something.
  // and/or reached end of document.

  override def keyTyped(ke: KeyEvent): Unit = {
    val expectedChar = text.charAt(pos)
    val pressedChar = ke.getKeyChar

    pressedChar match {
      case '\b' => {
        if (numIncorrect > 0) {
          numIncorrect = numIncorrect - 1
        } else if (pos > 0) {
          lastCorrectPos = lastCorrectPos - 1
        }
      }
      // newlines.. are considered as just 'incorrect' characters.
      case c => { // what about characters like 'Home'?
//          println(s"Pressed Key '$charAtPos':$caretPosition <= '$pressedChar'")

        if (pos + 1 < text.length()) {
          // TODO: Here is where we'd want to do any statistic-tracking.
          // e.g. which key-pairs go well together, which don't.,
          //      most "mistyped", etc.

          if (numIncorrect == 0 &&
              pressedChar == expectedChar) {
            lastCorrectPos = lastCorrectPos + 1
          } else if (numIncorrect < 5) { // **MAGIC** MaxIncorrectRule
            numIncorrect = numIncorrect + 1
          }
        }
      }
    }

    // The '}' still inserts a character, even if `editable` is false!
    ke.consume()

    callback(lastCorrectPos, numIncorrect)
  }
}