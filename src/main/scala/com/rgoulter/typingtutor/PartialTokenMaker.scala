package com.rgoulter.typingtutor

import org.fife.ui.rsyntaxtextarea.TokenMaker
import javax.swing.text.Segment
import org.fife.ui.rsyntaxtextarea.Token
import javax.swing.Action
import org.fife.ui.rsyntaxtextarea.OccurrenceMarker
import org.fife.ui.rsyntaxtextarea.TokenImpl
import org.fife.ui.rsyntaxtextarea.TokenTypes

class PartialTokenMaker(tokMak: TokenMaker) extends TokenMaker {
  // How far to keep "dull"
  var position: Int = 0



  def addToken(array: Array[Char], start: Int, end: Int, tokenType: Int, startOffset: Int): Unit = {
    // This one isn't used? Ok then..
    println("OUR PARTIAL addTOKEN!")

    tokMak.addToken(array, start, end, tokenType, startOffset)
  }



  def getTokenList(text: Segment, initTokType: Int, startOffset: Int): Token = {
    val tok = tokMak.getTokenList(text, initTokType, startOffset)

    def fixToken(tok: Token): Token = {
//      println(s"fixTok: StartOffset:$startOffset; ${tok.getOffset}, ${tok.getEndOffset}")

      if (tok == null || tok.getType() == TokenTypes.NULL) {
        tok
      } else {
        val nextTok = fixToken(tok.getNextToken())

        val fixedTok = new TokenImpl(tok)
        fixedTok.setNextToken(nextTok)

        val tokOffs = tok.getOffset()
        val tokEndOffs = tok.getEndOffset()

        if (tokEndOffs < position) {
          fixedTok
        } else if (tokOffs < position && tokEndOffs - tokOffs > 1) {
          // Hard case: the caret is in the middle of the word in the middle
          val headTok = new TokenImpl(text, tokOffs, position - 1, tokOffs, tok.getType(), tok.getLanguageIndex)
          val tailTok = new TokenImpl(text, position, tokEndOffs - 1, position, TokenTypes.IDENTIFIER, tok.getLanguageIndex)

          headTok.setNextToken(tailTok)
          tailTok.setNextToken(nextTok)

          headTok
        } else {
          fixedTok.setType(TokenTypes.IDENTIFIER)

          fixedTok
        }
      }
    }

    fixToken(tok)
  }

  def addNullToken(): Unit =
    tokMak.addNullToken()
  def getClosestStandardTokenTypeForInternalType(t: Int): Int =
    tokMak.getClosestStandardTokenTypeForInternalType(t)
  def getCurlyBracesDenoteCodeBlocks(i: Int): Boolean =
    tokMak.getCurlyBracesDenoteCodeBlocks(i)
  def getLastTokenTypeOnLine(segment: Segment, initTokType: Int): Int =
    tokMak.getLastTokenTypeOnLine(segment, initTokType)
  def getLineCommentStartAndEnd(i: Int): Array[String] =
    tokMak.getLineCommentStartAndEnd(i)
  def getInsertBreakAction(): Action =
    tokMak.getInsertBreakAction()
  def getMarkOccurrencesOfTokenType(t: Int): Boolean =
    tokMak.getMarkOccurrencesOfTokenType(t)
  def getOccurrenceMarker(): OccurrenceMarker =
    tokMak.getOccurrenceMarker()
  def getShouldIndentNextLineAfter(tok: Token): Boolean =
    tokMak.getShouldIndentNextLineAfter(tok)
  def isMarkupLanguage(): Boolean =
    tokMak.isMarkupLanguage()
  def isIdentifierChar(i: Int, c: Char): Boolean =
    tokMak.isIdentifierChar(i, c)
}