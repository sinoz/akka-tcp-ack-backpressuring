package org.sinoz.akka.tcp.backpressure

import java.net.InetSocketAddress

import akka.actor.SupervisorStrategy.Stop
import akka.actor.{Actor, ActorLogging, ActorRef, OneForOneStrategy, Props}
import akka.io.Tcp._
import akka.io.{IO, Tcp}
import org.sinoz.akka.tcp.backpressure.InboundHandlerManager.Inbound
import org.sinoz.akka.tcp.backpressure.InboundListener.{BindTo, CloseDown}

/** @author Sino */
final class InboundListener(handlerManager: ActorRef) extends Actor with ActorLogging {
  override def supervisorStrategy = OneForOneStrategy() {
    case _ => Stop
  }

  override def receive = {
    case BindTo(port) =>
      IO(Tcp)(context.system) ! Bind(self, new InetSocketAddress(port))

    case Bound(local) =>
      log.info(s"Local TCP channel bound at $local")
      context become bound(local)

    case msg =>
      unhandled(msg)
  }

  def bound(local: InetSocketAddress): Receive = {
    case Connected(_, _) =>
      handlerManager ! Inbound(sender())

    case CloseDown() =>
      IO(Tcp)(context.system) ! Unbind

    case Unbound =>
      context unbecome()

    case msg =>
      unhandled(msg)
  }
}

object InboundListener {
  case class BindTo(port: Int) extends InboundListenerMsg
  case class CloseDown() extends InboundListenerMsg
  sealed abstract class InboundListenerMsg

  def props(handlerManager: ActorRef) =
    Props(new InboundListener(handlerManager))
}
