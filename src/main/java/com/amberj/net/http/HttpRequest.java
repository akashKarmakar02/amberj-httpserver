package com.amberj.net.http;

import java.net.URI;
import java.util.List;
import java.util.Map;

public record HttpRequest(
        Map<String, Object> body,
        Map<String, String> pathParams,
        Map<String, List<String>> queryParams,
        Map<String, List<String>> header,
        Map<String, String> cookies,
        String method,
        URI uri
) {

    @Override
    public String toString() {
        return "HttpRequest{" +
                "body=" + body +
                ", pathParams=" + pathParams +
                ", queryParams=" + queryParams +
                ", header=" + header +
                ", method='" + method + '\'' +
                ", uri='" + uri + '\'' +
                '}';
    }
}
