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

package io.joshworks.snappy.ext;

import io.joshworks.snappy.AdminManager;
import io.joshworks.snappy.handler.MappedEndpoint;
import io.joshworks.snappy.http.ExceptionMapper;
import io.joshworks.snappy.http.Interceptors;

import java.util.List;

/**
 * Created by Josh Gontijo on 3/25/17.
 */
public class ServerData {

    public final int port;
    public final String bindAddress;
    public final boolean httpTracer;
    public final Interceptors interceptors;
    public final ExceptionMapper exceptionMapper;
    public final String basePath;
    public final List<MappedEndpoint> mappedEndpoints;
    public final AdminManager adminManager;
    public final long maxMultipartSize;

    public ServerData(int port,
                      String bindAddress,
                      boolean httpTracer,
                      long maxMultipartSize,
                      Interceptors interceptors,
                      ExceptionMapper exceptionMapper,
                      String basePath,
                      AdminManager adminManager,
                      List<MappedEndpoint> mappedEndpoints) {

        this.port = port;
        this.bindAddress = bindAddress;
        this.httpTracer = httpTracer;
        this.maxMultipartSize = maxMultipartSize;
        this.interceptors = interceptors;
        this.exceptionMapper = exceptionMapper;
        this.basePath = basePath;
        this.mappedEndpoints = mappedEndpoints;
        this.adminManager = adminManager;
    }
}
