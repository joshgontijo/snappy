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

package io.joshworks.snappy.http.body;

import io.joshworks.snappy.http.MediaType;
import io.undertow.server.handlers.form.FormData;
import io.undertow.util.HeaderMap;
import io.undertow.util.HeaderValues;
import io.undertow.util.Headers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static io.joshworks.snappy.SnappyServer.LOGGER_NAME;

/**
 * Created by Josh Gontijo on 3/22/17.
 */
public final class Part {

    private static final Logger logger = LoggerFactory.getLogger(LOGGER_NAME);

    private String name;
    private PartFile partFile;
    private MediaType contentType;
    private boolean isFile;
    private String value;
    private boolean valid = false;

    Part() {
    }

    Part(FormData.FormValue formValue, String name) {
        valid = formValue != null;
        if (!valid) {
            return;
        }

        this.name = name;
        HeaderMap headers = formValue.getHeaders();
        contentType = getMediaType(headers);
        isFile = formValue.isFile();
        if (isFile) {
            Path path = formValue.getPath();
            String fileName = formValue.getFileName();
            long size = getSize(path);
            partFile = new PartFile(path, fileName, size, contentType);

        } else {
            value = formValue.getValue();
        }
    }

    public String value() {
        return value;
    }

    public PartFile file() {
        if (!isFile) {
            logger.warn("Part is not a file, null will be returned");
        }
        return partFile;
    }

    public MediaType type() {
        return contentType;
    }

    public boolean isPresent() {
        return valid;
    }

    public boolean isFile() {
        return isFile;
    }

    public String name() {
        return name;
    }

    private long getSize(Path path) {
        try {
            return Files.size(path);
        } catch (IOException e) {
            logger.error("Failed to get part size", e);
        }
        return -1;
    }

    private MediaType getMediaType(HeaderMap headers) {
        if(headers == null) {
            return null;
        }
        HeaderValues headerValues = headers.get(Headers.CONTENT_TYPE);
        return headerValues.isEmpty() ? MediaType.TEXT_PLAIN_TYPE : MediaType.valueOf(headerValues.getFirst());
    }

}
