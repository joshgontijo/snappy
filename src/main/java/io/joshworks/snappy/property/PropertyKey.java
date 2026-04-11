/*
 * Copyright 2017 Josue Gontijo
 *
 * Licensed under the Apache License, Version 2.0 (the "License"));
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

package io.joshworks.snappy.property;

/**
 * Created by josh on 3/10/17.
 */
public final class PropertyKey {

    //http
    public static final String HTTP_PORT = "http.port";
    public static final String HTTP_PORT_OFFSET = "http.portOffset";
    public static final String HTTP_BIND_ADDRESS = "http.bind";
    public static final String HTTP_TRACER = "http.tracer";
    public static final String TCP_NO_DELAY = "tcp.nodelay";

    //admin
    public static final String ADMIN_HTTP_PORT = "admin.http.port";
    public static final String ADMIN_HTTP_BIND_ADDRESS = "admin.http.bind";

    //xnio
    public static final String XNIO_IO_THREADS = "xnio.io.threads";
    public static final String XNIO_MAX_WORKER_THREAD = "xnio.worker.maxThread";
    public static final String XNIO_CORE_WORKER_THREAD = "xnio.worker.coreThread";

}
