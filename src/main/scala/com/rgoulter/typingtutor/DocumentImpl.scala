package com.rgoulter.typingtutor

import scala.collection.immutable.BitSet
import scala.collection.immutable.SortedSet
import org.fife.ui.rsyntaxtextarea.Token
import org.fife.ui.rsyntaxtextarea.TokenTypes
import scala.collection.immutable.TreeMap



class SimpleDocumentImpl(text: String) extends Document {
  val size: Int = text.size

  val expectedChars: TreeMap[Int, Char] = {
    // whole range is typeable.
    val pairs = Range(0, size).toList.zip(text)
    new TreeMap[Int, Char]() ++ pairs
  }
}



class DocumentImpl(text: String, tokens: Iterable[Token]) extends Document {
  val size: Int = text.size

  val expectedChars: TreeMap[Int, Char] = {
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

    simplifiedTokens.foldLeft(new TreeMap[Int,Char]()) { (map, tok) =>
      if (tok.getType() == TokenTypes.NULL) {
        // This isn't quite right; the 'expected char' will be the character at
        // `bitset.last + 1`, whereas we want the expectedChar to be '\n'.
        val nextTup = ((map.last._1 + 1), '\n')
        map + nextTup
      } else if (tok.isWhitespace()) {
        // Limit whitespace characters we expect to just 1.
        val tokRange = Range(tok.getOffset(), tok.getOffset() + 1)
        map ++ tokRange.zip(tokRange.map(text.charAt))
      } else {
        val tokRange = Range(tok.getOffset(), tok.getEndOffset())
        map ++ tokRange.zip(tokRange.map(text.charAt))
      }
    }
  }
}
