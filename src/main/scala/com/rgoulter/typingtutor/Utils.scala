package com.rgoulter.typingtutor

import java.io.File
import javax.swing.text.Segment
import org.apache.commons.io.FilenameUtils
import org.fife.ui.rsyntaxtextarea.Token
import org.fife.ui.rsyntaxtextarea.TokenImpl
import org.fife.ui.rsyntaxtextarea.TokenMaker
import org.fife.ui.rsyntaxtextarea.TokenTypes
import scala.collection.immutable.HashMap

object Utils {

  /** Print the given [[Token]] list to stdout, for debugging purposes. */
  def dumpTokens(initToken: Token, description: String = "Tokens") {
    println(s"Dump $description:")
    var tok = initToken

    while (tok != null) {
      println(s"TOK: $tok")
      tok = tok.getNextToken()
    }
  }

  /** Construct `Seq[Token]` from a given [[Token]].
    *
    * Creates copies of the [[Token]].
    */
  def toTokenSeq(tok: Token): Seq[Token] = {
    if (tok == null) {
      Seq.empty
    } else {
      // Copy the token, otherwise strange things happen,
      // and it's difficult to debug what's going on.
      new TokenImpl(tok) +: toTokenSeq(tok.getNextToken())
    }
  }

  /** Return an [[Iterable]] sequence of [[Token]]s from given text, by parsing
    * it using the given [[TokenMaker]].
    */
  def tokenIteratorOf(text: String, tokenMaker: TokenMaker): Iterable[Token] = {
    // n.b. getTokenList(Segment, initType, offset) gives Token[NullType] at EOL,
    // whose nextToken() is null.

    // offsets for start-of-lines in text.
    val lineOffsets: Seq[Int] =
      0 +:
        (text.zipWithIndex
        .filter({ case (c, idx) => c == '\n' })
        .map({ case (c, idx) => idx + 1 }))

    lineOffsets.foldLeft(Seq[Token]())({ (res, offset) =>
      // TODO by right, TokenTypes.NULL may not be true, if the token
      // doesn't end on the end of the line.
      val segment =
        new Segment(text.toCharArray(), offset, text.length() - offset)
      val tokenSeq =
        toTokenSeq(tokenMaker.getTokenList(segment, TokenTypes.NULL, offset))

      res ++ tokenSeq
    })
  }

  import org.fife.ui.rsyntaxtextarea.modes._

  // Not all the filetypes RSyntaxTextArea supports
  // are here; some extensions missing.
  //  * x86 asm?
  //  * bbcode??
  //  * delphi?
  //  * dockerfile?
  //  * fortran?
  //  * ....
  //  * lisp??
  type Language = (String, String, TokenMaker)
  val Languages =
    Seq(
      ("actionscript", "as", new ActionScriptTokenMaker()),
      ("c", "c", new CTokenMaker()),
      ("clojure", "clj", new ClojureTokenMaker()),
      ("cplusplus", "cpp", new CPlusPlusTokenMaker()),
      ("csharp", "cs", new CSharpTokenMaker()),
      ("css", "css", new CSSTokenMaker()),
      ("d", "d", new DTokenMaker()),
      ("dart", "dart", new DartTokenMaker()),
      ("dtd", "dtd", new DtdTokenMaker()),
      ("groovy", "groovy", new GroovyTokenMaker()),
      ("html", "html", new HTMLTokenMaker()),
      ("java", "java", new JavaTokenMaker()),
      ("javascript", "js", new JavaScriptTokenMaker()),
      ("json", "json", new JsonTokenMaker()),
      ("jsp", "jsp", new JSPTokenMaker()),
      ("latex", "tex", new LatexTokenMaker()),
      ("less", "less", new LessTokenMaker()),
      ("lua", "lua", new LuaTokenMaker()),
      ("makefile", "mk", new MakefileTokenMaker()),
      ("perl", "pl", new PerlTokenMaker()),
      ("plaintext", "txt", new PlainTextTokenMaker()),
      ("php", "php", new PHPTokenMaker()),
      ("python", "py", new PythonTokenMaker()),
      ("ruby", "rb", new RubyTokenMaker()),
      ("sas", "sas", new SASTokenMaker()), // ???
      ("scala", "scala", new ScalaTokenMaker()),
      ("sql", "sql", new SQLTokenMaker()),
      ("tcl", "tcl", new TclTokenMaker()),
//      ("typescript", "ts", new TypeScriptTokenMaker()) // not in current ver of RSTA?
      ("unixshell", "sh", new UnixShellTokenMaker()),
      ("visualbasic", "vb", new VisualBasicTokenMaker()),
      ("windowsbatch", "bat", new WindowsBatchTokenMaker()),
      ("xml", "xml", new XMLTokenMaker())
    )

  val LanguageLookupByExtension =
    Languages.foldLeft(HashMap.empty[String, Language])({ (hm, lang) =>
      val (_, ext, _) = lang
      hm + (ext -> lang)
    })

  def languageForFile(f: File): Language = {
    // Special cases
    if (f.getName() == "makefile") {
      return ("makefile", "mk", new MakefileTokenMaker())
    }

    val ext = FilenameUtils.getExtension(f.getName())

    LanguageLookupByExtension.getOrElse(
      ext,
      ("plaintext", "txt", new PlainTextTokenMaker()))
  }

  def tokenMakerForFile(f: File): TokenMaker = {
    val (_, _, tokenMaker) = languageForFile(f)
    tokenMaker
  }
}
