import java.awt.BorderLayout
import java.awt.Color
import java.awt.Point
import java.awt.event.KeyEvent
import java.awt.event.KeyListener
import java.awt.event.MouseEvent
import java.awt.event.MouseListener
import java.awt.event.WindowAdapter
import java.awt.event.WindowEvent
import java.io.File

import javax.swing._
import javax.swing.event.CaretEvent
import javax.swing.event.CaretListener
import javax.swing.text.Segment

import scala.collection.JavaConverters.asScalaIteratorConverter

import org.fife.ui.rtextarea._
import org.fife.ui.rsyntaxtextarea._

import sodium.CellSink

import com.rgoulter.typingtutor.Document
import com.rgoulter.typingtutor.DocumentImpl
import com.rgoulter.typingtutor.PartialTokenMaker
import com.rgoulter.typingtutor.SimpleDocumentImpl
import com.rgoulter.typingtutor.TypingKeyListener



class TextEditorDemo extends JFrame {
  // XXX More difficult to extend 'frame', than to just make one..

  import com.rgoulter.typingtutor.Utils
  import com.rgoulter.typingtutor.Sample
  import Sample.{ SampleText, SampleDocument, SampleTextTokMak }

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
  textArea.addKeyListener(new KeyListener {
    override def keyPressed(ke: KeyEvent): Unit = {
      // Ctrl-O => Open file.
      if (ke.isControlDown()) {
        ke.getKeyCode() match {
          case KeyEvent.VK_O => {
            val chooser = new JFileChooser()
            val retVal = chooser.showOpenDialog(frame)

            if(retVal == JFileChooser.APPROVE_OPTION) {
              val selectedFile = chooser.getSelectedFile()

              // Use the file extension to set/update the TokenMaker
              val origTokMak = Utils.tokenMakerForFile(selectedFile)
              partialTokMak = new PartialTokenMaker(origTokMak)
              syntaxDoc.setSyntaxStyle(partialTokMak)

              val source = scala.io.Source.fromFile(selectedFile)
              val text = source.mkString
              source.close()

              val tokenIterable = Utils.tokenIteratorOf(text, origTokMak)
              val doc = new DocumentImpl(text, tokenIterable)

              updateText(text, doc.initialOffset)

              textCell.send(doc)
            }

            ke.consume()
          }
          case _ => ()
        }
      }
    }

    override def keyReleased(ke: KeyEvent): Unit = {}
    override def keyTyped(ke: KeyEvent): Unit = {}
  })

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

  updateText(SampleText, SampleDocument.initialOffset)



  val syntaxDoc = textArea.getDocument().asInstanceOf[RSyntaxDocument];

  // I don't like the idea of a mutable variable;
  // Maybe with appropriate signals/etc. in an FRP system,
  // could attach partialTokMak to listen to TypKL's cursor pos.
  var partialTokMak = new PartialTokenMaker(SampleTextTokMak)
  syntaxDoc.setSyntaxStyle(partialTokMak)


  private val textCell = new CellSink[Document](SampleDocument)
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



  val frame = this

  // Listen for an endgame
  val endGameListener = typeTutorKL.endGame.snapshot(typeTutorKL.stats).listen(stats => {
    // Remove all the key listeners
    for (kl <- textArea.getKeyListeners()) { textArea.removeKeyListener(kl) }

    // XXX This needs to be done using FRP
    // Show the stats (in a dialogue window?)
    val dialog = new JFrame("Statistics")
    val label = new JLabel(s"""<html>
<table>
  <tr><td>Total</td>    <td>${stats.numTotal}</td></tr>
  <tr><td>Correct</td>  <td>${stats.numCorrect}</td></tr>
  <tr><td>Incorrect</td><td>${stats.numIncorrect}</td></tr>
  <tr><td>Duration</td> <td>${stats.durationInMins} mins</td></tr>
  <tr><td>Accuracy</td> <td>${stats.accuracyPercent}%</td></tr>
  <tr><td>WPM</td>      <td>${stats.wpmStr}</td></tr>
</table></html>""")
    dialog.getContentPane().add(label)
    dialog.pack()
    dialog.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE)

    dialog.addWindowListener(new WindowAdapter {
      override def windowClosed(we: WindowEvent): Unit = {
        // ...and when that closes, quit.
        frame.dispose()
      }
    })
    dialog.setLocationRelativeTo(frame)

    dialog.setVisible(true)
  })

  // Disable mouse interaction
//  for (ml <- textArea.getMouseListeners()) { textArea.removeMouseListener(ml) }
//  for (ml <- textArea.getMouseMotionListeners()) { textArea.removeMouseMotionListener(ml) }

  val sp = new RTextScrollPane(textArea)
  val cp = new JPanel(new BorderLayout())
  cp.add(sp)
  setContentPane(cp)

  setTitle("Text Editor Demo")
  setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE)
  pack()
  setLocationRelativeTo(null)
}



object Main {
  def main(args: Array[String]): Unit = {
    // Start all Swing applications on the EDT.
    SwingUtilities.invokeLater(new Runnable() {
      def run(): Unit = {
        new TextEditorDemo().setVisible(true)
      }
    })
  }
}
