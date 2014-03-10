package sw.platform.db

import com.github.mauricio.async.db.pool._
import com.github.mauricio.async.db.postgresql.PostgreSQLConnection
import com.github.mauricio.async.db.postgresql.pool.PostgreSQLConnectionFactory
import com.github.mauricio.async.db._
import scala.concurrent._
import scala.Some
import scala.concurrent.duration._

import scala.concurrent.ExecutionContext.Implicits.global
import com.typesafe.config.ConfigFactory


case class DBConfig(id: Int, name:String, host:String, port:Int, databasename:String, username:String, password:String)

object DBConfig {
  import scala.language.implicitConversions
  import com.github.mauricio.async.db.RowData

  implicit def rowToDBConfig(row: RowData): DBConfig = {
    {
      DBConfig(
        row("id").asInstanceOf[Int],
        row("name").asInstanceOf[String],
        row("host").asInstanceOf[String],
        row("port").asInstanceOf[Int],
        row("databasename").asInstanceOf[String],
        row("username").asInstanceOf[String],
        row("password").asInstanceOf[String]
      )
    }
  }
}

object DBs extends Map[String, ConnectionPool[PostgreSQLConnection]] {

  import DBConfig._
  import DAO._

  val conf = ConfigFactory.load

  private val (host, port, username, database, password) =
      (
        if (conf.hasPath("maindb.host")) conf.getString("maindb.host") else "localhost",
        if (conf.hasPath("maindb.port")) conf.getInt("maindb.port") else 5432,
        conf.getString("maindb.username"),
        conf.getString("maindb.database"),
        if (conf.hasPath("maindb.password")) Some(conf.getString("maindb.password")) else None
      )

  val mainDB = new ConnectionPool(
    new PostgreSQLConnectionFactory(
      new Configuration(
        host = host,
        port = port,
        username = username,
        database = Some(database),
        password = password
      )
    ), PoolConfiguration.Default
  )

  private val dbconfigFuture = mainDB
    .sendQuery("SELECT id, name, host, port, databasename, username, password FROM DBConfig")
    .asListOf[DBConfig]

  private val dbconfig = Await.result(dbconfigFuture, 10.seconds)

  private val dbs = dbconfig.map { el =>
    el.name -> new ConnectionPool(
      new PostgreSQLConnectionFactory(
        new Configuration(
          host = el.host,
          port = el.port,
          username = el.username,
          database = Some(el.databasename)
        )
      ), PoolConfiguration.Default
    )

  }.toMap

  def get(key: String): Option[ConnectionPool[PostgreSQLConnection]] = dbs.get(key)

  def iterator: Iterator[(String, ConnectionPool[PostgreSQLConnection])] = dbs.iterator

  def -(key: String): Map[String, ConnectionPool[PostgreSQLConnection]] = dbs.-(key)

  def +[B1 >: ConnectionPool[PostgreSQLConnection]](kv: (String, B1)): Map[String, B1] = dbs.+[B1](kv)

}