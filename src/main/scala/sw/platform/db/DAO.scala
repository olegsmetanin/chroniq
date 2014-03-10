package sw.platform.db

import scala.concurrent.{ExecutionContextExecutor, Future}
import com.github.mauricio.async.db.{RowData, ResultSet, QueryResult}


/**
 * Created by olegsmetanin on 09/03/14.
 */

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

    def asMapOf[E,T](columnName:String)(implicit rd2T: RowData => T, dispatcher: ExecutionContextExecutor): Future[Map[E,T]] = f.map {
      qr: QueryResult =>
        qr.rows match {
          case Some(rs: ResultSet) =>
            rs.map {
              rd: RowData =>
                val t = rd2T(rd)
                rd.apply(columnName).asInstanceOf[E] -> t
            }.toMap
          case _ => Map[E, T]()
        }
    }

    def asMapOf[T](columnName1:String, columnName2:String)(implicit rd2T: RowData => T, dispatcher: ExecutionContextExecutor): Future[Map[String,T]] = f.map {
      qr: QueryResult =>
        qr.rows match {
          case Some(rs: ResultSet) =>
            rs.map {
              rd: RowData =>
                val t = rd2T(rd)
                rd.apply(columnName1).toString+rd.apply(columnName2).toString -> t
            }.toMap
          case _ => Map[String,T]()
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

  implicit def rowToString(rd:RowData):String = {
    rd.apply(0).toString
  }
}