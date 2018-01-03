package org.sinoz.akka.tcp.backpressure

import akka.actor.{Actor, ActorRef, Props}
import akka.io.Tcp.{Received, ResumeReading, SuspendReading}
import akka.util.ByteString
import org.sinoz.akka.tcp.backpressure.ReadThrottle.{NegativeAmtPendingBytes, ReadAck, ThrottledData}

/** @author Sino */
final class ReadThrottle(connection: ActorRef, inboundHandler: ActorRef, readThrottleConfig: ReadThrottleConfig) extends Actor {
  var pendingReads = Seq.empty[ByteString]
  var amtPendingBytes = 0L

  var suspended = false

  override def receive = {
    case Received(data) =>
      pendingReads = pendingReads :+ data
      amtPendingBytes = amtPendingBytes + data.size

      if (amtPendingBytes >= readThrottleConfig.highWaterMark) {
        suspended = true
        connection ! SuspendReading
      }

      inboundHandler ! ThrottledData(data, ReadAck)

    case ReadAck =>
      amtPendingBytes = amtPendingBytes - pendingReads.head.size
      if (amtPendingBytes < 0) throw NegativeAmtPendingBytes()

      if (suspended && amtPendingBytes < readThrottleConfig.lowWaterMark) {
        suspended = false
        connection ! ResumeReading
      }

      pendingReads = pendingReads drop 1

    case msg =>
      unhandled(msg)
  }
}

object ReadThrottle {
  case class NegativeAmtPendingBytes() extends Exception

  case class ThrottledData(data: ByteString, ack: Any)
  case object ReadAck

  def props(connection: ActorRef, inboundHandler: ActorRef, readThrottleConfig: ReadThrottleConfig) =
    Props(new ReadThrottle(connection, inboundHandler, readThrottleConfig))
}
