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
import org.apache.avro.specific.SpecificRecordBase;

/**
 * A second hand-written Avro specific record whose schema has a different name than {@link TestPersonRecord}, so
 * reading data written with one schema as the other is a schema incompatibility.
 */
public class TestOtherRecord extends SpecificRecordBase {

    public static final Schema SCHEMA$ = new Schema.Parser().parse(
            "{\"type\":\"record\",\"name\":\"TestOtherRecord\",\"namespace\":\"org.axonframework.conversion.avro\","
                    + "\"fields\":[{\"name\":\"name\",\"type\":\"string\"}]}");

    private CharSequence name;

    public TestOtherRecord() {
    }

    public static Schema getClassSchema() {
        return SCHEMA$;
    }

    @Override
    public Schema getSchema() {
        return SCHEMA$;
    }

    @Override
    public Object get(int field) {
        if (field == 0) {
            return name;
        }
        throw new IndexOutOfBoundsException("Invalid index: " + field);
    }

    @Override
    public void put(int field, Object value) {
        if (field == 0) {
            name = (CharSequence) value;
            return;
        }
        throw new IndexOutOfBoundsException("Invalid index: " + field);
    }
}
