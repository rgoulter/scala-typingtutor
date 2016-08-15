import java.awt.BorderLayout
import java.awt.CardLayout
import java.io.File

import javax.swing.JFrame
import javax.swing.JPanel
import javax.swing.SwingUtilities

import com.rgoulter.typingtutor.gui._



object Main {
  def main(args: Array[String]): Unit = {
    val SelectFileCard  = "select"
    val TypingTutorCard = "typing"
    val ShowStatsCard   = "stats"

    val cardLayout = new CardLayout()
    val cards = new JPanel()
    cards.setLayout(cardLayout)


    // TODO: "Select File" panel


    import com.rgoulter.typingtutor.Sample
    import Sample.{ SampleText, SampleDocument, SampleTextTokMak }
    val typingTutorPanel =
      new TypingTutorPanel(SampleText, SampleDocument, SampleTextTokMak)
    val endTypingListener = typingTutorPanel.statsStream.listen(_ =>
      // Stats emitted only at end of game/lesson.

      cardLayout.show(cards, ShowStatsCard)
    )
    cards.add(typingTutorPanel, TypingTutorCard)


    val statsPanel = new ShowStatsPanel(typingTutorPanel.statsStream)
    cards.add(statsPanel, ShowStatsCard)


    val frame = new JFrame("Typing Tutor")
    frame.setLayout(new BorderLayout())
    frame.setContentPane(cards)

    frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE)
    frame.pack()
    frame.setLocationRelativeTo(null)


    // Start all Swing applications on the EDT.
    SwingUtilities.invokeLater(new Runnable() {
      def run(): Unit = {
        frame.setVisible(true)
      }
    })
  }
}
