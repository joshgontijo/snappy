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

package io.joshworks.snappy.sse;

import io.undertow.server.handlers.sse.ServerSentEventConnection;

import java.util.Collections;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by josh on 3/9/17.
 */
public class SseBroadcaster {

    private final ConcurrentHashMap<ServerSentEventConnection, SseContext> connections = new ConcurrentHashMap<>();
    private final Map<String, Set<SseContext>> broadcastGroups = new ConcurrentHashMap<>();


    public void broadcast(String data) {
        for (SseContext context : connections.values()) {
            context.send(data);
        }
    }

    public void broadcast(EventData eventData) {
        for (SseContext context : connections.values()) {
            context.send(eventData);
        }
    }

    public void broadcast(String group, String data) {
        validateGroupName(group);
        Set<SseContext> contexts = broadcastGroups.get(group);
        if (contexts != null) {
            for (SseContext context : contexts) {
                context.send(data);
            }
        }
    }

    public void broadcast(String group, EventData eventData) {
        validateGroupName(group);
        Set<SseContext> contexts = broadcastGroups.get(group);
        if (contexts != null) {
            for (SseContext context : contexts) {
                context.send(eventData);
            }
        }
    }

    public int connected() {
        return connections.size();
    }

    public int connectedToGroup(String group) {
        Set<SseContext> connections = broadcastGroups.get(group);
        return connections == null ? 0 : connections.size();
    }

    void joinGroup(String group, SseContext context) {
        validateGroupName(group);
        broadcastGroups.compute(group, (k, v) -> {
            v = v == null ? Collections.newSetFromMap(new ConcurrentHashMap<>()) : v;
            v.add(context);
            return v;
        });
    }

    void leaveGroup(String group, SseContext context) {
        validateGroupName(group);
        Set<SseContext> contexts = broadcastGroups.get(group);
        if (contexts != null) {
            contexts.remove(context);
            if (contexts.isEmpty()) {
                broadcastGroups.remove(group);
            }
        }
    }

    void add(SseContext context) {
        connections.put(context.connection, context);
    }

    void remove(ServerSentEventConnection connection) {
        if (connection == null) {
            return;
        }
        SseContext context = connections.remove(connection);
        if (context == null) {
            return;
        }
        Iterator<Map.Entry<String, Set<SseContext>>> iterator = broadcastGroups.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<String, Set<SseContext>> entry = iterator.next();
            Set<SseContext> ctxs = entry.getValue();
            ctxs.remove(context);
            if (ctxs.isEmpty()) {
                iterator.remove();
            }
        }
    }

    private void validateGroupName(String group) {
        if (group == null || group.trim().isEmpty()) {
            throw new IllegalArgumentException("Group name must not me null or empty");
        }
    }

}
