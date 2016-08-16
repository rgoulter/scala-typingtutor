package com.rgoulter.typingtutor.sql

import java.io.File
import java.nio.file.Path
import java.sql.Connection
import java.sql.DriverManager
import java.sql.ResultSet
import java.sql.SQLException
import java.sql.Statement



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



class FileProgressDB(connection: Connection, dir: File) {
  type Entry = (String, Int)

  val TableName = "fileoffsets"
  val FileColumn   = "file"
  val OffsetColumn = "offset"

  val statement = connection.createStatement()

  val CreateTblSql = s"CREATE TABLE IF NOT EXISTS $TableName ($FileColumn TEXT PRIMARY KEY, $OffsetColumn INTEGER)"
  statement.executeUpdate(CreateTblSql)

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

  def addEntry(entry: Entry): Unit = {
    val (path, offset) = entry
    val Sql = s"INSERT OR REPLACE INTO $TableName VALUES ('$path', $offset)"

    statement.executeUpdate(Sql)
  }

  def removeEntry(path: String): Unit = {
    val Sql = s"DELETE FROM $TableName WHERE $FileColumn = '$path'"

    statement.executeUpdate(Sql)
  }
}