package com.amberj.net.httpserver;

import com.amberj.net.Config;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.amberj.net.http.HttpRequest;
import com.amberj.net.http.HttpResponse;

import java.io.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
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
    private final ArrayList<RouteDetails> patchHandlers;

    // route specific values
    private final String route;
    private List<String> params;
    private List<TriConsumer<HttpRequest, HttpResponse, Next>> middlewares;

    private RouteHandler(String route) {
        this.route = route;
        this.getHandlers = new ArrayList<>();
        this.postHandlers = new ArrayList<>();
        this.putHandlers = new ArrayList<>();
        this.deleteHandlers = new ArrayList<>();
        this.patchHandlers = new ArrayList<>();
    }

    public void setMiddlewares(List<TriConsumer<HttpRequest, HttpResponse, Next>> middlewares) {
        this.middlewares = middlewares;
    }

    public RouteHandler handle(com.amberj.net.httpserver.HttpHandler handler, ArrayList<String> pathParams, String regex) {
        this.getHandlers.add(new RouteDetails(pathParams, regex, handler::get));
        this.postHandlers.add(new RouteDetails(pathParams, regex, handler::post));
        this.deleteHandlers.add(new RouteDetails(pathParams, regex, handler::delete));
        this.putHandlers.add(new RouteDetails(pathParams, regex, handler::put));
        this.patchHandlers.add(new RouteDetails(pathParams, regex, handler::patch));
        return this;
    }

    public RouteHandler get(BiConsumer<HttpRequest, HttpResponse> handler, ArrayList<String> pathParams, String regex) {
        this.getHandlers.add(new RouteDetails(pathParams, regex, handler));
        return this;
    }

    public RouteHandler post(BiConsumer<HttpRequest, HttpResponse> handler, ArrayList<String> pathParams, String regex) {
        this.postHandlers.add(new RouteDetails(pathParams, regex, handler));
        return this;
    }

    public RouteHandler put(BiConsumer<HttpRequest, HttpResponse> handler, ArrayList<String> pathParams, String regex) {
        this.putHandlers.add(new RouteDetails(pathParams, regex, handler));
        return this;
    }

    public RouteHandler delete(BiConsumer<HttpRequest, HttpResponse> handler, ArrayList<String> pathParams, String regex) {
        this.deleteHandlers.add(new RouteDetails(pathParams, regex, handler));
        return this;
    }

    public RouteHandler patch(BiConsumer<HttpRequest, HttpResponse> handler, ArrayList<String> pathParams, String regex) {
        this.patchHandlers.add(new RouteDetails(pathParams, regex, handler));
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
        if (currRoute.endsWith("/")) {
            currRoute = currRoute.substring(0, currRoute.length() - 1);
        }

        String method = exchange.getRequestMethod().toUpperCase();

        RouteDetails matchedHandler;
        switch (method) {
            case "GET":
            case "HEAD":
                matchedHandler = matchRoute(currRoute, getHandlers);
                break;
            case "POST":
                matchedHandler = matchRoute(currRoute, postHandlers);
                break;
            case "PUT":
                matchedHandler = matchRoute(currRoute, putHandlers);
                break;
            case "DELETE":
                matchedHandler = matchRoute(currRoute, deleteHandlers);
                break;
            case "PATCH":
                matchedHandler = matchRoute(currRoute, patchHandlers);
                break;
            default:
                handleMethodNotAllowed(exchange);
                return;
        }

        if (matchedHandler == null) {
            handleNotFound(exchange);
        } else if ("HEAD".equals(method)) {
            handleHeadRequest(exchange, matchedHandler.pathParams, matchedHandler.handler);
        } else {
            handleRequest(exchange, matchedHandler.pathParams, matchedHandler.handler);
        }

        out.println(new Date() + " " + exchange.getRequestMethod() + ": " + exchange.getRequestURI().toString() + " " + exchange.getResponseCode());
    }



    private RouteDetails matchRoute(String currRoute, List<RouteDetails> handlers) {
        for (RouteDetails handler : handlers) {
            if (handler.regex == null && currRoute.equals(route)) {
                return handler;
            } else if (handler.regex != null) {
                params = matchWildcard(currRoute, handler.regex);
                if (!params.isEmpty() && !currRoute.equals("/")) {
                    return handler;
                }
            }
        }
        return null;
    }

    String getFileContent(String fileName) throws URISyntaxException, IOException {
        URL url = getClass().getClassLoader().getResource("static/" + fileName);
        String content = "";
        if (url != null) {
            URI uri = url.toURI();
            Path path = Paths.get(uri);
            content = Files.readString(path);
        }

        return content;
    }

    private void handleHeadRequest(HttpExchange exchange, ArrayList<String> pathParams, BiConsumer<HttpRequest, HttpResponse> handler) throws IOException {
        var httpRequest = HttpRequestUtil.getHttpRequest(exchange, params, pathParams);

        var httpResponse = new HttpResponse(exchange);

        try {
            handler.accept(httpRequest, httpResponse);
        } catch (Exception e) {
            handleError(exchange, e);
        }

        if (httpResponse.isMethodAllowed()) {
            handleMethodNotAllowed(exchange);
            return;
        }

        if (httpResponse.getRedirectURL() != null) {
            handleRedirect(exchange, httpResponse.getRedirectURL());
        }

        exchange.getResponseHeaders().set("Content-Type", httpResponse.getContentType());
        exchange.sendResponseHeaders(httpResponse.getStatus(), -1); // No response body for HEAD method
        exchange.getResponseBody().close();
    }

    private void handleStaticFileRequest(HttpExchange exchange) throws IOException {
        String requestMethod = exchange.getRequestMethod();

        if (requestMethod.equalsIgnoreCase("GET")) {
            String filePath = exchange.getRequestURI().getPath().substring("/static/".length());
            try {
                var fileContent = getFileContent(filePath);
                if (!Objects.equals(fileContent, "")) {

                    exchange.sendResponseHeaders(200, fileContent.length());
                    OutputStream outputStream = exchange.getResponseBody();
                    outputStream.write(fileContent.getBytes(StandardCharsets.UTF_8));
                    outputStream.close();
                } else {
                    String response = "File not found";
                    exchange.sendResponseHeaders(404, response.length());
                    OutputStream outputStream = exchange.getResponseBody();
                    outputStream.write(response.getBytes(StandardCharsets.UTF_8));
                    outputStream.close();
                }
            } catch (IOException | URISyntaxException e) {
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
    }

    private void handleRequest(HttpExchange exchange, ArrayList<String> pathParams, BiConsumer<HttpRequest, HttpResponse> handler) throws IOException {
        var httpRequest = HttpRequestUtil.getHttpRequest(exchange, params, pathParams);

        var httpResponse = new HttpResponse(exchange);

        try {
            if (!middlewares.isEmpty()) {
                Runnable chainedFunc = null;

                for (var middleware: middlewares.reversed()) {
                    if (chainedFunc == null) {
                        chainedFunc = () -> middleware.accept(httpRequest, httpResponse, () -> handler.accept(httpRequest, httpResponse));
                    } else {
                        Runnable finalChainedFunc = chainedFunc;
                        chainedFunc = () -> middleware.accept(httpRequest, httpResponse, finalChainedFunc::run);
                    }
                }

                assert chainedFunc != null;
                chainedFunc.run();
            } else {
                handler.accept(httpRequest, httpResponse);
            }
        } catch (Exception e) {
            handleError(exchange, e);
        }

        if (httpResponse.isMethodAllowed()) {
            handleMethodNotAllowed(exchange);
            return;
        }

        if (httpResponse.getRedirectURL() != null) {
            handleRedirect(exchange, httpResponse.getRedirectURL());
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
