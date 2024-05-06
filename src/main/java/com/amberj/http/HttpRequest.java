package com.amberj.http;

import java.util.Map;

public record HttpRequest(Map<String, Object> body, Map<String, String> pathParams) {

    @Override
    public String toString() {
        return "HttpRequest{" +
                "body=" + body +
                ", pathParams=" + pathParams +
                '}';
    }
}
