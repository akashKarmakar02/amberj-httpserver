package com.amberj.net.httpserver;

import com.amberj.net.Config;
import com.amberj.net.http.HttpResponse;
import com.sun.net.httpserver.HttpServer;
import com.amberj.net.http.HttpRequest;

import java.io.*;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.BiConsumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.lang.System.out;


interface Callback {
    void function();
}

public class Server {
    private final HttpServer server;
    private final int port;
    private final HashMap<String, RouteHandler> routeHandlerMap;
    private final ExecutorService executor;

    public Server(int port) throws IOException {
        this.routeHandlerMap = new HashMap<>();
        this.server = HttpServer.create(new InetSocketAddress(port), 512);
        this.port = port;
        this.executor = Executors.newVirtualThreadPerTaskExecutor();
    }

    public Server(int port, int backlog) throws IOException {
        this.routeHandlerMap = new HashMap<>();
        this.server = HttpServer.create(new InetSocketAddress(port), backlog);
        this.port = port;
        this.executor = Executors.newVirtualThreadPerTaskExecutor();
    }

    public static String getPrefixUntilWildcard(String input, String wildcardPattern) {
        // Escape special characters in the wildcard pattern
        String regexPattern = wildcardPattern.replace("*", "\\*");

        // Find the index of the first wildcard
        int wildcardIndex = regexPattern.indexOf("*");

        // If wildcard is not found, return the input string
        if (wildcardIndex == -1) {
            return input;
        }

        // Return the substring of the input string up to the first wildcard
        return input.substring(0, wildcardIndex - 1);
    }


    public void get(String route, BiConsumer<HttpRequest, HttpResponse> handler) {
        Pattern pattern = Pattern.compile("\\{([a-zA-Z]+):(int|str)}");
        Matcher matcher = pattern.matcher(route);
        ArrayList<String> pathParams = new ArrayList<>();
        String regex = null;

        while (matcher.find()) {
            String placeholder = matcher.group(1) + ":" + matcher.group(2);
            if (regex == null) {
                regex = route.replace("{" + placeholder + "}", "*");
            } else {
                regex = regex.replace("{" + placeholder + "}", "*");
            }
            out.println("Route: " + route + "\n" + "Regex: " + regex + "\n");
            pathParams.add(placeholder);
        }
        if (regex != null) {
            route = getPrefixUntilWildcard(route, regex);
        }

        if (routeHandlerMap.containsKey(route)) {
            routeHandlerMap.put(route, routeHandlerMap.get(route).get(handler, pathParams, regex));
        } else {
            routeHandlerMap.put(route, RouteHandler.create(route).get(handler, pathParams, regex));
        }
    }

    public void post(String route, BiConsumer<HttpRequest, HttpResponse> handler) {
        if (routeHandlerMap.containsKey(route)) {
            routeHandlerMap.put(route, routeHandlerMap.get(route).post(handler));
        } else {
            routeHandlerMap.put(route, RouteHandler.create(route).post(handler));
        }
    }

    public void setBaseDir(String path) {
        Config.BASE_DIR = path;
    }

    public void setStaticDir(String path) {
        Config.STATIC_DIR = path;
    }

    public void run(Callback function) {
        routeHandlerMap.keySet().forEach((route) -> server.createContext(route, routeHandlerMap.get(route)));

        server.setExecutor(executor);
        server.start();
        function.function();
    }

    public void run() {
        run(() -> out.println("Server is listening on: http://localhost:" + this.port));
    }
}

