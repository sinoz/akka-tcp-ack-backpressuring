package org.sinoz.akka.tcp.backpressure

import akka.actor.SupervisorStrategy.Escalate
import akka.actor.{Actor, ActorRef, OneForOneStrategy, Props}
import akka.io.Tcp.Register
import org.sinoz.akka.tcp.backpressure.InboundHandler.SubscribeDataHandler
import org.sinoz.akka.tcp.backpressure.InboundHandlerManager.Inbound

/** @author Sino */
final class InboundHandlerManager(config: InboundConfig, dataHandlerProducer: DataHandlerProducer) extends Actor {
  override def supervisorStrategy = OneForOneStrategy() {
    case _: Exception => Escalate
  }

  override def receive = {
    case Inbound(connection) =>
      val inboundHandler = context.actorOf(InboundHandler.props(connection, config))
      val dataHandler = context.actorOf(dataHandlerProducer.produce(inboundHandler))

      inboundHandler ! SubscribeDataHandler(dataHandler)

      connection ! Register(inboundHandler, keepOpenOnPeerClosed = true)

    case msg =>
      unhandled(msg)
  }
}

object InboundHandlerManager {
  case class Inbound(connection: ActorRef)

  def props(config: InboundConfig, dataHandlerProducer: DataHandlerProducer) =
    Props(new InboundHandlerManager(config, dataHandlerProducer))
}
