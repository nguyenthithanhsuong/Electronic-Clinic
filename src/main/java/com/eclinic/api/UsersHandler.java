package com.eclinic.api;

import com.sun.net.httpserver.HttpExchange;
import com.eclinic.dao.UserDAO;
import com.eclinic.models.User;
import com.eclinic.util.PasswordUtil;
import java.io.IOException;
import java.util.List;

public class UsersHandler extends BaseHandler {

    /** All user management operations require authentication.
     *  Role checking is done per-method in handleRequest(). */
    @Override
    protected boolean requiresAuth() {
        return true;
    }

    protected void handleRequest(HttpExchange exchange) throws IOException {
        String method = exchange.getRequestMethod();
        String path = normalizePath(exchange.getRequestURI().getPath());

        // All user management requires ADMIN role
        if (!requireRole(exchange, "ADMIN")) return;

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
                    List<User> users = dao.findAll();
                    sendJson(exchange, listToJson(users), 200);
                }
            } else if ("POST".equals(method)) {
                String body = readBody(exchange);
                String username = extractString(body, "username");
                String password = extractString(body, "password");
                String role = extractString(body, "role");
                String status = extractString(body, "status");

                if (username.length() == 0 || password.length() == 0 || role.length() == 0) {
                    sendError(exchange, "Invalid user payload", 400);
                    return;
                }
                role = role.toUpperCase();
                if (!isValidRole(role)) {
                    sendError(exchange, "Invalid role: must be one of [ADMIN, DOCTOR, RECEPTIONIST, PATIENT]", 400);
                    return;
                }
                if (status.length() == 0) {
                    status = "ACTIVE";
                }
                status = status.toUpperCase();
                if (!isValidStatus(status)) {
                    sendError(exchange, "Invalid status: must be one of [ACTIVE, INACTIVE, BLOCKED]", 400);
                    return;
                }

                // Hash password with bcrypt before storing
                String hashedPassword = PasswordUtil.hash(password);
                long id = dao.create(username, hashedPassword, role, status);
                sendJson(exchange, toJson(dao.findById(id)), 201);
            } else if ("PUT".equals(method)) {
                long id = parseId(path, "/api/users/");
                String body = readBody(exchange);
                String role = extractString(body, "role");
                String status = extractString(body, "status");
                String password = extractString(body, "password");
                User existing = dao.findById(id);
                if (existing == null) {
                    sendError(exchange, "User not found", 404);
                    return;
                }
                if (role.length() == 0) {
                    role = existing.getRole();
                } else {
                    role = role.toUpperCase();
                    if (!isValidRole(role)) {
                        sendError(exchange, "Invalid role: must be one of [ADMIN, DOCTOR, RECEPTIONIST, PATIENT]", 400);
                        return;
                    }
                }
                if (status.length() == 0) {
                    status = existing.getStatus();
                } else {
                    status = status.toUpperCase();
                    if (!isValidStatus(status)) {
                        sendError(exchange, "Invalid status: must be one of [ACTIVE, INACTIVE, BLOCKED]", 400);
                        return;
                    }
                }

                // Update role + status
                boolean updated = dao.update(id, role, status);

                // Update password if provided
                if (password.length() > 0) {
                    String hashedPassword = PasswordUtil.hash(password);
                    dao.updatePassword(id, hashedPassword);
                }

                if (updated) {
                    sendJson(exchange, toJson(dao.findById(id)), 200);
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

    /** Serialize user to JSON — passwordHash is NEVER included in responses. */
    private String toJson(User u) {
        if (u == null) return "null";
        return "{" +
            "\"id\": " + u.getId() + ", " +
            "\"username\": \"" + escapeJson(u.getUsername()) + "\", " +
            "\"role\": \"" + escapeJson(u.getRole()) + "\", " +
            "\"status\": \"" + escapeJson(u.getStatus()) + "\", " +
            "\"createdAt\": \"" + escapeJson(u.getCreatedAt()) + "\"" +
            "}";
    }

    private String listToJson(List<User> users) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < users.size(); i++) {
            if (i > 0) sb.append(",");
            sb.append(toJson(users.get(i)));
        }
        sb.append("]");
        return sb.toString();
    }

    private boolean isValidRole(String role) {
        return "ADMIN".equals(role) || "DOCTOR".equals(role) || "RECEPTIONIST".equals(role) || "PATIENT".equals(role);
    }

    private boolean isValidStatus(String status) {
        return "ACTIVE".equals(status) || "INACTIVE".equals(status) || "BLOCKED".equals(status);
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
