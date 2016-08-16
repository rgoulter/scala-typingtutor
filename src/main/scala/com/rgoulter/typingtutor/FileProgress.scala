package com.rgoulter.typingtutor

import java.nio.file.Path



// Path should be relative when storing,
// Path should be absolute when reading file.
class FileProgressEntry(val path: Path,
                        val offset: Int) {
  lazy val language: String = {
    val (lang,_,_) = Utils.languageForFile(path.toFile())
    lang
  }

  /** Whether the file given by path exists on disk. */
  def exists: Boolean = {
    path.toFile().exists()
  }

  def isAbsolute(): Boolean = path.isAbsolute()

  def relativize(dir: Path): FileProgressEntry = {
    val relPath = path.relativize(dir)
    new FileProgressEntry(relPath, offset)
  }

  def resolveIn(dir: Path): FileProgressEntry = {
    assert(!path.isAbsolute())
    val absPath = dir.resolve(path)
    new FileProgressEntry(absPath, offset)
  }

  /** Reads the contents of the file in the given path. */
  def text: String = {
    val source = scala.io.Source.fromFile(path.toFile())
    val contents = source.mkString
    source.close()

    contents
  }

  // Needs to read file to compute offset,
  lazy val largestOffset: Int = {
    // Without parsing using TokenMaker,
    // the largest offset is just going to be an estimate.
    // (But, probably a good one).

    text.length()
  }
}



abstract class FileProgress {
  def entries: List[FileProgressEntry]

  def addEntry(entry: FileProgressEntry): Unit

  def removeEntry(path: String): Unit
}
