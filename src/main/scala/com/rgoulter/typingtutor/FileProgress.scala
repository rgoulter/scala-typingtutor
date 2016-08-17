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



/** CRUD class for persisting progress in the files the user practices with.
  *
  * Unless otherwise stated, all [[Path]]s given to this class should be
  * relative to the source-files directory. (e.g. `./typingtutor/` by default)
  */
abstract class FileProgress {
  def addEntry(entry: FileProgressEntry): Unit

  def entries: List[FileProgressEntry]

  def updateEntry(path: Path, newOffset: Int): Unit

  def removeEntry(path: Path): Unit

  def offsetOf(path: Path): Option[Int] = {
    entries.find(_.path == path).map(_.offset)
  }

  /** Given a list of paths (relative to e.g. ./typingtutor/; so,
    * e.g. `List(sample/Hello.java)`),
    * add entries for any files not previously persisted,
    * remove entries for any files which aren't given.
    */
  def updateEntries(existingFilesInDir: List[Path]): Unit = {
    val persistedPaths = entries.map(_.path).toSet
    val onDiskPaths = existingFilesInDir.toSet

    for (p <- onDiskPaths -- persistedPaths) {
      addEntry(new FileProgressEntry(p, 0))
    }

    for (p <- persistedPaths -- onDiskPaths) {
      removeEntry(p)
    }
  }
}
