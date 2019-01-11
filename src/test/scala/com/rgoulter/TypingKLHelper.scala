package com.rgoulter

import sodium.Cell
import sodium.StreamSink

import com.rgoulter.typingtutor.Backspace
import com.rgoulter.typingtutor.Document
import com.rgoulter.typingtutor.TypedCharacter
import com.rgoulter.typingtutor.TypingEvent
import com.rgoulter.typingtutor.TypingKeyListener

/** Helper class for the Cucumber step definitions. */
class TypingKLHelper(
    val doc: Document,
    ks: StreamSink[TypingEvent] = new StreamSink[TypingEvent]())
    extends TypingKeyListener(new Cell(doc), ks) {
  def backspace() {
    ks.send(Backspace())
  }

  /** Sents an event for pressing the char. */
  def input(c: Char, t: => Long = System.currentTimeMillis()) {
    ks.send(TypedCharacter(c, t))
  }
}
