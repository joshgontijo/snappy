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

package io.joshworks.snappy.parser;

import com.google.gson.Gson;
import org.json.JSONObject;

import java.lang.reflect.Type;

/**
 * Created by josh on 3/6/17.
 */
public class JsonParser implements Parser {

    private final Gson gson = new Gson();
    private final com.google.gson.JsonParser parser = new com.google.gson.JsonParser();

    @Override
    public <T> T readValue(String value, Class<T> valueType) {
        return gson.fromJson(value, valueType);
    }

    @Override
    public <T> T readValue(String value, Type valueType) {
        return gson.fromJson(value, valueType);
    }

    @Override
    public String writeValue(Object input) {
        if (input instanceof JSONObject) {
            input = input.toString();
        }

        if (input instanceof String) {
            input = parser.parse((String) input);
        }

        return gson.toJson(input);
    }
}
