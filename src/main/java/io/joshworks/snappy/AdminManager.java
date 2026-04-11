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
import io.joshworks.snappy.http.ExceptionMapper;
import io.joshworks.snappy.http.Interceptors;
import io.joshworks.snappy.http.RequestInterceptor;
import io.undertow.server.HttpHandler;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Josh Gontijo on 3/19/17.
 */
public class AdminManager {

    public static final int ADMIN_PORT = 9100;

    private int port = ADMIN_PORT;
    private String bindAddress = "127.0.0.1";
    private final List<MappedEndpoint> endpoints = new ArrayList<>();
    final Interceptors interceptors = new Interceptors();

    private MappedEndpoint adminPage;

    AdminManager() {

    }

    HttpHandler resolveHandlers() {
        if (adminPage != null) {
            endpoints.add(adminPage);
        }
        return HandlerManager.createRootHandler(endpoints, interceptors, new ExceptionMapper(), HandlerUtil.BASE_PATH, false);
    }

    public void addEndpoint(MappedEndpoint endpoint) {
        endpoints.add(endpoint);
    }

    List<MappedEndpoint> getEndpoints() {
        return new ArrayList<>(endpoints);
    }

    //TODO implement interceptors to admin page
    public void setAdminPage(String url, String docPath, List<RequestInterceptor> interceptors) {
        adminPage = HandlerUtil.staticFiles(url, docPath);
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public String getBindAddress() {
        return bindAddress;
    }

    public void setBindAddress(String bindAddress) {
        this.bindAddress = bindAddress;
    }

}
