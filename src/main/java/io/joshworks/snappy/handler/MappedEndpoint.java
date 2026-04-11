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

package io.joshworks.snappy.handler;

import io.joshworks.snappy.parser.MediaTypes;
import io.undertow.server.HttpHandler;

/**
 * Created by josh on 3/7/17.
 */
public class MappedEndpoint {

    public final String method; //HTTP methods or WS etc
    public final String url;
    public final Type type;
    public final HttpHandler handler;
    public final MediaTypes[] mediaTypes;

    public MappedEndpoint(String method, String url, Type type, HttpHandler handler, MediaTypes[] mediaTypes) {
        this.method = method;
        this.url = url;
        this.type = type;
        this.handler = handler;
        this.mediaTypes = mediaTypes;
    }

    public MappedEndpoint(String method, String url, Type type, HttpHandler handler) {
        this(method, url, type, handler, new MediaTypes[0]);
    }

    public String toString(String basePath) {
        StringBuilder sb = new StringBuilder();
        String m = Type.REST.equals(type) ? method : type.name();
        for (int i = 0; i < 10 - m.length(); i++) {
            sb.append(" ");
        }
        String formatted = HandlerUtil.BASE_PATH.equals(basePath) ? url : basePath + url;
        return String.format("%s%s", m, sb.toString() + formatted);
    }

    public enum Type {
        REST, WS, STATIC, SSE
    }
}
