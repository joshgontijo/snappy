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

/**
 * Created by Josh Gontijo on 3/30/17.
 */
public class HttpException extends RuntimeException {

    public final int status;

    public HttpException(int status, String message) {
        super(message);
        this.status = status;
    }

    public static HttpException internalServerError() {
        return new HttpException(500, "Internal server error");
    }

    public static HttpException notFound(String message) {
        return new HttpException(404, message);
    }

    public static HttpException badRequest(String message) {
        return new HttpException(400, message);
    }

    public static HttpException unauthorized(String message) {
        return new HttpException(401, message);
    }

    public static HttpException forbidden(String message) {
        return new HttpException(403, message);
    }

    public static HttpException conflict(String message) {
        return new HttpException(409, message);
    }

    public static HttpException teapot() {
        return new HttpException(418, "I'm a teapot");
    }
}
