import java.awt.BorderLayout
import javax.swing._
import org.fife.ui.rtextarea._
import org.fife.ui.rsyntaxtextarea._
import java.awt.event.KeyListener
import java.awt.event.KeyEvent
import com.rgoulter.typingtutor.PartialTokenMaker
import org.fife.ui.rsyntaxtextarea.modes.JavaTokenMaker
import javax.swing.event.CaretListener
import javax.swing.event.CaretEvent
import java.awt.event.MouseListener
import java.awt.event.MouseEvent
import com.rgoulter.typingtutor.TypingKeyListener
import java.awt.Color
import java.awt.Point
import java.awt.event.WindowAdapter
import java.awt.event.WindowEvent

class TextEditorDemo extends JFrame {
  val SampleText = """public class HelloWorld {
  // This is a class
  public static void main(String args[]) {
    println("Hello World!")
  }
}"""

  val cp = new JPanel(new BorderLayout())

  val textArea = new RSyntaxTextArea(20, 60)

  val syntaxDoc = textArea.getDocument().asInstanceOf[RSyntaxDocument]
  val partialTokMak = new PartialTokenMaker(new JavaTokenMaker())
  syntaxDoc.setSyntaxStyle(partialTokMak)

  PartialTokenMaker.augmentStyleOfTextArea(textArea)

  textArea.setText(SampleText)

  textArea.setEditable(false)
  textArea.setHighlightCurrentLine(false)
  textArea.setCodeFoldingEnabled(false)
  textArea.setCaretStyle(0, CaretStyle.BLOCK_STYLE)
  textArea.getCaret().setVisible(true)
  textArea.getCaret().setBlinkRate(0)

  textArea.setCaretPosition(0)


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

  val typeTutorKL = new TypingKeyListener(textArea.getText(), (pos, numIncorrect) => {
    val caretColor =
      if (numIncorrect == 0) {
        Color.green
      } else {
        Color.red
      }

    textArea.setCaretColor(caretColor)
    textArea.setSelectionColor(caretColor)

    if (numIncorrect == 0) {
      textArea.setCaretPosition(pos)
    } else {
      textArea.setCaretPosition(pos + numIncorrect)
      textArea.moveCaretPosition(pos + 1)
    }

    forceRefresh()
  }, (stats) => {
    // Remove all the key listeners
    for (kl <- textArea.getKeyListeners()) { textArea.removeKeyListener(kl) }

    // Show the stats (in a dialogue window?)
    val dialog = new JFrame("Statistics")
    val label = new JLabel(s"""<html>
<table>
  <tr><td>Total</td><td>${stats.numTotal}</td></tr>
  <tr><td>Correct</td><td>${stats.numCorrect}</td></tr>
  <tr><td>Incorrect</td><td>${stats.numIncorrect}</td></tr>
</table></html>""")
    dialog.getContentPane().add(label)
    dialog.pack()
    dialog.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE)

    def disposeFrame() = this.dispose()

    dialog.addWindowListener(new WindowAdapter {
      override def windowClosed(we: WindowEvent): Unit = {
        // ...and when that closes, quit.
        disposeFrame()
      }
    })
    dialog.setLocationRelativeTo(this)

    dialog.setVisible(true)
  })

  // Open a file..
  textArea.addKeyListener(new KeyListener {
    override def keyPressed(ke: KeyEvent): Unit = {
      // Ctrl-O => Open file.
      if (ke.isControlDown()) {
        ke.getKeyCode() match {
          case KeyEvent.VK_O => {
            println("Open file")

            // TODO Save, Reset the Score.
            // (What's best to do if user starts typing, then opens file?).

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

  textArea.addKeyListener(typeTutorKL)

  // Disable mouse interaction
  for (ml <- textArea.getMouseListeners()) { textArea.removeMouseListener(ml) }
  for (ml <- textArea.getMouseMotionListeners()) { textArea.removeMouseMotionListener(ml) }

  val sp = new RTextScrollPane(textArea)
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