package com.amberj.net.httpserver;

import com.amberj.net.http.HttpRequest;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.*;

import static java.lang.System.out;

class HttpRequestUtil {
    static HttpRequest getHttpRequest(HttpExchange exchange, List<String> paramsList, ArrayList<String> pathParams) throws IOException {
        Map<String, Object> body;
        if (exchange.getRequestMethod().equalsIgnoreCase("POST") || exchange.getRequestMethod().equalsIgnoreCase("PUT")) {
            body = getBody(exchange);
        } else {
            body = new HashMap<>();
        }
        var params = getPathParams(pathParams, paramsList);
        var header = getHeader(exchange);
        var queryParams = getQueryParams(exchange);

        return new HttpRequest(body, params, queryParams, header, exchange.getRequestMethod(), exchange.getRequestURI());
    }

    private static Map<String, Object> getBody(HttpExchange exchange) throws IOException {
        var inputStream = exchange.getRequestBody();
        var contentType = exchange.getRequestHeaders().get("Content-Type");
        Map<String, Object> postData;
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
        if (contentType == null) {
            return new HashMap<>();
        }

        if (Objects.equals(contentType.getFirst(), "application/x-www-form-urlencoded")) {
            String line;
            postData = new HashMap<>();
            while ((line = reader.readLine()) != null) {
                String[] params = line.split("&");
                for (String param : params) {
                    String[] keyValue = param.split("=");
                    if (keyValue.length == 2) {
                        String key = keyValue[0];
                        String value = java.net.URLDecoder.decode(keyValue[1], StandardCharsets.UTF_8);
                        postData.put(key, value);
                    }
                }
            }
        } else if (Objects.equals(contentType.getFirst(), "application/json")) {
            ObjectMapper objectMapper = new ObjectMapper();
            StringBuilder jsonString = new StringBuilder();
            for (var line: reader.lines().toArray()) {
                jsonString.append(line);
            }
            postData = objectMapper.readValue(jsonString.toString(), new TypeReference<>() {});
        } else if (contentType.getFirst().startsWith("multipart/form-data")) {
            postData = MultipartFormDataParser.parseMultipartForm(exchange);
        } else {
            postData = new HashMap<>();
        }
        return postData;
    }

    private static Map<String, String> getPathParams(ArrayList<String> pathParams, List<String> params) {
        var data = new HashMap<String, String>();
        if (!pathParams.isEmpty()) {
            for (int i = 0; i < pathParams.size(); i++) {
                var key = pathParams.get(i).split(":")[0];
                data.put(key, params.get(i));
            }
        }
        return data;
    }

    private static Map<String, List<String>> getQueryParams(HttpExchange exchange) {
        var requestUri = exchange.getRequestURI();
        String query = requestUri.getQuery();
        Map<String, List<String>> queryParams = new HashMap<>();

        if (query == null || query.isEmpty()) {
            return queryParams;
        }

        String[] pairs = query.split("&");
        for (String pair : pairs) {
            int idx = pair.indexOf("=");
            String key = idx > 0 ? URLDecoder.decode(pair.substring(0, idx), StandardCharsets.UTF_8) : pair;
            String value = idx > 0 && pair.length() > idx + 1 ? URLDecoder.decode(pair.substring(idx + 1), StandardCharsets.UTF_8) : null;

            queryParams.computeIfAbsent(key, k -> new ArrayList<>()).add(value);
        }

        return queryParams;
    }

    private static Map<String, List<String>> getHeader(HttpExchange exchange) {
        var headers = exchange.getRequestHeaders();

        return new HashMap<>(headers);
    }
}
