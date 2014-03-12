package sw.chroniq.evolutions

import sw.platform.evolutions.Evolution

/**
 * Created by olegsmetanin on 11/03/14.
 */
object Evolutions {
  val list:List[Evolution] = List(new EV0(), new EV1())

  def apply() = {
      list.foreach(_.up)
  }
}
