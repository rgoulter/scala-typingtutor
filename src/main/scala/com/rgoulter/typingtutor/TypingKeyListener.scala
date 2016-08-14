package com.rgoulter.typingtutor

import java.awt.event.KeyEvent
import java.awt.event.KeyListener

import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea
import org.fife.ui.rsyntaxtextarea.Token
import org.fife.ui.rsyntaxtextarea.TokenTypes

import sodium.Cell
import sodium.CellSink
import sodium.Stream
import sodium.StreamSink



sealed trait TypingEvent



case class Backspace() extends TypingEvent



case class TypedCharacter(val c: Char, val time: Long) extends TypingEvent



case class ResetPosition(pos: Int = 0) extends TypingEvent



object State {
  // Since we want to skip over leading whitespace, blank lines,
  // comments,
  // and it may be that programs we type begin with these,
  // initial state shouldn't necessarily be at 0.
  def initialStateOf(doc: Document): State =
    State(0, 0, doc.initialOffset)
}



// Position represents the latest correctly typed input.
// Display in RSTA still adds numIncorrect for highlighting typing
// mistakes.
case class State(val numCorrect: Int,
                 val numIncorrect: Int,
                 val position : Int) {
}



// callback: (position, numIncorrect) => ()
class TypingKeyListener(val text: Cell[Document]) extends KeyListener {
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
  val markers = Cell.switchC(text.map { text =>
    typedOrReset.accum[State](State.initialStateOf(text), (te, state) => {
      te match {
        case ResetPosition(_) => State.initialStateOf(text)
        case Backspace() => {
          state match {
            // Pressed Backspace => Go back a character.
            case State(numCorrect, 0, position) => { // if numIncorrect == 0
              // ensure numCorrect >= 0
              val newNumCorrect = Math.max(0, numCorrect - 1)

              // Try to find a previous position
              val newPosition =
                text.previousTypeableOffset(position).getOrElse(position)
              State(newNumCorrect, 0, newPosition)
            }
            case State(numCorrect, numIncorrect, position) => {
              val newPosition =
                text.previousTypeableOffset(position).getOrElse(position)
              State(numCorrect, numIncorrect - 1, position)
            }
          }
        }
        case TypedCharacter(typedChar, time) => {
          state match {
            case State(numCorrect, 0, position) => { // if numIncorrect == 0
              val expectedChar = text.charAt(position)

              if (expectedChar == typedChar) {
                // Correctly typed character.
                // numCorrect < textSize
                val newNumCorrect = Math.min(numCorrect + 1, text.size)
                val newPosition =
                  text.nextTypeableOffset(position).getOrElse(position)
                State(newNumCorrect, 0, newPosition)
              } else {
                // Mis-typed character.
                // Previously didn't have any incorrect, now we do.
                State(numCorrect, 1, position)
              }
            }
            case State(numCorrect, numIncorrect, position) => {
              // **MAGIC** MaxIncorrectRule = 5
              val newNumIncorrect = Math.min(numIncorrect + 1, 5)
              State(numCorrect, newNumIncorrect, position)
            }
          }
        }
      }
    })
  })
  val numCorrect   = markers.map(_.numCorrect)
  val numIncorrect = markers.map(_.numIncorrect)
  val currentPos   = markers.map(_.position)

  val totalTypedCt = typedEvents.accum[Int](0, (_, n) => n + 1)
//  totalTypedCt.value().listen { n => println(s"Total Typed: $n keys.") }
  val totalTypedIncorrectCt = Cell.lift[Int, Int, Int]((total, correct) => total - correct, totalTypedCt, numCorrect)

  // collect list-of (exp, actual, time)
//  val keyEntryEvts = new Stream[(Char, Char, Long)]()
  val keyEntryEvts =
    typedCharEvents.map({ case (c, time) => {
      // more idiomatic way of achieving this?
      val expChar = Cell.lift((text: Document, idx: Int) => text.charAt(idx),
                              text,
                              currentPos).sample()

      (expChar, c, time)
    }})
  val keyEntries: Cell[Array[(Char, Char, Long)]] =
    keyEntryEvts.accum(Array(), (tup, acc) => { acc :+ tup })

  // TODO 'Quit after typed certain number'
  // although this needs to distinguish between 'num-typed-correct' and 'position'
  private val endGameAtEndOfText = currentPos.value().filter { currentPos =>
    // TODO Slightly imprecise here, as this will escape before we type the last char.
    currentPos == text.sample().size - 1
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
        val time = System.currentTimeMillis()
        typedEvents.send(TypedCharacter(c, time))

        // The '}' still inserts a character, even if `editable` is false!
        ke.consume()
      }
      case _ => {}
    }
  }
}
