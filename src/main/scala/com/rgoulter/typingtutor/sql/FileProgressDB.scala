package com.rgoulter.typingtutor.sql

import java.io.File
import java.nio.file.Path
import java.nio.file.Paths
import java.sql.Connection
import java.sql.DriverManager
import java.sql.ResultSet
import java.sql.SQLException
import java.sql.Statement

import com.rgoulter.typingtutor.FileProgress
import com.rgoulter.typingtutor.FileProgressEntry
import com.rgoulter.typingtutor.FileProgressEntry



object SQLHelper {
  // Connection
  def connectionFor(filename: String): Connection =
    DriverManager.getConnection(s"jdbc:sqlite:$filename")

//  def withConnectionTo[T](filename: String)(f: Connection => T): Option[T] = {
//    var conn : Connection = null
//
//    val res: Option[T] = try {
//      conn = connectionFor(filename)
//
//      Some(f(conn))
//    } catch {
//      case e: SQLException => {
//        System.err.println(e.getMessage())
//        None
//      }
//    } finally {
//      if (conn != null) {
//        try {
//          conn.close()
//        } catch {
//          case e: SQLException =>
//            System.err.println(e.getMessage())
//        }
//      }
//    }
//
//    None
//  }
}



object RawFileProgressDB {
  type Entry = (String, Int)
}



class RawFileProgressDB(connection: Connection) {
  import RawFileProgressDB.Entry

  val TableName = "fileoffsets"
  val FileColumn   = "file"
  val OffsetColumn = "offset"

  val statement = connection.createStatement()

  val CreateTblSql = s"CREATE TABLE IF NOT EXISTS $TableName ($FileColumn TEXT PRIMARY KEY, $OffsetColumn INTEGER)"
  statement.executeUpdate(CreateTblSql)

  def addEntry(entry: Entry): Unit = {
    val (path, offset) = entry
    val Sql = s"INSERT OR REPLACE INTO $TableName VALUES ('$path', $offset)"

    statement.executeUpdate(Sql)
  }

  def entries: List[Entry] = {
    val QuerySql = s"SELECT ($FileColumn, $OffsetColumn) FROM $TableName"
    val resultSet = statement.executeQuery(QuerySql)

    var res = List[Entry]()

    while (resultSet.next()) {
      val path = resultSet.getString(FileColumn)
      val offset = resultSet.getInt(OffsetColumn)

      res = res :+ (path -> offset)
    }

    res
  }

  def updateEntry(entry: Entry, newOffset: Int): Unit = {
    val (path, offset) = entry
    val Sql = s"UPDATE $TableName SET $OffsetColumn = $newOffset WHERE $FileColumn = '$path'"

    statement.executeUpdate(Sql)
  }

  def removeEntry(path: String): Unit = {
    val Sql = s"DELETE FROM $TableName WHERE $FileColumn = '$path'"

    statement.executeUpdate(Sql)
  }
}



object FileProgressDB {
  import RawFileProgressDB.Entry

  def fpEntryFromEntry(e: Entry): FileProgressEntry = {
    val (path, offset) = e

    new FileProgressEntry(Paths.get(path), offset)
  }

  def entryFromFpEntry(fpE: FileProgressEntry): Entry = {
    val pathStr = fpE.path.toString()
    val offset  = fpE.offset

    (pathStr, offset)
  }
}



/** CRUD class for persisting progress in the files the user practices with.
  * Stores using an SQLite database.
  *
  * Unless otherwise stated, all [[Path]]s given to this class should be
  * relative to the source-files directory. (e.g. `./typingtutor/` by default)
  */
class FileProgressDB(connection: Connection) extends FileProgress {
  import FileProgressDB.{ fpEntryFromEntry, entryFromFpEntry }

  val rawDB = new RawFileProgressDB(connection)

  override def addEntry(entry: FileProgressEntry): Unit = {
    rawDB.addEntry(entryFromFpEntry(entry))
  }

  override def entries: List[FileProgressEntry] = {
    rawDB.entries.map(fpEntryFromEntry)
  }

  override def updateEntry(entry: FileProgressEntry, newOffset: Int): Unit = {
    rawDB.updateEntry(entryFromFpEntry(entry), newOffset)
  }

  override def removeEntry(path: Path): Unit = {
    rawDB.removeEntry(path.toString())
  }
}
