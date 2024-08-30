package com.amberj.net.httpserver;

/**
 * Next function of the middleware chain
 */
@FunctionalInterface
public interface Next {
    void run();
}
