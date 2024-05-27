**Amberj HttpServer: A Beginner-Friendly Java Web Framework (Alpha Build 32)**

Amberj HttpServer is a lightweight Java web framework inspired by Go and Django templating, designed to make writing server-side code easier and modular, especially for beginners. It provides a simple and intuitive API for defining routes, handling requests, and rendering templates.

**Features:**

- **Simple Route Definition:** Define routes using familiar HTTP methods (`get`, `post`, `put`, `delete`).
- **Template Engine:** Inspired by Django templating, Amberj HttpServer allows embedding dynamic content within templates.
- **Request Handling:** Access request parameters (path, body, headers) for processing data.
- **Response Rendering:** Render templates with data objects for dynamic content generation.
- **Automatic JSON Conversion:** Converts the data object passed to response to json using `com.fasterxml.jackson` library.

**Getting Started:**

1. **Add Dependency:**
   Add the Amberj HttpServer library in your `pom.xml`.
   ```
   <dependency>
      <groupId>com.amberj</groupId>
      <artifactId>httpserver</artifactId>
      <version>0.1.0-alpha-BUILD32</version>
   </dependency>
   ```

2. **Create a Server:**

   ```java
   import com.amberj.net.httpserver.Server;
   import com.amberj.net.template.Data;

   import java.io.IOException;

   public class Main {
       public static void main(String[] args) throws IOException {
           var server = new Server(8000);
           // ... define routes and run the server
   
            server.run();
       }
   }
   ```

3. **Define Routes:**

   ```java
   server.get("/", (request, response) -> {
       response.render("index"); // Render the "index.html" template
   });

   // Only x-www-form data support currently
   server.post("/users", (request, response) -> {
       // Access request data (form data, JSON body, etc.)
       var body = request.body();
       String username = body.get("username");
       String email = body.get("email");
       // ... process data and generate response

       response.render("user_profile", new Data()
               .with("username", username)
               .with("email", email));
   });
   ```

4. **Templates (using JinJava Templating (Jinja2 templating for java)):**

   Create template files (e.g., `.html`) under `resouces/templates` in a maven project. Use Django-like syntax for variable inclusion:

   ```html
   <h1>Hello, {{ username }}!</h1>
   ```

   In your code, provide the data object to render the template:

   ```java
   User user = new User("John Doe", "john.doe@example.com");
   response.render("user_profile", new Data()
           .with("user", user));
   ```

   The `render` method would parse the template, replacing `{{ username }}` with the user's name from the `user` object.

**Additional Notes:**

- Amberj Net is currently in alpha stage (build 32). Features and functionalities might change in future releases.
- Error handling and advanced functionalities are not covered in this basic example.

**Example: Sending Response with Object and Path Parameters:**

```java
server.get("/users/{id}", (request, response) -> {
    String userId = request.pathParams().get("id"); // Access path parameter
    User user = // ... fetch user data from database based on ID

    response.render("user_profile", new Data()
            .with("user", user));
});
```

**Further Development:**

Amberj HttpServer is under active development. Here's a glimpse into potential future features:

- Built-in template engine for a more integrated experience.
- Database integration for data persistence.
- Session management.

We encourage you to contribute to the project's development to make it even better for beginners!
