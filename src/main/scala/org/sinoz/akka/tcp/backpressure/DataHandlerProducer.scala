package org.sinoz.akka.tcp.backpressure

import akka.actor.{ActorRef, Props}

/** @author Sino */
trait DataHandlerProducer {
  def produce(inboundHandler: ActorRef): Props
}
