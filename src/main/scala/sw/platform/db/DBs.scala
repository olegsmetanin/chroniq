package sw.platform.db

import com.github.mauricio.async.db.pool.{PoolConfiguration, ConnectionPool}
import com.github.mauricio.async.db.postgresql.PostgreSQLConnection
import com.github.mauricio.async.db.postgresql.pool.PostgreSQLConnectionFactory
import com.github.mauricio.async.db.{ResultSet, RowData, QueryResult, Configuration}
import scala.concurrent.{ExecutionContextExecutor, Future}
import scala.Some



object DBs extends Map[String, ConnectionPool[PostgreSQLConnection]] {

  private val dbs = Map(
    "saas" -> new ConnectionPool(
      new PostgreSQLConnectionFactory(
        new Configuration(
          port = 5432,
          username = "olegsmetanin",
          database = Some("saas")
        )
      ), PoolConfiguration.Default
    )
  )

  def get(key: String): Option[ConnectionPool[PostgreSQLConnection]] = dbs.get(key)

  def iterator: Iterator[(String, ConnectionPool[PostgreSQLConnection])] = dbs.iterator

  def -(key: String): Map[String, ConnectionPool[PostgreSQLConnection]] = dbs.-(key)

  def +[B1 >: ConnectionPool[PostgreSQLConnection]](kv: (String, B1)): Map[String, B1] = dbs.+[B1](kv)

}

object DAO {

  import scala.language.implicitConversions

  implicit class QROps(f: Future[QueryResult]) {
    def asListOf[T](implicit rd2T: RowData => T, dispatcher: ExecutionContextExecutor): Future[IndexedSeq[T]] = f.map {
      qr: QueryResult =>
        qr.rows match {
          case Some(rs: ResultSet) =>
            rs.map {
              rd: RowData =>
                rd2T(rd)
            }
          case _ => IndexedSeq[T]()
        }
    }

    def asValueOf[T](implicit rd2T: RowData => T, dispatcher: ExecutionContextExecutor): Future[Option[T]] = f.map {
      qr: QueryResult =>
        qr.rows match {
          case Some(rs: ResultSet) =>
            Some(rd2T(rs.head))
          case _ => None
        }
    }

  }


}