# Socket Reaper

## Index

- [Socket Reaper](#socket-reaper)
    - [Index](#index)
    - [Introduction](#introduction)
    - [Dependencies](#dependencies)
    - [Usage](#usage)
    - [Initializing a Server:](#initializing-a-server)
    - [Initializing a Client:](#initializing-a-client)
    - [Creating a responding Request:](#creating-a-responding-request)
    - [Creating a non-responding Request:](#creating-a-non-responding-request)
    - [Sending a Request:](#sending-a-request)
    - [License](#license)

## Introduction

Socket Reaper is a fairly simple tool that allows you to open a socket to a TCP-Socket and send a requests to it.
When the Request is being sent, you have the ability to wait for a response or just ignore the result.<br>
It is designed to be used as a communication tool between two applications which need to communicate in real-time.<br>

## Dependencies

- [JSON-Java](https://mvnrepository.com/artifact/org.json/json)

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

- This is the request that is expecting a response
- It has to extend
  the [RequestPromising](src/main/java/com/github/redreaperlp/socketapi/communication/request/special/RequestPromising.java)
  class

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
  class
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

<br>

#### Sending a Request:

```java
public static void main(String[] args) {
    [...] //Server or Client are initialized before
    Request req = client.getRequest(RequestRegister.class); //here we get the request, if it is a responding request, it will automatically generate an id for it
    req.setData(new JSONObject().put("name", "test")); //here we set the data of the request, this is what you get in the [RequestHandler](#handling-a-request)
    //now you have two options, either you send the request and wait for the response or you just send it and ignore the response
    //if you want to wait for the response, you can use the following code
    Response res = req.complete(); //this will send the request and wait for the response or is marked as failed when it times out
    if (req.failed()) { //here we check if the request failed
        //here you can handle the failure
    } else {
        //here you can handle the response
    }

    //if you want to ignore the response, you can use the following code
    req.queue(); //this will send the request and ignore the response
}
```

