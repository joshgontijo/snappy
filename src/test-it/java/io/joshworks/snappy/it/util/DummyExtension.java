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

package io.joshworks.snappy.it.util;

import io.joshworks.snappy.ext.ServerData;
import io.joshworks.snappy.ext.SnappyExtension;
import io.joshworks.snappy.property.AppProperties;

/**
 * Created by Josh Gontijo on 3/26/17.
 */
public class DummyExtension implements SnappyExtension {

    public boolean startedCalled = false;
    public boolean stopCalled = false;
    public String valueFromProperties;

    @Override
    public void onStart(ServerData config) {
        startedCalled = true;
        valueFromProperties = AppProperties.get("dummy.value").get();
    }

    @Override
    public void onShutdown() {
        stopCalled = true;
    }

    @Override
    public String name() {
        return "DUMMY";
    }
}
