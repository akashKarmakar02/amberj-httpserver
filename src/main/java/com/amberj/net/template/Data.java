package com.amberj.net.template;

import com.dslplatform.json.DslJson;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public class Data {
    private final Map<String, Object> data;
    DslJson<Object> dslJson;
    ByteArrayOutputStream output;

    public Data() {
        data = new HashMap<>();
        dslJson = new DslJson<>();
        output = new ByteArrayOutputStream();
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
            dslJson.serialize(data, output);
            return output.toString(StandardCharsets.UTF_8);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}
