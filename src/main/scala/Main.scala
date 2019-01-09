import java.awt.BorderLayout
import java.awt.CardLayout
import java.awt.Component
import java.awt.event.ComponentAdapter
import java.awt.event.ComponentEvent
import java.awt.event.WindowEvent
import java.awt.event.WindowAdapter
import java.io.File

import java.sql.SQLException

import javax.swing.JFrame
import javax.swing.JPanel
import javax.swing.SwingUtilities
import javax.swing.UIManager

import sodium.Cell
import sodium.Stream

import com.rgoulter.typingtutor.DocumentImpl
import com.rgoulter.typingtutor.Utils
import com.rgoulter.typingtutor.gui._
import com.rgoulter.typingtutor.sql.FileProgressDB
import com.rgoulter.typingtutor.sql.SQLHelper

class TypingTutFrame extends JFrame("Typing Tutor") {
  import com.rgoulter.typingtutor.Sample
  import Sample.{SampleText, SampleDocument, SampleTextTokMak}

  // Unpack sample file(s).
  val ApplicationDir = new File(".")
  val SourceFilesDir = new File(ApplicationDir, "typingtutor")
  SourceFilesDir.mkdirs()
  Sample.unpackIntoDir(SourceFilesDir)

  val DBName       = "typingtutor.db"
  val dbConn       = SQLHelper.connectionFor(DBName)
  val fileProgress = new FileProgressDB(dbConn)

  addWindowListener(new WindowAdapter {
    override def windowClosing(e: WindowEvent): Unit =
      try {
        dbConn.close()
      } catch {
        // connection close failed.
        case e: SQLException => System.err.println(e);
      }
  })

  val FileSelectCard  = "select"
  val TypingTutorCard = "typing"
  val ShowStatsCard   = "stats"

  val cardLayout = new CardLayout()
  val cards      = new JPanel()
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

  setLayout(new BorderLayout())
  setContentPane(cards)

  setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE)
  pack()
  setLocationRelativeTo(null)

  val fileSelectListener = fileSelectPanel.selectedFile.listen { maybeFile =>
    val (text, doc, tokMak) = maybeFile match {
      case Some(selectedFile) => {
        // DOCUMENT load from file
        val tokMak = Utils.tokenMakerForFile(selectedFile)

        val source = scala.io.Source.fromFile(selectedFile)
        val text   = source.mkString
        source.close()

        val relPath  = SourceFilesDir.toPath().relativize(selectedFile.toPath())
        val initOffs = fileProgress.offsetOf(relPath).getOrElse(0)

        val tokenIterable = Utils.tokenIteratorOf(text, tokMak)
        val doc           = new DocumentImpl(text, tokenIterable, initOffs)

        (text, doc, tokMak)
      }
      case None => {
        (SampleText, SampleDocument, SampleTextTokMak)
      }
    }

    typingTutorPanel.setDocument(text, doc, tokMak)

    cardLayout.show(cards, TypingTutorCard)
  }

  // Save exitOffset for the selectedFile
  val currentFile = fileSelectPanel.selectedFile.hold(None)
  val updateOffsetListener =
    Stream
      .filterOption(
        typingTutorPanel.finishingOffset
          .snapshot(currentFile, { (offset: Int, currFile: Option[File]) =>
            currFile.map { file =>
              (file, offset)
            }
          }))
      .listen({
        case (file: File, newOffset: Int) =>
          // Paths for fileProgress need to be relative to SourceFilesDir.
          val relPath = SourceFilesDir.toPath().relativize(file.toPath())
          fileProgress.updateEntry(relPath, newOffset)
      })

  val endTypingListener = typingTutorPanel.statsStream.listen({ _ =>
    // Stats emitted only at end of game/lesson.

    cardLayout.show(cards, ShowStatsCard)

    // kludge rather than work with focus subsystem.
//    statsPanel.continueSessionButton.requestFocus()
  })

  val afterStatsListener = statsPanel.afterStats.listen {
    case AfterStatsActions.ContinueSession(offset) => {
      // DOCUMENT with different offset
      typingTutorPanel.continueFromOffset(offset)
      cardLayout.show(cards, TypingTutorCard)
//      typingTutorPanel.textArea.requestFocus()
    }
    case AfterStatsActions.RedoSession(offset) => {
      // DOCUMENT with different offset
      typingTutorPanel.continueFromOffset(offset)
      cardLayout.show(cards, TypingTutorCard)
//      typingTutorPanel.textArea.requestFocus()
    }
    case AfterStatsActions.SelectFile() => {
      cardLayout.show(cards, FileSelectCard)

      // kludge b/c of mishandling of focus subsystem
//      fileSelectPanel.table.requestFocus()
    }
    case AfterStatsActions.ExitTypingTutor() => {
      // Exit the application.
      dispose()
    }
  }
}

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
        val frame = new TypingTutFrame()
        frame.setVisible(true)
      }
    })
  }
}
