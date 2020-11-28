BA Thesis Project: ZIO Message Broker
---

A simple Message Broker that uses CoAP on the publisher-side and gRPC on the subscriber-side. The internal storage is done with ZIO's Software Transactional Memory implementation to ensure thread-safety without deadlocks.

All data from the DatagramChannel goes through ZStreams up to the storage and from there is routed to its respective subscribers. Via gRPC bidirectional HTTP2s streams are used to push the message to the subscribers.

As of now, only a sunset of CoAP is implemented. JSON is not yet supported, so all data is parsed and passed as UTF-8 strings. Similar to the IETF CoAP Pub-Sub draft, a topic is defined by a CoAP's message URI-path while only PUT-coded messages are accepted. The application will acknowledge all correctly formated requests but is of course unable to send any data (in that direction).

Upcoming changes are retainer messages, discovery and broadcasting as well as simple logging and config reads (via their ZIO libraries).

Usage
---

The project uses SBT to compile, test, run and assemble the project. The following commands allow exactly that:

```
sbt compile
sbt test
sbt run
sbt assembly
```

An assembled .jar can be used like any other with ``scala *.jar``. 

Settings
---

The CoAP port is 5683 by default while the gRPC one is 8970. As of now, the only way to change these values is by changing their values in ``infrastructure/persistence/config/ConfigRepositoryInMemory.scala`` and ``infrastructure/SubscriberSerber.scala`` respectively.
