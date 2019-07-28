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

package io.joshworks.snappy.http;

import java.util.function.BiConsumer;

/**
 * Created by Josh Gontijo on 3/18/17.
 */
public class ResponseInterceptor extends Interceptor {
    private final BiConsumer<RequestContext, Response> handler;

    public ResponseInterceptor(String url, BiConsumer<RequestContext, Response> handler) {
        super(url);
        this.handler = handler;
    }

    public void intercept(RequestContext reqContext, Response response) {
        this.handler.accept(reqContext, response);
    }

}
