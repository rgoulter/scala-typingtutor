package com.rgoulter

import org.fife.ui.rsyntaxtextarea.modes.PlainTextTokenMaker

import org.scalatest.FlatSpec

import com.rgoulter.typingtutor.DocumentImpl
import com.rgoulter.typingtutor.Utils

class DocumentImplSpec extends FlatSpec {
  "DocumentImpl" should "not consider CR an expected-input character" in {
    /*
     * Context/Motivation:
     *  On Windows, text documents might be loaded with CR/LF
     *  as the system line ending.
     *  However, the text area considers 'enter' as '\n',
     *  and it's not intuitive for the user to enter '\r'
     *  themselves.
     */
    // Assemble
    val inputTextWithCR = "abc\r\ndef"
    val tokenMaker      = new PlainTextTokenMaker()
    val tokens          = Utils.tokenIteratorOf(inputTextWithCR, tokenMaker)
    val document        = new DocumentImpl(inputTextWithCR, tokens)

    // Act

    // Assert
    val expectedChars = document.expectedChars.values.toSet

    assert(!expectedChars.contains('\r'))
  }
}
