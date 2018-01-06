package org.sinoz.akka.tcp

import akka.actor.Actor

/** @author Sino */
package object backpressure {
  case class ReadThrottleConfig(lowWaterMark: Long, highWaterMark: Long)
  case class WriteThrottleConfig()

  case class InboundConfig(readConfig: ReadThrottleConfig, writeConfig: WriteThrottleConfig)

  trait AckHelper { actor: Actor =>
    final def deferAck(ack: Any)(operation: => Unit): Unit = {
      try {
        operation
      } finally {
        sender() ! ack
      }
    }
  }
}
