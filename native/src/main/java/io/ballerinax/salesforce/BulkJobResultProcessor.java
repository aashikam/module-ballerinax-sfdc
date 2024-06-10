/*
 * Copyright (c) 2024, WSO2 LLC. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 LLC. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package io.ballerinax.salesforce;

import io.ballerina.runtime.api.Environment;
import io.ballerina.runtime.api.Future;
import io.ballerina.runtime.api.PredefinedTypes;
import io.ballerina.runtime.api.TypeTags;
import io.ballerina.runtime.api.async.Callback;
import io.ballerina.runtime.api.creators.ErrorCreator;
import io.ballerina.runtime.api.creators.ValueCreator;
import io.ballerina.runtime.api.types.ArrayType;
import io.ballerina.runtime.api.types.Type;
import io.ballerina.runtime.api.utils.JsonUtils;
import io.ballerina.runtime.api.utils.StringUtils;
import io.ballerina.runtime.api.utils.ValueUtils;
import io.ballerina.runtime.api.values.BError;
import io.ballerina.runtime.api.values.BObject;
import io.ballerina.runtime.api.values.BString;
import io.ballerina.runtime.api.values.BTypedesc;

import java.io.IOException;
import java.io.StringReader;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvException;

/**
 * This class holds the methods involved with data binding the bulk job result content.
 *
 * @since 8.0.3
 */
public class BulkJobResultProcessor {
    public static Object parseResultsToInputType(Environment env, BObject client, BString bulkJobId, Object maxRecords,
                                                 BTypedesc bTypedesc) {
        return invokeClientMethod(env, client, "processGetBulkJobResults", bTypedesc,
                bulkJobId, true, maxRecords, true);
    }

    private static Object invokeClientMethod(Environment env, BObject client, String methodName, BTypedesc bTypedesc,
                                             Object... paramFeed) {
        Future balFuture = env.markAsync();
        env.getRuntime().invokeMethodAsync(client, methodName, null, null, new Callback() {
            @Override
            public void notifySuccess(Object result) {

                Object payload = createPayload((BString) result, bTypedesc.getDescribingType());
                balFuture.complete(payload);
            }

            @Override
            public void notifyFailure(BError bError) {
                balFuture.complete(bError);
            }
        }, null, PredefinedTypes.TYPE_STRING, paramFeed);
        return null;
    }

    private static Object createPayload(BString csvData, Type type) {
        if (type.getTag() != TypeTags.ARRAY_TAG) {
            return ErrorCreator.createError(StringUtils.fromString(
                    "Unsupported data type for data binding query results"));
        }

        try (CSVReader reader = new CSVReader(new StringReader(csvData.getValue()))) {
            List<String[]> records = reader.readAll();

            // Convert each row in records to BArray
            Object[] bArrayData = new Object[records.size()];
            for (int i = 1; i < records.size() - 1; i++) {
                String[] row = records.get(i);
                bArrayData[i - 1] = createRecordValue(row, records.get(0), ((ArrayType) type).getElementType());
            }

            return ValueCreator.createArrayValue(bArrayData, (ArrayType) type); // string[][]
        } catch (IOException | CsvException e) {
            return ErrorCreator.createError(StringUtils.fromString(e.getMessage()));
        }
    }

    private static Object createRecordValue(String[] row, String[] headings, Type type) {
        String jsonString = convertToString(headings, row);
        return ValueUtils.convert(JsonUtils.parse(jsonString), type);
    }

    public static String convertToString(String[] headings, String[] values) {
        if (headings.length != values.length) {
            throw new IllegalArgumentException("Headings and values arrays must have the same length");
        }

        Map<String, String> keyValuePairs = new HashMap<>();
        for (int i = 0; i < headings.length; i++) {
            keyValuePairs.put(headings[i], values[i]);
        }

        StringBuilder builder = new StringBuilder();
        builder.append("{");
        for (Map.Entry<String, String> entry : keyValuePairs.entrySet()) {
            builder.append("\"").append(entry.getKey()).append("\"")
                    .append(" : ")
                    .append("\"").append(entry.getValue()).append("\"")
                    .append(",");
        }
        // Remove the trailing comma
        if (!keyValuePairs.isEmpty()) {
            builder.deleteCharAt(builder.length() - 1);
        }
        builder.append("}");

        return builder.toString();
    }
}
