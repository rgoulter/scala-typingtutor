package com.rgoulter.typingtutor

import scala.collection.immutable.SortedMap
import scala.collection.immutable.SortedSet



/**
 * Abstraction used by [[TypingKeyListener]] for what keys
 * the typing tutor is expecting.
 *
 * Since the typing tutor wants to skip over e.g. blank lines, comments,
 * not all parts of the document presented to the user need to be typed.
 * The offsets of the parts which do need to be typed (and the characters
 * expected to be typed at those offsets) are represented by this class.
 */
abstract class Document {
  /** Character in the document at a particular index/offset. */
  def charAt(offset: Int): Char =
    expectedChars(offset)

  /** Map of which characters are expected at which offsets.
    *
    * It is expected that `charAt(offset)` is the same as `expectedChars(offset)`. */
  val expectedChars: SortedMap[Int, Char];

  /** The set of which offsets the document contains.
    *
    * This is expected to be equivalent to `expectedChars.keySet`.
    */
  lazy val typeableOffsets: SortedSet[Int] =
    expectedChars.keySet;

  /** The earliest offset which the typing tutor should expect user to type. */
  lazy val initialOffset: Int = typeableOffsets.head

  /** Length of the document. */
  lazy val size: Int = typeableOffsets.last;

  /** The latest offset before the given offset, if one exists. */
  def previousTypeableOffset(offset: Int): Option[Int] =
    typeableOffsets.to(offset - 1).lastOption

  /** The earliest offset after the given offset, if one exists. */
  def nextTypeableOffset(offset: Int): Option[Int] =
    typeableOffsets.from(offset + 1).headOption

  def withInitialOffset(newInitialOffset: Int): Document
}
