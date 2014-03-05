package sw.platform

import com.typesafe.config.ConfigFactory
import akka.actor.ActorSystem

import systems._

object Starter extends App {

  args.toList match {
    case argsParts: List[String] => {


      argsParts.foreach{ s =>
        val ts = s.trim
        if (ts.startsWith("-D")) {
          val kv = ts.substring(2).split("=")
          if (kv.length == 2) {
            System.setProperty(kv(0), kv(1))
          } else {
            println("Cant parse config:"+ts)
          }
        }
      }

      val configsNames = {
        val cmd = argsParts.last.trim

        if (cmd.startsWith("run")) {
          cmd.split(" ").tail.toList
        } else {
          argsParts
        }
      }

      if (configsNames.length==0) {
        throw new IllegalArgumentException("Wrong system names in agruments")
      }

      println("Load systems:"+configsNames.toString)


      import scala.reflect.runtime.universe

      val runtimeMirror = universe.runtimeMirror(getClass.getClassLoader)

      val configs = configsNames.map(ConfigFactory.load(_))

      val classes = configs.map(cfg => cfg.getString("loadclass"))

      val systems = classes.map {
        cls =>

          val module = runtimeMirror.staticModule(cls)

          val obj = runtimeMirror.reflectModule(module)

          obj.instance.asInstanceOf[GenSystem]

      }.toList

      val roles = systems.map(sys => sys.role)

      val config = ConfigFactory
        .parseString("akka.cluster.roles = [" + roles.mkString(",") + "]")
        .withFallback(configs.foldLeft(ConfigFactory.empty())((c, f) => c.withFallback(f)))

      implicit val system = ActorSystem("ClusterSystem", config)

      systems foreach (_())
    }

    case _ => println("nonvalid arg")
  }


}