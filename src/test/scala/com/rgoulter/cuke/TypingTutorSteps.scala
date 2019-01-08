package com.rgoulter.cuke

import scala.collection.JavaConverters.asScalaIteratorConverter

import cucumber.api.scala.{ScalaDsl, EN}
import cucumber.api.PendingException

import com.waioeka.sbt.runner.CucumberSpec

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



/** Allows SBT to run Cucumber tests with `sbt test`. */
class CucumberTestSuite extends CucumberSpec



/** Helper class for the Cucumber step definitions. */
class TypingKLHelper(val doc: Document,
                     ks: StreamSink[TypingEvent] = new StreamSink[TypingEvent]())
    extends TypingKeyListener(new Cell(doc), ks) {
  def backspace() {
    ks.send(Backspace())
  }

  /** Sents an event for pressing the char. */
  def input(c: Char, t: => Long = System.currentTimeMillis()) {
    ks.send(TypedCharacter(c, t))
  }
}



class TypingTutorSteps extends ScalaDsl with EN with Matchers {
  var partialTokMak : PartialTokenMaker = _
  var syntaxDoc: RSyntaxDocument = _
  var typingTutorDoc: Document = _
  var typingKeyListener: TypingKLHelper = _

  var initialPosition: Int = _
  var numberOfCharactersInput: Int = Integer.MAX_VALUE
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

    // NOTE: we use tokMak for making DocumentImpl,
    // since we want *full* sequence tokens in DocumentImpl
    // for deciding which parts of inputText are comments/skippable.
    val tokens = Utils.tokenIteratorOf(inputText, tokMak)
    typingTutorDoc = new DocumentImpl(inputText, tokens)

    typingKeyListener = new TypingKLHelper(typingTutorDoc) 

    syntaxDoc = new RSyntaxDocument("text/unknown")
    syntaxDoc.setSyntaxStyle(partialTokMak)
    syntaxDoc.insertString(0, inputText, null)

    initialPosition = typingKeyListener.currentPos.sample()
  }


  Given("""^a lexable document to practice typing on$"""){ () =>
    // Constraint: Needs to be syntax highlightable; e.g. Java.
    // Constraint: Also, as is implicit elsewhere, needs to have
    //   comments/whitespace which we skip over
//    val inputText = """  2 * 3 /* and ? */ + 4;"""
    val inputText = """public class X {
  // this is a comment, the first line has 16 chars + NL.
  public void main(String args[]) {
  }
}
"""
    val inputLang = "java"

    val tokMak = new JavaTokenMaker()
    partialTokMak = new PartialTokenMaker(tokMak)

    // NOTE: we use tokMak for making DocumentImpl,
    // since we want *full* sequence tokens in DocumentImpl
    // for deciding which parts of inputText are comments/skippable.
    val tokens = Utils.tokenIteratorOf(inputText, tokMak)
    typingTutorDoc = new DocumentImpl(inputText, tokens)

    typingKeyListener = new TypingKLHelper(typingTutorDoc) 

//    syntaxDoc = new PartialRSyntaxDocument(typingKeyListener, partialTokMak)
    syntaxDoc = new RSyntaxDocument("text/unknown")
    syntaxDoc.setSyntaxStyle(partialTokMak)
    syntaxDoc.insertString(0, inputText, null)

    initialPosition = typingKeyListener.currentPos.sample()
  }


  When("""^I input the expected characters, going passed whitespace/comments$"""){ () =>
    // TODO This is fragile
    // The best fix to this I can consider is, to find some Token(s) after the
    // initialPosition (where we start typing from) which are comments/whitespace (>1),
    // and type until the offset is that (counting number of characters pressed).
    numberOfCharactersInput = 23

    for (i <- 1 to numberOfCharactersInput) {
      val expectedChar = typingKeyListener.currentChar.sample()
      typingKeyListener.input(expectedChar)
    }

    // Update position of PartialTokenMaker
    currentPosition = typingKeyListener.currentPos.sample()
    partialTokMak.position = currentPosition
  }


  // XXX This is very similar to "I type in the expected characters";
  //   More than code dup; inconsistent terminology is the problem.
  //   What do we input? Keys or characters?
  //   We press keys, input characters!
  When("""^I input the correct characters$"""){ () =>
    // Must be less than the document length
    numberOfCharactersInput = 3

    for (i <- 1 to numberOfCharactersInput) {
      val expectedChar = typingKeyListener.currentChar.sample()
      typingKeyListener.input(expectedChar)
    }

    // Update position of PartialTokenMaker
    currentPosition = typingKeyListener.currentPos.sample()
    partialTokMak.position = currentPosition
  }


  When("""^I input several incorrect characters$"""){ () =>
    // Can only type up-to 5 incorrect chars before
    // things are ignored.
    // But, scenario gives no constraints, so.
    val numCharsToType = 3

    for (i <- 1 to numCharsToType) {
      val expectedChar = typingKeyListener.currentChar.sample()
      val incorrectChar = if (expectedChar == 'x') 'y' else 'x'
      typingKeyListener.input(incorrectChar)
    }

    currentPosition = typingKeyListener.currentPos.sample()
  }


  Then("""^the marker should advance$"""){ () =>
    // TODO Here we don't specify how much it advances by!
    //   Specifically, what we want is each time it advances,
    //   it should advance by at least 1.
    //   But in cases when skipping over comments/whitespace,
    //   it's possible a psychotic program could pass for just
    //   checking offsets.

    assert(currentPosition > initialPosition)
  }


  Then("""^the marker should indicate that incorrect characters have been input$"""){ () =>
    val currentNumIncorrect = typingKeyListener.markers.sample().numIncorrect

    assert(currentNumIncorrect > 0)
  }


  Then("""^it should skip over comments and extra whitespace$"""){ () =>
    val positionDifference = currentPosition - initialPosition
    assert(positionDifference > numberOfCharactersInput)
  }


  // I was running into trouble in the GUI with RSyntaxTextArea because the
  // RSyntaxDocument would cache a line of Tokens, so as the
  // PartialTokenMaker.position advanced *between* calls to
  // RSyntaxDocument.getTokensOnLine()
  // (e.g. called by modelToView, when caret position is changed),
  // the tokens used to paint would be 'stale'.


  Then("""^the document should be highlighted up to this point$"""){ () =>
    // Check tokens in doc. after offset are not Unstyled.
    import PartialTokenMaker.UNTYPED_TOKEN

    val tokens = syntaxDoc.iterator().asScala.toList
    assert(tokens.length > 0)

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

    val tokens = syntaxDoc.iterator().asScala.toList
    assert(tokens.length > 0)

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
