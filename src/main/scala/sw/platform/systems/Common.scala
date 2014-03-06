package sw.platform.systems

import akka.actor.{ActorSystem}


object Vars {
  val sessionID = "SessionID"
}


trait GenSystem {
  val role: String

  def apply()(implicit system: ActorSystem)
}



