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
import java.io.OutputStream;
import java.nio.ByteBuffer;

/**
 * Created by Josh Gontijo on 4/3/17.
 */
public class ByteBufferOutputStream extends OutputStream {

    private ByteBuffer byteBuffer;

    public ByteBufferOutputStream() {
    }

    public ByteBufferOutputStream(int bufferSize) {
        this(ByteBuffer.allocate(bufferSize));
    }

    public ByteBufferOutputStream(ByteBuffer byteBuffer) {
        this.byteBuffer = byteBuffer;
    }

    public ByteBuffer getByteBuffer() {
        return byteBuffer;
    }

    public void setByteBuffer(ByteBuffer byteBuffer) {
        this.byteBuffer = byteBuffer;
    }

    public void write(int b) throws IOException {
        if (!byteBuffer.hasRemaining()) flush();
        byteBuffer.put((byte) b);
    }

    public void write(byte[] bytes, int offset, int length) throws IOException {
        if (byteBuffer.remaining() < length) flush();
        byteBuffer.put(bytes, offset, length);
    }
}
