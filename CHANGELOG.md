# Changelog

## [1.0-1] - 2022-07-28

This is the first release of the minimal-jwebsocket Java library.

### Features
- Drop-in replacement for `java.net` `ServerSocket` and `Socket` classes, able to interact with regular java.net Socket clients and web browsers
- Automatic HTTP Upgrade handling to WebSocket protocol upon web browser handshake
- Automatic encoding and decoding of traffic between server and web browser client after WebSocket handshaking with the help of background processes