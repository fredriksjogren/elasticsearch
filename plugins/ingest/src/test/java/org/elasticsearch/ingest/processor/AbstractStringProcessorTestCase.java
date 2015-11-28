/*
 * Licensed to Elasticsearch under one or more contributor
 * license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright
 * ownership. Elasticsearch licenses this file to you under
 * the Apache License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.elasticsearch.ingest.processor;

import org.elasticsearch.ingest.IngestDocument;
import org.elasticsearch.ingest.RandomDocumentPicks;
import org.elasticsearch.test.ESTestCase;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;

public abstract class AbstractStringProcessorTestCase extends ESTestCase {

    protected abstract AbstractStringProcessor newProcessor(Collection<String> fields);

    protected String modifyInput(String input) {
        return input;
    }

    protected abstract String expectedResult(String input);

    public void testProcessor() throws Exception {
        IngestDocument ingestDocument = RandomDocumentPicks.randomIngestDocument(random());
        int numFields = randomIntBetween(1, 5);
        Map<String, String> expected = new HashMap<>();
        for (int i = 0; i < numFields; i++) {
            String fieldValue = RandomDocumentPicks.randomString(random());
            String fieldName = RandomDocumentPicks.addRandomField(random(), ingestDocument, modifyInput(fieldValue));
            expected.put(fieldName, expectedResult(fieldValue));
        }
        Processor processor = newProcessor(expected.keySet());
        processor.execute(ingestDocument);
        for (Map.Entry<String, String> entry : expected.entrySet()) {
            assertThat(ingestDocument.getFieldValue(entry.getKey(), String.class), equalTo(entry.getValue()));
        }
    }

    public void testFieldNotFound() throws Exception {
        String fieldName = RandomDocumentPicks.randomFieldName(random());
        Processor processor = newProcessor(Collections.singletonList(fieldName));
        IngestDocument ingestDocument = RandomDocumentPicks.randomIngestDocument(random(), new HashMap<>());
        try {
            processor.execute(ingestDocument);
            fail("processor should have failed");
        } catch (IllegalArgumentException e) {
            assertThat(e.getMessage(), containsString("not present as part of path [" + fieldName + "]"));
        }
    }

    public void testNullValue() throws Exception {
        Processor processor = newProcessor(Collections.singletonList("field"));
        IngestDocument ingestDocument = RandomDocumentPicks.randomIngestDocument(random(), Collections.singletonMap("field", null));
        try {
            processor.execute(ingestDocument);
            fail("processor should have failed");
        } catch (IllegalArgumentException e) {
            assertThat(e.getMessage(), equalTo("field [field] is null, cannot process it."));
        }
    }

    public void testNonStringValue() throws Exception {
        String fieldName = RandomDocumentPicks.randomFieldName(random());
        Processor processor = newProcessor(Collections.singletonList(fieldName));
        IngestDocument ingestDocument = RandomDocumentPicks.randomIngestDocument(random(), new HashMap<>());
        ingestDocument.setFieldValue(fieldName, randomInt());
        try {
            processor.execute(ingestDocument);
            fail("processor should have failed");
        } catch (IllegalArgumentException e) {
            assertThat(e.getMessage(), equalTo("field [" + fieldName + "] of type [java.lang.Integer] cannot be cast to [java.lang.String]"));
        }
    }
}
