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

package io.joshworks.snappy.it;

import io.joshworks.restclient.http.Unirest;
import io.joshworks.snappy.it.util.DummyExtension;
import org.junit.AfterClass;
import org.junit.Test;

import static io.joshworks.snappy.SnappyServer.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Created by Josh Gontijo on 3/26/17.
 */
public class ExtensionTest {

    @AfterClass
    public static void shutdown() {
        stop();
        Unirest.close();
    }

    @Test
    public void onStart() throws Exception {
        try {
            DummyExtension dummyExtension = new DummyExtension();

            register(dummyExtension);
            start();

            assertTrue(dummyExtension.startedCalled);

        } finally {
            stop();
        }
    }

    @Test
    public void onStop() throws Exception {
        try {
            DummyExtension dummyExtension = new DummyExtension();

            register(dummyExtension);
            start();
            stop();

            assertTrue(dummyExtension.stopCalled);
        } finally {
            stop();
        }
    }

    @Test
    public void properties() throws Exception {
        try {
            String valueFromPropFile = "Hello";
            DummyExtension dummyExtension = new DummyExtension();

            register(dummyExtension);
            start();

            assertEquals(valueFromPropFile, dummyExtension.valueFromProperties);
        } finally {
            stop();
        }
    }


}
