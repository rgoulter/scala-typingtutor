package com.rgoulter.typingtutor.gui

import java.awt.BorderLayout
import java.awt.event.ActionEvent
import java.awt.event.FocusEvent
import java.awt.event.FocusListener
import java.awt.event.InputEvent
import java.awt.event.KeyEvent
import java.awt.event.KeyListener
import java.io.File

import javax.swing.AbstractAction
import javax.swing.JComponent
import javax.swing.JFileChooser
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.JScrollPane
import javax.swing.JTable
import javax.swing.KeyStroke
import javax.swing.SwingConstants
import javax.swing.table.AbstractTableModel

import scala.collection.JavaConverters.asScalaIteratorConverter

import org.apache.commons.io.FileUtils

import sodium.Stream
import sodium.StreamSink

import com.rgoulter.typingtutor.FileProgressEntry



class FileSelectionPanel(sourceFilesDir: File) extends JPanel {
  // List the existing files from sourceFilesDir.
  val typingFiles =
    FileUtils.iterateFiles(sourceFilesDir, null, true).asScala.toList

  val typingFileEntries = typingFiles.map({ f =>
    val progressOfFile = 0
    new FileProgressEntry(f.toPath(), progressOfFile)
  })


  // PERSISTENCE: Get list of files which exist, (rm the files in DB which don't exist in path),


  private val tableModel = new AbstractTableModel() {
    // Columns: Path, Language
    override def getColumnCount(): Int = 2

    override def getColumnName(col: Int): String =
      col match {
        case 0 => "Filename"
        case 1 => "Language"
      }

    override def getRowCount(): Int =
      typingFileEntries.length

    override def getValueAt(row: Int, col: Int): Object = {
      val fileEntry = typingFileEntries(row)
      col match {
        case 0 => {
          val relPath = sourceFilesDir.toPath().relativize(fileEntry.path)
          relPath.toString()
        }

        case 1 =>
          fileEntry.language
      }
    }
  }



  // Use a label to explain the inputs.
  // input:
  //   ^O      = show open file dialog
  //   <Enter> = use the selected file
  setLayout(new BorderLayout)
  private val label = new JLabel("Press Ctrl+O to open file, or <Enter> to use selected file.")
  label.setHorizontalAlignment(SwingConstants.CENTER)
  add(label, BorderLayout.NORTH)


  private val table = new JTable(tableModel)
  table.setCellSelectionEnabled(false)
  table.setRowSelectionAllowed(true)
  table.setRowSelectionInterval(0, 0)
  // TODO: Well, what we want next is:
  // XXX Adjust the widths of the columns,
  // XXX Make so table is *sortable* by the columns, ...
  // XXX Center the Language?
  private val scrollPane = new JScrollPane(table)
  add(scrollPane, BorderLayout.CENTER)



  // TMP: For now, use Option[File], (None to represent Sample),
  // later sample shouldn't be distinguishable from normal files..
  private val selectedFileSink = new StreamSink[Option[File]]()
  val selectedFile: Stream[Option[File]] = selectedFileSink



  val SelectItem = "select"
  val OpenFile = "openfile"

  val enterKS = KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0)
  val ctrlOKS = KeyStroke.getKeyStroke(KeyEvent.VK_O, InputEvent.CTRL_DOWN_MASK)

  val tableInputMap = table.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT)
  tableInputMap.put(enterKS, SelectItem)
  tableInputMap.put(ctrlOKS, OpenFile)

  table.getActionMap().put(SelectItem, new AbstractAction {
    override def actionPerformed(evt: ActionEvent): Unit = {
      // Enter => Use selected file.
      val selectedRow = table.getSelectedRow()

      if (selectedRow >= 0) {
        val selectedModelIdx = table.convertRowIndexToModel(selectedRow)
        val fileEntry = typingFileEntries(selectedModelIdx)
        val selectedFile = fileEntry.path.toFile()

        selectedFileSink.send(Some(selectedFile))
      }
    }
  })

  val self = this
  table.getActionMap().put(OpenFile, new AbstractAction {
    override def actionPerformed(evt: ActionEvent): Unit = {
      // Ctrl-O => Open file.
      val chooser = new JFileChooser()
      val retVal = chooser.showOpenDialog(self)

      if(retVal == JFileChooser.APPROVE_OPTION) {
        val selectedFile = chooser.getSelectedFile()

        selectedFileSink.send(Some(selectedFile))
      }
    }
  })
}
