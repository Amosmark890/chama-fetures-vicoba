package com.eclectics.commons.util;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * @author Alex Maina
 * @created 11/01/2022
 */
public class JsonMapper {
    public static <T> List<T> readList(String str, Class<T> type) {
        return readList(str, ArrayList.class, type);
    }

    public static <T> List<T> readList(String str, Class<? extends Collection> type, Class<T> elementType) {
        final ObjectMapper mapper = newMapper();
        try {
            return mapper.readValue(str, mapper.getTypeFactory().constructCollectionType(type, elementType));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static ObjectMapper newMapper() {

        final ObjectMapper mapper = new ObjectMapper();
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        return mapper;
    }
}
