package com.amberj.net.template;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Maps;

import java.util.Map;

public class Data {
    private final Map<String, Object> data;
    private final ObjectMapper objectMapper;

    public Data() {
        data = Maps.newHashMap();
        objectMapper = new ObjectMapper();
    }

    public Data with(String key, Object value) {
        data.put(key, value);
        return this;
    }

    public Map<String, Object> getContext() {
        return this.data;
    }

    public String toJson() {
        try {
            return objectMapper.writeValueAsString(data);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}
