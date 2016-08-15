import java.awt.BorderLayout
import java.io.File

import javax.swing.JFrame
import javax.swing.JPanel
import javax.swing.SwingUtilities

import scala.collection.JavaConverters.asScalaIteratorConverter

import com.rgoulter.typingtutor.gui.TypingTutorPanel



class TextEditorDemo extends JFrame {
  import com.rgoulter.typingtutor.Utils
  import com.rgoulter.typingtutor.Sample
  import Sample.{ SampleText, SampleDocument, SampleTextTokMak }

  val frame = this

  // Listen for an endgame
//  val endGameListener = typeTutorKL.endGame.snapshot(typeTutorKL.stats).listen(stats => {
//    // Remove all the key listeners
//    for (kl <- textArea.getKeyListeners()) { textArea.removeKeyListener(kl) }
//
//    // XXX This needs to be done using FRP
//    // Show the stats (in a dialogue window?)
//    val dialog = new JFrame("Statistics")
//    val label = new JLabel(s"""<html>
//<table>
//  <tr><td>Total</td>    <td>${stats.numTotal}</td></tr>
//  <tr><td>Correct</td>  <td>${stats.numCorrect}</td></tr>
//  <tr><td>Incorrect</td><td>${stats.numIncorrect}</td></tr>
//  <tr><td>Duration</td> <td>${stats.durationInMins} mins</td></tr>
//  <tr><td>Accuracy</td> <td>${stats.accuracyPercent}%</td></tr>
//  <tr><td>WPM</td>      <td>${stats.wpmStr}</td></tr>
//</table></html>""")
//    dialog.getContentPane().add(label)
//    dialog.pack()
//    dialog.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE)
//
//    dialog.addWindowListener(new WindowAdapter {
//      override def windowClosed(we: WindowEvent): Unit = {
//        // ...and when that closes, quit.
//        frame.dispose()
//      }
//    })
//    dialog.setLocationRelativeTo(frame)
//
//    dialog.setVisible(true)
//  })

  // Disable mouse interaction
//  for (ml <- textArea.getMouseListeners()) { textArea.removeMouseListener(ml) }
//  for (ml <- textArea.getMouseMotionListeners()) { textArea.removeMouseMotionListener(ml) }

  val contentPane = new JPanel(new BorderLayout())
  val typingTutorPanel =
    new TypingTutorPanel(SampleText, SampleDocument, SampleTextTokMak)
  contentPane.add(typingTutorPanel)
  setContentPane(contentPane)

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
