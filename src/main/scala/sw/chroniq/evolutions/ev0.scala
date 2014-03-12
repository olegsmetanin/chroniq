package sw.chroniq.evolutions

import sw.platform.evolutions.Evolution
import sw.platform.db.DBs
import scala.concurrent._
import scala.concurrent.duration._
/**
 * Created by olegsmetanin on 11/03/14.
 */
class EV0 extends Evolution {

  def up: Unit = {


    val upgradeMainDB = DBs.mainDB.sendQuery(
      """
        |
        |DROP TABLE DBConfig;
        |
        |CREATE TABLE DBConfig (
        |    id SERIAL,
        |    name varchar(255) NOT NULL,
        |    host varchar(255) NOT NULL,
        |    port INTEGER NOT NULL,
        |    databasename varchar(255) NOT NULL,
        |    username varchar(255) NOT NULL,
        |    password varchar(255) NOT NULL,
        |    PRIMARY KEY (id)
        |);
        |
        |INSERT INTO DBConfig(
        |            name, host, port, databasename, username, password)
        |    VALUES ('events', 'localhost', 5432, 'saas', 'olegsmetanin', '');
        |
        |
        |DROP TABLE DBEvolution;
        |
        |CREATE TABLE DBEvolution (
        |    num INTEGER NOT NULL
        |);
        |
        |INSERT INTO DBEvolution(
        |        num)
        |    VALUES (0);
        |
        |
        |DROP TABLE UserSessions;
        |
        |CREATE TABLE UserSessions (
        |    id integer NOT NULL,
        |    userid varchar(255) NOT NULL,
        |    sessionid varchar(255) NOT NULL,
        |    PRIMARY KEY (id)
        |);
        |
      """.stripMargin)




    Await.result(upgradeMainDB, 100.seconds)

  }

  def down: Unit = {

  }


}
