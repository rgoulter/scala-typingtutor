package com.rgoulter.typingtutor.gui

import java.awt.BorderLayout
import java.awt.Color
import java.awt.Point
import java.awt.Rectangle
import java.awt.event.ActionEvent
import java.awt.event.ComponentAdapter
import java.awt.event.ComponentEvent
import java.awt.event.FocusAdapter
import java.awt.event.FocusEvent
import java.awt.event.KeyAdapter
import java.awt.event.KeyEvent

import javax.swing.AbstractAction
import javax.swing.InputMap
import javax.swing.JComponent
import javax.swing.JPanel
import javax.swing.JScrollBar
import javax.swing.KeyStroke
import javax.swing.ScrollPaneConstants
import javax.swing.text.BadLocationException

import org.fife.ui.rtextarea.CaretStyle
import org.fife.ui.rtextarea.RTextScrollPane
import org.fife.ui.rsyntaxtextarea.TokenMaker
import org.fife.ui.rsyntaxtextarea.RSyntaxDocument
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea

import sodium.Cell
import sodium.StreamSink

import com.rgoulter.typingtutor.Document
import com.rgoulter.typingtutor.PartialTokenMaker
import com.rgoulter.typingtutor.StreamOfKeyListener
import com.rgoulter.typingtutor.TypingKeyListener

/** Manages the `TextArea` with the typing tutor. */
class TypingTutorPanel(documentC: Cell[Document]) extends JPanel {
  val textArea = new RSyntaxTextArea(25, 100)
  textArea.setName("tutor_text_area")

  val scrollPane = new RTextScrollPane(textArea)
  // Never show scrollbars;
  // at the moment this is slightly a bad idea, since we don't particularly wrap text.
  scrollPane.setHorizontalScrollBarPolicy(
    ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER)
  scrollPane.setVerticalScrollBarPolicy(
    ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER)
  scrollPane.setWheelScrollingEnabled(false)

  PartialTokenMaker.augmentStyleOfTextArea(textArea)

  textArea.setEditable(false)
  textArea.setHighlightCurrentLine(false)
  textArea.setCodeFoldingEnabled(false)
  textArea.setCaretStyle(0, CaretStyle.BLOCK_STYLE)
  textArea.getCaret().setVisible(true)
  textArea.getCaret().setBlinkRate(0)

  for (ml <- textArea.getMouseListeners)
    textArea.removeMouseListener(ml)

  textArea.setCaretPosition(0)

  // Ignore/Suppress keys we don't use.
  // It would be distracting for user to be able to select-all,
  // or to scroll the scrollpane viewport up/down.
  private def clearInputMap(im: InputMap): Unit = {
    if (im != null) {
      im.clear()
      clearInputMap(im.getParent())
    }
  }
  clearInputMap(textArea.getInputMap(JComponent.WHEN_FOCUSED))
  clearInputMap(
    scrollPane.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT))

  val QuitSessionItem = "quitsession"

  val escKeyStroke  = KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0)
  val panelInputMap = getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT)
  panelInputMap.put(escKeyStroke, QuitSessionItem)

  val panelActionMap = getActionMap()
  panelActionMap.put(QuitSessionItem, new AbstractAction {
    override def actionPerformed(evt: ActionEvent): Unit = {
      pressedEscSink.send(())
    }
  })

  // Forces RSyntaxDocument to invalidate its cache for the tokenList of the
  // line the caret is on.
  // If this isn't called, the RSyntaxTextArea will only update the syntax
  // highlighting as the caret moves to new lines.
  private def forceRefresh(): Unit = {
    // This will invalidate RSyntaxDocument's cache, as the cache is
    // only kept for one line at a time.
    // TODO: May be better to subclass SyntaxDocument.
    val caretLine = textArea.getCaretLineNumber()
    syntaxDoc.getTokenListForLine(caretLine + 1)
  }

  val syntaxDoc = textArea.getDocument().asInstanceOf[RSyntaxDocument];
  // SMELL: 2016: I don't like the idea of a mutable variable;
  // Maybe with appropriate signals/etc. in an FRP system,
  // could attach partialTokMak to listen to TypKL's cursor pos.
  var partialTokMak: PartialTokenMaker = _;

  private def updateText(document: Document): Unit = {
    partialTokMak = new PartialTokenMaker(document.tokenMaker)
    syntaxDoc.setSyntaxStyle(partialTokMak)

    textArea.setText(document.text)
    textArea.setCaretPosition(document.initialOffset)
    textArea.getCaret().setVisible(true)
  }

  updateText(documentC.sample())
  documentC.updates().listen(updateText)

  private val typedEventsKL = new StreamOfKeyListener()
  val typeTutorKL           = new TypingKeyListener(documentC, typedEventsKL.typedEvents)

  def linesInView(): Option[(Int, Int)] = {
    val viewRect = scrollPane.getViewport().getViewRect()

    val topLeftPt = viewRect.getLocation()
    val bottomRightPt = new Point(
      topLeftPt.getX().toInt + viewRect.getWidth.toInt,
      topLeftPt.getY().toInt + viewRect.getHeight.toInt)

    // The main limitation of this computation is,
    // if even *1 pixel* of lineX is on screen, then lineX
    // it'll happily declare lineX as the first/last on screen.
    def firstOffset = textArea.viewToModel(topLeftPt)
    def lastOffset  = textArea.viewToModel(bottomRightPt)

    try {
      val firstLine = textArea.getLineOfOffset(firstOffset)
      val lastLine  = textArea.getLineOfOffset(lastOffset)

      Some((firstLine, lastLine))
    } catch {
      case e: BadLocationException => None
    }
  }

  // n.b. important that this TypingKeyListener gets added after the other KeyListeners,
  // which intercept keystrokes which need to be ignored.
  textArea.addKeyListener(typedEventsKL)

  textArea.addFocusListener(new FocusAdapter {
    override def focusGained(e: FocusEvent): Unit = {
      // Need to ensure caret remains visible,
      // e.g. when switching window and returning focus.
      textArea.getCaret().setVisible(true)
    }
  })

  def caretYInViewport: Int = {
    val caretYInTextArea =
      textArea.modelToView(textArea.getCaretPosition()).getY()
    val scrollPaneY = scrollPane.getViewport().getViewPosition().getY()

    (caretYInTextArea - scrollPaneY).toInt
  }

  // Of ScrollPane's height.
  def isCaretBeyondProportionOfHeight(prop: Double): Boolean = {
    val caretY = caretYInViewport
    val height = prop * scrollPane.getHeight()

    caretY > height
  }

  // Of ScrollPane's height.
  def rectOfCaretPlusProportionOfHeight(prop: Double): Rectangle = {
    assert(0 <= prop && prop <= 1)
    val padY   = (1 - prop) * scrollPane.getViewport().getHeight()
    val height = prop * scrollPane.getViewport().getHeight()
    val caretY = textArea.modelToView(textArea.getCaretPosition()).getY()

    new Rectangle(0,
                  caretY.toInt - padY.toInt,
                  textArea.getWidth(),
                  (caretY + height).toInt)
  }

  // n.b. it's important that a reference to this `listener` is retained,
  // or else the listener's callback won't be executed.
  private val listener = typeTutorKL.markers
    .values()
    .listen(state => {
      import state.numIncorrect
      import state.position

      val caretColor =
        if (numIncorrect == 0) {
          Color.green
        } else {
          Color.red
        }

      textArea.setCaretColor(caretColor)
      textArea.setSelectionColor(caretColor)

      if (numIncorrect == 0) {
        textArea.setCaretPosition(position)
      } else {
        textArea.setCaretPosition(position + numIncorrect)
        textArea.moveCaretPosition(position + 1)
      }

      if (textArea.getHeight() > 0 &&
          isCaretBeyondProportionOfHeight(0.8)) {
        // Can only check Caret position when size is positive.
        val rectToView = rectOfCaretPlusProportionOfHeight(0.8)

        // UI CHANGE! I hope the effect isn't too sudden.
        textArea.scrollRectToVisible(rectToView)
      }

      // SMELL: 2019-01: can just use a Cell, surely
      partialTokMak.position = position

      forceRefresh()
    })

  private val pressedEscSink = new StreamSink[Unit]()

  /** The offset when endgame is emitted. */
  val finishingOffset =
    typeTutorKL.reachedEnd
      .merge(pressedEscSink.snapshot(typeTutorKL.currentPos), { (l, r) =>
        l
      })

  /** Stats, emitted only at the end of the game/lesson. */
  val statsStream = finishingOffset.snapshot(typeTutorKL.stats)

  addComponentListener(new ComponentAdapter {
    override def componentShown(evt: ComponentEvent): Unit = {
      textArea.requestFocusInWindow()
    }
  })

  setLayout(new BorderLayout())
  add(scrollPane)
}
