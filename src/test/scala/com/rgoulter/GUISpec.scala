package com.rgoulter

import java.awt.CardLayout
import java.awt.event.KeyEvent.VK_DOWN
import java.awt.event.KeyEvent.VK_ENTER
import java.util.concurrent.Callable
import javax.swing.JFrame
import javax.swing.JPanel
import javax.swing.JTextField
import javax.swing.text.JTextComponent

import scala.collection.JavaConverters.asScalaIteratorConverter

import org.fife.ui.rsyntaxtextarea.RSyntaxDocument
import org.fife.ui.rsyntaxtextarea.modes.PlainTextTokenMaker

import org.assertj.swing.core.GenericTypeMatcher
import org.assertj.swing.edt.GuiActionRunner
import org.assertj.swing.fixture.FrameFixture

import org.scalatest.FeatureSpec
import org.scalatest.GivenWhenThen
import org.scalatest.Matchers

import com.rgoulter.tag.GUITest

import com.rgoulter.typingtutor.gui.TypingTutorFrame
import com.rgoulter.typingtutor.gui.TypingTutorPanel
import com.rgoulter.typingtutor.SimpleDocumentImpl
import com.rgoulter.typingtutor.DocumentImpl
import com.rgoulter.typingtutor.PartialTokenMaker
import com.rgoulter.typingtutor.TypingKeyListener
import com.rgoulter.typingtutor.Utils

/** Unit Tests, particularly for regressions. */
class GUISpec extends FeatureSpec with GivenWhenThen {
  feature("Typing Tutor Frame Session") {
    scenario("Happy path with the sample document", GUITest) {
      // SMELL: 2019-01: this just assumes the sample document is the first!

      Given("the user has opened the Typing Tutor application")
      // SMELL: 2019-01: w/ Scala 2.12, this could be a SAM?
      val frame: TypingTutorFrame =
        GuiActionRunner.execute(new Callable[TypingTutorFrame]() {
          def call(): TypingTutorFrame = new TypingTutorFrame()
        })
      val window = new FrameFixture(frame)
      window.show()

      And("the user has selected the first document in the list")
      val fileSelectTable = window.table("file_selection_table")
      fileSelectTable.pressAndReleaseKeys(VK_DOWN, VK_ENTER)

      When("the user types in the full document")
      val textArea = window.textBox("tutor_text_area")
      // SMELL: 2019-01: Typing this much text is VERY slow!
      // ^ can remedy this by using a smaller example. :-)
      textArea.enterText("""public class Hello {
public static void main(String args[]) {
int x;
System.out.println("Hello World!");
}
}
""")

      Then("the user should be shown the results panel")
      window.textBox(new GenericTypeMatcher[JTextComponent](classOf[JTextComponent]) {
        def isMatching(textBox: JTextComponent): Boolean =
          textBox.getName() == "tutor_text_area" &&
          !textBox.isShowing()
      })
      window.panel(new GenericTypeMatcher[JPanel](classOf[JPanel]) {
        def isMatching(panel: JPanel): Boolean =
          panel.getName() == "typing_tutor_stats" &&
          panel.isShowing()
      })
    }
  }
}
