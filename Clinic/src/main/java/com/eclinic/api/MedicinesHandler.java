package com.eclinic.api;

import com.sun.net.httpserver.HttpExchange;
import com.eclinic.dao.MedicineDAO;
import com.eclinic.models.Medicine;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;

public class MedicinesHandler extends BaseHandler {

    public void handle(HttpExchange exchange) throws IOException {
        String method = exchange.getRequestMethod();
        String path = exchange.getRequestURI().getPath();

        if ("OPTIONS".equals(method)) {
            exchange.sendResponseHeaders(204, -1);
            return;
        }

        MedicineDAO dao = new MedicineDAO();

        try {
            if ("GET".equals(method)) {
                if (path.matches("/api/medicines/\\d+")) {
                    long id = Long.parseLong(path.substring(15));
                    Medicine med = dao.findById(id);
                    if (med != null) {
                        String json = toJson(med);
                        sendJson(exchange, json, 200);
                    } else {
                        sendError(exchange, "Medicine not found", 404);
                    }
                } else {
                    List allMeds = dao.findAll();
                    String json = listToJson(allMeds);
                    sendJson(exchange, json, 200);
                }
            } else if ("POST".equals(method)) {
                String body = readBody(exchange);
                String name = extractString(body, "name");
                String unit = extractString(body, "unit");
                BigDecimal price = new BigDecimal(extractString(body, "price"));
                int stock = Integer.parseInt(extractString(body, "stockQuantity"));
                String expiry = extractString(body, "expiryDate");

                long id = dao.create(name, unit, price, stock, expiry);
                String json = "{\"id\": " + id + ", \"status\": \"created\"}";
                sendJson(exchange, json, 201);
            } else if ("PUT".equals(method)) {
                long id = Long.parseLong(path.substring(15));
                String body = readBody(exchange);
                int newQuantity = Integer.parseInt(extractString(body, "stockQuantity"));

                boolean updated = dao.updateStock(id, newQuantity);
                if (updated) {
                    sendJson(exchange, "{\"status\": \"updated\"}", 200);
                } else {
                    sendError(exchange, "Medicine not found", 404);
                }
            } else if ("DELETE".equals(method)) {
                long id = Long.parseLong(path.substring(15));
                boolean deleted = dao.delete(id);
                if (deleted) {
                    sendJson(exchange, "{\"status\": \"deleted\"}", 200);
                } else {
                    sendError(exchange, "Medicine not found", 404);
                }
            } else {
                sendError(exchange, "Method not allowed", 405);
            }
        } catch (Exception e) {
            sendError(exchange, e.getMessage(), 500);
        }
    }

    private String toJson(Medicine m) {
        if (m == null) return "null";
        return "{" +
            "\"id\": " + m.getId() + ", " +
            "\"name\": \"" + escapeJson(m.getName()) + "\", " +
            "\"unit\": \"" + escapeJson(m.getUnit()) + "\", " +
            "\"price\": " + m.getPrice() + ", " +
            "\"stockQuantity\": " + m.getStockQuantity() + ", " +
            "\"expiryDate\": \"" + escapeJson(m.getExpiryDate()) + "\"" +
            "}";
    }

    private String listToJson(List medicines) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < medicines.size(); i++) {
            if (i > 0) sb.append(",");
            sb.append(toJson((Medicine) medicines.get(i)));
        }
        sb.append("]");
        return sb.toString();
    }

    private String extractString(String json, String key) {
        String search = "\"" + key + "\":\"";
        int idx = json.indexOf(search);
        if (idx == -1) {
            search = "\"" + key + "\":";
            idx = json.indexOf(search);
            if (idx == -1) return "";
            int start = idx + search.length();
            int end = json.indexOf(",", start);
            if (end == -1) end = json.indexOf("}", start);
            return json.substring(start, end).trim();
        }
        int start = idx + search.length();
        int end = json.indexOf("\"", start);
        if (end == -1) return "";
        return json.substring(start, end);
    }
}
