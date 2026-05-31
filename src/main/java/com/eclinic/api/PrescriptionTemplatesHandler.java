package com.eclinic.api;

import com.sun.net.httpserver.HttpExchange;
import com.eclinic.database.ConnectionManager;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

public class PrescriptionTemplatesHandler extends BaseHandler {

    protected void handleRequest(HttpExchange exchange) throws IOException {
        String method = exchange.getRequestMethod();
        String path = normalizePath(exchange.getRequestURI().getPath());
        String query = exchange.getRequestURI().getQuery();

        try {
            if ("GET".equals(method)) {
                if (query != null && query.contains("doctorId=")) {
                    long doctorId = parseQueryLong(query, "doctorId");
                    handleGetByDoctor(exchange, doctorId);
                } else {
                    handleGetAll(exchange);
                }
            } else if ("POST".equals(method)) {
                handleCreate(exchange);
            } else if ("DELETE".equals(method)) {
                long id = parseId(path, "/api/prescription-templates/");
                handleDelete(exchange, id);
            } else {
                sendError(exchange, "Method not allowed", 405);
            }
        } catch (Exception e) {
            sendError(exchange, e.getMessage(), 500);
        }
    }

    private void handleGetAll(HttpExchange exchange) throws Exception {
        String sql = "SELECT id, doctor_id, name, items, created_at FROM prescription_templates ORDER BY created_at DESC";
        Connection conn = ConnectionManager.getConnection();
        try {
            PreparedStatement stmt = conn.prepareStatement(sql);
            ResultSet rs = stmt.executeQuery();
            String json = resultSetToJson(rs);
            sendJson(exchange, json, 200);
        } finally {
            ConnectionManager.closeConnection(conn);
        }
    }

    private void handleGetByDoctor(HttpExchange exchange, long doctorId) throws Exception {
        String sql = "SELECT id, doctor_id, name, items, created_at FROM prescription_templates WHERE doctor_id = ? ORDER BY created_at DESC";
        Connection conn = ConnectionManager.getConnection();
        try {
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setLong(1, doctorId);
            ResultSet rs = stmt.executeQuery();
            String json = resultSetToJson(rs);
            sendJson(exchange, json, 200);
        } finally {
            ConnectionManager.closeConnection(conn);
        }
    }

    private void handleCreate(HttpExchange exchange) throws Exception {
        String body = readBody(exchange);
        long doctorId = extractLong(body, "doctorId");
        String name = extractString(body, "name");
        String items = extractJsonArray(body, "items");

        if (name.isEmpty()) {
            sendError(exchange, "Name is required", 400);
            return;
        }

        String sql = "INSERT INTO prescription_templates (doctor_id, name, items) VALUES (?, ?, ?::jsonb) RETURNING id";
        Connection conn = ConnectionManager.getConnection();
        try {
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setLong(1, doctorId);
            stmt.setString(2, name);
            stmt.setString(3, items.isEmpty() ? "[]" : items);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                sendJson(exchange, "{\"id\": " + rs.getLong(1) + ", \"status\": \"created\"}", 201);
            }
        } finally {
            ConnectionManager.closeConnection(conn);
        }
    }

    private void handleDelete(HttpExchange exchange, long id) throws Exception {
        String sql = "DELETE FROM prescription_templates WHERE id = ?";
        Connection conn = ConnectionManager.getConnection();
        try {
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setLong(1, id);
            int rows = stmt.executeUpdate();
            if (rows > 0) {
                sendJson(exchange, "{\"status\": \"deleted\"}", 200);
            } else {
                sendError(exchange, "Template not found", 404);
            }
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
            sb.append("\"doctorId\": ").append(rs.getLong("doctor_id")).append(", ");
            sb.append("\"name\": \"").append(escapeJson(rs.getString("name"))).append("\", ");
            sb.append("\"items\": ").append(rs.getString("items")).append(", ");
            sb.append("\"createdAt\": \"").append(escapeJson(rs.getString("created_at"))).append("\"");
            sb.append("}");
        }
        sb.append("]");
        return sb.toString();
    }

    private String extractJsonArray(String json, String key) {
        String search = "\"" + key + "\":";
        int idx = json.indexOf(search);
        if (idx == -1) return "[]";
        int start = json.indexOf("[", idx);
        if (start == -1) return "[]";
        int depth = 0;
        for (int i = start; i < json.length(); i++) {
            if (json.charAt(i) == '[') depth++;
            else if (json.charAt(i) == ']') depth--;
            if (depth == 0) return json.substring(start, i + 1);
        }
        return "[]";
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
