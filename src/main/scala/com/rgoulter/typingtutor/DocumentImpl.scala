package com.rgoulter.typingtutor

import org.fife.ui.rsyntaxtextarea.Token
import scala.collection.immutable.SortedSet
import scala.collection.immutable.BitSet
import org.fife.ui.rsyntaxtextarea.TokenTypes

class SimpleDocumentImpl(text: String) extends Document {
  def charAt(offset: Int): Char = text.charAt(offset)

  val size: Int = text.size

  val typeableOffsets: SortedSet[Int] = {
    // whole range is typeable.
    BitSet.empty ++ (0 to size)
  }
}

class DocumentImpl(text: String, tokens: Iterable[Token]) extends Document {
  def charAt(offset: Int): Char = text.charAt(offset)

  val size: Int = text.size

  val typeableOffsets: SortedSet[Int] = {
    // Ideally, we:
    // - skip comments, (incl. trailing)
    // - skip leading spaces,
    // - skip trailing spaces (aside from '\n').
    // - skip blank lines

    def isSkippable(tok: Token): Boolean = {
      tok.isCommentOrWhitespace() ||
      tok.getType() == TokenTypes.NULL
    }

    def dominantSkippableTok(tok1: Token, tok2: Token): Token = {
      if (tok1.getType() == TokenTypes.NULL) {
        tok1
      } else if (tok2.getType() == TokenTypes.NULL) {
        tok2
      } else {
        tok1
      }
    }

    // The logic here is a bit convoluted,
    // but essentially only add the "dominant" skippable token between
    // non-skippable tokens.
    val init : (Seq[Token], Option[Token]) = (Seq(), None)
    val (simplifiedTokens,_) = tokens.foldRight(init) ({ (tok, tup) =>
      val (res, skippableTok) = tup

      if (tok.isComment()) {
        (res, skippableTok)
      } else if (tok.isWhitespace() || tok.getType() == TokenTypes.NULL) {
        skippableTok match {
          case Some(wsTok) =>
            (res, Some(dominantSkippableTok(tok, wsTok)))
          case None =>
            (res, Some(tok))
        }
      } else {
        skippableTok match {
          case Some(wsTok) => {
            (tok +: wsTok +: res, None)
          }
          case None => {
            (tok +: res, None)
          }
        }
      }
    })

    BitSet.empty ++ (0 to size)
    simplifiedTokens.foldLeft(BitSet.empty) { (bitset, tok) =>
      if (tok.getType() == TokenTypes.NULL) {
        // This isn't quite right; the 'expected char' will be the character at
        // `bitset.last + 1`, whereas we want the expectedChar to be '\n'.
        bitset + (bitset.last + 1)
      } else if (tok.isWhitespace()) {
        // Limit whitespace characters we expect to just 1.
        val tokRange = Range(tok.getOffset(), tok.getOffset() + 1)
        bitset ++ tokRange
      } else {
        val tokRange = Range(tok.getOffset(), tok.getEndOffset())
        bitset ++ tokRange
      }
    }
  }
}