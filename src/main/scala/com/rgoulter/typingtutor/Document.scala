package com.rgoulter.typingtutor

import scala.collection.immutable.SortedMap
import scala.collection.immutable.SortedSet



abstract class Document {
  def charAt(offset: Int): Char =
    expectedChars(offset)

  val size: Int;

  val expectedChars: SortedMap[Int, Char];

  lazy val typeableOffsets: SortedSet[Int] =
    expectedChars.keySet;

  lazy val initialOffset: Int = typeableOffsets.head

  // Return the previous typeable offset, if it exists.
  def previousTypeableOffset(offset: Int): Option[Int] =
    typeableOffsets.to(offset - 1).lastOption

  // Return the previous typeable offset, if it exists.
  def nextTypeableOffset(offset: Int): Option[Int] =
    typeableOffsets.from(offset + 1).headOption
}
