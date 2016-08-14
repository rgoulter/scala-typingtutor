package com.rgoulter.typingtutor

import org.fife.ui.rsyntaxtextarea.Token
import scala.collection.immutable.SortedSet
import scala.collection.immutable.BitSet

class SimpleDocumentImpl(text: String) extends Document {
  def charAt(offset: Int): Char = text.charAt(offset)

  val size: Int = text.size

  val typeableOffsets: SortedSet[Int] = {
    // whole range is typeable.
    BitSet.empty ++ (0 to size)
  }
}

class DocumentImpl(text: String, tokens: Iterator[Token]) extends Document {
  def charAt(offset: Int): Char = text.charAt(offset)

  val size: Int = text.size

  val typeableOffsets: SortedSet[Int] = {
    // For now, whole range is typeable.
    BitSet.empty ++ (0 to size)
  }
}