package com.rgoulter.typingtutor.gui

import javax.swing.JPanel
import javax.swing.JLabel
import sodium.Stream
import com.rgoulter.typingtutor.TypedStats
import java.awt.BorderLayout



class ShowStatsPanel(statsStream: Stream[TypedStats]) extends JPanel {
  private val label = new JLabel()

  setLayout(new BorderLayout())
  add(label, BorderLayout.CENTER)


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
    label.setText(stringOfStats(stats))
  }

  val listener = statsStream.listen(setStats)
}