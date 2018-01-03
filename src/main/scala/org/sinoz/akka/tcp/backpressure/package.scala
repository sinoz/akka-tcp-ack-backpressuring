package org.sinoz.akka.tcp

/** @author Sino */
package object backpressure {
  case class ReadThrottleConfig(lowWaterMark: Long, highWaterMark: Long)
  case class WriteThrottleConfig()

  case class InboundConfig(readConfig: ReadThrottleConfig, writeConfig: WriteThrottleConfig)
}
