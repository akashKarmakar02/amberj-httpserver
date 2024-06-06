package com.amberj.net.httpserver;

import com.amberj.net.Config;
import com.google.common.base.Charsets;
import com.google.common.io.Resources;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.amberj.net.http.HttpRequest;
import com.amberj.net.http.HttpResponse;

import java.io.*;
import java.net.URL;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiConsumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.lang.System.out;

class RouteHandler implements HttpHandler {

    // handler for request
    private final ArrayList<RouteDetails> getHandlers;
    private final ArrayList<RouteDetails> postHandlers;
    private final ArrayList<RouteDetails> putHandlers;
    private final ArrayList<RouteDetails> deleteHandlers;

    // route specific values
    private final String route;
    private ArrayList<String> pathParams;
    private String regex;
    private List<String> params;

    private RouteHandler(String route) {
        this.route = route;
        this.getHandlers = new ArrayList<>();
        this.postHandlers = new ArrayList<>();
        this.putHandlers = new ArrayList<>();
        this.deleteHandlers = new ArrayList<>();
    }

    public RouteHandler handle(com.amberj.net.httpserver.HttpHandler handler, ArrayList<String> pathParams, String regex) {
        this.getHandlers.add(new RouteDetails(pathParams, regex, handler::get));
        this.postHandlers.add(new RouteDetails(pathParams, regex, handler::post));
        this.putHandlers.add(new RouteDetails(pathParams, regex, handler::put));
        this.deleteHandlers.add(new RouteDetails(pathParams, regex, handler::delete));
        return this;
    }

    public RouteHandler get(BiConsumer<HttpRequest, HttpResponse> handler, ArrayList<String> pathParams, String regex) {
        this.getHandlers.add(new RouteDetails(pathParams, regex, handler));
        this.pathParams = pathParams;
        this.regex = regex;
        return this;
    }

    public RouteHandler post(BiConsumer<HttpRequest, HttpResponse> handler, ArrayList<String> pathParams, String regex) {
        this.postHandlers.add(new RouteDetails(pathParams, regex, handler));
        this.pathParams = pathParams;
        this.regex = regex;
        return this;
    }

    public RouteHandler put(BiConsumer<HttpRequest, HttpResponse> handler, ArrayList<String> pathParams, String regex) {
        this.putHandlers.add(new RouteDetails(pathParams, regex, handler));
        this.pathParams = pathParams;
        this.regex = regex;
        return this;
    }

    public RouteHandler delete(BiConsumer<HttpRequest, HttpResponse> handler, ArrayList<String> pathParams, String regex) {
        this.deleteHandlers.add(new RouteDetails(pathParams, regex, handler));
        this.pathParams = pathParams;
        this.regex = regex;
        return this;
    }

    public RouteHandler handler(com.amberj.net.httpserver.HttpHandler handler, ArrayList<String> pathParams, String regex) {
        this.getHandlers.add(new RouteDetails(pathParams, regex, handler::get));
        this.postHandlers.add(new RouteDetails(pathParams, regex, handler::post));
        this.deleteHandlers.add(new RouteDetails(pathParams, regex, handler::delete));
        this.putHandlers.add(new RouteDetails(pathParams, regex, handler::put));
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

        if (!currRoute.equals(route)) {
            if (pathParams.isEmpty()) {
                handleNotFound(exchange);
                return;
            }
        }

        if (exchange.getRequestMethod().equalsIgnoreCase("POST")) {
            if (postHandlers.isEmpty()) {
                handleMethodNotAllowed(exchange);
            } else {
                for (var handler: postHandlers) {
                    if (handler.regex == null && currRoute.equals(route)) {
                        handlePostRequest(exchange, handler.pathParams, handler.handler);
                        return;
                    } else if (handler.regex != null) {
                        params = matchWildcard(currRoute, handler.regex);
                        if (!params.isEmpty() && !currRoute.equals("/")) {
                            handlePostRequest(exchange, handler.pathParams, handler.handler);
                            return;
                        }
                    }
                }
                handleNotFound(exchange);
            }
        } else if (exchange.getRequestMethod().equalsIgnoreCase("GET")) {
            if (getHandlers.isEmpty()) {
                handleMethodNotAllowed(exchange);
            } else {
                for (var handler: getHandlers) {
                    if (handler.regex == null && currRoute.equals(route)) {
                        handleGetRequest(exchange, handler.pathParams, handler.handler);
                        return;
                    } else if (handler.regex != null) {
                        params = matchWildcard(currRoute, handler.regex);
                        if (!params.isEmpty() && !currRoute.equals("/")) {
                            handleGetRequest(exchange, handler.pathParams, handler.handler);
                            return;
                        }
                    }
                }
                handleNotFound(exchange);
            }
        } else if (exchange.getRequestMethod().equalsIgnoreCase("PUT")) {
            if (putHandlers.isEmpty()) {
                handleMethodNotAllowed(exchange);
            } else {
                for (var handler: putHandlers) {
                    if (handler.regex == null && currRoute.equals(route)) {
                        handlePutRequest(exchange, handler.pathParams, handler.handler);
                        return;
                    } else if (handler.regex != null) {
                        params = matchWildcard(currRoute, handler.regex);
                        if (!params.isEmpty() && !currRoute.equals("/")) {
                            handlePutRequest(exchange, handler.pathParams, handler.handler);
                            return;
                        }
                    }
                }
                handleNotFound(exchange);
            }
        } else if (exchange.getRequestMethod().equalsIgnoreCase("DELETE")) {
            if (deleteHandlers.isEmpty()) {
                handleMethodNotAllowed(exchange);
            } else {
                for (var handler: deleteHandlers) {
                    if (handler.regex == null && currRoute.equals(route)) {
                        handleDeleteRequest(exchange, handler.pathParams, handler.handler);
                        return;
                    } else if (handler.regex != null) {
                        params = matchWildcard(currRoute, handler.regex);
                        if (!params.isEmpty() && !currRoute.equals("/")) {
                            handleDeleteRequest(exchange, handler.pathParams, handler.handler);
                            return;
                        }
                    }
                }
                handleNotFound(exchange);
            }
        }
    }

    private void handleStaticFileRequest(HttpExchange exchange) throws IOException {
        String requestMethod = exchange.getRequestMethod();

        if (requestMethod.equalsIgnoreCase("GET")) {
            String filePath = "static/" + exchange.getRequestURI().getPath().substring("/static/".length());
            URL fileUrl = Resources.getResource(filePath);
            try {
                if (fileUrl.getPath() != null) {
                    String fileContent = Resources.toString(fileUrl, Charsets.UTF_8);

                    exchange.sendResponseHeaders(200, fileContent.length());
                    OutputStream outputStream = exchange.getResponseBody();
                    outputStream.write(fileContent.getBytes(Charsets.UTF_8));
                    outputStream.close();
                } else {
                    String response = "File not found";
                    exchange.sendResponseHeaders(404, response.length());
                    OutputStream outputStream = exchange.getResponseBody();
                    outputStream.write(response.getBytes(Charsets.UTF_8));
                    outputStream.close();
                }
            } catch (IOException e) {
                exchange.sendResponseHeaders(500, -1);
            }

        }
    }

    private void handleRedirect(HttpExchange exchange, String redirectUrl) throws IOException {
        exchange.getResponseHeaders().set("Location", redirectUrl);
        exchange.getResponseHeaders().set("Cache-Control", "no-cache");
        String response = "Redirecting to " + redirectUrl;
        exchange.sendResponseHeaders(301, response.getBytes().length);
        OutputStream os = exchange.getResponseBody();
        os.write(response.getBytes());
        os.close();
    }

    private void handleGetRequest(HttpExchange exchange, ArrayList<String> pathParams, BiConsumer<HttpRequest, HttpResponse> handler) throws IOException {
        var httpRequest = HttpRequestUtil.getHttpRequest(exchange, params, pathParams);

        var httpResponse = new HttpResponse();

        try {
            handler.accept(httpRequest, httpResponse);
        } catch (Exception e) {
            handleError(exchange, e);
        }

        if (httpResponse.getRedirectURL() != null) {
            handleRedirect(exchange, httpResponse.getRedirectURL());
        } else {
            handleRequest(exchange, httpResponse);
            out.println(new Date() + " GET: " + exchange.getRequestURI().toString() + " " + exchange.getResponseCode());
        }
    }

    private void handlePutRequest(HttpExchange exchange, ArrayList<String> pathParams, BiConsumer<HttpRequest, HttpResponse> handler) throws IOException {
        var httpRequest = HttpRequestUtil.getHttpRequest(exchange, params, pathParams);

        var httpResponse = new HttpResponse();

        try {
            handler.accept(httpRequest, httpResponse);
        } catch (Exception e) {
            handleError(exchange, e);
        }

        if (httpResponse.getRedirectURL() != null) {
            handleRedirect(exchange, httpResponse.getRedirectURL());
        } else {
            handleRequest(exchange, httpResponse);
            out.println(new Date() + " PUT: " + exchange.getRequestURI().toString() + " " + exchange.getResponseCode());
        }
    }

    private void handleDeleteRequest(HttpExchange exchange, ArrayList<String> pathParams, BiConsumer<HttpRequest, HttpResponse> handler) throws IOException {
        var httpRequest = HttpRequestUtil.getHttpRequest(exchange, params, pathParams);

        var httpResponse = new HttpResponse();

        try {
            handler.accept(httpRequest, httpResponse);
        } catch (Exception e) {
            handleError(exchange, e);
        }

        if (httpResponse.getRedirectURL() != null) {
            handleRedirect(exchange, httpResponse.getRedirectURL());
        } else {
            handleRequest(exchange, httpResponse);
            out.println(new Date() + " DELETE: " + exchange.getRequestURI().toString() + " " + exchange.getResponseCode());
        }
    }

    private void handlePostRequest(HttpExchange exchange, ArrayList<String> pathParams, BiConsumer<HttpRequest, HttpResponse> handler) throws IOException {

        var httpRequest = HttpRequestUtil.getHttpRequest(exchange, params, pathParams);

        var httpResponse = new HttpResponse();
        try {
            handler.accept(httpRequest, httpResponse);
        } catch (Exception e) {
            handleError(exchange, e);
        }

        if (httpResponse.getRedirectURL() != null) {
            handleRedirect(exchange, httpResponse.getRedirectURL());
        } else {
            handleRequest(exchange, httpResponse);
            out.println(new Date() + " POST: " + exchange.getRequestURI().toString() + " " + exchange.getResponseCode());
        }
    }

    private void handleError(HttpExchange exchange, Exception e) throws IOException {
        AtomicReference<String> errorMessage = new AtomicReference<>(e.getMessage());
        Arrays.stream(e.getStackTrace()).forEach((stackTraceElement) -> {
            errorMessage.updateAndGet(v -> v + "<br>&ensp;at " + stackTraceElement.toString());
        });
        e.printStackTrace();
        var html = """
                <html>
                <head>
                    <title>Internal Server Error</title>
                </head>
                <body>
                    <h1>500</h1><h3>(Internal Server Error)</h3>
                    <div>""" + errorMessage + """
                </div>
                </body>
                </html>
                """;
        exchange.getResponseHeaders().set("Content-Type", "text");
        exchange.sendResponseHeaders(500, html.getBytes().length);
        OutputStream os = exchange.getResponseBody();
        os.write(html.getBytes());
        os.close();
        out.println(new Date() + " " + exchange.getRequestMethod() + ": " + exchange.getRequestURI().toString() + " " + exchange.getResponseCode());
    }

    private void handleRequest(HttpExchange exchange, HttpResponse httpResponse) throws IOException {
        if (!httpResponse.isMethodAllowed()) {
            handleMethodNotAllowed(exchange);
            return;
        }
        String response = httpResponse.getResponse();
        exchange.getResponseHeaders().set("Content-Type", httpResponse.getContentType());
        exchange.sendResponseHeaders(httpResponse.getStatus(), response.getBytes().length);
        OutputStream os = exchange.getResponseBody();
        os.write(response.getBytes());
        os.close();
    }

    private void handleNotFound(HttpExchange exchange) throws IOException {
        String response = "404 (Not Found)\n";
        exchange.sendResponseHeaders(404, response.length());
        OutputStream os = exchange.getResponseBody();
        os.write(response.getBytes());
        os.close();
    }

    private void handleMethodNotAllowed(HttpExchange exchange) throws IOException {
        String response = "405 (Method Not Allowed)\n";
        exchange.sendResponseHeaders(405, response.length());
        OutputStream os = exchange.getResponseBody();
        os.write(response.getBytes());
        os.close();
    }

    record RouteDetails(
            ArrayList<String> pathParams,
            String regex,
            BiConsumer<HttpRequest, HttpResponse> handler
    ) {}
}
