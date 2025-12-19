
# Server

[ServerApp](src/main/java/server/ServerApp.java) has main method that starts Tomcat with Spring MVC.
The [SseController](src/main/java/server/SseController.java) exposes SSE endpoints.

To start the server within an IDE, run the `ServerApp` main method, or
otherwise use `./gradlew server` from the commandline.

# Client

[ClientApp](src/main/java/client/ClientApp.java) has a main method with several scenarios that
consume an SSE stream from the server.

To run the client within an IDE, run the `ServerApp` main method, or
otherwise use `./gradlew client` from the commandline.