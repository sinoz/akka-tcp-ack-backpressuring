package org.sinoz.akka.tcp.backpressure

import akka.actor.{Actor, ActorRef, Props}
import akka.io.Tcp
import akka.io.Tcp.Write
import akka.util.ByteString
import org.sinoz.akka.tcp.backpressure.WriteThrottle.ThrottleWrite

/** @author Sino */
final class WriteThrottle(connection: ActorRef, writeThrottleConfig: WriteThrottleConfig) extends Actor {
  var dispatchedWrite: Option[ByteString] = None
  var pendingWrites = Seq.empty[ByteString]

  override def receive = {
    case ThrottleWrite(data) =>
      if (dispatchedWrite.isEmpty) {
        connection ! Write(data, WriteAck)
        dispatchedWrite = Some(data)
      } else {
        pendingWrites :+= data
      }

    case WriteAck =>
      dispatchedWrite = None
      if (pendingWrites.nonEmpty) {
        connection ! Write(pendingWrites.head, WriteAck)
        dispatchedWrite = Some(pendingWrites.head)

        pendingWrites = pendingWrites drop 1
      }

    case msg =>
      unhandled(msg)
  }

  case object WriteAck extends Tcp.Event
}

object WriteThrottle {
  case class ThrottleWrite(data: ByteString)

  def props(connection: ActorRef, writeThrottleConfig: WriteThrottleConfig) =
    Props(new WriteThrottle(connection, writeThrottleConfig))
}
