package sw.platform.evolutions

/**
 * Created by olegsmetanin on 11/03/14.
 */
trait Evolution {

  def up: Unit

  def down: Unit

}
