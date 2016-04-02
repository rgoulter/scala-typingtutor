import java.awt.BorderLayout
import javax.swing._
import org.fife.ui.rtextarea._
import org.fife.ui.rsyntaxtextarea._
import java.awt.event.KeyListener
import java.awt.event.KeyEvent
import com.rgoulter.typingtutor.PartialTokenMaker
import org.fife.ui.rsyntaxtextarea.modes.JavaTokenMaker

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

  textArea.setText(SampleText)

  textArea.setEditable(false)
  textArea.setHighlightCurrentLine(false)
  textArea.setCodeFoldingEnabled(false)
  textArea.setCaretStyle(0, CaretStyle.BLOCK_STYLE)
  textArea.getCaret().setVisible(true)

  textArea.setCaretPosition(0)

  // To save time, the text area only updates when
  // things change. This isn't ideal.
  //
  // This implementation is hack-ish, but it's not
  // easy to figure out what to subclass.
  private def forceRefresh(): Unit = {
    val pos = textArea.getCaretPosition()
    textArea.setText(textArea.getText())
    textArea.setCaretPosition(pos)
  }

  textArea.addKeyListener(new KeyListener {
    override def keyPressed(ke: KeyEvent): Unit = {}
    override def keyReleased(ke: KeyEvent): Unit = {}
    override def keyTyped(ke: KeyEvent): Unit = {
      val caretPosition = textArea.getCaretPosition()
      val charAtPos = textArea.getText().charAt(caretPosition)
      val pressedChar = ke.getKeyChar

      pressedChar match {
        case '\b' => {
          if (caretPosition > 0) {
            textArea.setCaretPosition(caretPosition - 1)
            forceRefresh()
          }
        }
        case '\n' => {
          // ignore
        }
        case c => { // what about characters like 'Home'?
          println(s"Pressed Key '$charAtPos':$caretPosition <= '$pressedChar'")

          if (caretPosition + 1 < textArea.getText().length()) {
            textArea.setCaretPosition(caretPosition + 1)
            forceRefresh()
          }
        }
      }

      partialTokMak.position = textArea.getCaretPosition()
    }
  })

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