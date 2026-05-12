package com.eclinic.api;

import com.sun.net.httpserver.HttpExchange;
import com.eclinic.dao.UserDAO;
import com.eclinic.models.User;
import java.io.IOException;
import java.util.List;

public class UsersHandler extends BaseHandler {

    public void handle(HttpExchange exchange) throws IOException {
        String method = exchange.getRequestMethod();
        String path = normalizePath(exchange.getRequestURI().getPath());

        if ("OPTIONS".equals(method)) {
            exchange.sendResponseHeaders(204, -1);
            return;
        }

        UserDAO dao = new UserDAO();

        try {
            if ("GET".equals(method)) {
                if (path.startsWith("/api/users/")) {
                    long id = parseId(path, "/api/users/");
                    User user = dao.findById(id);
                    if (user != null) {
                        sendJson(exchange, toJson(user), 200);
                    } else {
                        sendError(exchange, "User not found", 404);
                    }
                } else {
                    List users = dao.findAll();
                    sendJson(exchange, listToJson(users), 200);
                }
            } else if ("POST".equals(method)) {
                String body = readBody(exchange);
                String username = extractString(body, "username");
                String passwordHash = extractString(body, "passwordHash");
                String role = extractString(body, "role");
                String status = extractString(body, "status");

                if (username.length() == 0 || passwordHash.length() == 0 || role.length() == 0) {
                    sendError(exchange, "Invalid user payload", 400);
                    return;
                }
                if (status.length() == 0) {
                    status = "ACTIVE";
                }

                long id = dao.create(username, passwordHash, role, status);
                sendJson(exchange, "{\"id\": " + id + ", \"status\": \"created\"}", 201);
            } else if ("PUT".equals(method)) {
                long id = parseId(path, "/api/users/");
                String body = readBody(exchange);
                String role = extractString(body, "role");
                String status = extractString(body, "status");

                if (role.length() == 0 || status.length() == 0) {
                    sendError(exchange, "Invalid user payload", 400);
                    return;
                }

                boolean updated = dao.update(id, role, status);
                if (updated) {
                    sendJson(exchange, "{\"status\": \"updated\"}", 200);
                } else {
                    sendError(exchange, "User not found", 404);
                }
            } else if ("DELETE".equals(method)) {
                long id = parseId(path, "/api/users/");
                boolean deleted = dao.delete(id);
                if (deleted) {
                    sendJson(exchange, "{\"status\": \"deleted\"}", 200);
                } else {
                    sendError(exchange, "User not found", 404);
                }
            } else {
                sendError(exchange, "Method not allowed", 405);
            }
        } catch (IllegalArgumentException e) {
            sendError(exchange, e.getMessage(), 400);
        } catch (Exception e) {
            sendError(exchange, e.getMessage(), 500);
        }
    }

    private String toJson(User u) {
        if (u == null) return "null";
        return "{" +
            "\"id\": " + u.getId() + ", " +
            "\"username\": \"" + escapeJson(u.getUsername()) + "\", " +
            "\"passwordHash\": \"" + escapeJson(u.getPasswordHash()) + "\", " +
            "\"role\": \"" + escapeJson(u.getRole()) + "\", " +
            "\"status\": \"" + escapeJson(u.getStatus()) + "\", " +
            "\"createdAt\": \"" + escapeJson(u.getCreatedAt()) + "\"" +
            "}";
    }

    private String listToJson(List users) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < users.size(); i++) {
            if (i > 0) sb.append(",");
            sb.append(toJson((User) users.get(i)));
        }
        sb.append("]");
        return sb.toString();
    }

    private String extractString(String json, String key) {
        String search = "\"" + key + "\":\"";
        int idx = json.indexOf(search);
        if (idx == -1) return "";
        int start = idx + search.length();
        int end = json.indexOf("\"", start);
        if (end == -1) return "";
        return json.substring(start, end);
    }

    private String normalizePath(String path) {
        if (path == null || path.isEmpty()) {
            return "";
        }
        String normalized = path.split("\\?")[0];
        while (normalized.length() > 1 && normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        return normalized;
    }

    private long parseId(String path, String prefix) {
        if (!path.startsWith(prefix) || path.length() <= prefix.length()) {
            throw new IllegalArgumentException("Missing resource id in path");
        }
        return Long.parseLong(path.substring(prefix.length()));
    }
}
