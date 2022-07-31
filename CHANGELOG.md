# Changelog

## [1.0-3] - 2022-07-31

Revision to initial release.

### Changes
- Attach JavaDoc and sources to release JAR
- Publish to Apache Maven Central Repository

### Features
- Drop-in replacement for `java.net` `ServerSocket` and `Socket` classes, able to interact with regular `java.net` `Socket` clients and web browsers
- Automatic HTTP Upgrade handling to WebSocket protocol upon web browser handshake
- Automatic encoding and decoding of traffic between server and web browser client after WebSocket handshaking with the help of background processes