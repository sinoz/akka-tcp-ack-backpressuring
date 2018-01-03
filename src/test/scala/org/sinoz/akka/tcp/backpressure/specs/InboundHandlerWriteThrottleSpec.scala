package org.sinoz.akka.tcp.backpressure.specs

import akka.actor.{Actor, ActorSystem, Props}
import akka.io.Tcp.Write
import akka.testkit.{ImplicitSender, TestKit}
import akka.util.ByteString
import org.scalatest.{BeforeAndAfterAll, FlatSpecLike}
import org.sinoz.akka.tcp.backpressure.InboundHandler.SendData
import org.sinoz.akka.tcp.backpressure.{InboundConfig, InboundHandler, ReadThrottleConfig, WriteThrottleConfig}

/** @author Sino */
final class InboundHandlerWriteThrottleSpec extends TestKit(ActorSystem("InboundHandlerWriteThrottleSpec"))
  with FlatSpecLike
  with ImplicitSender
  with BeforeAndAfterAll {

  val config = InboundConfig(
    ReadThrottleConfig(
      lowWaterMark = 5000,
      highWaterMark = 15000
    ),

    WriteThrottleConfig(
      // nothing
    )
  )

  "An InboundHandler" should "properly throttle frequent writes to the OS" in {
    val connection = childActorOf(Props(new MockConnection), "connection")
    val handler = childActorOf(InboundHandler.props(connection, config), "handler")

    0 until 25 foreach { id =>
      handler ! SendData(ByteString(id))
    }
  }

  final class MockConnection extends Actor {
    override def receive = {
      case Write(_, ack) =>
        sender() ! ack

      case msg =>
        unhandled(msg)
    }
  }

  override protected def afterAll() = {
    system.terminate()
  }
}
