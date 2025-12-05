import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpExchange;

import java.io.*;
import java.net.InetSocketAddress;
import java.sql.*;
import java.util.stream.Collectors;

public class Main {

    private static final String URL = "jdbc:postgresql://localhost:5432/student";
    private static final String USER = "postgres";
    private static final String PASS = "tajul123";

    public static void main(String[] args) throws Exception {
        // Start HTTP server on port 5050
        HttpServer server = HttpServer.create(new InetSocketAddress(5050), 0);

        // GET endpoint
        server.createContext("/hello", (exchange) -> {
            if (!exchange.getRequestMethod().equalsIgnoreCase("GET")) {
                sendResponse(exchange, "Only GET allowed", 405);
                return;
            }
            sendResponse(exchange, "Welcome to my page!", 200);
        });

        // POST endpoint to add student
        server.createContext("/add-student", (exchange) -> {
            if (!exchange.getRequestMethod().equalsIgnoreCase("POST")) {
                sendResponse(exchange, "Only POST allowed", 405);
                return;
            }

            String body = new BufferedReader(new InputStreamReader(exchange.getRequestBody()))
                    .lines().collect(Collectors.joining());

            String name = extractName(body);
            if (name == null || name.isEmpty()) {
                sendResponse(exchange, "Invalid JSON", 400);
                return;
            }

            try (Connection conn = DriverManager.getConnection(URL, USER, PASS)) {
                PreparedStatement ps = conn.prepareStatement("INSERT INTO students(name) VALUES(?)");
                ps.setString(1, name);
                ps.executeUpdate();
                sendResponse(exchange, "Student saved", 200);
            } catch (Exception e) {
                sendResponse(exchange, "DB error: " + e.getMessage(), 500);
            }
        });

        server.start();
        System.out.println("Server running on port 5050");
    }

    // Simple JSON parser for {"name":"..."}
    private static String extractName(String json) {
        try {
            json = json.trim();
            json = json.replace("{", "").replace("}", "").trim();
            String[] parts = json.split(":");
            if (parts.length == 2) {
                return parts[1].trim().replace("\"", "");
            }
        } catch (Exception ignored) {}
        return null;
    }

    // Helper to send HTTP response
    private static void sendResponse(HttpExchange exchange, String response, int status) throws IOException {
        exchange.getResponseHeaders().add("Content-Type", "text/plain; charset=UTF-8");
        exchange.sendResponseHeaders(status, response.getBytes().length);
        OutputStream os = exchange.getResponseBody();
        os.write(response.getBytes());
        os.close();
    }
}
