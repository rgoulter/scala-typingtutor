package com.rgoulter.typingtutor



/** For representing the typing statistics of a session. */
class TypedStats(val numTotal: Int,
                 val numCorrect: Int,
                 val numIncorrect: Int,
                 val entries: Array[(Char, Char, Long)],
                 val initialOffset: Int,
                 val finalOffset: Int) {
  /** Prints stats to stdout. */
  def print(): Unit = {
    println(s"Total: $numTotal")
    println(s"Correct: $numCorrect")
    println(s"Incorrect: $numIncorrect")
  }

  /** Time between the first key-event entry and the last key-event entry,
    * if any, in milliseconds.
    */
  val duration: Long = {
    if (!entries.isEmpty) {
      val start = entries.head._3
      val end   = entries.last._3
      end - start
    } else {
      0
    }
  }

  val durationInMins: Double =
    (duration / 1000).toDouble / 60

  /** String of duration formatted in `mm:ss` string. */
  val durationStr: String =
    s"${(duration / 1000) / 60}:${(duration / 1000) % 60}"

  /** Proportion of total key-events which are correct; between 0 and 1. */
  val accuracy = numCorrect.toDouble / numTotal

  /** Percentage of total key-events which are correct; between 0 and 100. */
  val accuracyPercent: Int = (accuracy * 100).toInt

  /** Estimation of "words per minute".
    *
    * Computed as: `wpm = (# chars / 5) / (time in mins)` (rounded).
    */
  val wpmStr =
    if (durationInMins > 0)
      ((numCorrect / 5) / durationInMins).toInt
    else
      "???"
}
