/*
 * Licensed to Think Big Analytics, Inc. under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  Think Big Analytics, Inc. licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * 
 * Copyright 2010 Think Big Analytics. All Rights Reserved.
 */
package tap.core;

import java.util.*;

import org.apache.avro.Schema;
import org.apache.avro.generic.GenericData;

import com.google.protobuf.Message;


public class ReflectionKeyExtractor<OUT> implements KeyExtractor<GenericData.Record, OUT> {
    final Map<String,java.lang.reflect.Field> inFields = new HashMap<String,java.lang.reflect.Field>();
    final Map<String,java.lang.reflect.Method> inGetters = new HashMap<String,java.lang.reflect.Method>();
    private final List<String> fieldNames;
    private final Schema keySchema;

    public ReflectionKeyExtractor(Schema schema, String groupBy, String sortBy) {               
        String[] groupFields = groupBy==null ? new String[0] : groupBy.split(",");
        String[] sortFields = sortBy==null ? new String[0] : sortBy.split(",");
        fieldNames = new ArrayList<String>(groupFields.length + sortFields.length);
        
        addFieldnames(fieldNames, groupFields);
        addFieldnames(fieldNames, sortFields);
        
        keySchema = Phase.group(schema, fieldNames);
    }

    public static void addFieldnames(List<String> fieldNames, String[] groupFields) {
        for (String name : groupFields) {
            String[] parts = name.trim().split("\\s", 2); // skip asc/desc
            if (fieldNames.contains(parts[0])) continue;
            fieldNames.add(parts[0]);            
        }
    }
    
    @Override
    public GenericData.Record getProtypeKey() {
        return new GenericData.Record(keySchema);
    }

    @Override
    public void setKey(OUT value, GenericData.Record key) {
        if (inFields.isEmpty()) {
            Class<?> inClass = value.getClass();

            try {
                for (String fieldName : fieldNames) {
                    if(Message.class.isAssignableFrom(inClass)) {
                        java.lang.reflect.Method getter = inClass.getMethod(getter(fieldName));
                        inGetters.put(fieldName, getter);
                    } else {
                        java.lang.reflect.Field field = inClass.getField(fieldName);
                        field.setAccessible(true);
                        inFields.put(fieldName, field);
                    }
                }
            }
            catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
        try {
            for (Map.Entry<String, java.lang.reflect.Field> entry : inFields.entrySet()) {
                String fieldName = entry.getKey();
                key.put(fieldName, entry.getValue().get(value));
            }
            for (Map.Entry<String, java.lang.reflect.Method> entry : inGetters.entrySet()) {
                String fieldName = entry.getKey();
                key.put(fieldName, entry.getValue().invoke(value));
            }
        }
        catch (Exception e) {
            throw new RuntimeException(e);
        }
        
    }
    
    private static String getter(String fieldName) {
        return "get" + Character.toUpperCase(fieldName.charAt(0)) + fieldName.substring(1);
    }

}
