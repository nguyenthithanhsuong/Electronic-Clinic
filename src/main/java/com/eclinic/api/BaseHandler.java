package com.eclinic.api;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
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
        handleRequest(exchange);
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
