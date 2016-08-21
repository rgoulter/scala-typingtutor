package com.rgoulter.typingtutor.gui

import java.awt.BorderLayout
import java.awt.event.ActionEvent
import java.awt.event.ComponentAdapter
import java.awt.event.ComponentEvent
import java.awt.event.InputEvent
import java.awt.event.KeyEvent
import java.io.File

import javax.swing.AbstractAction
import javax.swing.JComponent
import javax.swing.JFileChooser
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.JScrollPane
import javax.swing.JTable
import javax.swing.JTextField
import javax.swing.KeyStroke
import javax.swing.SwingConstants
import javax.swing.table.AbstractTableModel
import javax.swing.table.DefaultTableCellRenderer

import scala.collection.JavaConverters.asScalaIteratorConverter

import org.apache.commons.io.FileUtils

import sodium.Stream
import sodium.StreamSink

import com.rgoulter.typingtutor.FileProgress
import com.rgoulter.typingtutor.FileProgressEntry



class FileSelectionPanel(sourceFilesDir: File, fileProgress: FileProgress) extends JPanel {
  // List the existing files from sourceFilesDir.
  private val typingFiles =
    FileUtils.iterateFiles(sourceFilesDir, null, true).asScala.toList

  private val relPaths =
    typingFiles.map(f =>
      sourceFilesDir.toPath().relativize(f.toPath()))

  // n.b. fileProgress's Paths need to be relative to sourceFilesDir
  fileProgress.updateEntries(relPaths)

  private val typingFileEntries = fileProgress.entries


  private val tableModel = new AbstractTableModel() {
    // Columns: Path, Language
    override def getColumnCount(): Int = 3

    override def getColumnName(col: Int): String =
      col match {
        case 0 => "Filename"
        case 1 => "Language"
        case 2 => "Offset"
      }

    override def getColumnClass(c: Int): Class[_] = {
        getValueAt(0, c).getClass()
    }

    override def getRowCount(): Int =
      typingFileEntries.length

    override def getValueAt(row: Int, col: Int): Object = {
      val fileEntry = typingFileEntries(row)
      col match {
        case 0 => {
          fileEntry.path
        }

        case 1 =>
          fileEntry.language

        case 2 =>
          fileEntry.offset.asInstanceOf[Integer]
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


  val table = new JTable(tableModel)

  table.setCellSelectionEnabled(false)
  table.setRowSelectionAllowed(true)
  table.setRowSelectionInterval(0, 0)

  // Adjust the widths of the columns.
  val tableColumnModel = table.getColumnModel()
  tableColumnModel.getColumn(0).setPreferredWidth(500)
  tableColumnModel.getColumn(1).setPreferredWidth(100)

  // Enable sorting rows of the table
  table.setAutoCreateRowSorter(true)

  // Center the "Language" column
  val centerRenderer = new DefaultTableCellRenderer()
  centerRenderer.setHorizontalAlignment(SwingConstants.CENTER)
  table.getColumnModel().getColumn(1).setCellRenderer(centerRenderer)


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

        val relPath = fileEntry.path
        val selectedFile = sourceFilesDir.toPath().resolve(relPath).toFile()

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

        // Copy to sourceFilesDir,
        val destFile = new File(sourceFilesDir, selectedFile.getName())
        FileUtils.copyFile(selectedFile, destFile)

        val relPathToFile = sourceFilesDir.toPath().relativize(destFile.toPath())

        // Add to peristence / file progress.
        fileProgress.addEntry(new FileProgressEntry(relPathToFile, 0))

        selectedFileSink.send(Some(destFile))
      }
    }
  })


  addComponentListener(new ComponentAdapter {
    override def componentShown(evt: ComponentEvent): Unit = {
      table.requestFocusInWindow()
    }
  })
}
