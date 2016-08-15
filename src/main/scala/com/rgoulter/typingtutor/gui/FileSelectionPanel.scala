package com.rgoulter.typingtutor.gui

import java.awt.event.KeyListener
import java.awt.event.KeyEvent
import java.io.File

import javax.swing.JFileChooser
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.SwingConstants

import sodium.Stream
import sodium.StreamSink



class FileSelectionPanel extends JPanel {
  // For now, just show a label,
  // input:
  //   ^O      = show open file dialog
  //   <Enter> = use the sample
  private val label = new JLabel("Press Ctrl+O to open file, or <Enter> to use sample.")
  label.setHorizontalAlignment(SwingConstants.CENTER)
  add(label)

  // TMP: For now, use Option[File], (None to represent Sample),
  // later sample shouldn't be distinguishable from normal files..
  private val selectedFileSink = new StreamSink[Option[File]]()
  val selectedFile: Stream[Option[File]] = selectedFileSink

  private val self = this
  setFocusable(true)
  addKeyListener(new KeyListener {
    override def keyPressed(ke: KeyEvent): Unit = {
      ke.getKeyCode() match {
        case KeyEvent.VK_O if ke.isControlDown() => {
          // Ctrl-O => Open file.
          val chooser = new JFileChooser()
          val retVal = chooser.showOpenDialog(self)

          if(retVal == JFileChooser.APPROVE_OPTION) {
            val selectedFile = chooser.getSelectedFile()

            selectedFileSink.send(Some(selectedFile))
          }
        }
        case KeyEvent.VK_ENTER => {
          // Enter => Use SAMPLE file.
          selectedFileSink.send(None)
        }
        case _ => ()
      }
    }

    override def keyReleased(ke: KeyEvent): Unit = {}
    override def keyTyped(ke: KeyEvent): Unit = {}
  })
}
