package com.eclinic.api;

import com.sun.net.httpserver.HttpExchange;
import com.eclinic.database.ConnectionManager;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class NotificationsHandler extends BaseHandler {

    protected void handleRequest(HttpExchange exchange) throws IOException {
        String method = exchange.getRequestMethod();
        String path = normalizePath(exchange.getRequestURI().getPath());
        String query = exchange.getRequestURI().getQuery();

        try {
            if ("GET".equals(method)) {
                if (query != null && query.contains("userId=")) {
                    long userId = parseQueryLong(query, "userId");
                    handleGetByUser(exchange, userId);
                } else {
                    handleGetAll(exchange);
                }
            } else if ("POST".equals(method)) {
                if (path.endsWith("/read-all")) {
                    long userId = 0;
                    if (query != null && query.contains("userId=")) {
                        userId = parseQueryLong(query, "userId");
                    }
                    handleMarkAllRead(exchange, userId);
                } else {
                    handleCreate(exchange);
                }
            } else if ("PUT".equals(method)) {
                long id = parseId(path, "/api/notifications/");
                handleMarkRead(exchange, id);
            } else {
                sendError(exchange, "Method not allowed", 405);
            }
        } catch (Exception e) {
            sendError(exchange, e.getMessage(), 500);
        }
    }

    private void handleGetAll(HttpExchange exchange) throws Exception {
        String sql = "SELECT id, user_id, type, title, message, is_read, created_at FROM notifications ORDER BY created_at DESC LIMIT 50";
        Connection conn = ConnectionManager.getConnection();
        try {
            PreparedStatement stmt = conn.prepareStatement(sql);
            ResultSet rs = stmt.executeQuery();
            sendJson(exchange, resultSetToJson(rs), 200);
        } finally {
            ConnectionManager.closeConnection(conn);
        }
    }

    private void handleGetByUser(HttpExchange exchange, long userId) throws Exception {
        String sql = "SELECT id, user_id, type, title, message, is_read, created_at FROM notifications WHERE user_id = ? ORDER BY created_at DESC LIMIT 50";
        Connection conn = ConnectionManager.getConnection();
        try {
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setLong(1, userId);
            ResultSet rs = stmt.executeQuery();
            sendJson(exchange, resultSetToJson(rs), 200);
        } finally {
            ConnectionManager.closeConnection(conn);
        }
    }

    private void handleCreate(HttpExchange exchange) throws Exception {
        String body = readBody(exchange);
        long userId = extractLong(body, "userId");
        String type = extractString(body, "type");
        String title = extractString(body, "title");
        String message = extractString(body, "message");

        String sql = "INSERT INTO notifications (user_id, type, title, message) VALUES (?, ?, ?, ?) RETURNING id";
        Connection conn = ConnectionManager.getConnection();
        try {
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setLong(1, userId);
            stmt.setString(2, type);
            stmt.setString(3, title);
            stmt.setString(4, message);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                sendJson(exchange, "{\"id\": " + rs.getLong(1) + ", \"status\": \"created\"}", 201);
            }
        } finally {
            ConnectionManager.closeConnection(conn);
        }
    }

    private void handleMarkRead(HttpExchange exchange, long id) throws Exception {
        String sql = "UPDATE notifications SET is_read = TRUE WHERE id = ?";
        Connection conn = ConnectionManager.getConnection();
        try {
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setLong(1, id);
            int rows = stmt.executeUpdate();
            if (rows > 0) {
                sendJson(exchange, "{\"status\": \"read\"}", 200);
            } else {
                sendError(exchange, "Notification not found", 404);
            }
        } finally {
            ConnectionManager.closeConnection(conn);
        }
    }

    private void handleMarkAllRead(HttpExchange exchange, long userId) throws Exception {
        String sql = userId > 0
            ? "UPDATE notifications SET is_read = TRUE WHERE user_id = ? AND is_read = FALSE"
            : "UPDATE notifications SET is_read = TRUE WHERE is_read = FALSE";
        Connection conn = ConnectionManager.getConnection();
        try {
            PreparedStatement stmt = conn.prepareStatement(sql);
            if (userId > 0) stmt.setLong(1, userId);
            int rows = stmt.executeUpdate();
            sendJson(exchange, "{\"updated\": " + rows + "}", 200);
        } finally {
            ConnectionManager.closeConnection(conn);
        }
    }

    private String resultSetToJson(ResultSet rs) throws Exception {
        StringBuilder sb = new StringBuilder("[");
        boolean first = true;
        while (rs.next()) {
            if (!first) sb.append(",");
            first = false;
            sb.append("{");
            sb.append("\"id\": ").append(rs.getLong("id")).append(", ");
            sb.append("\"userId\": ").append(rs.getLong("user_id")).append(", ");
            sb.append("\"type\": \"").append(escapeJson(rs.getString("type"))).append("\", ");
            sb.append("\"title\": \"").append(escapeJson(rs.getString("title"))).append("\", ");
            sb.append("\"message\": \"").append(escapeJson(rs.getString("message") != null ? rs.getString("message") : "")).append("\", ");
            sb.append("\"isRead\": ").append(rs.getBoolean("is_read")).append(", ");
            sb.append("\"createdAt\": \"").append(escapeJson(rs.getString("created_at"))).append("\"");
            sb.append("}");
        }
        sb.append("]");
        return sb.toString();
    }

    private long extractLong(String json, String key) {
        String search = "\"" + key + "\":";
        int idx = json.indexOf(search);
        if (idx == -1) return 0;
        int end = json.indexOf(",", idx);
        if (end == -1) end = json.indexOf("}", idx);
        String num = json.substring(idx + search.length(), end).trim();
        try { return Long.parseLong(num); } catch (Exception e) { return 0; }
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
        if (path == null) return "";
        String normalized = path.split("\\?")[0];
        while (normalized.length() > 1 && normalized.endsWith("/"))
            normalized = normalized.substring(0, normalized.length() - 1);
        return normalized;
    }

    private long parseId(String path, String prefix) {
        return Long.parseLong(path.substring(prefix.length()));
    }

    private long parseQueryLong(String query, String key) {
        String[] parts = query.split("&");
        for (String part : parts) {
            String[] kv = part.split("=", 2);
            if (kv.length == 2 && key.equals(kv[0])) return Long.parseLong(kv[1]);
        }
        return 0;
    }
}
