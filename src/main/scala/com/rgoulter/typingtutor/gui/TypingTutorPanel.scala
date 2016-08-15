package com.rgoulter.typingtutor.gui

import java.awt.BorderLayout
import java.awt.Color
import java.awt.event.KeyEvent
import java.awt.event.KeyListener

import javax.swing.JPanel

import org.fife.ui.rtextarea.CaretStyle
import org.fife.ui.rtextarea.RTextScrollPane
import org.fife.ui.rsyntaxtextarea.TokenMaker
import org.fife.ui.rsyntaxtextarea.RSyntaxDocument
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea

import sodium.CellSink

import com.rgoulter.typingtutor.Document
import com.rgoulter.typingtutor.PartialTokenMaker
import com.rgoulter.typingtutor.TypingKeyListener



/** Manages the `TextArea` with the typing tutor. */
class TypingTutorPanel(text: String,
                       document: Document,
                       tokenMaker: TokenMaker) extends JPanel {
  val textArea = new RSyntaxTextArea(20, 60)

  PartialTokenMaker.augmentStyleOfTextArea(textArea)

  textArea.setEditable(false)
  textArea.setHighlightCurrentLine(false)
  textArea.setCodeFoldingEnabled(false)
  textArea.setCaretStyle(0, CaretStyle.BLOCK_STYLE)
  textArea.getCaret().setVisible(true)
  textArea.getCaret().setBlinkRate(0)

  textArea.setCaretPosition(0)

  // TMP: Open a file.
//  textArea.addKeyListener(new KeyListener {
//    override def keyPressed(ke: KeyEvent): Unit = {
//      // Ctrl-O => Open file.
//      if (ke.isControlDown()) {
//        ke.getKeyCode() match {
//          case KeyEvent.VK_O => {
//            val chooser = new JFileChooser()
//            val retVal = chooser.showOpenDialog(frame)
//
//            if(retVal == JFileChooser.APPROVE_OPTION) {
//              val selectedFile = chooser.getSelectedFile()
//
//              // Use the file extension to set/update the TokenMaker
//              val origTokMak = Utils.tokenMakerForFile(selectedFile)
//              partialTokMak = new PartialTokenMaker(origTokMak)
//              syntaxDoc.setSyntaxStyle(partialTokMak)
//
//              val source = scala.io.Source.fromFile(selectedFile)
//              val text = source.mkString
//              source.close()
//
//              val tokenIterable = Utils.tokenIteratorOf(text, origTokMak)
//              val doc = new DocumentImpl(text, tokenIterable)
//
//              updateText(text, doc.initialOffset)
//
//              textCell.send(doc)
//            }
//
//            ke.consume()
//          }
//          case _ => ()
//        }
//      }
//    }
//
//    override def keyReleased(ke: KeyEvent): Unit = {}
//    override def keyTyped(ke: KeyEvent): Unit = {}
//  })

  // Ignore/Suppress keys which move the cursor.
  textArea.addKeyListener(new KeyListener {
    override def keyPressed(ke: KeyEvent): Unit = {
      ke.getKeyCode() match {
        case KeyEvent.VK_LEFT  => ke.consume()
        case KeyEvent.VK_RIGHT => ke.consume()
        case KeyEvent.VK_UP    => ke.consume()
        case KeyEvent.VK_DOWN  => ke.consume()
        case KeyEvent.VK_END   => ke.consume()
        case KeyEvent.VK_HOME  => ke.consume()
        case KeyEvent.VK_PAGE_UP   => ke.consume()
        case KeyEvent.VK_PAGE_DOWN => ke.consume()
        case _ => ()
      }
    }

    override def keyReleased(ke: KeyEvent): Unit = {}
    override def keyTyped(ke: KeyEvent): Unit = {}
  })


  // To save time, the text area only updates when
  // things change. This isn't ideal.
  //
  // This implementation is hack-ish, but it's not
  // easy to figure out what to subclass.
  private def forceRefresh(): Unit = {
    val pos = textArea.getCaretPosition()
    val selStart = textArea.getSelectionStart
    val selEnd = textArea.getSelectionEnd

    partialTokMak.position = selStart
    textArea.setText(textArea.getText())

    textArea.setCaretPosition(selStart)
    textArea.moveCaretPosition(selEnd)
  }

  // Every time we set the text..
  def updateText(text: String, initPos: Int = 0): Unit = {
    textArea.setText(text)
    textArea.setCaretPosition(initPos)
    textArea.getCaret().setVisible(true)
  }

  updateText(text, document.initialOffset)


  val syntaxDoc = textArea.getDocument().asInstanceOf[RSyntaxDocument];

  // I don't like the idea of a mutable variable;
  // Maybe with appropriate signals/etc. in an FRP system,
  // could attach partialTokMak to listen to TypKL's cursor pos.
  var partialTokMak = new PartialTokenMaker(tokenMaker)
  syntaxDoc.setSyntaxStyle(partialTokMak)


  private val textCell = new CellSink[Document](document)
  val typeTutorKL = new TypingKeyListener(textCell)

  // n.b. important that this TypingKeyListener gets added after the other KeyListeners,
  // which intercept keystrokes which need to be ignored.
  textArea.addKeyListener(typeTutorKL)

  // n.b. it's important that a reference to this `listener` is retained,
  // or else the listener's callback won't be executed.
  private val listener = typeTutorKL.markers.value().listen(state => {
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

    forceRefresh()
  })


  val scrollPane = new RTextScrollPane(textArea)
  setLayout(new BorderLayout())
  add(scrollPane)
}
