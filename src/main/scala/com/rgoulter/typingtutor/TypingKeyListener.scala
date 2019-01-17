package com.rgoulter.typingtutor

import java.awt.event.KeyAdapter
import java.awt.event.KeyEvent

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

object State {

  /** Returns a [[State]] with the initial offset of the [[Document]],
    * and `numCorrect`/`numIncorrect` counters reset to `0`. */
  def initialStateOf(doc: Document): State =
    State(0, 0, doc.initialOffset)
}

// Position represents the latest correctly typed input.
// Display in RSTA still adds numIncorrect for highlighting typing
// mistakes.
/** Represents the position of the cursor in the typing tutor,
  * distinguishing between how many correct letters the user has entered,
  * and what position/offset in the text document the user is at.
  *
  * `numIncorrect` can be used for showing the user how far it's been since
  * they typed an incorrect letter. It does not represent how many incorrect
  * keys the user has pressed in total.
  */
case class State(val numCorrect: Int,
                 val numIncorrect: Int,
                 val position: Int) {}

class StreamOfKeyListener extends KeyAdapter {
  private val typedEventsSink          = new StreamSink[TypingEvent]
  val typedEvents: Stream[TypingEvent] = typedEventsSink

  override def keyTyped(ke: KeyEvent): Unit = {
    val pressedChar = ke.getKeyChar

    pressedChar match {
      case '\b' => {
        typedEventsSink.send(Backspace())
      }

      // newlines.. are considered as just 'incorrect' characters.
      case c if !ke.isControlDown() => { // what about characters like 'Home'?
        val time = System.currentTimeMillis()
        typedEventsSink.send(TypedCharacter(c, time))

        // The '}' still inserts a character, even if `editable` is false!
        ke.consume()
      }
      case _ => {}
    }
  }
}

// callback: (position, numIncorrect) => ()
class TypingKeyListener(val text: Cell[Document],
                        typedEvents: Stream[TypingEvent]) {

  /** Stream of `(character, time)` values, representing
    *  which keys were pressed at what time.
    */
  private val typedCharEvents = typedEvents
    .filter({
      case TypedCharacter(_, _) => true
      case _                    => false
    })
    .map({
      case TypedCharacter(c, time) => (c, time)
      case _                       => throw new IllegalStateException()
    })

  /** Stream of the [[State]] of the typing tutor.
    *
    * n.b. the [[State]] resets when the document changes.
    */
  val markers = Cell.switchC(text.map { text =>
    typedEvents.accum[State](
      State.initialStateOf(text),
      (te, state) => {
        te match {
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

                  // If the position didn't advance,
                  // must be at the end.
                  if (position == newPosition) {
                    reachedEndSink.send(position + 1)
                  }

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
      }
    )
  })

  val numCorrect = markers.map(_.numCorrect)

  val numIncorrect = markers.map(_.numIncorrect)

  /** Current offset into the document. (Doesn't include `numIncorrect` in this
    * calculation).
    */
  val currentPos = markers.map(_.position)

  // totalTypedCt needs to reset after each change to `text` cell.
  val totalTypedCt =
    Cell.switchC(text.map(_ => typedEvents.accum[Int](0, (_, n) => n + 1)))

  val totalTypedIncorrectCt =
    totalTypedCt.lift(numCorrect, (total, correct: Int) => total - correct)

  val currentChar =
    text.lift(currentPos, (text: Document, idx: Int) => text.charAt(idx))

  /** Stream of `(actual char, expected char, time)` values. */
  val keyEntryEvts =
    typedCharEvents.snapshot(currentChar, {
      (typedCharEvt, expectedChar: Char) =>
        val (typedChar, time) = typedCharEvt
        (expectedChar, typedChar, time)
    })

  val keyEntries: Cell[Array[(Char, Char, Long)]] =
    Cell.switchC(
      text.map(_ => keyEntryEvts.accum(Array(), (tup, acc) => { acc :+ tup })))

  private val reachedEndSink = new StreamSink[Int]()

  /** Stream for when the typing tutor should finish. (e.g. user has typed in enough). */
  val reachedEnd: Stream[Int] =
    reachedEndSink

  // Convenience function since Cell.lift only supports up-to 4-ary functions.
  private def mkStats(tup: (Int, Int, Int),
                      entries: Array[(Char, Char, Long)],
                      offsets: (Int, Int)): TypedStats = {
    val (numTypedTotal, numTypedCorrect, numTypedIncorrect) = tup
    val (initialOffset, finalOffset)                        = offsets

    new TypedStats(numTypedTotal,
                   numTypedCorrect,
                   numTypedIncorrect,
                   entries,
                   initialOffset,
                   finalOffset)
  }

  private val numTypedTuple =
    totalTypedCt.lift(
      numCorrect,
      totalTypedIncorrectCt,
      (x: Int, y: Int, z: Int) => (x, y, z)
    )
  private val offsets =
    text
      .map(_.initialOffset)
      .lift(
        currentPos,
        (initOffs: Int, finalOffs: Int) => (initOffs, finalOffs)
      )
  val stats: Cell[TypedStats] =
    numTypedTuple.lift(keyEntries, offsets, mkStats)
}
