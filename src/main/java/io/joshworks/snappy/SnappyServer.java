/*
 * Copyright 2017 Josue Gontijo
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package io.joshworks.snappy;

import io.joshworks.snappy.handler.HandlerManager;
import io.joshworks.snappy.handler.HandlerUtil;
import io.joshworks.snappy.handler.MappedEndpoint;
import io.joshworks.snappy.http.ErrorHandler;
import io.joshworks.snappy.http.ExceptionMapper;
import io.joshworks.snappy.http.Group;
import io.joshworks.snappy.http.Handler;
import io.joshworks.snappy.http.HttpException;
import io.joshworks.snappy.http.Interceptors;
import io.joshworks.snappy.http.MediaType;
import io.joshworks.snappy.http.Request;
import io.joshworks.snappy.http.RequestContext;
import io.joshworks.snappy.http.RequestInterceptor;
import io.joshworks.snappy.http.Response;
import io.joshworks.snappy.http.ResponseInterceptor;
import io.joshworks.snappy.parser.JsonParser;
import io.joshworks.snappy.parser.MediaTypes;
import io.joshworks.snappy.parser.Parsers;
import io.joshworks.snappy.parser.PlainTextParser;
import io.joshworks.snappy.property.AppProperties;
import io.joshworks.snappy.property.PropertyKey;
import io.joshworks.snappy.sse.SseBroadcaster;
import io.joshworks.snappy.sse.SseHandler;
import io.joshworks.snappy.websocket.WebsocketEndpoint;
import io.undertow.Undertow;
import io.undertow.UndertowOptions;
import io.undertow.server.HttpHandler;
import io.undertow.util.HttpString;
import io.undertow.util.Methods;
import io.undertow.websockets.core.AbstractReceiveListener;
import io.undertow.websockets.core.BufferedTextMessage;
import io.undertow.websockets.core.WebSocketChannel;
import io.undertow.websockets.spi.WebSocketHttpExchange;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xnio.OptionMap;
import org.xnio.Options;
import org.xnio.Xnio;
import org.xnio.XnioWorker;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.BiPredicate;
import java.util.function.Consumer;
import java.util.function.Supplier;

import static io.joshworks.snappy.handler.HandlerUtil.BASE_PATH;
import static io.undertow.UndertowOptions.DEFAULT_MAX_ENTITY_SIZE;

/// Static façade for configuring and starting an embedded [Undertow](https://undertow.io) HTTP server.
///
/// ## Quick Start
///
/// ```java
/// import static io.joshworks.snappy.SnappyServer.*;
///
/// get("/hello", req -> Response.ok("Hello, World!"));
/// start();
/// ```
///
/// ## Configuration
///
/// All configuration methods **must** be called before [start()][SnappyServer#start()].
/// Calling any configuration method after the server has started throws [IllegalStateException].
///
/// ## Server Lifecycle
///
/// 1. Register endpoints, interceptors, and options.
/// 2. Call [start()][SnappyServer#start()] — the server binds on the configured port (default `9000`)
///    and a separate admin port (default `9100`).
/// 3. Call [stop()][SnappyServer#stop()] to gracefully shut down; the singleton is reset so
///    the server can be started again.
///
/// ## Thread Safety
///
/// All public static methods are `synchronized`. The server uses a double-checked locking singleton;
/// a fresh instance is created each time the server is started after a previous stop.
public class SnappyServer {

    public static final String LOGGER_NAME = "snappy";
    private static final int DEFAULT_PORT = 9000;

    private static final Logger logger = LoggerFactory.getLogger(LOGGER_NAME);

    private final AdminManager adminManager = new AdminManager();
    private XnioWorker worker;

    //--------------------------------------------
    private final List<Runnable> startListeners = new ArrayList<>();
    private final List<Runnable> shutdownListeners = new ArrayList<>();

    //--------------------------------------------
    private final OptionMap.Builder optionBuilder = OptionMap.builder();
    //-------------------------------------------

    private final List<MappedEndpoint> endpoints = new ArrayList<>();
    private final ExceptionMapper exceptionMapper = new ExceptionMapper();

    //--------------------- HTTP -------------------
    private Undertow server;
    private int port = DEFAULT_PORT;
    private String bindAddress = "0.0.0.0";
    private boolean httpTracer;
    private final Interceptors interceptors = new Interceptors();
    private String basePath = BASE_PATH;
    private boolean started = false;

    private static final Object LOCK = new Object();
    private static volatile SnappyServer INSTANCE;
    private long maxEntitySize = DEFAULT_MAX_ENTITY_SIZE;
    private long maxMultipartSize;


    private SnappyServer() {
        optionBuilder.set(Options.TCP_NODELAY, true);
        int processors = Runtime.getRuntime().availableProcessors();
        this.optionBuilder.set(Options.WORKER_IO_THREADS, processors);
        this.optionBuilder.set(Options.WORKER_TASK_CORE_THREADS, processors);
        this.optionBuilder.set(Options.WORKER_TASK_MAX_THREADS, processors * 10);
        this.optionBuilder.set(Options.REUSE_ADDRESSES, true);
        this.optionBuilder.set(Options.TCP_NODELAY, true);
        this.optionBuilder.set(Options.KEEP_ALIVE, true);
        this.optionBuilder.set(Options.WORKER_NAME, "snappy-worker");
    }

    private static SnappyServer instance() {
        if (INSTANCE == null) {
            synchronized (LOCK) {
                if (INSTANCE == null) {
                    INSTANCE = new SnappyServer();
                }
            }
        }
        return INSTANCE;
    }

    /// Starts the HTTP server.
    ///
    /// Loads `snappy.properties`, applies property overrides, registers default parsers,
    /// resolves all endpoints and interceptors, then binds on the configured port.
    /// Fires all [onStart][SnappyServer#onStart(Runnable)] listeners after the server is ready.
    ///
    /// @throws IllegalStateException if the server is already running
    public static synchronized void start() {
        instance().startServer();
    }

    /// Stops the HTTP server and resets the singleton instance.
    ///
    /// Stops the Undertow server, shuts down the XNIO worker, then fires all
    /// [onShutdown][SnappyServer#onShutdown(Runnable)] listeners.
    /// Safe to call even when the server is not running.
    public static synchronized void stop() {
        instance().stopServer();
        INSTANCE = null;
    }

    /// Sets the TCP `NO_DELAY` socket option.
    ///
    /// When `true`, disables Nagle's algorithm so small packets are sent immediately.
    ///
    /// @param tcpNoDelay `true` to disable Nagle's algorithm
    /// @throws IllegalStateException if the server is already running
    public static synchronized void tcpNoDeplay(boolean tcpNoDelay) {
        checkStarted();
        instance().optionBuilder.set(Options.TCP_NODELAY, tcpNoDelay);
    }

    /// Sets the TCP `KEEP_ALIVE` socket option.
    ///
    /// @param keepAlive `true` to enable TCP keep-alive probes
    /// @throws IllegalStateException if the server is already running
    public static synchronized void keepAlive(boolean keepAlive) {
        checkStarted();
        instance().optionBuilder.set(Options.KEEP_ALIVE, keepAlive);
    }

    /// Sets the read timeout in milliseconds for incoming connections.
    ///
    /// @param timeout read timeout in milliseconds; `0` means no timeout
    /// @throws IllegalStateException if the server is already running
    public static synchronized void readTimeout(int timeout) {
        checkStarted();
        instance().optionBuilder.set(Options.READ_TIMEOUT, timeout);
    }

    /// Controls whether the server socket address can be reused immediately after the server stops.
    ///
    /// @param reuseAddress `true` to enable `SO_REUSEADDR`
    /// @throws IllegalStateException if the server is already running
    public static synchronized void reuseAddress(boolean reuseAddress) {
        checkStarted();
        instance().optionBuilder.set(Options.REUSE_ADDRESSES, reuseAddress);
    }

    /// Sets the maximum allowed size in bytes for a single HTTP request entity body.
    ///
    /// Requests that exceed this limit receive a `413 Request Entity Too Large` response.
    /// Defaults to Undertow's [DEFAULT_MAX_ENTITY_SIZE][io.undertow.UndertowOptions#DEFAULT_MAX_ENTITY_SIZE].
    ///
    /// @param maxEntitySize maximum body size in bytes
    /// @throws IllegalStateException if the server is already running
    public static synchronized void maxEntitySize(long maxEntitySize) {
        checkStarted();
        instance().maxEntitySize = maxEntitySize;
    }

    /// Sets the maximum allowed size in bytes for a single multipart file upload.
    ///
    /// Individual parts that exceed this limit cause a `413 Request Entity Too Large` response.
    ///
    /// @param maxMultipartSize maximum individual part size in bytes
    /// @throws IllegalStateException if the server is already running
    public static synchronized void maxMultipartSize(long maxMultipartSize) {
        checkStarted();
        instance().maxMultipartSize = maxMultipartSize;
    }

    /// Overrides the admin server port (default `9100`).
    ///
    /// @param adminPort the port for the admin HTTP listener
    /// @throws IllegalStateException if the server is already running
    public static synchronized void adminPort(int adminPort) {
        checkStarted();
        instance().adminManager.setPort(adminPort);
    }

    /// Overrides the bind address for the admin HTTP listener (default `127.0.0.1`).
    ///
    /// @param address the bind address for the admin listener
    /// @throws IllegalStateException if the server is already running
    public static synchronized void adminAddress(String address) {
        checkStarted();
        instance().adminManager.setBindAddress(address);
    }

    /// Sets the HTTP port the server listens on (default `9000`).
    ///
    /// @param port the HTTP port
    /// @throws IllegalStateException if the server is already running
    public static synchronized void port(int port) {
        checkStarted();
        instance().port = port;
    }

    /// Applies a port offset to both the main HTTP port and the admin port.
    ///
    /// Resulting port = `9000 + offset`; resulting admin port = `9100 + offset`.
    /// Useful for running multiple instances on the same host.
    ///
    /// @param offset the value added to the default ports
    /// @throws IllegalStateException if the server is already running
    public static synchronized void portOffset(int offset) {
        checkStarted();
        instance().port = DEFAULT_PORT + offset;
        instance().adminManager.setPort(AdminManager.ADMIN_PORT + offset);
    }

    /// Sets the bind address for the main HTTP listener (default `0.0.0.0`).
    ///
    /// @param address the bind address, e.g. `"127.0.0.1"` to listen only on loopback
    /// @throws IllegalStateException if the server is already running
    public static synchronized void address(String address) {
        checkStarted();
        instance().bindAddress = address;
    }

    /// Sets the number of XNIO I/O threads.
    ///
    /// Defaults to the number of available processors.
    ///
    /// @param ioThreads number of I/O threads
    /// @throws IllegalStateException if the server is already running
    public static synchronized void ioThreads(int ioThreads) {
        checkStarted();
        instance().optionBuilder.set(Options.WORKER_IO_THREADS, ioThreads);
    }

    /// Sets the core and maximum worker thread counts for the XNIO thread pool.
    ///
    /// @param coreThreads minimum number of threads kept alive in the pool
    /// @param maxThreads  maximum number of threads allowed in the pool
    /// @throws IllegalStateException if the server is already running
    public static synchronized void workerThreads(int coreThreads, int maxThreads) {
        checkStarted();
        instance().optionBuilder.set(Options.WORKER_TASK_CORE_THREADS, coreThreads);
        instance().optionBuilder.set(Options.WORKER_TASK_MAX_THREADS, maxThreads);
    }

    /// Sets the core, maximum, and keep-alive time for the XNIO worker thread pool.
    ///
    /// @param coreThreads      minimum number of threads kept alive in the pool
    /// @param maxThreads       maximum number of threads allowed in the pool
    /// @param keepAliveMillis  time in milliseconds that idle threads above `coreThreads` are kept alive
    /// @throws IllegalStateException if the server is already running
    public static synchronized void workerThreads(int coreThreads, int maxThreads, int keepAliveMillis) {
        checkStarted();
        workerThreads(coreThreads, maxThreads);
        instance().optionBuilder.set(Options.WORKER_TASK_KEEPALIVE, keepAliveMillis);
    }

    /// Enables the HTTP request/response tracer which logs every exchange at `DEBUG` level.
    ///
    /// @throws IllegalStateException if the server is already running
    public static synchronized void enableTracer() {
        checkStarted();
        instance().httpTracer = true;
    }

    /// Returns the mutable XNIO [OptionMap.Builder][org.xnio.OptionMap.Builder] for advanced tuning.
    ///
    /// Options set on the returned builder take effect when the server starts.
    ///
    /// @return the XNIO option map builder
    /// @throws IllegalStateException if the server is already running
    public static synchronized OptionMap.Builder xnioOptions() {
        checkStarted();
        return instance().optionBuilder;
    }

    /// Registers a custom exception handler for the given exception type.
    ///
    /// When an endpoint throws an exception of type `T` (or a subtype), the provided
    /// `handler` is invoked instead of the default `500 Internal Server Error` response.
    ///
    /// ```java
    /// exception(IllegalArgumentException.class, (e, req) ->
    ///     Response.badRequest().body(e.getMessage()));
    /// ```
    ///
    /// @param <T>       the exception type
    /// @param exception the class of the exception to handle
    /// @param handler   the handler invoked with the error context and the original request
    /// @throws IllegalStateException if the server is already running
    public static synchronized <T extends Exception> void exception(Class<T> exception, ErrorHandler<T> handler) {
        checkStarted();
        instance().exceptionMapper.put(exception, handler);
    }

    /// Sets a base path that is prepended to all REST endpoint URLs.
    ///
    /// This has **no effect** on WebSocket, SSE, static-files, or multipart endpoints.
    ///
    /// ```java
    /// basePath("/api/v1");
    /// get("/users", req -> Response.ok()); // reachable at /api/v1/users
    /// ```
    ///
    /// @param basePath the path prefix, e.g. `"/api/v1"`
    /// @throws IllegalStateException if the server is already running
    public static synchronized void basePath(String basePath) {
        checkStarted();
        instance().basePath = HandlerUtil.parseUrl(basePath);
    }

    /// Groups a set of endpoint registrations under a common path prefix.
    ///
    /// ```java
    /// group("/api", () -> {
    ///     get("/users", req -> Response.ok());  // /api/users
    ///     post("/users", req -> Response.created()); // /api/users
    /// });
    /// ```
    ///
    /// @param groupPath the path prefix for all endpoints declared inside `group`
    /// @param group     a lambda that registers the grouped endpoints
    /// @throws IllegalStateException if the server is already running
    public static synchronized void group(String groupPath, Group group) {
        checkStarted();
        HandlerUtil.group(groupPath, group);
    }

    /// Registers a root request interceptor that runs **before all other handlers** for URLs matching `url`.
    ///
    /// The interceptor can inspect or modify the request, or abort it entirely via
    /// [RequestContext#abortWith(Response)][io.joshworks.snappy.http.RequestContext].
    /// Only exact URL matches and wildcard (`*`) patterns are supported.
    ///
    /// @param url      the URL pattern, e.g. `"/*"` or `"/api/admin/*"`
    /// @param consumer the code to execute when the pattern matches
    /// @throws IllegalStateException if the server is already running
    public static synchronized void beforeAll(String url, Consumer<RequestContext> consumer) {
        checkStarted();
        instance().interceptors.addRoot(new RequestInterceptor(HandlerUtil.parseUrl(url), consumer));
    }

    /// Registers a security interceptor that enforces `Authorization` header validation.
    ///
    /// Returns `401 Unauthorized` when the header's scheme does not equal `type` or
    /// when `expected.get()` does not match the credential value.
    ///
    /// @param url      the URL pattern to protect
    /// @param type     the expected `Authorization` scheme prefix (`"Basic"`, `"Bearer"`, etc.)
    /// @param expected a supplier returning the expected credential value
    /// @throws IllegalStateException if the server is already running
    public static synchronized void secured(String url, String type, Supplier<String> expected) {
        secured(url, (s, s2) -> s.equals(type) && s2.equals(expected.get()));
    }

    /// Registers a security interceptor using a custom `Authorization` header predicate.
    ///
    /// The `authenticator` receives the scheme and the credential value.
    /// Returns `401 Unauthorized` when it evaluates to `false`.
    ///
    /// @param url           the URL pattern to protect
    /// @param authenticator a predicate that receives `(scheme, credential)` and returns `true` to allow the request
    /// @throws IllegalStateException if the server is already running
    public static synchronized void secured(String url, BiPredicate<String, String> authenticator) {
        checkStarted();
        instance().interceptors.addRoot(Interceptors.secured(url, authenticator));
    }

    /// Registers an HTTP Basic Authentication interceptor.
    ///
    /// The `Authorization` header is decoded and the decoded `username` and `password` are
    /// passed to `userPswAuthenticator`. Returns `401 Unauthorized` when it evaluates to `false`.
    ///
    /// @param url                  the URL pattern to protect
    /// @param userPswAuthenticator a predicate that receives `(username, password)` and returns `true` to allow
    /// @throws IllegalStateException if the server is already running
    public static synchronized void basicAuthSecured(String url, BiPredicate<String, String> userPswAuthenticator) {
        checkStarted();
        instance().interceptors.addRoot(Interceptors.basicAuthentication(url, userPswAuthenticator));
    }

    /// Registers a request interceptor that runs immediately **before** the matched endpoint handler.
    ///
    /// Unlike [beforeAll][SnappyServer#beforeAll(String, Consumer)], this interceptor executes
    /// after the root interceptors and only for the matched route.
    ///
    /// @param url      the URL pattern the interceptor applies to
    /// @param consumer the code to execute when the pattern matches
    /// @throws IllegalStateException if the server is already running
    public static synchronized void before(String url, Consumer<RequestContext> consumer) {
        checkStarted();
        instance().interceptors.add(new RequestInterceptor(HandlerUtil.parseUrl(url), consumer));
    }

    /// Registers a response interceptor that runs **after** the endpoint handler.
    ///
    /// Modifications to the [Request][io.joshworks.snappy.http.Request] have no effect at this point.
    /// Use the [Response][io.joshworks.snappy.http.Response] parameter to add or modify response headers.
    ///
    /// @param url      the URL pattern the interceptor applies to
    /// @param consumer a consumer that receives `(request, response)` after the handler executes
    /// @throws IllegalStateException if the server is already running
    public static synchronized void after(String url, BiConsumer<RequestContext, Response> consumer) {
        checkStarted();
        instance().interceptors.add(new ResponseInterceptor(HandlerUtil.parseUrl(url), consumer));
    }

    /// Enables CORS by adding a root interceptor that sets the following headers on every response:
    ///
    /// | Header | Value |
    /// |--------|-------|
    /// | `Access-Control-Allow-Origin` | `*` |
    /// | `Access-Control-Allow-Credentials` | `true` |
    /// | `Access-Control-Allow-Methods` | `GET, POST, PUT, DELETE, OPTIONS, HEAD` |
    /// | `Access-Control-Allow-Headers` | `Origin, Accept, X-Requested-With, Content-Type, Authorization, ...` |
    ///
    /// `OPTIONS` preflight requests are answered immediately with `200 OK`.
    /// For custom CORS headers use [before(String, Consumer)][SnappyServer#before(String, Consumer)] instead.
    ///
    /// @throws IllegalStateException if the server is already running
    public static synchronized void cors() {
        checkStarted();
        instance().interceptors.addRoot(Interceptors.cors());
        instance().adminManager.interceptors.addRoot(Interceptors.cors());
    }

    /// Registers a `GET` endpoint at the given URL.
    ///
    /// @param url        the relative URL, e.g. `"/users/{id}"`
    /// @param handler    the request handler
    /// @param mediaTypes optional consumed/produced media type constraints
    public static void get(String url, Handler handler, MediaTypes... mediaTypes) {
        addResource(Methods.GET, url, handler, mediaTypes);
    }

    /// Registers a `POST` endpoint at the given URL.
    ///
    /// @param url        the relative URL
    /// @param endpoint   the request handler
    /// @param mediaTypes optional consumed/produced media type constraints
    public static void post(String url, Handler endpoint, MediaTypes... mediaTypes) {
        addResource(Methods.POST, url, endpoint, mediaTypes);
    }

    /// Registers a `PUT` endpoint at the given URL.
    ///
    /// @param url        the relative URL
    /// @param endpoint   the request handler
    /// @param mediaTypes optional consumed/produced media type constraints
    public static void put(String url, Handler endpoint, MediaTypes... mediaTypes) {
        addResource(Methods.PUT, url, endpoint, mediaTypes);
    }

    /// Registers a `DELETE` endpoint at the given URL.
    ///
    /// @param url        the relative URL
    /// @param endpoint   the request handler
    /// @param mediaTypes optional consumed/produced media type constraints
    public static void delete(String url, Handler endpoint, MediaTypes... mediaTypes) {
        addResource(Methods.DELETE, url, endpoint, mediaTypes);
    }

    /// Registers an `OPTIONS` endpoint at the given URL.
    ///
    /// @param url        the relative URL
    /// @param endpoint   the request handler
    /// @param mediaTypes optional consumed/produced media type constraints
    public static void options(String url, Handler endpoint, MediaTypes... mediaTypes) {
        addResource(Methods.OPTIONS, url, endpoint, mediaTypes);
    }

    /// Registers a `HEAD` endpoint at the given URL.
    ///
    /// @param url        the relative URL
    /// @param endpoint   the request handler
    /// @param mediaTypes optional consumed/produced media type constraints
    public static void head(String url, Handler endpoint, MediaTypes... mediaTypes) {
        addResource(Methods.HEAD, url, endpoint, mediaTypes);
    }

    /// Registers a `GET` endpoint at the root path (`/`).
    ///
    /// @param endpoint   the request handler
    /// @param mediaTypes optional consumed/produced media type constraints
    public static void get(Handler endpoint, MediaTypes... mediaTypes) {
        addResource(Methods.GET, HandlerUtil.BASE_PATH, endpoint, mediaTypes);
    }

    /// Registers a `POST` endpoint at the root path (`/`).
    ///
    /// @param endpoint   the request handler
    /// @param mediaTypes optional consumed/produced media type constraints
    public static void post(Handler endpoint, MediaTypes... mediaTypes) {
        addResource(Methods.POST, HandlerUtil.BASE_PATH, endpoint, mediaTypes);
    }

    /// Registers a `PUT` endpoint at the root path (`/`).
    ///
    /// @param endpoint   the request handler
    /// @param mediaTypes optional consumed/produced media type constraints
    public static void put(Handler endpoint, MediaTypes... mediaTypes) {
        addResource(Methods.PUT, HandlerUtil.BASE_PATH, endpoint, mediaTypes);
    }

    /// Registers a `DELETE` endpoint at the root path (`/`).
    ///
    /// @param endpoint   the request handler
    /// @param mediaTypes optional consumed/produced media type constraints
    public static void delete(Handler endpoint, MediaTypes... mediaTypes) {
        addResource(Methods.DELETE, HandlerUtil.BASE_PATH, endpoint, mediaTypes);
    }

    /// Registers an `OPTIONS` endpoint at the root path (`/`).
    ///
    /// @param endpoint   the request handler
    /// @param mediaTypes optional consumed/produced media type constraints
    public static void options(Handler endpoint, MediaTypes... mediaTypes) {
        addResource(Methods.OPTIONS, HandlerUtil.BASE_PATH, endpoint, mediaTypes);
    }

    /// Registers a `HEAD` endpoint at the root path (`/`).
    ///
    /// @param endpoint   the request handler
    /// @param mediaTypes optional consumed/produced media type constraints
    public static void head(Handler endpoint, MediaTypes... mediaTypes) {
        addResource(Methods.HEAD, HandlerUtil.BASE_PATH, endpoint, mediaTypes);
    }

    /// Registers a `PATCH` endpoint at the root path (`/`).
    ///
    /// @param endpoint   the request handler
    /// @param mediaTypes optional consumed/produced media type constraints
    public static void patch(Handler endpoint, MediaTypes... mediaTypes) {
        addResource(Methods.PATCH, HandlerUtil.BASE_PATH, endpoint, mediaTypes);
    }

    /// Registers a `PATCH` endpoint at the given URL.
    ///
    /// @param url        the relative URL
    /// @param endpoint   the request handler
    /// @param mediaTypes optional consumed/produced media type constraints
    public static void patch(String url, Handler endpoint, MediaTypes... mediaTypes) {
        addResource(Methods.PATCH, url, endpoint, mediaTypes);
    }

    /// Registers an endpoint for any HTTP method at the given URL.
    ///
    /// ```java
    /// add(Methods.TRACE, "/trace", req -> Response.ok());
    /// ```
    ///
    /// @param method     the HTTP method
    /// @param url        the relative URL
    /// @param endpoint   the request handler
    /// @param mediaTypes optional consumed/produced media type constraints
    public static void add(HttpString method, String url, Handler endpoint, MediaTypes... mediaTypes) {
        addResource(method, url, endpoint, mediaTypes);
    }

    private static synchronized void addResource(HttpString method, String url, Handler endpoint, MediaTypes... mediaTypes) {
        checkStarted();
        instance().endpoints.add(HandlerUtil.rest(method, url, instance().maxMultipartSize, endpoint, instance().exceptionMapper, mediaTypes));
    }

    /// Registers a WebSocket endpoint using a raw Undertow [AbstractReceiveListener].
    ///
    /// Path variables are supported, e.g. `"/chat/{room}"`.
    ///
    /// @param url      the relative URL for the WebSocket upgrade
    /// @param endpoint the low-level receive listener
    /// @throws IllegalStateException if the server is already running
    public static synchronized void websocket(String url, AbstractReceiveListener endpoint) {
        checkStarted();
        instance().endpoints.add(HandlerUtil.websocket(url, endpoint));
    }

    /// Registers a simplified WebSocket endpoint that handles text messages only.
    ///
    /// Path variables are supported.
    ///
    /// ```java
    /// websocket("/chat", (channel, message) ->
    ///     WebSockets.sendText("echo: " + message.getData(), channel, null));
    /// ```
    ///
    /// @param url       the relative URL for the WebSocket upgrade
    /// @param onMessage handler invoked for each full text message received
    /// @throws IllegalStateException if the server is already running
    public static synchronized void websocket(String url, BiConsumer<WebSocketChannel, BufferedTextMessage> onMessage) {
        checkStarted();
        instance().endpoints.add(HandlerUtil.websocket(url, new WebsocketEndpoint() {
            @Override
            public void onConnect(WebSocketHttpExchange exchange, WebSocketChannel channel) {

            }

            @Override
            protected void onFullTextMessage(WebSocketChannel channel, BufferedTextMessage message) throws IOException {
                onMessage.accept(channel, message);
            }
        }));
    }

    /// Registers a WebSocket endpoint using a [WebsocketEndpoint] handler.
    ///
    /// Path variables are supported.
    ///
    /// @param url      the relative URL for the WebSocket upgrade
    /// @param endpoint the endpoint handler
    /// @throws IllegalStateException if the server is already running
    public static synchronized void websocket(String url, WebsocketEndpoint endpoint) {
        checkStarted();
        instance().endpoints.add(HandlerUtil.websocket(url, endpoint));
    }

    /// Registers a Server-Sent Events endpoint at the root path (`/`) with no connect handler.
    ///
    /// Clients connect only to receive broadcasts via the returned [SseBroadcaster].
    ///
    /// @return the broadcaster used to push events to all connected clients
    /// @throws IllegalStateException if the server is already running
    public static synchronized SseBroadcaster sse() {
        return sse(HandlerUtil.BASE_PATH);
    }

    /// Registers a Server-Sent Events endpoint at the root path (`/`) with a maximum connection limit.
    ///
    /// @param maxConnections maximum number of concurrent SSE connections allowed
    /// @return the broadcaster used to push events to connected clients
    /// @throws IllegalStateException if the server is already running
    public static synchronized SseBroadcaster sse(int maxConnections) {
        return sse(HandlerUtil.BASE_PATH, sse -> {
        }, maxConnections);
    }

    /// Registers a Server-Sent Events endpoint at the given URL with no connect handler.
    ///
    /// @param url the relative URL
    /// @return the broadcaster used to push events to connected clients
    /// @throws IllegalStateException if the server is already running
    public static synchronized SseBroadcaster sse(String url) {
        return sse(url, sse -> {
        });
    }

    /// Registers a Server-Sent Events endpoint at the given URL with a maximum connection limit.
    ///
    /// @param url            the relative URL
    /// @param maxConnections maximum number of concurrent SSE connections allowed
    /// @return the broadcaster used to push events to connected clients
    /// @throws IllegalStateException if the server is already running
    public static synchronized SseBroadcaster sse(String url, int maxConnections) {
        return sse(url, sse -> {
        }, maxConnections);
    }

    /// Registers a Server-Sent Events endpoint at the root path (`/`) with a connect handler.
    ///
    /// The `handler` is called each time a client establishes an SSE connection.
    /// Events can be sent via [SseContext][io.joshworks.snappy.sse.SseContext] or the returned [SseBroadcaster].
    ///
    /// @param handler the handler invoked on each new SSE connection
    /// @return the broadcaster used to push events to all connected clients
    /// @throws IllegalStateException if the server is already running
    public static synchronized SseBroadcaster sse(SseHandler handler) {
        return sse(HandlerUtil.BASE_PATH, handler);
    }

    /// Registers a Server-Sent Events endpoint at the root path with a connect handler and connection limit.
    ///
    /// @param handler        the handler invoked on each new SSE connection
    /// @param maxConnections maximum number of concurrent SSE connections allowed
    /// @return the broadcaster used to push events to connected clients
    /// @throws IllegalStateException if the server is already running
    public static synchronized SseBroadcaster sse(SseHandler handler, int maxConnections) {
        return sse(HandlerUtil.BASE_PATH, handler, maxConnections);
    }

    /// Registers a Server-Sent Events endpoint at the given URL with a connect handler.
    ///
    /// @param url     the relative URL
    /// @param handler the handler invoked on each new SSE connection
    /// @return the broadcaster used to push events to connected clients
    /// @throws IllegalStateException if the server is already running
    public static synchronized SseBroadcaster sse(String url, SseHandler handler) {
        return sse(url, handler, SseBroadcaster.DEFAULT_MAX_CONNECTIONS);
    }

    /// Registers a Server-Sent Events endpoint at the given URL with a connect handler and connection limit.
    ///
    /// @param url            the relative URL
    /// @param handler        the handler invoked on each new SSE connection
    /// @param maxConnections maximum number of concurrent SSE connections allowed
    /// @return the broadcaster used to push events to connected clients
    /// @throws IllegalStateException if the server is already running
    public static synchronized SseBroadcaster sse(String url, SseHandler handler, int maxConnections) {
        checkStarted();
        SseBroadcaster broadcaster = new SseBroadcaster(maxConnections);
        instance().endpoints.add(HandlerUtil.sse(url, handler, broadcaster));
        return broadcaster;
    }

    /// Serves static files from `docPath` on the classpath under the given URL prefix.
    ///
    /// `index.html` is served automatically for directory requests.
    /// Path variables are **not** supported for static file endpoints.
    ///
    /// @param url     the URL prefix, e.g. `"/static"`
    /// @param docPath the classpath-relative folder, e.g. `"public"`
    /// @throws IllegalStateException if the server is already running
    public static synchronized void staticFiles(String url, String docPath) {
        checkStarted();
        instance().endpoints.add(HandlerUtil.staticFiles(url, docPath));
    }

    /// Serves static files from the default `static` classpath folder under the given URL prefix.
    ///
    /// @param url the URL prefix, e.g. `"/"`
    /// @throws IllegalStateException if the server is already running
    public static synchronized void staticFiles(String url) {
        checkStarted();
        instance().endpoints.add(HandlerUtil.staticFiles(url));
    }

    /// Registers a listener that is called once after the server has fully started.
    ///
    /// All endpoints, parsers, and interceptors are active when this listener fires.
    ///
    /// @param task the task to execute on startup
    /// @throws IllegalStateException if the server is already running
    public static synchronized void onStart(Runnable task) {
        checkStarted();
        instance().startListeners.add(task);
    }

    /// Registers a listener that is called once after the server has fully stopped.
    ///
    /// @param task the task to execute on shutdown
    /// @throws IllegalStateException if the server is already running
    public static synchronized void onShutdown(Runnable task) {
        checkStarted();
        instance().shutdownListeners.add(task);
    }

    private static void checkStarted() {
        if (instance().started) {
            throw new IllegalStateException("Server already started");
        }
    }

    private void startServer() {
        try {
            checkStarted();
            Info.printLogo();
            Info.printVersion();

            Runtime.getRuntime().addShutdownHook(new Thread(SnappyServer::stop));

            long start = System.currentTimeMillis();
            logger.info("Starting server...");

            AppProperties.load();
            overrideFromProps();

            Parsers.clear();
            Parsers.register(MediaType.APPLICATION_JSON_TYPE, new JsonParser());
            Parsers.register(MediaType.TEXT_PLAIN_TYPE, new PlainTextParser());

            worker = Xnio.getInstance().createWorker(optionBuilder.getMap());
            Undertow.Builder serverBuilder = Undertow.builder()
                    .setWorker(worker)
                    .setServerOption(UndertowOptions.MAX_ENTITY_SIZE, maxEntitySize);



            Info.httpConfig(bindAddress, port, adminManager.getBindAddress(), adminManager.getPort(), httpTracer);
            Info.serverConfig(optionBuilder);
            Info.endpoints("ENDPOINTS", endpoints, basePath);
            Info.endpoints("ADMIN ENDPOINTS", adminManager.getEndpoints(), BASE_PATH);

            HttpHandler rootHandler = HandlerManager.createRootHandler(
                    endpoints,
                    interceptors,
                    exceptionMapper,
                    basePath,
                    httpTracer);


            server = serverBuilder
                    .addHttpListener(port, bindAddress, rootHandler)
                    .addHttpListener(adminManager.getPort(), adminManager.getBindAddress(), adminManager.resolveHandlers(exceptionMapper))
                    .build();

            server.start();
            started = true;

            logger.info("Server started in {}ms", System.currentTimeMillis() - start);

            startListeners.forEach(Runnable::run);

        } catch (Exception e) {
            started = false;
            logger.error("Error while starting the server", e);
            stop();
            System.exit(1);
        }
    }

    private void overrideFromProps() {
        //http
        int offset = AppProperties.getInt(PropertyKey.HTTP_PORT_OFFSET).orElse(0);
        int port = this.port + offset;

        this.port = AppProperties.getInt(PropertyKey.HTTP_PORT).orElse(port);
        this.httpTracer = AppProperties.getBoolean(PropertyKey.HTTP_TRACER).orElse(this.httpTracer);
        this.bindAddress = AppProperties.get(PropertyKey.HTTP_BIND_ADDRESS).orElse(this.bindAddress);

        //xnio
        OptionMap map = optionBuilder.getMap();
        Integer ioThreads = map.get(Options.WORKER_IO_THREADS);
        Integer maxWorkers = map.get(Options.WORKER_TASK_MAX_THREADS);
        Integer coreWorkers = map.get(Options.WORKER_TASK_CORE_THREADS);
        Boolean tcpNoDelay = map.get(Options.TCP_NODELAY);

        ioThreads = AppProperties.getInt(PropertyKey.XNIO_IO_THREADS).orElse(ioThreads);
        maxWorkers = AppProperties.getInt(PropertyKey.XNIO_MAX_WORKER_THREAD).orElse(maxWorkers);
        coreWorkers = AppProperties.getInt(PropertyKey.XNIO_CORE_WORKER_THREAD).orElse(coreWorkers);
        tcpNoDelay = AppProperties.getBoolean(PropertyKey.TCP_NO_DELAY).orElse(tcpNoDelay);

        optionBuilder.set(Options.WORKER_IO_THREADS, ioThreads);
        optionBuilder.set(Options.WORKER_TASK_MAX_THREADS, maxWorkers);
        optionBuilder.set(Options.WORKER_TASK_CORE_THREADS, coreWorkers);
        optionBuilder.set(Options.TCP_NODELAY, tcpNoDelay);

        exportDefaultProperties();
    }

    //sets the properties that are may be useful outside the application
    private void exportDefaultProperties() {
        AppProperties.set(PropertyKey.HTTP_PORT, String.valueOf(this.port));
    }


    private void stopServer() {
        try {
            if (server != null && started) {
                logger.info("Stopping server...");

                server.stop();
                shutdownWorkers();

                INSTANCE = null;
                started = false;

                shutdownListeners.forEach(Runnable::run);

            }
        } catch (Exception e) {
            logger.error("Error while shutting down", e);
        }
    }


    private void shutdownWorkers() {
        try {
            worker.shutdownNow();
        } catch (Exception e) {
            logger.error("Error shutting down workers", e);
        }
    }

}