package com.rgoulter.typingtutor

import scala.collection.immutable.SortedSet

abstract class Document {
//      charAt(), and tokenAt(), and documentSize..?
  def charAt(offset: Int): Char;

  val size: Int;

  val typeableOffsets: SortedSet[Int];
}