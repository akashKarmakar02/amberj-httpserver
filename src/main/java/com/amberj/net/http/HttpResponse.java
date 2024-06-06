package com.amberj.net.http;

import com.amberj.net.template.Data;
import com.amberj.net.template.JinjavaTemplating;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.*;
import java.util.Collections;
import java.util.Objects;


public class HttpResponse {

    private String response;
    private int status;
    private String redirectURL;
    private final JinjavaTemplating templatingEngine;
    private static FileSystem jarFileSystem;
    private String contentType;
    private boolean isMethodAllowed;

    public HttpResponse() {
        templatingEngine = new JinjavaTemplating();
        status = 200;
        this.isMethodAllowed = true;
    }

    public void render(String template, Data data) {
        String filePath = template + ".html";
        contentType = "text/html";

        try {
            var html = getFileContent("templates", filePath);
            if (!Objects.equals(html, "")) {
                html = templatingEngine.parse(html, data.getContext());

                this.response = html;
            } else {
                this.response = "<h1>Template name is invalid " + filePath + " </h1>";
            }

        } catch (URISyntaxException | IOException e) {
            this.response = "<h1>Template name is invalid " + filePath + " </h1>";
        }
    }

    String getFileContent(String basePath, String fileName) throws URISyntaxException, IOException {
        URL url = getClass().getResource("/" + basePath + "/" + fileName);
        String content = "";
        if (url != null) {
            URI uri = url.toURI();
            Path path;
            if (uri.getScheme().equals("jar")) {
                if (jarFileSystem == null || !jarFileSystem.isOpen()) {
                    try {
                        jarFileSystem = FileSystems.newFileSystem(uri, Collections.emptyMap());
                    } catch (FileSystemAlreadyExistsException e) {
                        jarFileSystem = FileSystems.getFileSystem(uri);
                    }
                }
                path = jarFileSystem.getPath("/" + basePath + "/" + fileName);
            } else {
                path = Paths.get(uri);
            }
            content = Files.readString(path);
        }

        return content;
    }

    public void render(String template) {
        String filePath = template + ".html";
        contentType = "text/html";

        try {
            var html = getFileContent("templates", filePath);
            if (!Objects.equals(html, "")) {
                this.response = html;
            } else {
                this.response = "<h1>Template name is invalid " + filePath + " </h1>";
            }
        } catch (URISyntaxException | IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void json(Data data) {
        contentType = "application/json";
        response = data.toJson();
    }

    public void write(String response) {
        contentType = "text/html";
        this.response = response;
    }

    @SuppressWarnings("unused")
    public HttpResponse status(int status) {
        this.status = status;
        return this;
    }

    public String getResponse() {
        return response;
    }

    @SuppressWarnings("unused")
    public int getStatus() {
        return status;
    }

    public boolean isMethodAllowed() {
        return isMethodAllowed;
    }

    public String getContentType() {
        return this.contentType;
    }

    public void redirect(String url) {
        this.redirectURL = url;
    }

    public String getRedirectURL() {
        return this.redirectURL;
    }

    public void methodNotAllowed() {
        this.isMethodAllowed = false;
    }
}
