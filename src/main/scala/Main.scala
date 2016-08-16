import java.awt.BorderLayout
import java.awt.CardLayout
import java.io.File

import javax.swing.JFrame
import javax.swing.JPanel
import javax.swing.SwingUtilities

import com.rgoulter.typingtutor.DocumentImpl
import com.rgoulter.typingtutor.Utils
import com.rgoulter.typingtutor.gui._



object Main {
  def main(args: Array[String]): Unit = {
    import com.rgoulter.typingtutor.Sample
    import Sample.{ SampleText, SampleDocument, SampleTextTokMak }
//
    // (unpack sample file(s)... + add to DB).
    val ApplicationDir = new File(".")
    val SourceFilesDir = new File(ApplicationDir, "typingtutor")
    SourceFilesDir.mkdirs()
    Sample.unpackIntoDir(SourceFilesDir)

    // PERSISTENCE: Get list of files which exist, (rm the files in DB which don't exist in path),


    val FileSelectCard  = "select"
    val TypingTutorCard = "typing"
    val ShowStatsCard   = "stats"

    val cardLayout = new CardLayout()
    val cards = new JPanel()
    cards.setLayout(cardLayout)


    val fileSelectPanel = new FileSelectionPanel()
    cards.add(fileSelectPanel, FileSelectCard)


    // TODO: "Settings" panel


    // TODO initial typingTutorPanel to a blank document..
    val typingTutorPanel =
      new TypingTutorPanel(SampleText, SampleDocument, SampleTextTokMak)
    cards.add(typingTutorPanel, TypingTutorCard)


    val statsPanel = new ShowStatsPanel(typingTutorPanel.statsStream)
    cards.add(statsPanel, ShowStatsCard)


    val frame = new JFrame("Typing Tutor")
    frame.setLayout(new BorderLayout())
    frame.setContentPane(cards)

    frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE)
    frame.pack()
    frame.setLocationRelativeTo(null)


    val fileSelectListener = fileSelectPanel.selectedFile.listen { maybeFile =>
      // PERSISTENCE: Get initialOffset for the selected file.
      val (text, doc, tokMak) = maybeFile match {
        case Some(selectedFile) => {
          val tokMak = Utils.tokenMakerForFile(selectedFile)

          val source = scala.io.Source.fromFile(selectedFile)
          val text = source.mkString
          source.close()

          val tokenIterable = Utils.tokenIteratorOf(text, tokMak)
          val doc = new DocumentImpl(text, tokenIterable)

          (text, doc, tokMak)
        }
        case None => {
          (SampleText, SampleDocument, SampleTextTokMak)
        }
      }

      typingTutorPanel.setDocument(text, doc, tokMak)

      cardLayout.show(cards, TypingTutorCard)
      typingTutorPanel.requestFocus()
    }

    // PERSISTENCE: Save exitOffset for the selectedFile (or 0, if at end..).
    val endTypingListener = typingTutorPanel.statsStream.listen(_ =>
      // Stats emitted only at end of game/lesson.

      cardLayout.show(cards, ShowStatsCard)
    )


    // Start all Swing applications on the EDT.
    SwingUtilities.invokeLater(new Runnable() {
      def run(): Unit = {
        frame.setVisible(true)
      }
    })
  }
}
