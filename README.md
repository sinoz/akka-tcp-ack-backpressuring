# akka-tcp-ack-backpressuring
A small layer on top of the Akka TCP module implementing ACK based backpressuring for both read and writes. The Akka framework does not provide users with a backpressuring layer as congestion must be handled on the user level. This module implements ACK based backpressuring allowing you, the user, to continue being productive (hopefully).

## Write Backpressure
Write Backpressure is achieved by only allowing one write to be processed at a time per `InboundHandler` (the reader/writer actor that is connected to the internal connection actor in Akka). Further write requests are enqueued and the inbound handler actor will await a `ReadAck` message which indicates that the write was successfully processed by the OS.

## Read Backpressure
Read Backpressure is achieved differently from how Write Backpressure is achieved, although still ACK based. `Received` messages coming from the Akka connection actor, are flown to the injected data handler actor until the `InboundHandler` has reached the configured high watermark. The connection actor will then be notified to suspend reading from the socket until it receives the `ResumeReading` message from the `InboundHandler` actor.

## Installation
TODO

## Usage
Making use of this is as easy as creating an actor:

```
val inboundConfig = InboundConfig(
  ReadThrottleConfig(
    lowWaterMark = 5000,
    highWaterMark = 15000
  ),

  WriteThrottleConfig(
    // nothing
  )
)

val tcp = context.actorOf(TcpServer.props(inboundConfig, dataHandlerProducer), "tcp")
```

In order to start listening at a specific port, simply send the `TcpServer` actor a `BindTo` message:
`tcp ! BindTo(port = 8888)`

To stop the `TcpServer` actor from listening for inbound connections, a `CloseDown()` message may be sent.

### Handling your data
As you've probably noticed, a `dataHandlerProducer` is passed in the `TcpServer` actor `Props` configuration. To create your own customized producer, simply inherit the `DataHandlerProducer` trait:

```
final class MyCustomDataHandlerProducer(eventHandler: ActorRef) extends DataHandlerProducer {
  override def produce(inboundHandler: ActorRef) =
    Props(new MyCustomDataHandler(eventHandler, inboundHandler))
}
```

And don't forget your custom data handler, which will listen for `DataAvailable(data, ack)` messages.

```
final class MyCustomDataHandler(eventHandler: ActorRef, inboundHandler: ActorRef) extends Actor {
  override def receive = {
    case DataAvailable(data, ack) =>
      try {
        eventHandler ! MyEvent(data.head)
      } finally {
        sender() ! ack
      }
  }
}
```

To maintain backpressure, it is very important to send back the given `Ack` message back to the client once your data is processed, as is demonstrated in the example above. Should you find the usual Try/Finally blocks aesthetically displeasing, the helper `AckHelper` trait comes to the rescue:

```
case DataAvailable(data, ack) =>
  deferAck(ack) {
      eventHandler ! MyEvent(data.head)
  }
```

### Sending data back
Data can be sent to the remote TCP client by sending the injected `inboundHandler` in your custom data handler, a `SendData(ByteString())` message.

### Supervisor Strategy
Errors that come from children of the `TcpServer` which is also your data handler, are automatically escalated to the parent of the `TcpServer` actor. Simply override the `supervisorStrategy` method:

```
override def supervisorStrategy = OneForOneStrategy() {
  case _: Exception => Restart
}
```
