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

import io.joshworks.snappy.ext.ExtensionProxy;
import io.joshworks.snappy.ext.ServerData;
import io.joshworks.snappy.ext.SnappyExtension;
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

/**
 * Created by josh on 3/5/17.
 */
public class SnappyServer {

    public static final String LOGGER_NAME = "snappy";
    private static final int DEFAULT_PORT = 9000;

    private static final Logger logger = LoggerFactory.getLogger(LOGGER_NAME);

    private final AdminManager adminManager = new AdminManager();
    private final ExtensionProxy extensions = new ExtensionProxy();
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

    public static synchronized void start() {
        instance().startServer();
    }

    public static synchronized void stop() {
        instance().stopServer();
        INSTANCE = null;
    }

    public static synchronized void tcpNoDeplay(boolean tcpNoDelay) {
        checkStarted();
        instance().optionBuilder.set(Options.TCP_NODELAY, tcpNoDelay);
    }

    public static synchronized void keepAlive(boolean tcpNoDelay) {
        checkStarted();
        instance().optionBuilder.set(Options.KEEP_ALIVE, tcpNoDelay);
    }

    public static synchronized void readTimeout(int timeout) {
        checkStarted();
        instance().optionBuilder.set(Options.READ_TIMEOUT, timeout);
    }

    public static synchronized void reuseAddress(boolean reuseAddress) {
        checkStarted();
        instance().optionBuilder.set(Options.REUSE_ADDRESSES, reuseAddress);
    }

    public static synchronized void maxEntitySize(long maxEntitySize) {
        checkStarted();
        instance().maxEntitySize = maxEntitySize;
    }

    public static synchronized void maxMultipartSize(long maxMultipartSize) {
        checkStarted();
        instance().maxMultipartSize = maxMultipartSize;
    }

    public static synchronized void adminPort(int adminPort) {
        checkStarted();
        instance().adminManager.setPort(adminPort);
    }

    public static synchronized void adminAddress(String address) {
        checkStarted();
        instance().adminManager.setBindAddress(address);
    }

    public static synchronized void port(int port) {
        checkStarted();
        instance().port = port;
    }

    public static synchronized void portOffset(int offset) {
        checkStarted();
        instance().port = DEFAULT_PORT + offset;
        instance().adminManager.setPort(AdminManager.ADMIN_PORT + offset);
    }

    public static synchronized void address(String address) {
        checkStarted();
        instance().bindAddress = address;
    }

    public static synchronized void ioThreads(int ioThreads) {
        checkStarted();
        instance().optionBuilder.set(Options.WORKER_IO_THREADS, ioThreads);
    }

    public static synchronized void workerThreads(int coreThreads, int maxThreads) {
        checkStarted();
        instance().optionBuilder.set(Options.WORKER_TASK_CORE_THREADS, coreThreads);
        instance().optionBuilder.set(Options.WORKER_TASK_MAX_THREADS, maxThreads);
    }

    public static synchronized void workerThreads(int coreThreads, int maxThreads, int keepAliveMillis) {
        checkStarted();
        workerThreads(coreThreads, maxThreads);
        instance().optionBuilder.set(Options.WORKER_TASK_KEEPALIVE, keepAliveMillis);
    }

    public static synchronized void enableTracer() {
        checkStarted();
        instance().httpTracer = true;
    }

    public static synchronized OptionMap.Builder xnioOptions() {
        checkStarted();
        return instance().optionBuilder;
    }

    /**
     * Register an error interceptor that captures original thrown from endpoints, allowing to change the http response.
     * The default response is mapped to any {@link Exception} and returns an {@link HttpException} body
     *
     * @param <T>       The exception type
     * @param exception The original type that will trigger the handler
     * @param handler   The handler that handles the original and send the appropriate response.
     */
    public static synchronized <T extends Exception> void exception(Class<T> exception, ErrorHandler<T> handler) {
        checkStarted();
        instance().exceptionMapper.put(exception, handler);
    }

    /**
     * Set the base path for REST endpoints only. This configuration has no effect on ws, sse, staticFiles and multipart endpoints
     *
     * @param basePath The base path for the REST endpoints
     */
    public static synchronized void basePath(String basePath) {
        checkStarted();
        instance().basePath = HandlerUtil.parseUrl(basePath);
    }

    /**
     * @param groupPath The path to be used by the endpoints declared under this group
     * @param group     The grouped endpoints, meant to be used as lambda function.
     */
    public static synchronized void group(String groupPath, Group group) {
        checkStarted();
        HandlerUtil.group(groupPath, group);
    }

    /**
     * Register a new Extension that will be executed on startup. Extensions allows the implementation to change the server behavior.
     *
     * @param extension The extension to be registered
     */
    public static synchronized void register(SnappyExtension extension) {
        checkStarted();
        instance().extensions.register(extension);
    }

    /**
     * Adds an interceptor that executes before any other handler.
     *
     * @param url      The URL pattern the interceptor will execute, only exact matches and wildcard (*) is allowed
     * @param consumer The code to be executed when a URL matches the provided pattern
     */
    public static synchronized void beforeAll(String url, Consumer<RequestContext> consumer) {
        checkStarted();
        instance().interceptors.addRoot(new RequestInterceptor(HandlerUtil.parseUrl(url), consumer));
    }

    /**
     * Adds an interceptor that returns 401 status code when the 'Authorization' header value does not match the type or the expected value.
     *
     * @param url      The URL pattern the interceptor will execute, only exact matches and wildcard (*) is allowed
     * @param type     The 'Authorization' value prefix to match against, 'Basic', 'Bearer', etc
     * @param expected The expected value supplier
     */
    public static synchronized void secured(String url, String type, Supplier<String> expected) {
        secured(url, (s, s2) -> s.equals(type) && s2.equals(expected.get()));
    }

    /**
     * Adds an interceptor that returns 401 status code when the 'Authorization' header value does not match the type or the expected value.
     *
     * @param url           The URL pattern the interceptor will execute, only exact matches and wildcard (*) is allowed
     * @param authenticator The authenticator that accepts the 'type' and the value 'value' of the Authorization header.
     *                      If it evaluates to false, then 401 is returned
     */
    public static synchronized void secured(String url, BiPredicate<String, String> authenticator) {
        checkStarted();
        instance().interceptors.addRoot(Interceptors.secured(url, authenticator));
    }

    /**
     * Adds an interceptor that returns 401 status code when the 'Authorization' header value does not match the type or the expected value.
     *
     * @param url                  The URL pattern the interceptor will execute, only exact matches and wildcard (*) is allowed
     * @param userPswAuthenticator The username and password authenticator, when return is false the request is aborted with 401
     */
    public static synchronized void basicAuthSecured(String url, BiPredicate<String, String> userPswAuthenticator) {
        checkStarted();
        instance().interceptors.addRoot(Interceptors.basicAuthentication(url, userPswAuthenticator));
    }

    /**
     * Adds an interceptor that executed right before the endpoint.
     *
     * @param url      The URL pattern the interceptor will execute, only exact matches and wildcard (*) is allowed
     * @param consumer The code to be executed when a URL matches the provided pattern
     */
    public static synchronized void before(String url, Consumer<RequestContext> consumer) {
        checkStarted();
        instance().interceptors.add(new RequestInterceptor(HandlerUtil.parseUrl(url), consumer));
    }

    /**
     * Adds an interceptor that executed after the endpoint, changes to {@link Request} has no effect.
     *
     * @param url      The URL pattern the interceptor will execute, only exact matches and wildcard (*) is allowed
     * @param consumer The code to be executed when a URL matches the provided pattern
     */
    public static synchronized void after(String url, BiConsumer<RequestContext, Response> consumer) {
        checkStarted();
        instance().interceptors.add(new ResponseInterceptor(HandlerUtil.parseUrl(url), consumer));
    }

    /**
     * Enable CORS by setting:
     * <ul>
     * <li>Access-Control-Allow-Origin: *</li>
     * <li>Access-Control-Allow-Credentials: true</li>
     * <li>Access-Control-Allow-Methods: GET, POST, PUT, DELETE, OPTIONS, HEAD</li>
     * <li>Access-Control-Allow-Headers: Origin, Accept, X-Requested-With, Content-Type, Authorization, Access-Control-Request-Method, Access-Control-Request-Headers</li>
     * </ul>
     * <p>
     * For custom headers, use {@link SnappyServer#before(String, Consumer)}
     */
    public static synchronized void cors() {
        checkStarted();
        instance().interceptors.addRoot(Interceptors.cors());
        instance().adminManager.interceptors.addRoot(Interceptors.cors());
    }

    /**
     * Define a REST endpoint mapped to HTTP GET
     *
     * @param url        The relative URL to be map this endpoint.
     * @param handler    The endpoint handler
     * @param mediaTypes (Optional) The accepted and returned types for this endpoint
     */
    public static void get(String url, Handler handler, MediaTypes... mediaTypes) {
        addResource(Methods.GET, url, handler, mediaTypes);
    }

    /**
     * Define a REST endpoint mapped to HTTP POST
     *
     * @param url        The relative URL to be map this endpoint.
     * @param endpoint   The endpoint handler
     * @param mediaTypes (Optional) The accepted and returned types for this endpoint
     */
    public static void post(String url, Handler endpoint, MediaTypes... mediaTypes) {
        addResource(Methods.POST, url, endpoint, mediaTypes);
    }

    /**
     * Define a REST endpoint mapped to HTTP PUT
     *
     * @param url        The relative URL to be map this endpoint.
     * @param endpoint   The endpoint handler
     * @param mediaTypes (Optional) The accepted and returned types for this endpoint
     */
    public static void put(String url, Handler endpoint, MediaTypes... mediaTypes) {
        addResource(Methods.PUT, url, endpoint, mediaTypes);
    }

    /**
     * Define a REST endpoint mapped to HTTP DELETE
     *
     * @param url        The relative URL to be map this endpoint.
     * @param endpoint   The endpoint handler
     * @param mediaTypes (Optional) The accepted and returned types for this endpoint
     */
    public static void delete(String url, Handler endpoint, MediaTypes... mediaTypes) {
        addResource(Methods.DELETE, url, endpoint, mediaTypes);
    }

    /**
     * Define a REST endpoint mapped to HTTP OPTIONS
     *
     * @param url        The relative URL to be map this endpoint.
     * @param endpoint   The endpoint handler
     * @param mediaTypes (Optional) The accepted and returned types for this endpoint
     */
    public static void options(String url, Handler endpoint, MediaTypes... mediaTypes) {
        addResource(Methods.OPTIONS, url, endpoint, mediaTypes);
    }

    /**
     * Define a REST endpoint mapped to HTTP HEAD
     *
     * @param url        The relative URL to be map this endpoint.
     * @param endpoint   The endpoint handler
     * @param mediaTypes (Optional) The accepted and returned types for this endpoint
     */
    public static void head(String url, Handler endpoint, MediaTypes... mediaTypes) {
        addResource(Methods.HEAD, url, endpoint, mediaTypes);
    }

    /**
     * Define a REST endpoint mapped to HTTP GET with default path "/" as url
     *
     * @param endpoint   The endpoint handler
     * @param mediaTypes (Optional) The accepted and returned types for this endpoint
     */
    public static void get(Handler endpoint, MediaTypes... mediaTypes) {
        addResource(Methods.GET, HandlerUtil.BASE_PATH, endpoint, mediaTypes);
    }

    /**
     * Define a REST endpoint mapped to HTTP POST with default path "/" as url
     *
     * @param endpoint   The endpoint handler
     * @param mediaTypes (Optional) The accepted and returned types for this endpoint
     */
    public static void post(Handler endpoint, MediaTypes... mediaTypes) {
        addResource(Methods.POST, HandlerUtil.BASE_PATH, endpoint, mediaTypes);
    }

    /**
     * Define a REST endpoint mapped to HTTP PUT with default path "/" as url
     *
     * @param endpoint   The endpoint handler
     * @param mediaTypes (Optional) The accepted and returned types for this endpoint
     */
    public static void put(Handler endpoint, MediaTypes... mediaTypes) {
        addResource(Methods.PUT, HandlerUtil.BASE_PATH, endpoint, mediaTypes);
    }

    /**
     * Define a REST endpoint mapped to HTTP DELETE with default path "/" as url
     *
     * @param endpoint   The endpoint handler
     * @param mediaTypes (Optional) The accepted and returned types for this endpoint
     */
    public static void delete(Handler endpoint, MediaTypes... mediaTypes) {
        addResource(Methods.DELETE, HandlerUtil.BASE_PATH, endpoint, mediaTypes);
    }

    /**
     * Define a REST endpoint mapped to HTTP OPTIONS with default path "/" as url
     *
     * @param endpoint   The endpoint handler
     * @param mediaTypes (Optional) The accepted and returned types for this endpoint
     */
    public static void options(Handler endpoint, MediaTypes... mediaTypes) {
        addResource(Methods.OPTIONS, HandlerUtil.BASE_PATH, endpoint, mediaTypes);
    }

    /**
     * Define a REST endpoint mapped to HTTP HEAD with default path "/" as url
     *
     * @param endpoint   The endpoint handler
     * @param mediaTypes (Optional) The accepted and returned types for this endpoint
     */
    public static void head(Handler endpoint, MediaTypes... mediaTypes) {
        addResource(Methods.HEAD, HandlerUtil.BASE_PATH, endpoint, mediaTypes);
    }

    /**
     * Define a REST endpoint mapped to HTTP PATCH with default path "/" as url
     *
     * @param endpoint   The endpoint handler
     * @param mediaTypes (Optional) The accepted and returned types for this endpoint
     */
    public static void patch(Handler endpoint, MediaTypes... mediaTypes) {
        addResource(Methods.PATCH, HandlerUtil.BASE_PATH, endpoint, mediaTypes);
    }

    /**
     * Define a REST endpoint mapped to HTTP HEAD
     *
     * @param url        The relative URL to be map this endpoint.
     * @param endpoint   The endpoint handler
     * @param mediaTypes (Optional) The accepted and returned types for this endpoint
     */
    public static void patch(String url, Handler endpoint, MediaTypes... mediaTypes) {
        addResource(Methods.PATCH, url, endpoint, mediaTypes);
    }

    /**
     * Define a REST endpoint mapped to the specified HTTP method with default path "/" as url
     *
     * @param method     The HTTP method
     * @param url        The relative URL of the endpoint.
     * @param endpoint   The endpoint handler
     * @param mediaTypes (Optional) The accepted and returned types for this endpoint
     */
    public static void add(HttpString method, String url, Handler endpoint, MediaTypes... mediaTypes) {
        addResource(method, url, endpoint, mediaTypes);
    }

    private static synchronized void addResource(HttpString method, String url, Handler endpoint, MediaTypes... mediaTypes) {
        checkStarted();
        instance().endpoints.add(HandlerUtil.rest(method, url, instance().maxMultipartSize, endpoint, instance().exceptionMapper, mediaTypes));
    }

    /**
     * Define a Websocket endpoint. Supports path variables
     *
     * @param url      The relative URL to be map this endpoint.
     * @param endpoint The endpoint handler
     */
    public static synchronized void websocket(String url, AbstractReceiveListener endpoint) {
        checkStarted();
        instance().endpoints.add(HandlerUtil.websocket(url, endpoint));
    }

    /**
     * A simplified Websocket endpoint. Supports path variables
     *
     * @param url       The relative URL to be map this endpoint.
     * @param onMessage The handler for when a new message is received
     */
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

    /**
     * Define a Websocket endpoint. Supports path variables
     *
     * @param url      The relative URL to be map this endpoint.
     * @param endpoint The endpoint handler
     */
    public static synchronized void websocket(String url, WebsocketEndpoint endpoint) {
        checkStarted();
        instance().endpoints.add(HandlerUtil.websocket(url, endpoint));
    }

    /**
     * Define a Server sent events endpoint without a handler at base path of the root or current endpoint group.
     * Clients connected to this server will only be able to receive message via {@link SseBroadcaster#broadcast(String)}
     */
    public static synchronized SseBroadcaster sse() {
        return sse(HandlerUtil.BASE_PATH);
    }

    /**
     * Define a Server sent events endpoint without a handler at base path.
     *
     * @param maxConnections Maximum number of concurrent SSE connections allowed
     */
    public static synchronized SseBroadcaster sse(int maxConnections) {
        return sse(HandlerUtil.BASE_PATH, sse -> {
        }, maxConnections);
    }

    /**
     * Define a Server sent events endpoint without a handler.
     * Clients connected to this server will only be able to receive message via {@link SseBroadcaster#broadcast(String)}
     *
     * @param url The url this endpoint will be available
     */
    public static synchronized SseBroadcaster sse(String url) {
        return sse(url, sse -> {
        });
    }

    /**
     * Define a Server sent events endpoint without a handler.
     *
     * @param url            The url this endpoint will be available
     * @param maxConnections Maximum number of concurrent SSE connections allowed
     */
    public static synchronized SseBroadcaster sse(String url, int maxConnections) {
        return sse(url, sse -> {
        }, maxConnections);
    }

    /**
     * Define a Server sent events endpoint with a specified handler. Supports path variables
     * Data can be broadcast to this endpoint by using {@link io.joshworks.snappy.sse.SseContext} or {@link io.joshworks.snappy.sse.SseBroadcaster}
     * The handler is the handler is called when the connection is established
     *
     * @param handler Endpoint handler
     */
    public static synchronized SseBroadcaster sse(SseHandler handler) {
        return sse(HandlerUtil.BASE_PATH, handler);
    }

    /**
     * Define a Server sent events endpoint with a specified handler. Supports path variables
     *
     * @param handler        Endpoint handler
     * @param maxConnections Maximum number of concurrent SSE connections allowed
     */
    public static synchronized SseBroadcaster sse(SseHandler handler, int maxConnections) {
        return sse(HandlerUtil.BASE_PATH, handler, maxConnections);
    }

    /**
     * Define a Server sent events endpoint with a specified handler. Supports path variables
     * Data can be broadcast to this endpoint by using {@link io.joshworks.snappy.sse.SseContext} or {@link io.joshworks.snappy.sse.SseBroadcaster}
     * The handler is the handler is called when the connection is established
     *
     * @param url     The relative URL to be map this endpoint.
     * @param handler Endpoint handler
     */
    public static synchronized SseBroadcaster sse(String url, SseHandler handler) {
        return sse(url, handler, SseBroadcaster.DEFAULT_MAX_CONNECTIONS);
    }

    /**
     * Define a Server sent events endpoint with a specified handler. Supports path variables
     * Data can be broadcast to this endpoint by using {@link io.joshworks.snappy.sse.SseContext} or {@link io.joshworks.snappy.sse.SseBroadcaster}
     * The handler is the handler is called when the connection is established
     *
     * @param url            The relative URL to be map this endpoint.
     * @param handler        Endpoint handler
     * @param maxConnections Maximum number of concurrent SSE connections allowed
     */
    public static synchronized SseBroadcaster sse(String url, SseHandler handler, int maxConnections) {
        checkStarted();
        SseBroadcaster broadcaster = new SseBroadcaster(maxConnections);
        instance().endpoints.add(HandlerUtil.sse(url, handler, broadcaster));
        return broadcaster;
    }

    /**
     * Serve static files from a given url. Path variables are not supported.
     *
     * @param url     The relative URL to be map this endpoint.
     * @param docPath The relative path to the classpath.
     */
    public static synchronized void staticFiles(String url, String docPath) {
        checkStarted();
        instance().endpoints.add(HandlerUtil.staticFiles(url, docPath));
    }

    /**
     * Serve static files from a given url from the default "/static" folder in the classpath. Path variables are not supported.
     *
     * @param url The relative URL to be map this endpoint.
     */
    public static synchronized void staticFiles(String url) {
        checkStarted();
        instance().endpoints.add(HandlerUtil.staticFiles(url));
    }

    /**
     * Register a server startup listener that executes after all resources and extensions are loaded.
     *
     * @param task the {@link Runnable} to be executed.
     */
    public static synchronized void onStart(Runnable task) {
        checkStarted();
        instance().startListeners.add(task);
    }

    /**
     * Register a server shutdown listener that executes after all resources have been shut down.
     *
     * @param task the {@link Runnable} to be executed.
     */
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


            //Extension are capable of adding / removing mapped endpoints,
            // therefore they must execute before the handler resolution
            bootstrapExtensions();

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
        int adminPort = this.adminManager.getPort() + offset;

        this.port = AppProperties.getInt(PropertyKey.HTTP_PORT).orElse(port);
        this.httpTracer = AppProperties.getBoolean(PropertyKey.HTTP_TRACER).orElse(this.httpTracer);
        this.bindAddress = AppProperties.get(PropertyKey.HTTP_BIND_ADDRESS).orElse(this.bindAddress);

        //admin http
        this.adminManager.setPort(AppProperties.getInt(PropertyKey.ADMIN_HTTP_PORT).orElse(adminPort));
        this.adminManager.setBindAddress(AppProperties.get(PropertyKey.ADMIN_HTTP_BIND_ADDRESS).orElse(this.adminManager.getBindAddress()));

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
        AppProperties.set(PropertyKey.ADMIN_HTTP_PORT, String.valueOf(this.adminManager.getPort()));
    }

    private void bootstrapExtensions() {
        extensions.onStart(
                new ServerData(port,
                        bindAddress,
                        httpTracer,
                        maxMultipartSize,
                        interceptors,
                        exceptionMapper,
                        basePath,
                        adminManager,
                        endpoints));
    }

    private void stopServer() {
        try {
            if (server != null && started) {
                logger.info("Stopping server...");

                shutdownExtensions();
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

    private void shutdownExtensions() {
        try {
            extensions.onShutdown();
        } catch (Exception e) {
            logger.error("Error shutting down extensions", e);
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