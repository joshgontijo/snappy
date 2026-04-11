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

import io.joshworks.snappy.handler.HandlerUtil;
import io.joshworks.snappy.http.MediaType;
import io.undertow.util.HeaderValues;

import java.util.HashSet;
import java.util.List;

/**
 * Created by josh on 3/12/17.
 */
public class MediaTypes extends HashSet<MediaType> {

    public static final MediaTypes DEFAULT_CONSUMES = new MediaTypes(Context.CONSUMES, MediaType.APPLICATION_JSON, MediaType.TEXT_PLAIN);
    public static final MediaTypes DEFAULT_PRODUCES = new MediaTypes(Context.CONSUMES, MediaType.APPLICATION_JSON, MediaType.TEXT_PLAIN);

    private final Context context;

    private MediaType defaultType;

    private MediaTypes(Context context) {
        this.context = context;
    }

    private MediaTypes(Context context, String... types) {
        this(context);
        for (String mt : types) {
            MediaType mediaType = MediaType.valueOf(mt);
            if (defaultType == null) {
                defaultType = mediaType;
            }
            add(mediaType);
        }
        defaultType = defaultType == null ? MediaType.TEXT_PLAIN_TYPE : defaultType;
    }

    public static MediaTypes consumes(String... types) {
        return new MediaTypes(Context.CONSUMES, types);
    }

    public static MediaTypes produces(String... types) {
        return new MediaTypes(Context.PRODUCES, types);
    }

    /**
     * @param headerValues The list of headers to be matched against
     * @return The first match of the provided content Mime context
     */
    public MediaType match(HeaderValues headerValues) {
        if (headerValues != null) {
            List<String> values = HandlerUtil.splitHeaderValues(headerValues);
            for (String headerVal : values) {
                try {
                    MediaType mediaType = MediaType.valueOf(headerVal);
                    boolean match = this.stream().anyMatch(t -> t.isCompatible(mediaType));
                    if (match) {
                        if (mediaType.isWildcardType() && mediaType.isWildcardSubtype()) {
                            return this.defaultType;
                        }
                        return mediaType;
                    }
                } catch (Exception e) {
                    //ignore mime
                }
            }
        }
        return null;
    }

    public Context getContext() {
        return context;
    }

    public MediaType getDefaultType() {
        return defaultType;
    }

    public enum Context {
        PRODUCES, CONSUMES

    }


}
