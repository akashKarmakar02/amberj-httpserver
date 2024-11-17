package com.amberj.net.http;

import com.amberj.net.template.Data;
import com.amberj.net.template.TemplateEngine;
import com.dslplatform.json.NonNull;
import com.sun.net.httpserver.HttpExchange;

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
    private final TemplateEngine templatingEngine;
    private static FileSystem jarFileSystem;
    private String contentType;
    private boolean isMethodAllowed;
    private HttpExchange exchange;

    public HttpResponse(HttpExchange exchange) {
        templatingEngine = new TemplateEngine();
        status = 200;
        this.isMethodAllowed = true;
        this.exchange = exchange;
    }

    /**
     * This function gets a <code>.html</code> file from the resources of classpath
     * and parses it through a template parser.
     * Which in this case is <code>PebbleEngine</code> which is a very light-weight template parser
     * with similar syntax to <code>Django Template Engine</code>.
     *
     * @param template name of the template file
     * @param data data which can be accessed in template
     */
    public void render(@NonNull String template, Data data) {
        String filePath = template + ".html";
        contentType = "text/html";

        try {
            this.response = templatingEngine.parse("templates/"+filePath, data.getContext());
        } catch (IOException e) {
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

    /**
     * The template which is in resources/templates directory
     * @param template the html file name
     */
    public void render(String template) {
        String filePath = template + ".html";
        contentType = "text/html";

        try {
            this.response = templatingEngine.parse("templates/"+filePath, null);
        } catch (IOException e) {
            this.response = "<h1>Template name is invalid " + filePath + " </h1>";
        }
    }

    public void json(Data data) {
        contentType = "application/json";
        response = data.toJson();
    }

    /**
     * Writes the given string into the output buffer
     * @param response
     */
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
        return !isMethodAllowed;
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

    public HttpExchange getExchange() {
        return this.exchange;
    }
}
