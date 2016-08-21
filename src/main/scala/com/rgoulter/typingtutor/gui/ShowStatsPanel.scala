package com.rgoulter.typingtutor.gui

import java.awt.BorderLayout
import java.awt.event.ActionEvent
import java.awt.event.ComponentAdapter
import java.awt.event.ComponentEvent
import java.awt.event.KeyEvent

import javax.swing.AbstractAction
import javax.swing.JButton
import javax.swing.JComponent
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.KeyStroke

import sodium.Stream
import sodium.StreamSink

import com.rgoulter.typingtutor.TypedStats



object AfterStatsActions {
  sealed trait AfterStats

  class ContinueFrom(val offset: Int = 0)

  case class ContinueSession(finalOffset: Int) extends ContinueFrom(finalOffset) with AfterStats

  case class RedoSession(initialOffset: Int) extends ContinueFrom(initialOffset) with AfterStats

  case class SelectFile() extends AfterStats

  case class ExitTypingTutor() extends AfterStats
}



class ShowStatsPanel(statsStream: Stream[TypedStats]) extends JPanel {
  // Keep track of initial/final offsets in mutable state.
  private var initialOffset: Int = 0
  private var finalOffset: Int = 0

  import AfterStatsActions.AfterStats
  private val afterStatsSink = new StreamSink[AfterStats]()
  val afterStats: Stream[AfterStats] = afterStatsSink

  val ContinueSessionAction = new AbstractAction {
    override def actionPerformed(evt: ActionEvent): Unit = {
      afterStatsSink.send(AfterStatsActions.ContinueSession(finalOffset))
    }
  }

  val RedoSessionAction = new AbstractAction {
    override def actionPerformed(evt: ActionEvent): Unit = {
      afterStatsSink.send(AfterStatsActions.RedoSession(initialOffset))
    }
  }

  val SelectFileAction = new AbstractAction {
    override def actionPerformed(evt: ActionEvent): Unit = {
      afterStatsSink.send(AfterStatsActions.SelectFile())
    }
  }

  val ExitAction = new AbstractAction {
    override def actionPerformed(evt: ActionEvent): Unit = {
      afterStatsSink.send(AfterStatsActions.ExitTypingTutor())
    }
  }


  private val label = new JLabel()

  setLayout(new BorderLayout())
  add(label, BorderLayout.CENTER)


  // Panel with buttons for each of the 'after actions'.
  val actionsPanel = new JPanel()
  add(actionsPanel, BorderLayout.SOUTH)

  val continueSessionButton = new JButton("Continue")
  continueSessionButton.addActionListener(ContinueSessionAction)
  actionsPanel.add(continueSessionButton)

  val redoSessionButton = new JButton("Redo")
  redoSessionButton.addActionListener(RedoSessionAction)
  actionsPanel.add(redoSessionButton)

  val selectFileButton = new JButton("Select File")
  selectFileButton.addActionListener(SelectFileAction)
  actionsPanel.add(selectFileButton)

  val exitButton = new JButton("Exit")
  exitButton.addActionListener(ExitAction)
  actionsPanel.add(exitButton)

  // Bind 'Esc' to ExitAction on JButton as well.
  val ExitItem = "exit_typingtutor"
  val exitInputMap = getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT)
  exitInputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), ExitItem)
  getActionMap().put(ExitItem, ExitAction)

  // Something has been quite messed up if this is necessary.
  val EnterKS = KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0)
  continueSessionButton.getInputMap(JComponent.WHEN_FOCUSED).put(EnterKS, "continue-sess")
  continueSessionButton.getActionMap().put("continue-sess", ContinueSessionAction)
  redoSessionButton.getInputMap(JComponent.WHEN_FOCUSED).put(EnterKS, "redo-sess")
  redoSessionButton.getActionMap().put("redo-sess", RedoSessionAction)
  selectFileButton.getInputMap(JComponent.WHEN_FOCUSED).put(EnterKS, "select-file")
  selectFileButton.getActionMap().put("select-file", SelectFileAction)
  exitButton.getInputMap(JComponent.WHEN_FOCUSED).put(EnterKS, "exit")
  exitButton.getActionMap().put("exit", ExitAction)


  private def stringOfStats(stats: TypedStats): String =
    s"""<html>
<table>
  <tr><td>Total</td>    <td>${stats.numTotal}</td></tr>
  <tr><td>Correct</td>  <td>${stats.numCorrect}</td></tr>
  <tr><td>Incorrect</td><td>${stats.numIncorrect}</td></tr>
  <tr><td>Duration</td> <td>${stats.durationInMins} mins</td></tr>
  <tr><td>Accuracy</td> <td>${stats.accuracyPercent}%</td></tr>
  <tr><td>WPM</td>      <td>${stats.wpmStr}</td></tr>
</table></html>"""

  private def setStats(stats: TypedStats): Unit = {
    initialOffset = stats.initialOffset
    finalOffset = stats.finalOffset

    label.setText(stringOfStats(stats))
  }

  val listener = statsStream.listen(setStats)


  addComponentListener(new ComponentAdapter {
    override def componentShown(evt: ComponentEvent): Unit = {
      continueSessionButton.requestFocusInWindow()
    }
  })
}