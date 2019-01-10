package com.rgoulter

import scala.collection.JavaConverters.asScalaIteratorConverter

import cucumber.api.scala.{ScalaDsl, EN}
import cucumber.api.PendingException

import com.waioeka.sbt.runner.CucumberSpec

import org.fife.ui.rsyntaxtextarea.RSyntaxDocument
import org.fife.ui.rsyntaxtextarea.modes.JavaTokenMaker
import org.fife.ui.rsyntaxtextarea.modes.PlainTextTokenMaker

import org.scalatest.FeatureSpec
import org.scalatest.GivenWhenThen
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

class SyntaxHighlightingSpec extends FeatureSpec with GivenWhenThen {

  info("As a ...")
  info("I want ...")
  info("So I can ...")
  info("And ...")

  feature("Syntax Highlighting") {
    // 2016: regression:
    //   I was running into trouble in the GUI with RSyntaxTextArea because the
    //   RSyntaxDocument would cache a line of Tokens, so as the
    //   PartialTokenMaker.position advanced *between* calls to
    //   RSyntaxDocument.getTokensOnLine()
    //   (e.g. called by modelToView, when caret position is changed),
    //   the tokens used to paint would be 'stale'.

    // SMELL: 2019-01: this is a bad scenario title
    scenario("Partially typed document") {
      // SMELL: 2019-01: BDD step definition code should be congruent with step
      Given("a lexable document to practice typing on")
      // Constraint: Needs to be syntax highlightable; e.g. Java.
      // Constraint: Also, as is implicit elsewhere, needs to have
      //   comments/whitespace which we skip over
      val inputText = """public class X {
  //   this is a comment, the first line has 16 chars + NL.
  pu  blic void main(String args[]) {
  }
}
"""
      val inputLang = "java"

      val tokMak = new JavaTokenMaker()
      val partialTokMak = new PartialTokenMaker(tokMak)

      // NOTE: we use tokMak for making DocumentImpl,
      // since we want *full* sequence tokens in DocumentImpl
      // for deciding which parts of inputText are comments/skippable.
      val tokens = Utils.tokenIteratorOf(inputText, tokMak)
      val typingTutorDoc = new DocumentImpl(inputText, tokens)

      val typingKeyListener = new TypingKLHelper(typingTutorDoc)

//      syntaxDoc = new PartialRSyntaxDocument(typingKeyListener, partialTokMak)
      val syntaxDoc = new RSyntaxDocument("text/unknown")
      syntaxDoc.setSyntaxStyle(partialTokMak)
      syntaxDoc.insertString(0, inputText, null)

      val initialPosition = typingKeyListener.currentPos.sample()

      // SMELL: 2019-01: BDD step definition code should be congruent with step
      When("I input the correct characters")
      // Must be less than the document length
      val numberOfCharactersInput = 3

      for (i <- 1 to numberOfCharactersInput) {
        val expectedChar = typingKeyListener.currentChar.sample()
        typingKeyListener.input(expectedChar)
      }

      // Update position of PartialTokenMaker
      val currentPosition = typingKeyListener.currentPos.sample()
      partialTokMak.position = currentPosition

      Then("the marker should advance")
      // SMELL: 2016: Here we don't specify how much it advances by!
      //   Specifically, what we want is each time it advances,
      //   it should advance by at least 1.
      //   But in cases when skipping over comments/whitespace,
      //   it's possible a psychotic program could pass for just
      //   checking offsets.
      assert(currentPosition > initialPosition)

      // SMELL: 2019-01: BDD step definition code should be congruent with step
      And("the document should be highlighted up to this point")
      // Check tokens in doc. after offset are not Unstyled.
      import PartialTokenMaker.UNTYPED_TOKEN

      val highlightedTokens = syntaxDoc.iterator().asScala.toList
      assert(highlightedTokens.length > 0)

      for (tok <- highlightedTokens) {
        if (tok.getEndOffset() < currentPosition) {
          assert(tok.getType() != UNTYPED_TOKEN)
        }
      }

      // SMELL: 2019-01: BDD step definition code should be congruent with step
      And("the document should not highlighted further than this")
      // Check tokens in doc. after offset are Unstyled.

      // Implicit constraint on input: Whole document not yet typed in
      assume(currentPosition < typingTutorDoc.size)

      for (tok <- highlightedTokens) {
        if (tok.getOffset() > currentPosition) {
          assert(tok.getType() == UNTYPED_TOKEN)
        }
      }
    }

    // SMELL: 2019-01: this is a bad scenario title
    scenario("Skip over insignificant parts of program") {
      // SMELL: 2019-01: BDD step definition code should be congruent with step
      Given("a lexable document to practice typing on")
      // Constraint: Needs to be syntax highlightable; e.g. Java.
      // Constraint: Also, as is implicit elsewhere, needs to have
      //   comments/whitespace which we skip over
      val inputText = """public class X {
  //   this is a comment, the first line has 16 chars + NL.
  pu  blic void main(String args[]) {
  }
}
"""
      val inputLang = "java"

      val tokMak = new JavaTokenMaker()
      val partialTokMak = new PartialTokenMaker(tokMak)

      // NOTE: we use tokMak for making DocumentImpl,
      // since we want *full* sequence tokens in DocumentImpl
      // for deciding which parts of inputText are comments/skippable.
      val tokens = Utils.tokenIteratorOf(inputText, tokMak)
      val typingTutorDoc = new DocumentImpl(inputText, tokens)

      val typingKeyListener = new TypingKLHelper(typingTutorDoc)

//      syntaxDoc = new PartialRSyntaxDocument(typingKeyListener, partialTokMak)
      val syntaxDoc = new RSyntaxDocument("text/unknown")
      syntaxDoc.setSyntaxStyle(partialTokMak)
      syntaxDoc.insertString(0, inputText, null)

      val initialPosition = typingKeyListener.currentPos.sample()

      When("I input the expected characters, going passed whitespace/comments")
      // SMELL: 2016: This is fragile
      //  The best fix to this I can consider is, to find some Token(s)
      //  after the initialPosition (where we start typing from) which
      //  are comments/whitespace (>1), and type until the offset is
      //  that (counting number of characters pressed).
      val numberOfCharactersInput = 23

      for (i <- 1 to numberOfCharactersInput) {
        val expectedChar = typingKeyListener.currentChar.sample()
        typingKeyListener.input(expectedChar)
      }

      // Update position of PartialTokenMaker
      val currentPosition = typingKeyListener.currentPos.sample()
      partialTokMak.position = currentPosition

      Then("the marker should advance")
      // SMELL: 2016: Here we don't specify how much it advances by!
      //   Specifically, what we want is each time it advances,
      //   it should advance by at least 1.
      //   But in cases when skipping over comments/whitespace,
      //   it's possible a psychotic program could pass for just
      //   checking offsets.
      assert(currentPosition > initialPosition)

      // SMELL: 2019-01: BDD step definition code should be congruent with step
      And("it should skip over comments and extra whitespace")
      val positionDifference = currentPosition - initialPosition
      assert(positionDifference > numberOfCharactersInput)
    }
  }
}
