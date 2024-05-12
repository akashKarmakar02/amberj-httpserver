package com.amberj.net.http;


import com.amberj.net.template.DjangoTemplating;
import com.amberj.net.Config;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;


public class HttpResponse {

    private String response;
    private int status;
    private String redirectURL;

    private final DjangoTemplating templatingEngine;

    public HttpResponse() {
        templatingEngine = new DjangoTemplating();
    }

    public void render(String template, Object data) {
        String filePath = template + ".html";

        try {
            var html = getClass().getResource("/templates/" + filePath);
            if (html != null) {
                Path path = Paths.get(html.getPath());


                // Set the response
                String templateContent = Files.readString(path);


                templateContent = templatingEngine.parse(templateContent, data);

                // Set the response
                this.response = templateContent;
            } else {
                this.response = "<h1>Template name is invalid " + filePath + " </h1>";
            }

        } catch (IOException e) {
            this.response = "<h1>Template name is invalid " + filePath + " </h1>";
        }
    }

    public void render(String template) {
        String filePath = template + ".html";

        try {
            var html = getClass().getResource("/templates/" + filePath);
            if (html != null) {
                Path path = Paths.get(html.getPath());


                // Set the response
                this.response = Files.readString(path);
            } else {
                this.response = "<h1>Template name is invalid " + filePath + " </h1>";
            }
        } catch (IOException e) {
            this.response = "<h1>Template name is invalid " + filePath + " </h1>";
        }
    }

    public void setResponse(String response) {
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

    public void redirect(String url) {
        this.redirectURL = url;
    }

    public String getRedirectURL() {
        return this.redirectURL;
    }

}
