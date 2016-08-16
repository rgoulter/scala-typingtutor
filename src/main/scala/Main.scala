import java.awt.BorderLayout
import java.awt.CardLayout
import java.io.File

import javax.swing.JFrame
import javax.swing.JPanel
import javax.swing.SwingUtilities

import sodium.Cell
import sodium.Stream

import com.rgoulter.typingtutor.DocumentImpl
import com.rgoulter.typingtutor.Utils
import com.rgoulter.typingtutor.gui._
import com.rgoulter.typingtutor.sql.FileProgressDB
import com.rgoulter.typingtutor.sql.SQLHelper



object Main {
  def main(args: Array[String]): Unit = {
    import com.rgoulter.typingtutor.Sample
    import Sample.{ SampleText, SampleDocument, SampleTextTokMak }

    // Unpack sample file(s).
    val ApplicationDir = new File(".")
    val SourceFilesDir = new File(ApplicationDir, "typingtutor")
    SourceFilesDir.mkdirs()
    Sample.unpackIntoDir(SourceFilesDir)


    val DBName = "typingtutor.db"
    val dbConn = SQLHelper.connectionFor(DBName)
    val fileProgress = new FileProgressDB(dbConn)


    val FileSelectCard  = "select"
    val TypingTutorCard = "typing"
    val ShowStatsCard   = "stats"

    val cardLayout = new CardLayout()
    val cards = new JPanel()
    cards.setLayout(cardLayout)


    val fileSelectPanel = new FileSelectionPanel(SourceFilesDir, fileProgress)
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


    // Save exitOffset for the selectedFile
    val currentFile = fileSelectPanel.selectedFile.hold(None)
    val updateOffsetListener =
      Stream.filterOption(typingTutorPanel.finishingOffset
                                          .snapshot(currentFile,
                                                    { (offset: Int,
                                                       currFile: Option[File]) =>
        currFile match {
          case Some(file) => Some((file, offset))
          case None => None
        }
      })).listen({ case (file: File, newOffset: Int) =>
        // Paths for fileProgress need to be relative to SourceFilesDir.
        val relPath = SourceFilesDir.toPath().relativize(file.toPath())
        fileProgress.updateEntry(relPath, newOffset)
      })


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
