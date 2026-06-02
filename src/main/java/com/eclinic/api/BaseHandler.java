package com.eclinic.api;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.eclinic.util.JwtUtil;
import java.io.IOException;
import java.io.ByteArrayOutputStream;
import java.io.OutputStream;

public abstract class BaseHandler implements HttpHandler {

    private void setCorsHeaders(HttpExchange exchange) {
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        exchange.getResponseHeaders().set("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
        exchange.getResponseHeaders().set("Access-Control-Allow-Headers", "Content-Type, Authorization");
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        setCorsHeaders(exchange);
        if ("OPTIONS".equals(exchange.getRequestMethod())) {
            exchange.sendResponseHeaders(204, -1);
            return;
        }
        // Authentication check — override requiresAuth() to return false for public endpoints
        if (requiresAuth()) {
            Long userId = getAuthUserId(exchange);
            if (userId == null) {
                sendError(exchange, "Unauthorized — token không hợp lệ hoặc đã hết hạn", 401);
                return;
            }
        }
        handleRequest(exchange);
    }

    /** Override to return false for endpoints that don't require authentication (e.g. login). */
    protected boolean requiresAuth() {
        return true;
    }

    /** Extract authenticated user ID from Authorization header. Returns null if invalid. */
    protected Long getAuthUserId(HttpExchange exchange) {
        String authHeader = exchange.getRequestHeaders().getFirst("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return JwtUtil.verifyToken(authHeader.substring(7));
        }
        return null;
    }

    /** Extract authenticated user role from Authorization header. Returns null if invalid. */
    protected String getAuthRole(HttpExchange exchange) {
        String authHeader = exchange.getRequestHeaders().getFirst("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return JwtUtil.extractRole(authHeader.substring(7));
        }
        return null;
    }

    protected abstract void handleRequest(HttpExchange exchange) throws IOException;

    protected void sendJson(HttpExchange exchange, String json, int statusCode) throws IOException {
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        byte[] response = json.getBytes("UTF-8");
        exchange.sendResponseHeaders(statusCode, response.length);
        OutputStream os = exchange.getResponseBody();
        os.write(response);
        os.close();
    }

    protected void sendError(HttpExchange exchange, String message, int statusCode) throws IOException {
        String json = "{\"error\": \"" + escapeJson(message) + "\"}";
        sendJson(exchange, json, statusCode);
    }

    protected String escapeJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

    protected String readBody(HttpExchange exchange) throws IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        byte[] chunk = new byte[1024];
        int read;
        while ((read = exchange.getRequestBody().read(chunk)) != -1) {
            buffer.write(chunk, 0, read);
        }
        return new String(buffer.toByteArray(), "UTF-8");
    }
}
