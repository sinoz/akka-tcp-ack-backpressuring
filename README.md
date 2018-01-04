# akka-tcp-ack-backpressuring
A small layer on top of the Akka TCP module implementing ACK based backpressuring for both read and writes. The Akka framework does not provide users with a backpressuring layer as congestion must be handled on the user level. This module implements ACK based backpressuring allowing you, the user, to continue being productive (hopefully).

## Write Backpressure
Write Backpressure is achieved by only allowing one write to be processed at a time per `InboundHandler` (the reader/writer actor that is connected to the internal connection actor in Akka). Further write requests are enqueued and the inbound handler actor will await a `ReadAck` message which indicates that the write was successfully processed by the OS.

## Read Backpressure
Read Backpressure is achieved differently from how Write Backpressure is achieved, although still ACK based. `Received` messages coming from the Akka connection actor, are flown to the injected data handler actor until the `InboundHandler` has reached the configured high watermark. The connection actor will then be notified to suspend reading from the socket until it receives the `ResumeReading` message from the `InboundHandler` actor.
