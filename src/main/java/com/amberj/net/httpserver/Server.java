package com.amberj.net.httpserver;

import com.amberj.net.Config;
import com.amberj.net.http.HttpResponse;
import com.sun.net.httpserver.HttpServer;
import com.amberj.net.http.HttpRequest;

import java.io.*;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.BiConsumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.lang.System.out;

public class Server {
    private final HttpServer server;
    private final int port;
    private final HashMap<String, RouteHandler> routeHandlerMap;
    private final ExecutorService executor;
    private final List<TriConsumer<HttpRequest, HttpResponse, Next>> middlewares;


    /**
     * @param port The port you want to start your server
     * @throws IOException
     */
    public Server(int port) throws IOException {
        this.routeHandlerMap = new HashMap<>();
        this.server = HttpServer.create(new InetSocketAddress(port), 512);
        this.port = port;
        this.executor = Executors.newVirtualThreadPerTaskExecutor();
        this.middlewares = new ArrayList<>();
    }

    public Server(int port, int backlog) throws IOException {
        this.routeHandlerMap = new HashMap<>();
        this.server = HttpServer.create(new InetSocketAddress(port), backlog);
        this.port = port;
        this.executor = Executors.newVirtualThreadPerTaskExecutor();
        this.middlewares = new ArrayList<>();
    }

    public static String getPrefixUntilWildcard(String input, String wildcardPattern) {
        String regexPattern = wildcardPattern.replace("*", "\\*");

        int wildcardIndex = regexPattern.indexOf("*");

        if (wildcardIndex == -1) {
            return input;
        }

        return input.substring(0, wildcardIndex - 1);
    }

    private RouteDetails extractPathParams(String route) {
        Pattern pattern = Pattern.compile("\\{([a-zA-Z]+)}");
        Matcher matcher = pattern.matcher(route);
        ArrayList<String> pathParams = new ArrayList<>();
        String regex = null;

        while (matcher.find()) {
            String placeholder = matcher.group(1);
            if (regex == null) {
                regex = route.replace("{" + placeholder + "}", "*");
            } else {
                regex = regex.replace("{" + placeholder + "}", "*");
            }
            pathParams.add(placeholder);
        }

        if (regex != null) {
            route = getPrefixUntilWildcard(route, regex);
        }

        return new RouteDetails(route, pathParams, regex);
    }

    public void get(String route, BiConsumer<HttpRequest, HttpResponse> handler) {
        RouteDetails routeDetails = extractPathParams(route);
        if (routeHandlerMap.containsKey(routeDetails.cleanedRoute)) {
            routeHandlerMap.put(routeDetails.cleanedRoute, routeHandlerMap.get(routeDetails.cleanedRoute).get(handler, routeDetails.pathParams, routeDetails.regex));
        } else {
            routeHandlerMap.put(routeDetails.cleanedRoute, RouteHandler.create(routeDetails.cleanedRoute).get(handler, routeDetails.pathParams, routeDetails.regex));
        }
    }

    public void post(String route, BiConsumer<HttpRequest, HttpResponse> handler) {
        RouteDetails routeDetails = extractPathParams(route);
        if (routeHandlerMap.containsKey(routeDetails.cleanedRoute)) {
            routeHandlerMap.put(routeDetails.cleanedRoute, routeHandlerMap.get(routeDetails.cleanedRoute).post(handler, routeDetails.pathParams, routeDetails.regex));
        } else {
            routeHandlerMap.put(routeDetails.cleanedRoute, RouteHandler.create(routeDetails.cleanedRoute).post(handler, routeDetails.pathParams, routeDetails.regex));
        }
    }

    public void put(String route, BiConsumer<HttpRequest, HttpResponse> handler) {
        RouteDetails routeDetails = extractPathParams(route);
        if (routeHandlerMap.containsKey(routeDetails.cleanedRoute)) {
            routeHandlerMap.put(routeDetails.cleanedRoute, routeHandlerMap.get(routeDetails.cleanedRoute).put(handler, routeDetails.pathParams, routeDetails.regex));
        } else {
            routeHandlerMap.put(routeDetails.cleanedRoute, RouteHandler.create(routeDetails.cleanedRoute).put(handler, routeDetails.pathParams, routeDetails.regex));
        }
    }

    public void patch(String route, BiConsumer<HttpRequest, HttpResponse> handler) {
        RouteDetails routeDetails = extractPathParams(route);
        if (routeHandlerMap.containsKey(routeDetails.cleanedRoute)) {
            routeHandlerMap.put(routeDetails.cleanedRoute, routeHandlerMap.get(routeDetails.cleanedRoute).patch(handler, routeDetails.pathParams, routeDetails.regex));
        } else {
            routeHandlerMap.put(routeDetails.cleanedRoute, RouteHandler.create(routeDetails.cleanedRoute).patch(handler, routeDetails.pathParams, routeDetails.regex));
        }
    }

    public void delete(String route, BiConsumer<HttpRequest, HttpResponse> handler) {
        RouteDetails routeDetails = extractPathParams(route);
        if (routeHandlerMap.containsKey(routeDetails.cleanedRoute)) {
            routeHandlerMap.put(routeDetails.cleanedRoute, routeHandlerMap.get(routeDetails.cleanedRoute).delete(handler, routeDetails.pathParams, routeDetails.regex));
        } else {
            routeHandlerMap.put(routeDetails.cleanedRoute, RouteHandler.create(routeDetails.cleanedRoute).delete(handler, routeDetails.pathParams, routeDetails.regex));
        }
    }

    public void handle(String route, HttpHandler handler) {
        RouteDetails routeDetails = extractPathParams(route);
        if (routeHandlerMap.containsKey(routeDetails.cleanedRoute)) {
            routeHandlerMap.put(routeDetails.cleanedRoute, routeHandlerMap.get(routeDetails.cleanedRoute).handle(handler, routeDetails.pathParams, routeDetails.regex));
        } else {
            routeHandlerMap.put(routeDetails.cleanedRoute, RouteHandler.create(routeDetails.cleanedRoute).handle(handler, routeDetails.pathParams, routeDetails.regex));
        }
    }

    public void use(TriConsumer<HttpRequest, HttpResponse, Next> middleware) {
        this.middlewares.add(middleware);
    }

    public void setStaticDir(String path) {
        Config.STATIC_DIR = path;
    }

    public void run(Runnable function) {
        routeHandlerMap.keySet().forEach((route) -> {
            RouteHandler handler = routeHandlerMap.get(route);
            handler.setMiddlewares(this.middlewares);
            server.createContext(route, handler);
        });

        server.setExecutor(executor);
        server.start();
        function.run();
    }

    public void run() {
        run(() -> out.println("Server is listening on: http://localhost:" + this.port));
    }

    private static class RouteDetails {
        String cleanedRoute;
        ArrayList<String> pathParams;
        String regex;

        RouteDetails(String cleanedRoute, ArrayList<String> pathParams, String regex) {
            this.cleanedRoute = cleanedRoute;
            this.pathParams = pathParams;
            this.regex = regex;
        }
    }

}
