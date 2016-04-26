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
case class TypedCharacter(val c: Char, val time: Long) extends TypingEvent
case class ResetPosition(pos: Int = 0) extends TypingEvent

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
class TypingKeyListener(val text: Cell[String]) extends KeyListener {
  private val typedEvents = new StreamSink[TypingEvent]
  // Need to be able to reset the markers on text changing..
  private val typedOrReset =
    typedEvents.merge(text.updates().map(t => ResetPosition()),
                      (te, reset) => reset)
  val backspaceEvents = typedEvents.filter({
    case Backspace() => true
    case _ => false
  })
  val typedCharEvents = typedEvents.filter({
    case TypedCharacter(_, _) => true
    case _ => false
  }).map({
    case TypedCharacter(c, time) => (c, time)
    case _ => throw new IllegalStateException()
  })

  // lastCorrect is a cell. How?
  val markers = typedOrReset.accum[(Int, Int)]((0, 0), (te, pair) => {
    val (numCorrect, numIncorrect) = pair

    te match {
      case ResetPosition(_) => (0, 0)
      case Backspace() => {
        if (numIncorrect == 0) {
          // numCorrect >= 0
          (Math.max(0, numCorrect - 1), numIncorrect)
        } else {
          (numCorrect, numIncorrect - 1)
        }
      }
      case TypedCharacter(typedChar, time) => {
        if (numIncorrect == 0) {
          val expectedChar = text.sample().charAt(numCorrect)

          if (expectedChar == typedChar) {
            println(s"Markers: expected $expectedChar got $typedChar @ ($numCorrect, $numIncorrect)")

            // numCorrect < textSize
            val textSize = text.sample().size
            (Math.min(numCorrect + 1, textSize - 1), numIncorrect)
          } else {
            (numCorrect, numIncorrect + 1)
          }
        } else {
          // **MAGIC** MaxIncorrectRule = 5
          (numCorrect, Math.min(numIncorrect + 1, 5))
        }
      }
    }
  })
  val numCorrect   = markers.map(_._1)
  val numIncorrect = markers.map(_._2)

  val totalTypedCt = typedEvents.accum[Int](0, (_, n) => n + 1)
  val totalTypedIncorrectCt = Cell.lift[Int, Int, Int]((total, correct) => total - correct, totalTypedCt, numCorrect)

  // collect list-of (exp, actual, time)
//  val keyEntryEvts = new Stream[(Char, Char, Long)]()
  val keyEntryEvts =
    typedCharEvents.map({ case (c, time) => {
      // more idiomatic way of achieving this?
      val expChar = Cell.lift((text: String, idx: Int) => text.charAt(idx),
                              text,
                              numCorrect).sample()

//      println(s"KeyEntry: expecting: $expChar got $c at time $time")
      (expChar, c, time)
    }})
  val keyEntries: Cell[Array[(Char, Char, Long)]] =
    keyEntryEvts.accum(Array(), (tup, acc) => { acc :+ tup })

  // TODO 'Quit after typed certain number'
  // although this needs to distinguish between 'num-typed-correct' and 'position'
  private val endGameAtEndOfText = numCorrect.value().filter { numCorrect =>
    // TODO Slightly imprecise here, as this will escape before we type the last char.
    numCorrect == text.sample().length() - 1
  }
  private val endGameSink = new StreamSink[Unit]()
  val endGame: Stream[Unit] =
    endGameSink.merge(endGameAtEndOfText.map(_ => ()),
                      (l, r) => l)

  def stats: Cell[TypedStats] =
    Cell.lift((numTypedTotal: Int, numTypedCorrect: Int, numTypedIncorrect: Int, keyEntries: Array[(Char, Char, Long)]) =>
                new TypedStats(numTypedTotal, numTypedCorrect, numTypedIncorrect, keyEntries),
              totalTypedCt,
              numCorrect,
              totalTypedIncorrectCt,
              keyEntries)

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
        val time = System.currentTimeMillis()
        typedEvents.send(TypedCharacter(c, time))

        // The '}' still inserts a character, even if `editable` is false!
        ke.consume()
      }
      case _ => {}
    }
  }
}