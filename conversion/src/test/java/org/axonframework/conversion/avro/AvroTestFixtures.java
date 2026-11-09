/*
 * Copyright (c) 2026. Terrence Daniels
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

package org.axonframework.conversion.avro;

import org.apache.avro.Schema;
import org.apache.avro.generic.GenericData;
import org.apache.avro.generic.GenericRecord;
import org.apache.avro.message.BinaryMessageEncoder;

import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * Schemas and helpers shared by the Avro tests, built on the Avro library only (no generated classes).
 */
final class AvroTestFixtures {

    /**
     * A record with a single required {@code name} field.
     */
    static final Schema PERSON_V1 = new Schema.Parser().parse(
            "{\"type\":\"record\",\"name\":\"Person\",\"namespace\":\"test\",\"fields\":["
                    + "{\"name\":\"name\",\"type\":\"string\"}]}");

    /**
     * {@link #PERSON_V1} plus an {@code age} field that has a default, so readers of this schema can read data written
     * with {@link #PERSON_V1}.
     */
    static final Schema PERSON_V2_WITH_DEFAULT = new Schema.Parser().parse(
            "{\"type\":\"record\",\"name\":\"Person\",\"namespace\":\"test\",\"fields\":["
                    + "{\"name\":\"name\",\"type\":\"string\"},"
                    + "{\"name\":\"age\",\"type\":\"int\",\"default\":0}]}");

    /**
     * {@link #PERSON_V1} plus a required {@code age} field without a default, so readers of this schema cannot read
     * data written with {@link #PERSON_V1}.
     */
    static final Schema PERSON_V3_REQUIRED_FIELD = new Schema.Parser().parse(
            "{\"type\":\"record\",\"name\":\"Person\",\"namespace\":\"test\",\"fields\":["
                    + "{\"name\":\"name\",\"type\":\"string\"},"
                    + "{\"name\":\"age\",\"type\":\"int\"}]}");

    private AvroTestFixtures() {
    }

    static GenericRecord person(String name) {
        GenericRecord record = new GenericData.Record(PERSON_V1);
        record.put("name", name);
        return record;
    }

    static GenericRecord person(String name, int age) {
        GenericRecord record = new GenericData.Record(PERSON_V3_REQUIRED_FIELD);
        record.put("name", name);
        record.put("age", age);
        return record;
    }

    /**
     * Encodes the record in Avro's single object encoding, straight through the Avro library.
     */
    static byte[] singleObjectEncoded(GenericRecord record) throws IOException {
        ByteBuffer encoded = new BinaryMessageEncoder<GenericRecord>(GenericData.get(), record.getSchema())
                .encode(record);
        byte[] bytes = new byte[encoded.remaining()];
        encoded.get(bytes);
        return bytes;
    }
}
