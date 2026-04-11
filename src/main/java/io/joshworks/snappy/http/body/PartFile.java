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

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

/**
 * Created by Josh Gontijo on 3/22/17.
 */
public class PartFile {

    private final Path path;
    private final String fileName;
    private final long size;
    private MediaType contentType;

    public PartFile(Path path, String fileName, long size, MediaType contentType) {
        this.path = path;
        this.fileName = fileName;
        this.size = size;
        this.contentType = contentType;
    }

    public String name() {
        return fileName;
    }

    public long size() {
        return size;
    }

    public Path path() {
        return path;
    }

    public MediaType type() {
        return contentType;
    }

    public InputStream stream() {
        try {
            return Files.newInputStream(path, StandardOpenOption.READ);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
