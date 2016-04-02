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
  })

  textArea.addKeyListener(typeTutorKL)

  // Disable mouse interaction
  val typeTutorML = new MouseListener {
    def mousePressed(me: MouseEvent): Unit = {
      me.consume()
    }
    def mouseReleased(me: MouseEvent): Unit = {
      me.consume()
    }
    def mouseClicked(me: MouseEvent): Unit = {
      me.consume()
    }
    def mouseEntered(me: MouseEvent): Unit = {}
    def mouseExited(me: MouseEvent): Unit = {}
  }

  for (ml <- textArea.getMouseListeners()) { textArea.removeMouseListener(ml) }

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