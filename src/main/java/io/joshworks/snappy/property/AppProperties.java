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

package io.joshworks.snappy.property;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;
import java.util.Properties;

import static io.joshworks.snappy.SnappyServer.LOGGER_NAME;

/**
 * Created by josh on 3/10/17.
 */
public class AppProperties {

    private static final Logger logger = LoggerFactory.getLogger(LOGGER_NAME);

    private static final String PROPERTIES_NAME = "snappy.properties";
    private static final Properties properties = new Properties();

    private AppProperties() {
    }

    public static void load() {
        InputStream is = Thread.currentThread().getContextClassLoader().getResourceAsStream(PROPERTIES_NAME);
        if (is != null) {
            logger.info("Loading {}", PROPERTIES_NAME);
            try {
                properties.load(is);
            } catch (IOException e) {
                throw new RuntimeException("Error while loading " + PROPERTIES_NAME, e);
            }
        } else {
            logger.info("{} not found", PROPERTIES_NAME);
        }
    }

    public static Properties getProperties() {
        Properties copy = new Properties();
        copy.putAll(properties);
        return copy;
    }

    private static String resolveProperty(Properties source, String key) {
        String fromEnv = fromEnv(key);
        String fromSystem = System.getProperty(key, fromEnv);

        return fromSystem == null ? source.getProperty(key) : fromSystem;
    }

    private static String fromEnv(String key) {
        if (key == null) {
            return null;
        }
        String envKey = key.replaceAll("\\.", "_").replaceAll("-", "_").toUpperCase();
        return System.getenv(envKey);
    }

    public static void set(String key, String value) {
        properties.setProperty(key, value);
    }

    public static Optional<Integer> getInt(String key) {
        Optional<String> value = get(key);
        return value.map(Integer::parseInt);
    }

    public static Optional<Boolean> getBoolean(String key) {
        return get(key).map(Boolean::parseBoolean);
    }

    public static Optional<Double> getDouble(String key) {
        return get(key).map(Double::parseDouble);
    }

    public static Optional<Long> getLong(String key) {
        return get(key).map(Long::parseLong);
    }

    public static Optional<String> get(String key) {
        if (key.isEmpty()) {
            logger.warn("No property key found for '{}'", key);
            return Optional.empty();
        }
        return Optional.ofNullable(resolveProperty(properties, key));
    }
}
