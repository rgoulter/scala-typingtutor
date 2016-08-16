package com.rgoulter.typingtutor

import org.fife.ui.rsyntaxtextarea.modes.JavaTokenMaker

/** For sample document(s) so the user doesn't have to open a file
  * before they can try using the typing tutor.
  */
object Sample {
  // Eclipse building is a bit awkward if just `Hello.java`
  val SampleFiles = Seq("sample/Hello.java.sample")

  val SampleTexts = SampleFiles.map({ sampleURL =>
    val url = Sample.getClass().getClassLoader().getResource(sampleURL)
    val source = scala.io.Source.fromURL(url)
    val text = source.mkString
    source.close()

    (sampleURL -> text)
  }).toMap

  val SampleText = SampleTexts("sample/Hello.java.sample")

  val SampleTextTokMak = new JavaTokenMaker()

  val SampleDocument =
    new DocumentImpl(SampleText,
                     Utils.tokenIteratorOf(SampleText, SampleTextTokMak))
}