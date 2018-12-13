package org.rockscache.utils

import java.sql.{Connection, PreparedStatement}

import com.zaxxer.hikari.HikariDataSource

object SqlDataAccess {
  def usingConnection[A](code: Connection => A)(implicit dataSource: HikariDataSource): A = {
    var connection: Option[Connection] = None

    try {
      connection = Some(dataSource.getConnection())
      code(connection.get)
    } finally {
      connection.foreach(_.close)
    }
  }

  def withStatement[A](sql: String)(code: PreparedStatement => A)(implicit connection: Connection): A = {
    var statement: Option[PreparedStatement] = None

    try {
      statement = Some(connection.prepareStatement(sql))
      code(statement.get)
    } finally {
      statement.foreach(_.close)
    }
  }
}
