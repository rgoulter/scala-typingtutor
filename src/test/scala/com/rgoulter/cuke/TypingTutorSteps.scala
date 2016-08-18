package com.rgoulter.cuke

import scala.collection.JavaConverters.asScalaIteratorConverter

import cucumber.api.scala.{ScalaDsl, EN}
import cucumber.api.PendingException

import org.fife.ui.rsyntaxtextarea.RSyntaxDocument
import org.fife.ui.rsyntaxtextarea.modes.JavaTokenMaker
import org.fife.ui.rsyntaxtextarea.modes.PlainTextTokenMaker

import org.scalatest.Matchers

import sodium.Cell
import sodium.StreamSink

import com.rgoulter.typingtutor.Backspace
import com.rgoulter.typingtutor.Document
import com.rgoulter.typingtutor.DocumentImpl
import com.rgoulter.typingtutor.PartialTokenMaker
import com.rgoulter.typingtutor.TypedCharacter
import com.rgoulter.typingtutor.TypingEvent
import com.rgoulter.typingtutor.TypingKeyListener
import com.rgoulter.typingtutor.Utils



/** Helper class for the Cucumber step definitions. */
class TypingKLHelper(val doc: Document,
                     ks: StreamSink[TypingEvent] = new StreamSink[TypingEvent]())
    extends TypingKeyListener(new Cell(doc), ks) {
  def backspace() {
    ks.send(Backspace())
  }

  /** Sents an event for pressing the char. */
  def press(c: Char, t: => Long = System.currentTimeMillis()) {
    ks.send(TypedCharacter(c, t))
  }
}



class TypingTutorSteps extends ScalaDsl with EN with Matchers {
  var partialTokMak : PartialTokenMaker = _
  var syntaxDoc: RSyntaxDocument = _
  var typingTutorDoc: Document = _
  var typingKeyListener: TypingKLHelper = _

  var initialPosition: Int = _
  var numberOfCharactersPressed: Int = Integer.MAX_VALUE
  var currentPosition: Int = _


  Before() { scenario =>
    // Called before each scenario.
    // Run any cleanup here.
  }


  Given("""^a document to practice typing on$"""){ () =>
    // Arbitrary; any kind of document
    val inputText = """hello world this is a plain document
"""
    val inputLang = "plaintext"

    val tokMak = new PlainTextTokenMaker()
    partialTokMak = new PartialTokenMaker(tokMak)

    syntaxDoc = new RSyntaxDocument("text/unknown")
    syntaxDoc.setSyntaxStyle(partialTokMak)

    // NOTE: we use tokMak for making DocumentImpl,
    // since we want *full* sequence tokens in DocumentImpl
    // for deciding which parts of inputText are comments/skippable.
    val tokens = Utils.tokenIteratorOf(inputText, tokMak)
    typingTutorDoc = new DocumentImpl(inputText, tokens)

    typingKeyListener = new TypingKLHelper(typingTutorDoc) 

    initialPosition = typingKeyListener.currentPos.sample()
  }


  Given("""^a lexable document to practice typing on$"""){ () =>
    // Constraint: Needs to be syntax highlightable; e.g. Java.
    // Constraint: Also, as is implicit elsewhere, needs to have
    //   comments/whitespace which we skip over
    val inputText = """  2 * 3 /* and ? */ + 4;"""
    val inputLang = "java"

    val tokMak = new JavaTokenMaker()
    partialTokMak = new PartialTokenMaker(tokMak)

    syntaxDoc = new RSyntaxDocument("text/unknown")
    syntaxDoc.setSyntaxStyle(partialTokMak)

    // NOTE: we use tokMak for making DocumentImpl,
    // since we want *full* sequence tokens in DocumentImpl
    // for deciding which parts of inputText are comments/skippable.
    val tokens = Utils.tokenIteratorOf(inputText, tokMak)
    typingTutorDoc = new DocumentImpl(inputText, tokens)

    typingKeyListener = new TypingKLHelper(typingTutorDoc) 

    initialPosition = typingKeyListener.currentPos.sample()
  }


  Given("""^I have typed some of it$"""){ () =>
    // Arbitrary; just correctly type some of it.
    numberOfCharactersPressed = 8

    for (i <- 1 to numberOfCharactersPressed) {
      val expectedChar = typingKeyListener.currentChar.sample()
      typingKeyListener.press(expectedChar)
    }
  }


  When("""^I type in the expected characters$"""){ () =>
    // XXX Implicit constraint on input:
    //   An additional assumption here (used in SyntaxHighlighting feature)
    //   is that the Cursor passes over some comments/whitespace.

    // XXX This is fragile
    numberOfCharactersPressed = 8

    for (i <- 1 to numberOfCharactersPressed) {
      val expectedChar = typingKeyListener.currentChar.sample()
      typingKeyListener.press(expectedChar)
    }
  }


  // XXX This is very similar to "I type in the expected characters";
  //   More than code dup; inconsistent terminology is the problem.
  //   What do we input? Keys or characters?
  //   We press keys, input characters!
  When("""^I type in the correct keys$"""){ () =>
    // Must be less than the document length
    numberOfCharactersPressed = 3

    for (i <- 1 to numberOfCharactersPressed) {
      val expectedChar = typingKeyListener.currentChar.sample()
      typingKeyListener.press(expectedChar)
    }
  }


  When("""^I type in several incorrect keys$"""){ () =>
    // Can only type up-to 5 incorrect chars before
    // things are ignored.
    // But, scenario gives no constraints, so.
    val numCharsToType = 3

    for (i <- 1 to numCharsToType) {
      val expectedChar = typingKeyListener.currentChar.sample()
      val incorrectChar = if (expectedChar == 'x') 'y' else 'x'
      typingKeyListener.press(incorrectChar)
    }
  }


  Then("""^the cursor should advance$"""){ () =>
    // XXX Here we don't specify how much it advances by!
    //   Specifically, what we want is each time it advances,
    //   it should advance by at least 1.
    //   But in cases when skipping over comments/whitespace,
    //   it's possible a psychotic program could pass for just
    //   checking offsets.

    currentPosition = typingKeyListener.currentPos.sample()

    assert(currentPosition > initialPosition)
  }


  Then("""^the cursor should indicate an error$"""){ () =>
    // XXX: s/"an error"/"some incorrect chars have been typed"/
    // AFAICT, this means
    //   Cursor.numIncorrect > 0
    val currentNumIncorrect = typingKeyListener.markers.sample().numIncorrect

    assert(currentNumIncorrect > 0)
  }


  Then("""^it should skip over comments and extra whitespace$"""){ () =>
    // XXX Implicit constraint on input:
    //   i.e. that there are comments and whitespace to skip over!

    val positionDifference = currentPosition - initialPosition
    assert(positionDifference > numberOfCharactersPressed)
  }


  Then("""^the document should be highlighted up to this point$"""){ () =>
    // Check tokens in doc. after offset are not Unstyled.
    import PartialTokenMaker.UNTYPED_TOKEN

    val tokens = syntaxDoc.iterator().asScala

    for (tok <- tokens) {
      if (tok.getEndOffset() < currentPosition) {
        assert(tok.getType() != UNTYPED_TOKEN)
      }
    }
  }


  Then("""^the document should not highlighted further than this$"""){ () =>
    // Check tokens in doc. after offset are Unstyled.
    import PartialTokenMaker.UNTYPED_TOKEN

    // Implicit constraint on input: Whole document not yet typed in
    assume(currentPosition < typingTutorDoc.size)

    val tokens = syntaxDoc.iterator().asScala

    for (tok <- tokens) {
      if (tok.getOffset() > currentPosition) {
        assert(tok.getType() == UNTYPED_TOKEN)
      }
    }
  }


//  Then("""^the user interface should reflect this$"""){ () =>
//    // AFAICT, just check state of Cursor, & that RSynTA or so has this?
//
//    // n.b. I'm not particularly sure how to check/assert that the *visual*
//    // highlighting in RSyntaxTextArea is as we expect it to be.
//    //
//    // e.g. RSyntaxTextArea only refreshes the syntax highlighting where the
//    // text changes, and our code manually forces it to refresh, somehow.
//    //
//    // -- My point is, how to assert here that the presented styles
//    // did change?
//    // It may be that checking
//    //    RSyntaxDocument.iterator(): Iterator<Token>
//    // is enough. (Exploratory Unit testing may show this).
//
//    // XXX I get the feeling that whatever code I use here will be usable
//    // in the actual TypKL / TypTutPanel.RSynDoc,
//
//    throw new PendingException()
//  }
}
