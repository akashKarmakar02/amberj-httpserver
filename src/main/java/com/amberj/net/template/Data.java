package com.amberj.net.template;

import com.google.common.collect.Maps;

import java.util.Map;

public class Data {
    private final Map<String, Object> data;

    public Data() {
        data = Maps.newHashMap();
    }

    public Data with(String key, Object value) {
        data.put(key, value);
        return this;
    }

    public Map<String, Object> getContext() {
        return this.data;
    }
}
