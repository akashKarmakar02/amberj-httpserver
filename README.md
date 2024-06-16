# Amberj HttpServer: A Beginner-Friendly Java Web Framework

Amberj HttpServer is a lightweight Java web framework inspired by Go and Django templating, designed to make writing server-side code easier and modular, especially for beginners. It provides a simple and intuitive API for defining routes, handling requests, and rendering templates.

## Features

- **Simple Route Definition:** Define routes using familiar HTTP methods (`get`, `post`, `put`, `delete`, `patch`).
- **Template Engine:** Inspired by Django templating, allows embedding dynamic content within templates.
- **Request Handling:** Access request parameters (path, body, headers) for processing data.
- **Response Rendering:** Render templates with data objects for dynamic content generation.
- **Automatic JSON Conversion:** Converts data objects to JSON using the `com.fasterxml.jackson` library.

## Getting Started

### 1. Add Dependency

Add the Amberj HttpServer library in your `pom.xml`:

```xml
<repositories>
    <repository>
        <id>jitpack.io</id>
        <url>https://jitpack.io</url>
    </repository>
</repositories>

<dependency>
    <groupId>com.github.akashKarmakar02</groupId>
    <artifactId>amberj</artifactId>
    <version>0.1.0-BETA-02</version>
</dependency>
```

### 2. Create a Server

Create a simple server in your `Main` class:

```java
import com.amberj.net.httpserver.Server;
import com.amberj.net.template.Data;
import java.io.IOException;

public class Main {
    public static void main(String[] args) throws IOException {
        var server = new Server(8000);
        // Define routes and run the server
        server.run();
    }
}
```

### 3. Define Routes

Define routes using the server instance:

```java
server.get("/", (request, response) -> {
    response.render("index"); // Render the "index.html" template
});

server.post("/users", (request, response) -> {
    var body = request.body();
    String username = body.get("username");
    String email = body.get("email");

    response.render("user_profile", new Data()
            .with("username", username)
            .with("email", email));
});
```

### 4. Templates

Create template files (e.g., `.html`) under `resources/templates` in a Maven project using Django-like syntax:

```html
<h1>Hello, {{ username }}!</h1>
```

Provide the data object to render the template:

```java
User user = new User("John Doe", "john.doe@example.com");
response.render("user_profile", new Data().with("user", user));
```

### Example: Sending Response with Object and Path Parameters

```java
server.get("/users/{id}", (request, response) -> {
    String userId = request.pathParams().get("id");
    User user = // fetch user data from database based on ID

    response.render("user_profile", new Data().with("user", user));
});
```

### Example: Handling PATCH Request

```java
server.patch("/users/{id}", (request, response) -> {
    String userId = request.pathParams().get("id");
    var body = request.body();
    String email = body.get("email");

    // Update user email in database based on ID
    User updatedUser = // update user logic

    response.render("user_profile", new Data().with("user", updatedUser));
});
```

### Example: Using Custom Request Handlers

You can define custom request handlers by implementing the `HttpHandler` interface:

```java
import com.amberj.net.http.HttpRequest;
import com.amberj.net.http.HttpResponse;
import com.amberj.net.httpserver.HttpHandler;

public class FormHandler implements HttpHandler {

    @Override
    public void get(HttpRequest request, HttpResponse response) {
        response.render("form");
    }

    @Override
    public void post(HttpRequest request, HttpResponse response) {
        var body = request.body();
        //some processing
        
        response.redirect("/");
    }
}

import com.amberj.net.httpserver.Server;

import java.io.IOException;

public class Main {
    public static void main(String[] args) throws IOException {
        var server = new Server(3000);

        server.handle("/", new FormHandler());

        server.run();
    }
}
```

## Additional Notes

- Amberj HttpServer is currently in beta stage (version 0.1.0-BETA-02). Features and functionalities might change in future releases.
- Error handling and advanced functionalities are not covered in this basic example.

## Further Development

Amberj HttpServer is under active development. Potential future features include:

- Middleware support
- Database integration for data persistence
- Session management

We encourage you to contribute to the project's development to make it even better for beginners!

## Contributing

Feel free to fork this repository and submit pull requests. Contributions are welcome!

## License

Amberj HttpServer is licensed under the GPL-3.0 License. See the [LICENSE](LICENSE) file for more details.
