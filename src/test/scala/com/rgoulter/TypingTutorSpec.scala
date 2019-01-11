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

class TypingTutorSpec extends FeatureSpec with GivenWhenThen {

  info("As a person who types in code using the keyboard")
  info("I want a typing tutor which accepts only correct input")
  info("So I can practice disciplined typing")

  feature("Typing Tutor") {
    // SMELL: 2019-01: this is a bad scenario title
    scenario("Correctly type in shown document") {
      // SMELL: 2019-01: BDD step definition code should be congruent with step
      Given("a document to practice typing on")
      // Arbitrary; any kind of document
      val inputText = """hello world this is a plain document
"""
      val inputLang = "plaintext"

      val tokMak = new PlainTextTokenMaker()
      val partialTokMak = new PartialTokenMaker(tokMak)

      // NOTE: we use tokMak for making DocumentImpl,
      // since we want *full* sequence tokens in DocumentImpl
      // for deciding which parts of inputText are comments/skippable.
      val tokens = Utils.tokenIteratorOf(inputText, tokMak)
      val typingTutorDoc = new DocumentImpl(inputText, tokens)

      val typingKeyListener = new TypingKLHelper(typingTutorDoc)

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
    }

    // SMELL: 2019-01: this is a bad scenario title
    scenario("Incorrectly type in shown document") {
      // SMELL: 2019-01: BDD step definition code should be congruent with step
      Given("a document to practice typing on")
      // Arbitrary; any kind of document
      val inputText = """hello world this is a plain document
"""
      val inputLang = "plaintext"

      val tokMak = new PlainTextTokenMaker()
      val partialTokMak = new PartialTokenMaker(tokMak)

      // NOTE: we use tokMak for making DocumentImpl,
      // since we want *full* sequence tokens in DocumentImpl
      // for deciding which parts of inputText are comments/skippable.
      val tokens = Utils.tokenIteratorOf(inputText, tokMak)
      val typingTutorDoc = new DocumentImpl(inputText, tokens)

      val typingKeyListener = new TypingKLHelper(typingTutorDoc)

      val syntaxDoc = new RSyntaxDocument("text/unknown")
      syntaxDoc.setSyntaxStyle(partialTokMak)
      syntaxDoc.insertString(0, inputText, null)

      val initialPosition = typingKeyListener.currentPos.sample()

      When("I input several incorrect characters")
      // Can only type up-to 5 incorrect chars before
      // things are ignored.
      // But, scenario gives no constraints, so.
      val numCharsToType = 3

      for (i <- 1 to numCharsToType) {
        val expectedChar  = typingKeyListener.currentChar.sample()
        val incorrectChar = if (expectedChar == 'x') 'y' else 'x'
        typingKeyListener.input(incorrectChar)
      }

      val currentPosition = typingKeyListener.currentPos.sample()

      Then("the marker should indicate that incorrect characters have been input")
      val currentNumIncorrect = typingKeyListener.markers.sample().numIncorrect

      assert(currentNumIncorrect > 0)
    }
  }
}
