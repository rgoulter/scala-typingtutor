package com.rgoulter.typingtutor

import java.io.File
import java.io.PrintWriter

import org.fife.ui.rsyntaxtextarea.modes.JavaTokenMaker

/** For sample document(s) so the user doesn't have to open a file
  * before they can try using the typing tutor.
  */
object Sample {
  // Eclipse building is a bit awkward if just `Hello.java`
  val SampleFiles = Seq("sample/Hello.java.sample")

  val SampleTexts = SampleFiles
    .map({ sampleURL =>
      val url    = Sample.getClass().getClassLoader().getResource(sampleURL)
      val source = scala.io.Source.fromURL(url)
      val text   = source.mkString
      source.close()

      (sampleURL -> text)
    })
    .toMap

  val SampleText = SampleTexts("sample/Hello.java.sample")

  val SampleTextTokMak = new JavaTokenMaker()

  val SampleDocument =
    new DocumentImpl(
      SampleText,
      Utils.tokenIteratorOf(SampleText, SampleTextTokMak),
      SampleTextTokMak
    )

  // dir should be e.g. $APPDIR/typingtutor,
  // and so this writes to e.g. $APPDIR/typingtutor/sample/Hello.java
  def unpackIntoDir(dir: File): Unit = {
    // This is a bit MAGIC in assuming `sample` is the dir name.
    val SampleDir = new File(dir, "sample")

    if (!SampleDir.exists()) {
      SampleDir.mkdirs()

      for ((sampleFile, text) <- SampleTexts) {
        val outputRelPath =
          sampleFile.substring(0, sampleFile.length() - ".sample".length())

        val outputPath = dir.toPath().resolve(outputRelPath)
        val outputFile = outputPath.toFile()

        val out = new PrintWriter(outputFile)
        out.print(text)
        out.close()
      }
    }
  }
}
