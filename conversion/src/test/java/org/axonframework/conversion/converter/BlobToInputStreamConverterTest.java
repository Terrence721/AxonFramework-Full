/*
 * Copyright (c) 2010-2026. Axon Framework
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and limitations
 * under the License.
 */

package org.axonframework.conversion.converter;

import org.junit.jupiter.api.Test;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.sql.Blob;
import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class BlobToInputStreamConverterTest {

    @Test
    void convertsBlobToStream() throws SQLException, java.io.IOException {
        Blob blob = new Blob() {
            public long length() { return 5; }
            public byte[] getBytes(long pos, int length) { return "hello".getBytes(); }
            public InputStream getBinaryStream() { return new ByteArrayInputStream("hello".getBytes()); }
            public InputStream getBinaryStream(long pos, long length) { return getBinaryStream(); }
            public long position(byte[] pattern, long start) { return 0; }
            public long position(Blob pattern, long start) { return 0; }
            public int setBytes(long pos, byte[] bytes) { return 0; }
            public int setBytes(long pos, byte[] bytes, int offset, int len) { return 0; }
            public java.io.OutputStream setBinaryStream(long pos) { return null; }
            public void truncate(long len) {}
            public void free() {}
        };
        BlobToInputStreamConverter converter = new BlobToInputStreamConverter();

        InputStream stream = converter.convert(blob);

        assertEquals("hello", new String(stream.readAllBytes()));
    }

    @Test
    void nullInputReturnsNull() {
        BlobToInputStreamConverter converter = new BlobToInputStreamConverter();

        assertNull(converter.convert(null));
    }
}
