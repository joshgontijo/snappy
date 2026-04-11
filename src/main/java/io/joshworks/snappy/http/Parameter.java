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

import java.util.Optional;

/**
 * Created by Josh Gontijo on 3/17/17.
 */
public class Parameter {

    private final String value;

    public Parameter(String value) {
        this.value = value;
    }

    public Optional<Integer> asInt() {
        return Optional.ofNullable(parseInt());
    }

    public Optional<Double> asDouble() {
        return Optional.ofNullable(parseDouble());
    }

    public Optional<Float> asFloat() {
        return Optional.ofNullable(parseFloat());
    }

    public Optional<Boolean> asBoolean() {
        return Optional.ofNullable(parseBoolean());
    }

    public Optional<Long> asLong() {
       return Optional.ofNullable(parseLong());
    }

    public Optional<String> asString() {
        return Optional.ofNullable(value);
    }

    private Integer parseInt() {
        if(!isPresent()) {
            return null;
        }
        try {
            return Integer.parseInt(value);
        }catch (NumberFormatException nex) {
            return null;
        }
    }

    private Double parseDouble() {
        if(!isPresent()) {
            return null;
        }
        try {
            return Double.parseDouble(value);
        }catch (NumberFormatException nex) {
            return null;
        }
    }

    private Float parseFloat() {
        if(!isPresent()) {
            return null;
        }
        try {
            return Float.parseFloat(value);
        }catch (NumberFormatException nex) {
            return null;
        }
    }

    private Boolean parseBoolean() {
        if(!isPresent()) {
            return null;
        }
        try {
            return Boolean.parseBoolean(value);
        }catch (NumberFormatException nex) {
            return null;
        }
    }

    private Long parseLong() {
        if(!isPresent()) {
            return null;
        }
        try {
            return Long.parseLong(value);
        }catch (NumberFormatException nex) {
            return null;
        }
    }

    private boolean isPresent() {
        return value != null && !value.isEmpty();
    }
}
