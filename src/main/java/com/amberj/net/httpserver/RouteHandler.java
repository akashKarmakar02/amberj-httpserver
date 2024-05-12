package com.amberj.net.httpserver;

import com.amberj.net.Config;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.amberj.net.http.HttpRequest;
import com.amberj.net.http.HttpResponse;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.lang.System.out;

class RouteHandler implements HttpHandler {

    private BiConsumer<HttpRequest, HttpResponse> getHandler;
    private BiConsumer<HttpRequest, HttpResponse> postHandler;
    private final String route;
    private ArrayList<String> pathParams;
    private String regex;
    private List<String> params;

    private RouteHandler(String route) {
        this.route = route;
    }

    public RouteHandler get(BiConsumer<HttpRequest, HttpResponse> handler, ArrayList<String> pathParams, String regex) {
        this.getHandler = handler;
        this.pathParams = pathParams;
        this.regex = regex;
        return this;
    }

    public RouteHandler post(BiConsumer<HttpRequest, HttpResponse> handler) {
        this.postHandler = handler;
        return this;
    }

    public static RouteHandler create(String route) {
        return new RouteHandler(route);
    }

    private static List<String> matchWildcard(String input, String wildcardPattern) {
        String regexPattern = wildcardPattern.replace("*", "([^/]*)");

        Pattern pattern = Pattern.compile(regexPattern);

        Matcher matcher = pattern.matcher(input);

        List<String> matches = new ArrayList<>();

        if (matcher.matches()) {
            for (int i = 1; i <= matcher.groupCount(); i++) {
                matches.add(matcher.group(i));
            }
        }

        return matches;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        exchange.getResponseHeaders().set("Cache-Control", "no-cache");
        String currRoute = exchange.getRequestURI().getPath();

        if (currRoute.startsWith("/" + Config.STATIC_DIR)) {
            handleStaticFileRequest(exchange);
            return;
        }


        if (regex != null) {
            params = matchWildcard(currRoute, regex);
            if (!params.isEmpty()) {
                handleGetRequest(exchange);
            } else {
                handleNotFound(exchange);
            }
            return;
        }

        if ((!currRoute.equals(route))) {
            if (pathParams.isEmpty()) {
                handleNotFound(exchange);
                return;
            }
        }

        if (exchange.getRequestMethod().equalsIgnoreCase("POST")) {
            if (postHandler == null) {
                handleMethodNotAllowed(exchange);
            } else {
                handlePostRequest(exchange);
            }
        } else {
            if (getHandler == null) {
                handleMethodNotAllowed(exchange);
            } else {
                handleGetRequest(exchange);
            }
        }
    }

    private void handleStaticFileRequest(HttpExchange exchange) throws IOException {
        String requestMethod = exchange.getRequestMethod();

        // Only handle GET requests
        if (requestMethod.equalsIgnoreCase("GET")) {
            String filePath = Config.BASE_DIR + Config.STATIC_DIR + exchange.getRequestURI().getPath().substring("/static/".length());

            // Try to read the file
            File file = new File(filePath);
            if (file.exists() && !file.isDirectory()) {
                exchange.sendResponseHeaders(200, file.length());
                OutputStream outputStream = exchange.getResponseBody();
                FileInputStream fileInputStream = new FileInputStream(file);
                fileInputStream.transferTo(outputStream);
                fileInputStream.close();
                outputStream.close();
            } else {
                // File not found, send 404
                String response = "File not found";
                exchange.sendResponseHeaders(404, response.length());
                OutputStream outputStream = exchange.getResponseBody();
                outputStream.write(response.getBytes());
                outputStream.close();
            }
        }
    }

    private void handleGetRequest(HttpExchange exchange) throws IOException {
        HttpRequest httpRequest;

        if (pathParams.isEmpty()) {
            httpRequest = new HttpRequest(new HashMap<>(), new HashMap<>());
        } else {
            var data = new HashMap<String, String>();
            for (int i = 0; i < pathParams.size(); i++) {
                var key = pathParams.get(i).split(":")[0];
                data.put(key, params.get(i));
            }
            httpRequest = new HttpRequest(new HashMap<>(), data);
        }

        var httpResponse = new HttpResponse();

        getHandler.accept(httpRequest, httpResponse);
        if (httpResponse.getRedirectURL() != null) {
            exchange.getResponseHeaders().set("Location", httpResponse.getRedirectURL());
            exchange.getResponseHeaders().set("Cache-Control", "no-cache");
            String response = "Redirecting to " + httpResponse.getRedirectURL();
            exchange.sendResponseHeaders(301, response.getBytes().length);
            OutputStream os = exchange.getResponseBody();
            os.write(response.getBytes());
            os.close();
        } else {
            String response = httpResponse.getResponse();
            exchange.sendResponseHeaders(200, response.getBytes().length);
            OutputStream os = exchange.getResponseBody();
            os.write(response.getBytes());
            os.close();
            out.println(new Date() + " GET: " + exchange.getRequestURI().toString() + " " + exchange.getResponseCode());
        }
    }

    private void handlePostRequest(HttpExchange exchange) throws IOException {
        var inputStream = exchange.getRequestBody();
        Map<String, Object> postData = new HashMap<>();
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
        String line;
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
        reader.close();


        var httpRequest = new HttpRequest(postData, new HashMap<>());

        var httpResponse = new HttpResponse();

        postHandler.accept(httpRequest, httpResponse);

        if (httpResponse.getRedirectURL() != null) {
            exchange.getResponseHeaders().set("Location", httpResponse.getRedirectURL());
            String response = "Redirecting to " + httpResponse.getRedirectURL();
            exchange.sendResponseHeaders(301, response.getBytes().length);
            OutputStream os = exchange.getResponseBody();
            os.write(response.getBytes());
            os.close();
        } else {
            String response = httpResponse.getResponse();
            exchange.sendResponseHeaders(200, response.getBytes().length);
            OutputStream os = exchange.getResponseBody();
            os.write(response.getBytes());
            os.close();
            out.println(new Date() + " POST: " + exchange.getRequestURI().toString() + " " + exchange.getResponseCode());
        }

    }

    private void handleNotFound(HttpExchange exchange) throws IOException {
        int statusCode = 404;
        String response = "404 Not Found";
        exchange.sendResponseHeaders(statusCode, response.getBytes().length);
        OutputStream os = exchange.getResponseBody();
        os.write(response.getBytes());
        os.close();
        out.println(new Date() + " GET: " + exchange.getRequestURI().toString() + " " + exchange.getResponseCode());
    }

    private void handleMethodNotAllowed(HttpExchange exchange) throws IOException {
        int status = 405;
        String response = "405 Method Not Allowed";
        exchange.sendResponseHeaders(status, response.getBytes().length);
        OutputStream os = exchange.getResponseBody();
        os.write(response.getBytes());
        os.close();
    }
}