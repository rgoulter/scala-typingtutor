package com.rgoulter.typingtutor.gui

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
import sodium.CellSink
import sodium.Stream
import sodium.StreamLoop
import sodium.Transaction

import com.rgoulter.typingtutor.Document
import com.rgoulter.typingtutor.DocumentImpl
import com.rgoulter.typingtutor.Utils
import com.rgoulter.typingtutor.sql.FileProgressDB
import com.rgoulter.typingtutor.sql.SQLHelper

sealed trait DocumentChangeEvent

case class SelectedFile(val document: Document) extends DocumentChangeEvent
case class ChangedOffset(val offset: Int)       extends DocumentChangeEvent

class TypingTutorFrame extends JFrame("Typing Tutor") {
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
  cards.setName("typing_tutor_cards")
  cards.setLayout(cardLayout)

  val fileSelectPanel = new FileSelectionPanel(SourceFilesDir, fileProgress)
  fileSelectPanel.setName("file_selection_panel")
  cards.add(fileSelectPanel, FileSelectCard)

  // TODO: "Settings" panel

  // There's a dependency cycle:
  //  - TypingTutorPanel needs documentC.
  //  - documentC changes based on Stream[DocumentChangeEvent]
  //    from FileSelectionPanel and ShowStatsPanel.
  //    But, ShowStatsPanel needs the stats Stream from
  //    typing tutor panel.
  //  - A CellLoop needs to be created/looped within a transaction.
  //    so, I'm using a CellSink of streams, and just sending
  //    the Stream[DocumentChangeEvent] later.
  val changeEventSC = new CellSink[Stream[DocumentChangeEvent]](new Stream())
  val documentC: Cell[Document] =
    Cell
      .switchS(changeEventSC)
      .accum(SampleDocument, (changeEvent, doc) => {
        changeEvent match {
          case SelectedFile(newDoc)     => newDoc
          case ChangedOffset(newOffset) => doc.withInitialOffset(newOffset)
        }
      })
  val fileSelectListener =
    fileSelectPanel.selectedFile.updates.listen { maybeFile =>
      cardLayout.show(cards, TypingTutorCard)
    }

  // TODO initial typingTutorPanel to a blank document..
  val typingTutorPanel =
    new TypingTutorPanel(documentC)
  cards.add(typingTutorPanel, TypingTutorCard)

  val statsPanel = new ShowStatsPanel(typingTutorPanel.statsStream)
  statsPanel.setName("typing_tutor_stats")
  cards.add(statsPanel, ShowStatsCard)

  setLayout(new BorderLayout())
  setContentPane(cards)

  setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE)
  pack()
  setLocationRelativeTo(null)

  val selectedDocumentS: Stream[Document] =
    fileSelectPanel.selectedFile.value.map {
      case Some(selectedFile) => {
        // DOCUMENT load from file
        val tokMak = Utils.tokenMakerForFile(selectedFile)

        val source = scala.io.Source.fromFile(selectedFile)
        val text   = source.mkString
        source.close()

        val relPath  = SourceFilesDir.toPath().relativize(selectedFile.toPath())
        val initOffs = fileProgress.offsetOf(relPath).getOrElse(0)

        val tokenIterable = Utils.tokenIteratorOf(text, tokMak)
        new DocumentImpl(text, tokenIterable, tokMak, initOffs)
      }
      case None => SampleDocument
    }

  val changedDocumentS: Stream[DocumentChangeEvent] =
    selectedDocumentS.map(SelectedFile(_))
  val changedOffsetS: Stream[DocumentChangeEvent] =
    Stream.filterOption(statsPanel.afterStats.map {
      case AfterStatsActions.ContinueSession(offset) =>
        Some(ChangedOffset(offset))
      case AfterStatsActions.RedoSession(offset) => Some(ChangedOffset(offset))
      case _                                     => None
    })
  changeEventSC.send(changedDocumentS.merge(changedOffsetS))

  // Save exitOffset for the selectedFile
  val updateOffsetListener =
    Stream
      .filterOption(
        typingTutorPanel.finishingOffset
          .snapshot(fileSelectPanel.selectedFile, {
            (offset: Int, currFile: Option[File]) =>
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
      cardLayout.show(cards, TypingTutorCard)
//      typingTutorPanel.textArea.requestFocus()
    }
    case AfterStatsActions.RedoSession(offset) => {
      // DOCUMENT with different offset
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
