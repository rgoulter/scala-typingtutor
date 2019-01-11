import javax.swing.SwingUtilities
import javax.swing.UIManager

import com.rgoulter.typingtutor.gui.TypingTutorFrame

object Main {
  def main(args: Array[String]): Unit = {
    try {
      UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName())
    } catch {
      case e: Throwable =>
    }

    // Start all Swing applications on the EDT.
    SwingUtilities.invokeLater(new Runnable() {
      def run(): Unit = {
        val frame = new TypingTutorFrame()
        frame.setVisible(true)
      }
    })
  }
}
