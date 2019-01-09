package com.rgoulter.typingtutor

import java.awt.Color

import javax.swing.Action
import javax.swing.text.Segment

import org.fife.ui.rsyntaxtextarea.OccurrenceMarker
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea
import org.fife.ui.rsyntaxtextarea.Style
import org.fife.ui.rsyntaxtextarea.Token
import org.fife.ui.rsyntaxtextarea.TokenImpl
import org.fife.ui.rsyntaxtextarea.TokenMaker
import org.fife.ui.rsyntaxtextarea.TokenTypes

object PartialTokenMaker {
  val UNTYPED_TOKEN = TokenTypes.DEFAULT_NUM_TOKEN_TYPES + 0;
  val UntypedStyle  = new Style(Color.gray)

  /** Adds `UntypedStyle` to the [[SyntaxScheme]] of the given
    * [[RSyntaxTextArea]].
    */
  def augmentStyleOfTextArea(textArea: RSyntaxTextArea): Unit = {
    val scheme = textArea.getSyntaxScheme()

    // Can't just setStyle(idx, Style),
    // as this is out of the default range.

    scheme.setStyles(scheme.getStyles() :+ UntypedStyle)
  }
}

/** Provides a "partially syntax-highlighted" effect for `RSyntaxTextArea`.
  * Used to adapt [[TokenMaker]] so that tokens after an offset are instead
  * of an `PartialTokenMaker.UntypedStyle`.
  *
  * It is important to call
  * `PartialTokenMaker.augmentStyleOfTextArea` with the [[RSyntaxTextArea]]
  * to add the styles which this class uses to the text area.
  *
  * Pass this to a syntax document by calling
  * `RSyntaxDocument.setSyntaxStyle` with an instance of this object.
  *
  * Update the effect of where the syntax style is highlighted-up-to.
  * (Although it can be finicky to force the [[RSyntaxTextArea]] to refresh
  * the syntax highlighting; re-setting the text works, but this then requires
  * the caret to be set again, too).
  */
class PartialTokenMaker(tokMak: TokenMaker) extends TokenMaker {
  // How far to keep "dull"
  var position: Int = 0

  def addToken(array: Array[Char],
               start: Int,
               end: Int,
               tokenType: Int,
               startOffset: Int): Unit = {
    // This one isn't used? Ok then.
    // println("OUR PARTIAL addTOKEN!")

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

        val tokOffs    = tok.getOffset()
        val tokEndOffs = tok.getEndOffset()

        if (tokEndOffs < position) {
          fixedTok
        } else if (tokOffs < position && tokEndOffs - tokOffs > 1) {
          // Hard case: the caret is in the middle of the word in the middle
          val headTok = new TokenImpl(text,
                                      tokOffs,
                                      position - 1,
                                      tokOffs,
                                      tok.getType(),
                                      tok.getLanguageIndex)
          val tailTok = new TokenImpl(text,
                                      position,
                                      tokEndOffs - 1,
                                      position,
                                      PartialTokenMaker.UNTYPED_TOKEN,
                                      tok.getLanguageIndex)

          headTok.setNextToken(tailTok)
          tailTok.setNextToken(nextTok)

          headTok
        } else {
          fixedTok.setType(PartialTokenMaker.UNTYPED_TOKEN)

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
