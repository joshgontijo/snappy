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

import io.joshworks.snappy.handler.MappedEndpoint;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xnio.OptionMap;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.Comparator;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.ThreadPoolExecutor;

import static io.joshworks.snappy.SnappyServer.LOGGER_NAME;

/**
 * Created by josh on 3/11/17.
 */
public class Info {

    private static final String LOGO = "logo.txt";
    private static final String VERSION = "version.properties";
    private static final String VERSION_KEY = "version";
    private static final String UNKNOWN = "UNKNOWN";

    private static final Logger logger = LoggerFactory.getLogger(LOGGER_NAME);

    public static void printLogo() {
        try (BufferedReader br = new BufferedReader(new InputStreamReader(Thread.currentThread().getContextClassLoader().getResourceAsStream(LOGO)))) {
            br.lines().forEach(System.err::println);
        } catch (Exception e) {
            //do nothing
        }
    }

    public static void printVersion() {
        System.err.println(String.format("Version: %s%n%n", version()));
    }

    public static String version() {
        try {
            Properties properties = new Properties();
            properties.load(Thread.currentThread().getContextClassLoader().getResourceAsStream(VERSION));
            return String.valueOf(properties.getOrDefault(VERSION_KEY, UNKNOWN));
        } catch (Exception ex) {
            return UNKNOWN;
        }
    }

    public static void httpConfig(String bindAddress,
                                  int port,
                                  String adminBindAddress,
                                  int adminPort,
                                  boolean httpTracer) {

        System.err.println("---------------- HTTP CONFIG ----------------");
        System.err.println(String.format("Bind address: %s", bindAddress));
        System.err.println(String.format("Port: %d", port));
        System.err.println(String.format("Admin Bind address: %s", adminBindAddress));
        System.err.println(String.format("Admin Port: %d", adminPort));
        System.err.println(String.format("Http tracer : %b", httpTracer));

        System.err.println();
    }

    public static void serverConfig(OptionMap.Builder options) {
        System.err.println("--------------- SERVER CONFIG ---------------");
        options.getMap().forEach(option -> {
            System.err.println(String.format("%s: %s", option.getName().replaceAll("_", " ").toLowerCase(), options.getMap().get(option)));
        });
        System.err.println();
    }

    public static void endpoints(String title, List<MappedEndpoint> endpoints, String basePath) {
        System.err.println("----------------- " + title + " -----------------");
        logEndpoints(endpoints, basePath);
        System.err.println();
    }

    private static void logEndpoints(List<MappedEndpoint> endpoints, String basePath) {
        if (endpoints.isEmpty()) {
            System.err.println("NO ENDPOINTS FOUND");
            return;
        }
        endpoints.sort(Comparator.comparing(me -> me.url));
        endpoints.stream().sorted(Comparator.comparing(me -> me.url)).forEach(e -> System.err.println(e.toString(basePath)));
    }

    private static void logExecutors(String name, ThreadPoolExecutor executor) {
        System.err.println(String.format("Pool name: %s", name));
        System.err.println(String.format("   Core pool size: %d", executor.getCorePoolSize()));
        System.err.println(String.format("   Maximum pool size: %d", executor.getMaximumPoolSize()));
        System.err.println(String.format("   Queue size: %d", executor.getQueue().remainingCapacity()));
        System.err.println(String.format("   Rejection policy: %s", executor.getRejectedExecutionHandler().getClass().getSimpleName()));
    }

}
