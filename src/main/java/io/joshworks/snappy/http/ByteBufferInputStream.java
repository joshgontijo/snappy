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

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;

/**
 * Created by Josh Gontijo on 4/3/17.
 */
public class ByteBufferInputStream extends InputStream {
    private ByteBuffer byteBuffer;

    public ByteBufferInputStream() {
    }

    public ByteBufferInputStream(int bufferSize) {
        this(ByteBuffer.allocate(bufferSize));
        byteBuffer.flip();
    }

    public ByteBufferInputStream(ByteBuffer byteBuffer) {
        this.byteBuffer = byteBuffer;
    }

    public ByteBuffer getByteBuffer() {
        return byteBuffer;
    }

    public void setByteBuffer(ByteBuffer byteBuffer) {
        this.byteBuffer = byteBuffer;
    }

    public int read() throws IOException {
        if (!byteBuffer.hasRemaining()) return -1;
        return byteBuffer.get() & 0xFF;
    }

    public int read(byte[] bytes, int offset, int length) throws IOException {
        if (length == 0) return 0;
        int count = Math.min(byteBuffer.remaining(), length);
        if (count == 0) return -1;
        byteBuffer.get(bytes, offset, count);
        return count;
    }

    public int available() throws IOException {
        return byteBuffer.remaining();
    }
}
