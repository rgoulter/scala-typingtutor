package com.rgoulter.typingtutor

import scala.collection.immutable.BitSet
import scala.collection.immutable.SortedSet
import scala.collection.immutable.TreeMap

import org.fife.ui.rsyntaxtextarea.Token
import org.fife.ui.rsyntaxtextarea.TokenMaker
import org.fife.ui.rsyntaxtextarea.TokenTypes
import org.fife.ui.rsyntaxtextarea.modes._

/** Trivial implementation of [[Document]] given a [[String]].
  *
  * All characters in the given text must be typed, (including
  * blank lines, comments).
  */
class SimpleDocumentImpl(text: String) extends Document {
  override def text(): String = text

  override def tokenMaker(): TokenMaker =
    new PlainTextTokenMaker()

  val expectedChars: TreeMap[Int, Char] = {
    // whole range is typeable.
    val pairs = Range(0, text.length()).toList.zip(text)
    new TreeMap[Int, Char]() ++ pairs
  }

  // SimpleDocumentImpl doesn't really do initialOffset
  def withInitialOffset(newInitialOffset: Int): Document =
    this
}

/** Implementation of [[Document]] which skips over e.g. comments, blank lines,
  * etc. using the given `Iterable[Token]`.
  */
class DocumentImpl(
    text: String,
    tokens: Iterable[Token],
    tokenMaker: TokenMaker,
    initOffs: Int = 0
) extends Document {
  override def text(): String = text

  override def tokenMaker(): TokenMaker = tokenMaker

  val expectedChars: TreeMap[Int, Char] = {
    // Ideally, we:
    // - skip comments, (incl. trailing)
    // - skip leading spaces,
    // - skip trailing spaces (aside from '\n').
    // - skip blank lines

    // Assuming `tok1` occurs before `tok2` in `tokens`,
    // select the earliest token for Newline,
    // or the earliest token otherwise.
    def dominantSkippableTok(tok1: Token, tok2: Token): Token = {
      if (tok1.getType() == TokenTypes.NULL) {
        tok1
      } else if (tok2.getType() == TokenTypes.NULL) {
        tok2
      } else {
        tok1
      }
    }

    // Reduce tokens down to simplifiedTokens such that at most one
    // whitespace/newline Token occurs between other Tokens.
    // (Tokens for comments are omitted entirely).
    val init: (Seq[Token], Option[Token]) = (Seq(), None)
    val (simplifiedTokens, _) = tokens.foldRight(init)({ (tok, tup) =>
      val (res, skippableTok) = tup

      if (tok.isComment()) {
        (res, skippableTok)
      } else if (tok.isWhitespace() ||
                 tok.getType() == TokenTypes.NULL ||
                 text.charAt(tok.getOffset()) == '\r') {
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

    simplifiedTokens.foldLeft(new TreeMap[Int, Char]()) { (map, tok) =>
      if (tok.getType() == TokenTypes.NULL) {
        // Token is Newline; expect user to type in '\n'.
        // Offset `map.last._1 + 1` deviates from `tok.getOffset()` in case
        // where document line has trailing spaces/comment.
        val nextTup = ((map.last._1 + 1), '\n')
        map + nextTup
      } else if (tok.isWhitespace()) {
        // Limit whitespace characters we expect to just 1 char.
        val tokRange = Range(tok.getOffset(), tok.getOffset() + 1)
        map ++ tokRange.zip(tokRange.map(text.charAt))
      } else {
        val tokRange = Range(tok.getOffset(), tok.getEndOffset())
        map ++ tokRange.zip(tokRange.map(text.charAt))
      }
    }
  }

  override lazy val initialOffset = {
    // This is where the 'starting offset' gets reset
    //  to the beginning of the document if the 'offset'
    //  reached the last typeable offset in a previous session.
    val fromGivenInitOffs = typeableOffsets.from(initOffs)

    // Need to compare against 'tail' of remaining
    //  typeable offsets, since the 'offset' will
    //  reach the last typeable offset but may not
    //  go beyond it (in TypingKeyListener).
    if (fromGivenInitOffs.tail.isEmpty) {
      typeableOffsets.head
    } else {
      fromGivenInitOffs.head
    }
  }

  def withInitialOffset(newInitialOffset: Int): Document = {
    new DocumentImpl(text, tokens, tokenMaker, newInitialOffset)
  }
}
