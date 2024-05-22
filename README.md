**Amberj Net: A Beginner-Friendly Java Web Framework (Alpha Build 20)**

Amberj Net is a lightweight Java web framework inspired by Go templating and Django, designed to make writing server-side code easier, especially for beginners. It provides a simple and intuitive API for defining routes, handling requests, and rendering templates.

**Features:**

- **Simple Route Definition:** Define routes using familiar HTTP methods (`get`, `post`, `put`, `delete`).
- **Template Engine:** Inspired by Django templating, Amberj Net allows embedding dynamic content within templates.
- **Request Handling:** Access request parameters (path, body, headers) for processing data.
- **Response Rendering:** Render templates with data objects for dynamic content generation.

**Getting Started:**

1. **Add Dependency (placeholder, assuming manual download):**
   Download the Amberj Net library and include it in your project's classpath.

2. **Create a Server:**

   ```java
   import com.amberj.net.httpserver.Server;

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

   server.post("/users", (request, response) -> {
       // Access request data (form data, JSON body, etc.)
       String username = request.getParam("username");
       String email = request.getParam("email");
       // ... process data and generate response

       // Render a template with user data
       User user = new User(username, email);
       response.render("user_profile", user);
   });
   ```

4. **Templates (using DjangoTemplating class - not included):**

   Create template files (e.g., `.html`) under `resouces/templates` in a maven project. Use Django-like syntax for variable inclusion:

   ```html
   <h1>Hello, {{ username }}!</h1>
   ```

   In your code, provide the data object to render the template:

   ```java
   User user = new User("John Doe", "john.doe@example.com");
   response.render("user_profile", user);
   ```

   The `render` method would parse the template, replacing `{{ username }}` with the user's name from the `user` object.

**Additional Notes:**

- Amberj Net is currently in alpha stage (build 20). Features and functionalities might change in future releases.
- Error handling and advanced functionalities are not covered in this basic example.

**Example: Sending Response with Object and Path Parameters:**

```java
server.get("/users/:id", (request, response) -> {
    String userId = request.getPathParam("id"); // Access path parameter
    User user = // ... fetch user data from database based on ID

    response.render("user_profile", user);
});
```

**Further Development:**

Amberj Net is under active development. Here's a glimpse into potential future features:

- Built-in template engine for a more integrated experience.
- Automatic content negotiation (JSON, HTML).
- Database integration for data persistence.
- Session management.

We encourage you to contribute to the project's development to make it even better for beginners!
