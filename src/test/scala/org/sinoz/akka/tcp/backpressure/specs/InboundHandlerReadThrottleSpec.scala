package org.sinoz.akka.tcp.backpressure.specs

import akka.actor.{Actor, ActorLogging, ActorRef, ActorSystem, Props}
import akka.io.Tcp.Received
import akka.testkit.{ImplicitSender, TestKit}
import akka.util.ByteString
import org.scalatest.{BeforeAndAfterAll, FlatSpecLike}
import org.sinoz.akka.tcp.backpressure.InboundHandler.{DataAvailable, SubscribeDataHandler}
import org.sinoz.akka.tcp.backpressure.{InboundConfig, InboundHandler, ReadThrottleConfig, WriteThrottleConfig}

/** @author Sino */
final class InboundHandlerReadThrottleSpec extends TestKit(ActorSystem("InboundHandlerReadThrottleSpec"))
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

  "An InboundHandler" should "throttle frequent received socket reads" in {
    val connection = childActorOf(Props(new MockConnection), "connection")

    val inboundHandler = childActorOf(InboundHandler.props(connection, config), "handler")
    val dataHandler = childActorOf(Props(new MyDataHandler(inboundHandler)))

    inboundHandler ! SubscribeDataHandler(dataHandler)

    0 until 10 foreach { _ =>
      inboundHandler ! Received(ByteString(23))
    }

    Thread.sleep(10000)
  }

  final class MyDataHandler(inboundHandler: ActorRef) extends Actor with ActorLogging {
    override def receive = {
      case DataAvailable(data, ack) =>
        println(s"Received available data $data")
        sender() ! ack

      case msg =>
        unhandled(msg)
    }
  }

  final class MockConnection extends Actor with ActorLogging {
    override def receive = {
      case msg =>
        log.info(s"$msg")
    }
  }

  override protected def afterAll() = {
    system.terminate()
  }
}
