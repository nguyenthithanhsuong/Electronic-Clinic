package com.eclinic.api;

import com.sun.net.httpserver.HttpExchange;
import com.eclinic.dao.ReceptionistDAO;
import com.eclinic.dao.UserDAO;
import com.eclinic.models.Receptionist;
import com.eclinic.models.User;
import java.io.IOException;
import java.util.List;

public class ReceptionistsHandler extends BaseHandler {

    protected void handleRequest(HttpExchange exchange) throws IOException {
        String method = exchange.getRequestMethod();
        String path = exchange.getRequestURI().getPath();
        String query = exchange.getRequestURI().getQuery();

        // RBAC: write operations require ADMIN
        if (!"GET".equals(method)) {
            if (!requireRole(exchange, "ADMIN")) return;
        }

        ReceptionistDAO dao = new ReceptionistDAO();

        try {
            if ("GET".equals(method)) {
                if (path.matches("/api/receptionists/\\d+")) {
                    long id = Long.parseLong(path.substring(19));
                    Receptionist receptionist = dao.findById(id);
                    if (receptionist != null) {
                        String json = toJson(receptionist);
                        sendJson(exchange, json, 200);
                    } else {
                        sendError(exchange, "Receptionist not found", 404);
                    }
                } else {
                    List allReceptionists = dao.findAll();
                    String json = listToJson(allReceptionists);
                    sendJson(exchange, json, 200);
                }
            } else if ("POST".equals(method)) {
                String body = readBody(exchange);
                Long requestedUserId = extractNullableLong(body, "userId");
                String username = extractString(body, "username");
                String password = extractString(body, "password");
                String fullName = extractString(body, "fullName");
                String department = extractString(body, "department");
                String phone = extractString(body, "phone");
                String email = extractString(body, "email");

                if (fullName.length() == 0 || phone.length() == 0 ) {
                    sendError(exchange, "Invalid receptionist payload", 400);
                    return;
                }

                long userId = resolveOrCreateReceptionistUserId(requestedUserId, username, password, fullName, department, email);

                long id = dao.create(userId, fullName, department, phone, email);
                String json = "{\"id\": " + id + ", \"status\": \"created\"}";
                sendJson(exchange, json, 201);
            } else if ("PUT".equals(method)) {
                long id = Long.parseLong(path.substring(19));
                String body = readBody(exchange);
                String username = extractString(body, "username");
                String password = extractString(body, "password");
                String fullName = extractString(body, "fullName");
                String department = extractString(body, "department");
                String email = extractString(body, "email");
                String phone = extractString(body, "phone");

                ReceptionistDAO daoLocal = new ReceptionistDAO();
                Receptionist receptionist = daoLocal.findById(id);
                if (receptionist == null) {
                    sendError(exchange, "Receptionist not found", 404);
                    return;
                }

                boolean updated = daoLocal.update(id, fullName, department, email, phone);
                if (!updated) {
                    sendError(exchange, "Failed to update receptionist", 500);
                    return;
                }

                Long userId = daoLocal.getUserIdForReceptionist(id);
                if (userId != null) {
                    UserDAO userDAO = new UserDAO();
                    if (username != null && username.length() > 0 && password != null && password.length() > 0) {
                        userDAO.updateUsernameAndPassword(userId, username, password);
                    } else if (username != null && username.length() > 0) {
                        userDAO.updateUsername(userId, username);
                    } else if (password != null && password.length() > 0) {
                        userDAO.updatePassword(userId, password);
                    }
                }

                sendJson(exchange, "{\"status\": \"updated\"}", 200);
            } else if ("DELETE".equals(method)) {
                long id = Long.parseLong(path.substring(19));
                boolean deleted = dao.delete(id);
                if (deleted) {
                    sendJson(exchange, "{\"status\": \"deleted\"}", 200);
                } else {
                    sendError(exchange, "Receptionist not found", 404);
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

    private String toJson(Receptionist r) {
        if (r == null) return "null";
        return "{" +
            "\"id\": " + r.getId() + ", " +
            "\"userId\": " + r.getUserId() + ", " +
            "\"username\": \"" + escapeJson(r.getUsername()) + "\", " +
            "\"fullName\": \"" + escapeJson(r.getFullName()) + "\", " +
            "\"department\": \"" + escapeJson(r.getDepartment()) + "\", " +
            "\"phone\": \"" + escapeJson(r.getPhone()) + "\", " +
            "\"email\": \"" + escapeJson(r.getEmail()) + "\", " +
            "\"createdAt\": \"" + escapeJson(r.getCreatedAt()) + "\"" +
            "}";
    }

    private String listToJson(List receptionists) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < receptionists.size(); i++) {
            if (i > 0) sb.append(",");
            sb.append(toJson((Receptionist) receptionists.get(i)));
        }
        sb.append("]");
        return sb.toString();
    }

    private Long extractNullableLong(String json, String key) {
        String search = "\"" + key + "\":";
        int idx = json.indexOf(search);
        if (idx == -1) return null;
        int start = idx + search.length();
        int end = json.indexOf(",", start);
        if (end == -1) end = json.indexOf("}", start);
        if (end == -1) throw new IllegalArgumentException("Invalid JSON payload");

        String raw = json.substring(start, end).trim();
        if (raw.length() == 0 || "null".equals(raw)) {
            return null;
        }
        if (raw.startsWith("\"") && raw.endsWith("\"") && raw.length() >= 2) {
            raw = raw.substring(1, raw.length() - 1).trim();
        }
        if (raw.length() == 0) {
            return null;
        }
        try {
            return Long.valueOf(Long.parseLong(raw));
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid userId");
        }
    }

    private long resolveOrCreateReceptionistUserId(Long requestedUserId, String username, String password, String fullName, String department, String email) throws Exception {
        UserDAO userDAO = new UserDAO();

        if (requestedUserId != null && requestedUserId.longValue() > 0) {
            User existing = userDAO.findById(requestedUserId.longValue());
            if (existing == null) {
                throw new IllegalArgumentException("userId does not exist");
            }
            if (!"RECEPTIONIST".equals(existing.getRole())) {
                throw new IllegalArgumentException("userId must belong to a RECEPTIONIST user");
            }
            return requestedUserId.longValue();
        }

        String resolvedUsername = isBlank(username) ? generateUsername("recept", fullName, email) : username;
        String resolvedPassword = isBlank(password) ? "TEMP_HASH" : password;
        return userDAO.create(resolvedUsername, resolvedPassword, "RECEPTIONIST", "ACTIVE");
    }

    private String generateUsername(String prefix, String fullName, String fallback) {
        String base = fallback;
        if (base == null || base.trim().length() == 0) {
            base = fullName;
        }
        if (base == null || base.trim().length() == 0) {
            base = prefix;
        }

        String normalized = base.toLowerCase();
        int at = normalized.indexOf('@');
        if (at > 0) {
            normalized = normalized.substring(0, at);
        }
        normalized = normalized.replaceAll("[^a-z0-9]", "");
        if (normalized.length() == 0) {
            normalized = prefix;
        }
        return prefix + "_" + normalized + "_" + System.currentTimeMillis();
    }

    private String extractString(String json, String key) {
    String search = "\"" + key + "\"";
    int idx = json.indexOf(search);
    if (idx == -1) return "";
    int colon = json.indexOf(":", idx + search.length());
    if (colon == -1) return "";
    int quote1 = json.indexOf("\"", colon + 1);
    if (quote1 == -1) return "";
    int quote2 = json.indexOf("\"", quote1 + 1);
    if (quote2 == -1) return "";
    return json.substring(quote1 + 1, quote2);
}

    private boolean isBlank(String value) {
        return value == null || value.trim().length() == 0;
    }
}
