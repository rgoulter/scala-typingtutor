package com.rgoulter.typingtutor

import javax.swing.text.Segment

import org.fife.ui.rsyntaxtextarea.Token
import org.fife.ui.rsyntaxtextarea.TokenImpl
import org.fife.ui.rsyntaxtextarea.TokenMaker
import org.fife.ui.rsyntaxtextarea.TokenTypes



object Utils {
  /** Print the given [[Token]] list to stdout, for debugging purposes. */
  def dumpTokens(initToken: Token, description: String = "Tokens"){
    println(s"Dump $description:")
    var tok = initToken

    while (tok != null) {
      println(s"TOK: $tok")
      tok = tok.getNextToken()
    }
  }

  /** Construct `Seq[Token]` from a given [[Token]].
    *
    * Creates copies of the [[Token]].
    */
  def toTokenSeq(tok: Token): Seq[Token] = {
    if (tok == null) {
      Seq.empty
    } else {
      // Copy the token, otherwise strange things happen,
      // and it's difficult to debug what's going on.
      new TokenImpl(tok) +: toTokenSeq(tok.getNextToken())
    }
  }

  /** Return an [[Iterable]] sequence of [[Token]]s from given text, by parsing
    * it using the given [[TokenMaker]].
    */
  def tokenIteratorOf(text: String, tokenMaker: TokenMaker): Iterable[Token] = {
    // n.b. getTokenList(Segment, initType, offset) gives Token[NullType] at EOL,
    // whose nextToken() is null.

    // offsets for start-of-lines in text.
    val lineOffsets: Seq[Int] =
      0 +:
      (text.zipWithIndex.filter({ case (c, idx) => c == '\n' })
                        .map({ case (c, idx) => idx + 1 }))

    lineOffsets.foldLeft(Seq[Token]())({ (res, offset) =>
      // TODO by right, TokenTypes.NULL may not be true, if the token
      // doesn't end on the end of the line.
      val segment = new Segment(text.toCharArray(), offset, text.length() - offset)
      val tokenSeq = toTokenSeq(tokenMaker.getTokenList(segment, TokenTypes.NULL, offset))

      res ++ tokenSeq
    })
  }
}
