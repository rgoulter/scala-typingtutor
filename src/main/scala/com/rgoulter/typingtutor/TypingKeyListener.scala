package com.rgoulter.typingtutor

import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea
import java.awt.event.KeyEvent
import java.awt.event.KeyListener
import sodium.Cell
import sodium.CellSink
import sodium.Stream
import sodium.StreamSink

sealed trait TypingEvent
case class Backspace() extends TypingEvent
case class TypedCharacter(val c: Char) extends TypingEvent

class TypedStats(val numTotal: Int,
                 val numCorrect: Int,
                 val numIncorrect: Int,
                 val entries: Array[(Char, Char, Long)]) {
  // XXX This is a problem if numTotal is 0... :/
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
class TypingKeyListener(var text: String) extends KeyListener {
  /*
   * So how do we do this?
   *
   * endGame becomes a Steam<Unit> we export.
   *
   * callback of (latestCorrect, numIncorrect) is a cell,
   *   lifted from latestCorrect cell,
   *               numIncorrect cell
   *
   * numTypedTotal might as well be a cell, also.
   * 
   * I'm not sure what the idiomatic way of getting mutKeyEntries is.
   *
   * Regardless, a Stream of (Expected, Got),
   * had from [Backspace | Key c] event,
   * & can get 'expected' by mapping from latestCorrect -> text ...
   * 
   * XXX The text is also a Cell, incidentally.
   *
   * stats... Hmm.
   * Well, since we can't "get the value" from FRP system, right,
   *  -- can 'lift' from the other values to get it,
   * stats should be a cell, we take snapshot of this at time of game end, I guess?
   */

//  private var lastCorrectPos = 0
//  private var numIncorrect = 0

//  private def pos: Int = lastCorrectPos + numIncorrect

  private var numTypedTotal: Int = 0
  private var numTypedIncorrect: Int = 0
  
  private def numTypedCorrect: Int =
    numTypedTotal - numTypedIncorrect

  // collect list-of (exp, actual, time)
  private val mutKeyEntries = new scala.collection.mutable.ArrayBuffer[(Char, Char, Long)](1000)
  
  
  
  private val typedEvents = new StreamSink[TypingEvent]
  val backspaceEvents = typedEvents.filter({
    case Backspace() => true
    case _ => false
  })
  val typedCharEvents = typedEvents.filter({
    case TypedCharacter(_) => true
    case _ => false
  }).map({
    case TypedCharacter(c) => c
    case _ => throw new IllegalStateException()
  })

  // lastCorrect is a cell. How?
  val markers = typedEvents.accum[(Int, Int)]((0, 0), (te, pair) => {
    val (numCorrect, numIncorrect) = pair

    if (numIncorrect == 0) {
      te match {
        case Backspace() => {
          // numCorrect >= 0
          (Math.max(0, numCorrect - 1), numIncorrect)
        }
        case TypedCharacter(typedChar) => {
          val expectedChar = text.charAt(numCorrect)

          if (expectedChar == typedChar) {
            // numCorrect < textSize
            val textSize = text.size
            (Math.min(numCorrect + 1, textSize - 1), numIncorrect)
          } else {
            (numCorrect, numIncorrect + 1)
          }
        }
      }
    } else {
      te match {
        case Backspace() => {
          (numCorrect, numIncorrect - 1)
        }
        case TypedCharacter(t) => {
          // **MAGIC** MaxIncorrectRule = 5
          (numCorrect, Math.min(numIncorrect + 1, 5))
        }
      }
    }
  })
  val numCorrect   = markers.map(_._1)
  val numIncorrect = markers.map(_._2)

  // TODO: Send 'quit' event when reached end of text; when send too many..
  // e.g. **MAGIC** MaxCorrectTypedRule = 1000

  val totalTypedCt = typedEvents.accum[Int](0, (_, n) => n + 1)

  // So... backspace should decrease numIncorrect until 0; then decrease latestCorrect.

  private val endGameSink = new StreamSink[Unit]()
  val endGame: Stream[Unit] = endGameSink

  def stats: TypedStats = {
    // XXX should be using FRP
    new TypedStats(numTypedTotal, numTypedCorrect, numTypedIncorrect, Array())
  }

  override def keyPressed(ke: KeyEvent): Unit = {}

  override def keyReleased(ke: KeyEvent): Unit = {}

  override def keyTyped(ke: KeyEvent): Unit = {
    val pressedChar = ke.getKeyChar

    pressedChar match {
      case KeyEvent.VK_ESCAPE => {
        endGameSink.send(())
      }

      case '\b' => {
        typedEvents.send(Backspace())
      }

      // newlines.. are considered as just 'incorrect' characters.
      case c if !ke.isControlDown() => { // what about characters like 'Home'?
//          println(s"Pressed Key '$charAtPos':$caretPosition <= '$pressedChar'")
        typedEvents.send(TypedCharacter(c))

        // The '}' still inserts a character, even if `editable` is false!
        ke.consume()
      }
      case _ => {}
    }
  }
}