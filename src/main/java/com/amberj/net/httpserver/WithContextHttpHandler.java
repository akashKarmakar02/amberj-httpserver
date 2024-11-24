package com.amberj.net.httpserver;

import com.amberj.net.http.HttpRequest;
import com.amberj.net.http.HttpResponse;

public interface WithContextHttpHandler<T> {
    default void get(HttpRequest request, HttpResponse response, T context) {
        response.methodNotAllowed();
    }

    default void post(HttpRequest request, HttpResponse response, T context) {
        response.methodNotAllowed();
    }

    default void delete(HttpRequest request, HttpResponse response, T context) {
        response.methodNotAllowed();
    }

    default void put(HttpRequest request, HttpResponse response, T context) {
        response.methodNotAllowed();
    }

    default void patch(HttpRequest request, HttpResponse response, T context) { response.methodNotAllowed(); }
}
