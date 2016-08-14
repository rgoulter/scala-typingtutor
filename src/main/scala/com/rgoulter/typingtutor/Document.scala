package com.rgoulter.typingtutor

import scala.collection.immutable.SortedSet

abstract class Document {
//      charAt(), and tokenAt(), and documentSize..?
  def charAt(offset: Int): Char;

  val size: Int;

  val typeableOffsets: SortedSet[Int];

  lazy val initialOffset: Int = typeableOffsets.head

  // Return the previous typeable offset, if it exists.
  def previousTypeableOffset(offset: Int): Option[Int] =
    typeableOffsets.to(offset - 1).lastOption

  // Return the previous typeable offset, if it exists.
  def nextTypeableOffset(offset: Int): Option[Int] =
    typeableOffsets.from(offset + 1).headOption
}