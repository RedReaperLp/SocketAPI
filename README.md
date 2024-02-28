# Socket Reaper

## Index

- [Socket Reaper](#socket-reaper)
    - [Index](#index)
    - [Introduction](#introduction)
    - [Implementation](#implementation)
        - [Gradle Kotlin:](#gradle-kotlin)
        - [Maven:](#maven)
    - [Dependencies](#dependencies)
    - [Usage](#usage)
        - [Initializing a Server](#initializing-a-server)
        - [Initializing a Client](#initializing-a-client)
            - [Requests](#requests)
                - [Creating a responding Request](#creating-a-responding-request)
                - [Creating a non-responding Request](#creating-a-non-responding-request)
                - [Registering a Request](#registering-a-request)
                - [Handling a Request](#handling-a-request)
                - [Sending a Request](#sending-a-request)
            - [Custom Connection classes](#custom-connection-classes)
                - [Registering a Custom Connection class](#registering-a-custom-connection-class)
    - [License](#license)

## Introduction

Socket Reaper is a fairly simple tool that allows you to open a socket to a TCP-Socket and send a requests to it.
When the Request is being sent, you have the ability to wait for a response or just ignore the result.<br>
It is designed to be used as a communication tool between two applications which need to communicate in real-time.<br>
For help join my [Discord server](https://discord.gg/ghhKXDGQhD) or contact me privately on Discord (@redreaperlp)

## Dependencies

- [JSON-Java](https://mvnrepository.com/artifact/org.json/json)

## Implementation

### Gradle Kotlin:

```kotlin
repositories {
    maven("https://eldonexus.de/repository/maven-public/")
}

dependencies {
    implementation("com.github.redreaperlp", "socket-reaper", "1.0.0")
}
```

### Maven:

```xml

<project>
	...
	<repositories>
		<repository>
			<id>eldonexus</id>
			<url>https://eldonexus.de/repository/maven-public/</url>
		</repository>
	</repositories>
	<dependency>
		<groupId>com.github.redreaperlp</groupId>
		<artifactId>socketapi</artifactId>
		<version>1.0</version>
	</dependency>
</project>
```

## Initializing a Server:

- The server is initialized with a port on which it will listen for incoming connections

```java
    SocketServer server = new SocketServer(port);
```

<br>

## Initializing a Client:

- The client is initialized with a host and a port to which it will connect

```java
    SocketClient client = new SocketClient(host, port);
```

<br>

## Requests:

#### Creating a responding Request:

- This is the request that is expecting a response
- It has to extend
  the [RequestPromising](src/main/java/com/github/redreaperlp/socketapi/communication/request/special/RequestPromising.java)
  abstract class

```java
public class RequestPing extends RequestPromising {
    public static final String name = "ping"; // The name of the request (used for identification so it should be unique)

    public RequestPing(long id) { // The id is used to identify the request when the response is received
        super(id);
    }

    @Override
    public String getName() { // Returns the name of the request
        return name;
    }
}
```

<br>

#### Creating a non-responding Request:

- This is basically the same but with a slight change
- It has to extend
  the [RequestVoiding](src/main/java/com/github/redreaperlp/socketapi/communication/request/special/RequestVoiding.java)
  abstract class
- The id is not needed as there is no response expected

```java
public class RequestPing extends RequestVoiding {
    public static final String name = "notification"; // The name of the request (used for identification so it should be unique)

    public RequestPing() {
        super();
    }

    @Override
    public String getName() { // Returns the name of the request
        return name;
    }
}
```

<br>

#### Registering a Request:

- The registration has to be done in both the client and the server like in the example below
    - RequestPing.name refers to the name of the request [RequestPing](#creating-a-responding-request)
    - RequestPing.class refers to the class of the request [RequestPing](#creating-a-responding-request)

```java
public static void main(String[] args) {
    RequestManager.registerRequest(RequestPing.name, RequestPing.class);
    RequestManager.registerRequest(RequestRegister.name, RequestRegister.class);
}
``` 

<br>

#### Handling a Request:

- The handler handling the request has to be registered. This can be done in both the client and the server like in the
  example below
    - [SocketServer](#initializing-a-server) and [SocketClient](#initializing-a-client) both extend the
      [NetInstance](src/main/java/com/github/redreaperlp/socketapi/ns/NetInstance.java) class
- Encrypted communication is also possible, you can use
  the [useEncryption()](src/main/java/com/github/redreaperlp/socketapi/ns/NetInstance.java#L23) method to specify a key
    - Note: The key has to be the same for the server and the client, the best way is to manually set it in the
      configuration of your application

```java
public static void main(String[] args) {
    SocketServer server = new SocketServer(port);
    //or
    SocketClient client = new SocketClient(host, port);

    //optionally you can specify an encryption key
    server.useEncryption("key");
    client.useEncryption("key");

    //here we register the handler for the request
    server.registerPromisingHandler(RequestPing.class, (req, data) -> {
        //here you can handle the request, the data is the data that was sent with the request
        //the req is the request itself (here RequestPing so you can access all fields and methods of the request)
    });
    //for servers, these handlers are applied to all clients, 
    //if you want to handle a request for a specific type of client, you have to create a new Custom Connection class
}
```

- instead of creating a handler, you can also override
  the [validateResponse()](src/main/java/com/github/redreaperlp/socketapi/ns/RequestHandler.java#L33) method in
  the [RequestPromising](src/main/java/com/github/redreaperlp/socketapi/communication/request/special/RequestVoiding.java)
  class
    - this method is called when a response is received and the request is
      a [RequestPromising](src/main/java/com/github/redreaperlp/socketapi/communication/request/special/RequestPromising.java)
    - if the response is not satisfying, you can call
      the [failed(int)](src/main/java/com/github/redreaperlp/socketapi/communication/request/special/RequestPromising.java#L172)
      method to mark the request as failed
      <br>

#### Sending a Request:

- You can send a request from both the client and the server.
- Here we take the [Initialization](#initializing-a-server) of the server as an example

```java
public static void main(String[] args) {
    // Server or client is initialized beforehand (...)

    // Request is retrieved, an ID is automatically generated if it's a response request
    Request req = client.getRequest(RequestRegister.class);

    // Data of the request is set, which is used in the RequestHandler
    req.setData(new JSONObject().put("name", "test"));

    // Two options: send the request and wait for the response or send the request and ignore the response
    // If waiting for the response:
    Response res = req.complete(); // This sends the request, waits for the response, or marks it as failed if it times out
    if (req.failed()) {
        // Handle the failure here
    } else {
        // Handle the response here
    }

    // If ignoring the response:
    req.queue(); // This sends the request and ignores the response
}
```

- if you have any fields in your request, you have to overwrite
  the [pack()](src/main/java/com/github/redreaperlp/socketapi/communication/request/Request.java#L23) like in the
  example below to send the request with the data
  this is also the data that is received in
  the [RequestHandler](src/main/java/com/github/redreaperlp/socketapi/communication/handler/RequestHandler.java#L79)

```java
public class RequestPlayerMoved extends RequestPromising {
    private Player player;
    private int x;
    private int y;

    public RequestPlayerMoved(long id, Player player, int x, int y) {
        super(id);
        this.player = player;
        this.x = x;
        this.y = y;
    }

    @Override
    public void pack() {
        setData(new JSONObject()
                .put("player", player.toJson())
                .put("x", x)
                .put("y", y)
        );
    }
}
```

## Custom Connection classes

- These are used to have connections with different handlers, for instance, you have a Minecraft Server with Proxy and
  Games, on the Proxy you want to handle each game differently
    - Normally you would have to write a complex logic to determine which game is sending the request and then handle it
      accordingly, this is where Custom Connection classes come into play
- You can create a Custom Connection class by extending
  the [Connection](src/main/java/com/github/redreaperlp/socketapi/communication/Connection.java) class

```java
public class GameConnection extends Connection {
    public GameConnection(Socket socket, SocketServer server) {
        super(socket, server);
    }

    @Override
    public void registerHandlers() { // Here you can register the handlers for the specific connection and requests
        getRequestHandler().registerPromisingHandler(RequestPlayerMoved.class, (req, data) -> {
            //handle the request
        });
    }
}
```

#### Registering a Custom Connection class

- You can register a Custom Connection class by calling
  the [registerCustomConnection()](src/main/java/com/github/redreaperlp/socketapi/ns/ConnectionHandler.java#L31) method
- The first parameter is the name of the connection, the second parameter is the class of the connection
- Note: If you have registered at least one Custom Connection class, you have to specify one of your created classes,
  otherwise the server will reject the
  connection [client.setConnectionIdentifier(String name)](src/main/java/com/github/redreaperlp/socketapi/ns/client/SocketClient.java#L55)

```java
public void start() {
    ConnectionHandler h = ConnectionHandler.getInstance(); //This is singleton so you can call it from anywhere
    h.registerCustomConnection("name", GameConnection.class);
}
```

### Planned Features:

- [x] Use a secure connection (Custom)

## License

This project is licensed under the Apache License 2.0 - see the [LICENSE](LICENSE.txt) file for details
