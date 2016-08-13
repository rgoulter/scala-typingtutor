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
import java.io.File
import org.apache.commons.io.FilenameUtils
import sodium.CellSink

class TextEditorDemo extends JFrame {
  // XXX More difficult to extend 'frame', than to just make one..

  val SampleText = """public class HelloWorld {
  // This is a class
  public static void main(String args[]) {
    println("Hello World!")
  }
}"""

  val cp = new JPanel(new BorderLayout())

  val textArea = new RSyntaxTextArea(20, 60)

  val syntaxDoc = textArea.getDocument().asInstanceOf[RSyntaxDocument]

  // I don't like the idea of a mutable variable;
  // Maybe with appropriate signals/etc. in an FRP system,
  // could attach partialTokMak to listen to TypKL's cursor pos.
  var partialTokMak = new PartialTokenMaker(new JavaTokenMaker())
  syntaxDoc.setSyntaxStyle(partialTokMak)

  PartialTokenMaker.augmentStyleOfTextArea(textArea)

  private val textCell = new CellSink[String](SampleText)

  // Every time we set the text..
  textCell.value().listen(text => {
    textArea.setText(text)
    textArea.setCaretPosition(0)
    textArea.getCaret().setVisible(true)

              // TODO Save, Reset the Score in the typeTutorKL
              // (What's best to do if user starts typing, then opens file?).
  })

//  textArea.setText(SampleText) //XXX

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

  val typeTutorKL = new TypingKeyListener(textCell)

  val listener = typeTutorKL.markers.value().listen(state => {
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
  def disposeFrame() = this.dispose()

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
        disposeFrame()
      }
    })
    dialog.setLocationRelativeTo(frame)

    dialog.setVisible(true)
  })

  // Open a file..
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
              partialTokMak = new PartialTokenMaker(Main.tokenMakerForFile(selectedFile))
              syntaxDoc.setSyntaxStyle(partialTokMak)

              val source = scala.io.Source.fromFile(selectedFile)
              val text = source.mkString
              source.close()

              textCell.send(text)
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

  textArea.addKeyListener(typeTutorKL)

  // Disable mouse interaction
//  for (ml <- textArea.getMouseListeners()) { textArea.removeMouseListener(ml) }
//  for (ml <- textArea.getMouseMotionListeners()) { textArea.removeMouseMotionListener(ml) }

  val sp = new RTextScrollPane(textArea)
  cp.add(sp)

  setContentPane(cp)
  setTitle("Text Editor Demo")
  setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE)
  pack()
  setLocationRelativeTo(null)
}

object Main {
  def tokenMakerForFile(f: File): TokenMaker = {
    import org.fife.ui.rsyntaxtextarea.modes._

    // Special cases
    if (f.getName() == "makefile") {
      return new MakefileTokenMaker()
    }

    val ext = FilenameUtils.getExtension(f.getName())

    // Not all the filetypes RSyntaxTextArea supports
    // are here; some extensions missing.
//       * x86 asm?
//       * bbcode??
//       * delphi?
//       * dockerfile?
//       * fortran?
//       * ....
//       * lisp??
    ext match {
      case "as"     => new ActionScriptTokenMaker()
      case "c"      => new CTokenMaker()
      case "clj"    => new ClojureTokenMaker()
      case "cpp"    => new CPlusPlusTokenMaker()
      case "cs"     => new CSharpTokenMaker()
      case "css"    => new CSSTokenMaker()
      case "d"      => new DTokenMaker()
      case "dart"   => new DartTokenMaker()
      case "dtd"    => new DtdTokenMaker()
      case "groovy" => new GroovyTokenMaker()
      case "html"   => new HTMLTokenMaker()
      case "java"   => new JavaTokenMaker()
      case "js"     => new JavaScriptTokenMaker()
      case "json"   => new JsonTokenMaker()
      case "jsp"    => new JSPTokenMaker()
      case "tex"    => new LatexTokenMaker()
      case "less"   => new LessTokenMaker()
      case "lua"    => new LuaTokenMaker()
      case "mk"     => new MakefileTokenMaker()
      case "pl"     => new PerlTokenMaker()
      case "php"    => new PHPTokenMaker()
      case "py"     => new PythonTokenMaker()
      case "rb"     => new RubyTokenMaker()
      case "sas"    => new SASTokenMaker() // ???
      case "scala"  => new ScalaTokenMaker()
      case "sql"    => new SQLTokenMaker()
      case "tcl"    => new TclTokenMaker()
//      case "ts"     => new TypeScriptTokenMaker()
      case "sh"     => new UnixShellTokenMaker()
      case "vb"     => new VisualBasicTokenMaker()
      case "bat"    => new WindowsBatchTokenMaker()
      case "xml"    => new XMLTokenMaker()
      case _ => new PlainTextTokenMaker()
    }
  }


  def main(args: Array[String]): Unit = {
    // Start all Swing applications on the EDT.
    SwingUtilities.invokeLater(new Runnable() {
      def run(): Unit = {
        new TextEditorDemo().setVisible(true)
      }
    })
  }
}
