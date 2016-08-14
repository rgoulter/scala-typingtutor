package com.rgoulter.typingtutor

import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea
import org.fife.ui.rsyntaxtextarea.Token
import java.awt.event.KeyEvent
import java.awt.event.KeyListener
import sodium.Cell
import sodium.CellSink
import sodium.Stream
import sodium.StreamSink
import org.fife.ui.rsyntaxtextarea.TokenTypes

sealed trait TypingEvent
case class Backspace() extends TypingEvent
case class TypedCharacter(val c: Char, val time: Long) extends TypingEvent
case class ResetPosition(pos: Int = 0) extends TypingEvent

object State {
  // Since we want to skip over leading whitespace, blank lines,
  // comments,
  // and it may be that programs we type begin with these,
  // initial state shouldn't necessarily be at 0.
  def initialStateOf(): State =
    State()
}

case class State(val numCorrect: Int = 0,
                 val numIncorrect: Int = 0) {
  val position = numCorrect
}

// callback: (position, numIncorrect) => ()
class TypingKeyListener(val text: Cell[Token]) extends KeyListener {
  Utils.dumpTokens(text.sample(), "TypKL text.sample Tokens")

  // Computing the size of the text is somewhat more involved
  // from `text: Token` compared to `text: String`.
  private val textSize: Cell[Int] = text.map { initTok =>
    if (initTok == null) {
      0
    } else {
      var tok = initTok

      while (tok.getNextToken() != null &&
             tok.getNextToken().getType() != TokenTypes.NULL) {
        tok = tok.getNextToken()
      }

      tok.getEndOffset()
    }
  }

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
  val markers = typedOrReset.accum[State](State.initialStateOf(), (te, state) => {
    te match {
      case ResetPosition(_) => State.initialStateOf()
      case Backspace() => {
        state match {
          // Pressed Backspace => Go back a character.
          case State(numCorrect, 0) => { // if numIncorrect == 0
            // ensure numCorrect >= 0
            State(Math.max(0, numCorrect - 1), 0)
          }
          case State(numCorrect, numIncorrect) => {
            State(numCorrect, numIncorrect - 1)
          }
        }
      }
      case TypedCharacter(typedChar, time) => {
        state match {
          case State(numCorrect, 0) => { // if numIncorrect == 0
            val expectedChar = text.sample().charAt(numCorrect - text.sample().getOffset())

            if (expectedChar == typedChar) {
              // Correctly typed character.
              // numCorrect < textSize
              State(Math.min(numCorrect + 1, textSize.sample()), 0)
            } else {
              // Mis-typed character.
              // Previously didn't have any incorrect, now we do.
              State(numCorrect, 1)
            }
          }
          case State(numCorrect, numIncorrect) => {
            // **MAGIC** MaxIncorrectRule = 5
            State(numCorrect, Math.min(numIncorrect + 1, 5))
          }
        }
      }
    }
  })
  val numCorrect   = markers.map(_.numCorrect)
  val numIncorrect = markers.map(_.numIncorrect)

  val totalTypedCt = typedEvents.accum[Int](0, (_, n) => n + 1)
//  totalTypedCt.value().listen { n => println(s"Total Typed: $n keys.") }
  val totalTypedIncorrectCt = Cell.lift[Int, Int, Int]((total, correct) => total - correct, totalTypedCt, numCorrect)

  // collect list-of (exp, actual, time)
//  val keyEntryEvts = new Stream[(Char, Char, Long)]()
  val keyEntryEvts =
    typedCharEvents.map({ case (c, time) => {
      // more idiomatic way of achieving this?
      val expChar = Cell.lift((text: Token, idx: Int) => text.charAt(idx - text.getOffset()),
                              text,
                              numCorrect).sample()

      (expChar, c, time)
    }})
  val keyEntries: Cell[Array[(Char, Char, Long)]] =
    keyEntryEvts.accum(Array(), (tup, acc) => { acc :+ tup })

  // TODO 'Quit after typed certain number'
  // although this needs to distinguish between 'num-typed-correct' and 'position'
  private val endGameAtEndOfText = numCorrect.value().filter { numCorrect =>
    // TODO Slightly imprecise here, as this will escape before we type the last char.
    numCorrect == textSize.sample()
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
