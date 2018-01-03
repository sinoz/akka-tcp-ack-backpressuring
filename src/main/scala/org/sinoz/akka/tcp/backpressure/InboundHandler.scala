package org.sinoz.akka.tcp.backpressure

import akka.actor.SupervisorStrategy.{Escalate, Restart}
import akka.actor.{Actor, ActorRef, OneForOneStrategy, Props}
import akka.io.Tcp.{ConnectionClosed, Received}
import akka.util.ByteString
import org.sinoz.akka.tcp.backpressure.InboundHandler.{DataAvailable, SendData, SubscribeDataHandler, UnsubscribeDataHandler}
import org.sinoz.akka.tcp.backpressure.ReadThrottle.{NegativeAmtPendingBytes, ThrottledData}
import org.sinoz.akka.tcp.backpressure.WriteThrottle.ThrottleWrite

/** @author Sino */
final class InboundHandler(connection: ActorRef, inboundConfig: InboundConfig) extends Actor {
  val writeThrottle = context.actorOf(WriteThrottle.props(connection, inboundConfig.writeConfig), "write-throttle")
  val readThrottle = context.actorOf(ReadThrottle.props(connection, self, inboundConfig.readConfig), "read-throttle")

  override def supervisorStrategy = OneForOneStrategy() {
    case NegativeAmtPendingBytes() => Restart
    case _: Exception => Escalate
  }

  override def receive =
    inbound(dataHandler = None)

  def inbound(dataHandler: Option[ActorRef]): Receive = {
    case msg: Received =>
      readThrottle forward msg

    case SubscribeDataHandler(handler) =>
      context.become(inbound(dataHandler = Some(handler)))

    case UnsubscribeDataHandler() =>
      context.become(inbound(dataHandler = None))

    case SendData(data) =>
      writeThrottle forward ThrottleWrite(data)

    case ThrottledData(data, ack) =>
      dataHandler.foreach(_.forward(DataAvailable(data, ack)))

    case _: ConnectionClosed =>
      context stop self

    case msg =>
      unhandled(msg)
  }
}

object InboundHandler {
  case class SubscribeDataHandler(handler: ActorRef)
  case class UnsubscribeDataHandler()

  case class DataAvailable(data: ByteString, ack: Any)
  case class SendData(data: ByteString)

  def props(connection: ActorRef, inboundConfig: InboundConfig) =
    Props(new InboundHandler(connection, inboundConfig))
}
