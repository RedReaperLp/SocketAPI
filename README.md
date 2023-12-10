# Socket Reaper

## Index

- [Socket Reaper](#socket-reaper)
    - [Index](#index)
    - [Introduction](#introduction)
    - [Artifacts](#artifacts)
    - [Dependencies](#dependencies)
        - [Initializing a Server:](#initializing-a-server)
        - [Initializing a Client:](#initializing-a-client)
        - [Creating a responding Request:](#creating-a-responding-request)
        - [Creating a non-responding Request:](#creating-a-non-responding-request)
        -
        - [Sending a Request:](#sending-a-request)
    - [Usage](#usage)
    - [Installation](#installation)
    - [Configuration](#configuration)
    - [License](#license)

## Introduction

Socket Reaper is a fairly simple tool that allows you to open a socket to a TCP-Socket and send a requests to it.
When the Request is being sent, you have the ability to wait for a response or just ignore the result.<br>
It is designed to be used as a communication tool between two applications which need to communicate in real-time.<br>

## Dependencies

## Artifacts

## Usage

#### Initializing a Server:

```java
    SocketServer server = new SocketServer(port);
```

<br>

#### Initializing a Client:

```java
    SocketClient client = new SocketClient(host, port);
```

<br>

#### Creating a responding Request:

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

```java
public class RequestPing extends RequestVoiding {
    public static final String name = "notification"; // The name of the request (used for identification so it should be unique)

    public RequestPing() { // As this request is not expecting a response, there is no need to pass an id
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
```java
public static void main(String[] args) {
    SocketServer server = new SocketServer(port);
    //or
    SocketClient client = new SocketClient(host, port); 
    
    //here we register the handler for the request
    server.getRequestHandler().registerPromisingHandler(RequestPing.class, (req, data) -> {
        //here you can handle the request, the data is the data that was sent with the request
        //the req is the request itself (here RequestPing so you can access all fields and methods of the request)
    });
}
```