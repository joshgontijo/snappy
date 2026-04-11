# Snappy

A simple and small web server API built on top of [Undertow](https://undertow.io).

## Features

- Request/response interceptors (before, after, CORS, auth)
- Server-Sent Events with broadcast groups
- WebSockets
- Static files
- `snappy.properties` config with environment variable and system property overrides
- Small footprint — single JAR, no classpath magic

---

## Maven

```xml
<dependency>
    <groupId>io.joshworks.snappy</groupId>
    <artifactId>snappy</artifactId>
    <version>0.7.0</version>
</dependency>
```

---

## Quick Start

```java
import static io.joshworks.snappy.SnappyServer.*;
import static io.joshworks.snappy.http.Response.*;

public class App {
    public static void main(String[] args) {
        get("/hello", req -> ok("Hello, World!"));
        start(); // listens on port 9000
    }
}
```

---

## REST Endpoints

All HTTP methods are supported. Handlers receive a `Request` and return a `Response`.

```java
get("/users",          req -> ok(userList));
post("/users",         req -> created(save(req.body().asObject(User.class))));
put("/users/{id}",     req -> ok(update(req.pathParameter("id"), req.body().asObject(User.class))));
delete("/users/{id}",  req -> { delete(req.pathParameter("id")); return noContent(); });
patch("/users/{id}",   req -> ok(patch(req.pathParameter("id"), req.body().asObject(User.class))));
```

For any custom HTTP method use `add()`:

```java
add(Methods.TRACE, "/trace", req -> ok());
```

### Path Parameters

```java
get("/users/{id}", req -> {
    String id = req.pathParameter("id");
    return ok(findUser(id));
});
```

### Query Parameters

```java
get("/search", req -> {
    String q    = req.queryParameter("q");
    int    page = req.queryParameterVal("page").asInt(1);
    return ok(search(q, page));
});
```

### Receiving JSON

```java
post("/users", req -> {
    User user = req.body().asObject(User.class);
    return created(save(user));
});
```

### Returning JSON

```java
get("/users", req -> ok(new User("John Doe")));
```

The response body is serialized using the registered parser for the negotiated `Content-Type`
(defaults to `application/json`).

### Content Negotiation

Restrict which media types an endpoint accepts or produces:

```java
import static io.joshworks.snappy.parser.MediaTypes.*;

post("/data", req -> ok(req.body().asObject(MyType.class)),
    consumes("application/json"),
    produces("application/json"));
```

---

## Response Builder

```java
Response.ok()                 // 200
Response.ok(body)             // 200 with body
Response.created(body)        // 201 with body
Response.noContent()          // 204
Response.badRequest()         // 400
Response.unauthorized()       // 401
Response.notFound()           // 404
Response.internalServerError() // 500

// Chaining headers, cookies, and status
Response.withStatus(202)
        .body(result)
        .header("X-Request-Id", id)
        .type("application/json");
```

---

## Interceptors

Interceptors run before or after endpoint handlers. URL patterns support exact paths and wildcards (`*`).

```java
// Runs before ALL handlers (root interceptor)
beforeAll("/*", req -> {
    System.out.println("→ " + req.method() + " " + req.path());
});

// Runs before the matched endpoint only
before("/users/*", req -> {
    // e.g. validate a header
});

// Runs after the matched endpoint
after("/users/*", (req, res) -> {
    res.header("X-Powered-By", "Snappy");
});

get("/users", req -> ok(userList));
start();

// Execution order for GET /users:
// beforeAll → before → handler → after
```

---

## Security

### Bearer / Custom Token

```java
// Returns 401 if the Authorization header does not match scheme + value
secured("/api/*", "Bearer", () -> System.getenv("API_TOKEN"));
```

### Custom Predicate

```java
secured("/api/*", (scheme, token) -> "Bearer".equals(scheme) && isValid(token));
```

### HTTP Basic Auth

```java
basicAuthSecured("/admin/*", (user, password) -> "admin".equals(user) && verify(password));
```

Inside a handler you can read the decoded credentials directly:

```java
get("/me", req -> {
    String token = req.bearerAuth(); // strips "Bearer " prefix
    String basic = req.basicAuth();  // strips "Basic " prefix
    return ok();
});
```

### CORS

Enables `Access-Control-Allow-*` headers on every response and handles `OPTIONS` preflight automatically:

```java
cors();
start();
```

---

## Error Handling

Register typed exception handlers to control the HTTP response for any exception thrown from an endpoint:

```java
exception(IllegalArgumentException.class, (e, req) ->
    Response.badRequest().body(e.getMessage()));

exception(NotFoundException.class, (e, req) ->
    Response.notFound());

get("/users/{id}", req -> ok(findOrThrow(req.pathParameter("id"))));

start();
```

Unhandled exceptions return a `500` response with a JSON body:

```json
{ "id": "550e8400-e29b-41d4-a716-446655440000", "message": "Something went wrong" }
```

---

## Static Files

Serve classpath resources. `index.html` is served for directory requests.

```java
// serves src/main/resources/static/** at /
staticFiles("/");

// serves src/main/resources/public/** at /pages
staticFiles("/pages", "public");

start();
```

---

## Multipart / File Upload

```java
import static io.joshworks.snappy.parser.MediaTypes.consumes;

post("/upload", req -> {
    Part file = req.multiPartBody().part("theFile");
    Files.copy(file.file().path(), targetOutputStream);
    return ok();
}, consumes("multipart/form-data"));

start();
```

---

## Server-Sent Events

### Broadcast to All Clients

```java
SseBroadcaster broadcaster = sse("/events");

onStart(() -> {
    ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    scheduler.scheduleAtFixedRate(
        () -> broadcaster.broadcast(new Date().toString()),
        0, 1, TimeUnit.SECONDS);
});

start();
// curl http://localhost:9000/events
```

### Connect Handler

```java
SseBroadcaster broadcaster = sse("/notifications", ctx -> {
    System.out.println("New connection from " + ctx.remoteAddress());
    ctx.keepAlive(15_000); // send keep-alive comment every 15 s
});
```

### Broadcast Groups / Topics

```java
SseBroadcaster broadcaster = sse("/subscribe/{topic}", ctx -> {
    ctx.joinGroup(ctx.pathParameter("topic"));
});

post("/publish/{topic}", req -> {
    broadcaster.broadcast(req.pathParameter("topic"), req.body().asString());
    return ok();
});

start();
// curl http://localhost:9000/subscribe/news
// curl -X POST http://localhost:9000/publish/news -d "breaking!"
```

### Connection Limit

```java
SseBroadcaster broadcaster = sse("/events", 500); // max 500 concurrent connections
```

---

## WebSockets

### Simple Text Handler

```java
websocket("/chat", (channel, message) ->
    WebSockets.sendText("echo: " + message.getData(), channel, null));

start();
```

### Full Endpoint

```java
websocket("/chat", new WebsocketEndpoint() {
    @Override
    public void onConnect(WebSocketHttpExchange exchange, WebSocketChannel channel) {
        System.out.println("Connected: " + channel.getPeerAddress());
    }

    @Override
    protected void onFullTextMessage(WebSocketChannel channel, BufferedTextMessage message) {
        WebSockets.sendText(message.getData(), channel, null);
    }
});
```

---

## Resource Groups

Group related endpoints under a common path prefix. Groups can be nested.

```java
group("/api/v1", () -> {
    get("/users",  req -> ok(allUsers()));      // GET  /api/v1/users
    post("/users", req -> created(newUser()));  // POST /api/v1/users

    group("/admin", () -> {
        get("/stats", req -> ok(stats()));      // GET  /api/v1/admin/stats
    });
});

start();
```

---

## Custom Parsers

Register a parser for any media type before calling `start()`. Built-in parsers cover
`application/json` (Gson) and `text/plain`.

```java
public class XmlParser implements Parser {
    @Override
    public <T> T readValue(String value, Class<T> type) { /* ... */ }

    @Override
    public String writeValue(Object value) { /* ... */ }
}

Parsers.register(MediaType.valueOf("application/xml"), new XmlParser());

get("/data", req -> ok(myObject), produces("application/xml"));

start();
```

---

## Configuration

### snappy.properties

Place `snappy.properties` on the classpath. Every key can be overridden by a system property
(`-Dhttp.port=8080`) or an environment variable (`HTTP_PORT=8080`).

```properties
http.port=8080
http.bind=0.0.0.0
http.portOffset=0
http.tracer=false

xnio.io.threads=4
xnio.worker.coreThread=4
xnio.worker.maxThread=40

tcp.nodelay=true
```

### Programmatic Configuration

All configuration methods must be called **before** `start()`.

```java
port(8080);
address("127.0.0.1");
maxEntitySize(1024 * 1024);         // 1 MB request body limit
maxMultipartSize(10 * 1024 * 1024); // 10 MB per part
workerThreads(4, 40);
enableTracer();                     // logs every exchange at DEBUG level

adminPort(9100);                    // admin listener port  (default 9100)
adminAddress("127.0.0.1");          // admin listener address (default 127.0.0.1)

start();
```

### Lifecycle Listeners

```java
onStart(()    -> System.out.println("Server is up"));
onShutdown(() -> System.out.println("Server is down"));

start();
```

---

## Base Path

Prepend a fixed prefix to **all** REST endpoints (no effect on WebSocket, SSE, or static-file endpoints):

```java
basePath("/api/v1");

get("/users", req -> ok()); // reachable at /api/v1/users

start();
```