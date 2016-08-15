package com.rgoulter.typingtutor

import org.fife.ui.rsyntaxtextarea.modes.JavaTokenMaker

/** For sample document(s) so the user doesn't have to open a file
  * before they can try using the typing tutor.
  */
object Sample {
  val SampleText = """public class HelloWorld {
  // This is a class

  public static void main(String args[]) {
    int x; // trailing comment

    println("Hello World!");
  }
}"""

  val SampleTextTokMak = new JavaTokenMaker()

  val SampleDocument =
    new DocumentImpl(SampleText,
                     Utils.tokenIteratorOf(SampleText, SampleTextTokMak))
}