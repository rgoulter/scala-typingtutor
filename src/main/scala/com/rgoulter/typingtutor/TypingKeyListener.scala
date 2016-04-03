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
  def print(): Unit = {
    println(s"Total: $numTotal")
    println(s"Correct: $numCorrect")
    println(s"Incorrect: $numIncorrect")
  }

  val duration: Long = {
    if (!entries.isEmpty) {
      val start = entries.head._3
      val end   = entries.last._3
      end - start
    } else {
      0
    }
  }

  val durationInMins: Double =
    (duration / 1000).toDouble / 60

  val durationStr: String =
    s"${(duration / 1000) / 60}:${(duration / 1000) % 60}"

  val accuracy = numCorrect.toDouble / numTotal

  val accuracyPercent: Int = (accuracy * 100).toInt

  // wpm = (# chars / 5) / (time in mins)
  // Rounded to int is close enough
  val wpmStr =
    if (durationInMins > 0)
      ((numCorrect / 5) / durationInMins).toInt
    else
      "???"
}

// callback: (position, numIncorrect) => ()
class TypingKeyListener(var text: String) extends KeyListener {
  // collect list-of (exp, actual, time)
  //? How to collect *with time* in FRP? Cheat? Use listen? Snapshot of some cell?
//  private val mutKeyEntries = new scala.collection.mutable.ArrayBuffer[(Char, Char, Long)](1000)
  
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
  val totalTypedIncorrectCt = Cell.lift[Int, Int, Int]((total, correct) => total - correct, totalTypedCt, numCorrect)

  private val endGameSink = new StreamSink[Unit]()
  val endGame: Stream[Unit] = endGameSink

  def stats = Cell.lift[Int, Int, Int, TypedStats]((numTypedTotal, numTypedCorrect, numTypedIncorrect) =>
    // XXX should be using FRP
    new TypedStats(numTypedTotal, numTypedCorrect, numTypedIncorrect, Array()),
    totalTypedCt,
    numCorrect,
    totalTypedIncorrectCt)

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