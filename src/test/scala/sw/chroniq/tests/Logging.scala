package sw.chroniq.tests

import org.slf4j.LoggerFactory

/** @author Stephen Samuel */
trait Logging {
    val logger = LoggerFactory.getLogger(getClass)
}
