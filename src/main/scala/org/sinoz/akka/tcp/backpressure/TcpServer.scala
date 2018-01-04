package org.sinoz.akka.tcp.backpressure

import akka.actor.SupervisorStrategy.Escalate
import akka.actor.{Actor, OneForOneStrategy, Props}
import org.sinoz.akka.tcp.backpressure.InboundListener.InboundListenerMsg

/** @author Sino */
final class TcpServer(config: InboundConfig, dataHandlerProducer: DataHandlerProducer) extends Actor {
  val handlerManager = context.actorOf(InboundHandlerManager.props(config, dataHandlerProducer), "inbound-handler-manager")
  val inboundListener = context.actorOf(InboundListener.props(handlerManager), "inbound-listener")

  override def supervisorStrategy = OneForOneStrategy() {
    case _: Exception => Escalate
  }

  override def receive = {
    case msg: InboundListenerMsg =>
      inboundListener forward msg

    case msg =>
      unhandled(msg)
  }
}

object TcpServer {
  def props(config: InboundConfig, dataHandlerProducer: DataHandlerProducer) =
    Props(new TcpServer(config, dataHandlerProducer))
}
