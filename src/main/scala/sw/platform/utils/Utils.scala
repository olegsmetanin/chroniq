package sw.platform.utils

import java.net.{ServerSocket}
import java.lang.management.ManagementFactory
import java.io.File
import spray.http.MediaTypes._
import collection.JavaConversions._
import spray.http.{ContentType, ContentTypes}
import java.util.jar.JarFile



object Utils {

  def freePort = {
    val server = new ServerSocket(0)
    val port = server.getLocalPort()
    server.close
    port
  }

  val debugMode = ManagementFactory.getRuntimeMXBean.getInputArguments.contains("-Xdebug")

  def isJar = {
    val className = getClass.getName.replace('.', '/')
    val classJar = getClass.getResource("/" + className + ".class").toString()
    classJar.startsWith("jar:")
  }

}
