package com.eclinic.api;

import com.sun.net.httpserver.HttpExchange;
import com.eclinic.dao.PrescriptionDetailDAO;
import com.eclinic.database.ConnectionManager;
import com.eclinic.models.PrescriptionDetail;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.List;

public class PrescriptionDetailHandler extends BaseHandler {

    protected void handleRequest(HttpExchange exchange) throws IOException {
        String method = exchange.getRequestMethod();
        String path = normalizePath(exchange.getRequestURI().getPath());
        String query = exchange.getRequestURI().getQuery();

        // RBAC: only DOCTOR and ADMIN can manage prescription details
        if (!requireRole(exchange, "DOCTOR", "ADMIN")) return;

        PrescriptionDetailDAO dao = new PrescriptionDetailDAO();

        try {
            if ("GET".equals(method)) {
                // GET /api/prescriptions/details/ID
                if (path.startsWith("/api/prescriptions/details/")) {
                    long id = parseId(path, "/api/prescriptions/details/");
                    PrescriptionDetail detail = dao.findById(id);
                    if (detail != null) {
                        String json = toJson(detail);
                        sendJson(exchange, json, 200);
                    } else {
                        sendError(exchange, "Prescription detail not found", 404);
                    }
                }
                // GET /api/prescriptions/details?prescriptionId=X
                else if (query != null && query.contains("prescriptionId=")) {
                    long prescriptionId = parseQueryLong(query, "prescriptionId");
                    List details = dao.findByPrescriptionId(prescriptionId);
                    String json = listToJson(details);
                    sendJson(exchange, json, 200);
                } else {
                    List details = dao.findAll();
                    String json = listToJson(details);
                    sendJson(exchange, json, 200);
                }
            }
            // POST /api/prescriptions/details - create detail
            else if ("POST".equals(method)) {
                String body = readBody(exchange);
                long prescriptionId = extractLong(body, "prescriptionId");
                long medicineId = extractLong(body, "medicineId");
                int quantity = (int) extractLong(body, "quantity");
                String dosage = extractString(body, "dosage");
                if (!exists("prescriptions", prescriptionId)) {
                    sendError(exchange, "Prescription not found", 404);
                    return;
                }
                if (!exists("medicines", medicineId)) {
                    sendError(exchange, "Medicine not found", 404);
                    return;
                }
                if (quantity <= 0) {
                    sendError(exchange, "quantity must be greater than 0", 400);
                    return;
                }
                if (dosage.length() == 0) {
                    sendError(exchange, "dosage is required", 400);
                    return;
                }

                long id = dao.create(prescriptionId, medicineId, quantity, dosage);
                sendJson(exchange, toJson(dao.findById(id)), 201);
            }
            // PUT /api/prescriptions/details/ID - update detail
            else if ("PUT".equals(method)) {
                long id = parseId(path, "/api/prescriptions/details/");
                String body = readBody(exchange);
                int quantity = (int) extractLong(body, "quantity");
                String dosage = extractString(body, "dosage");

                boolean updated = dao.update(id, quantity, dosage);
                if (updated) {
                    sendJson(exchange, "{\"status\": \"updated\"}", 200);
                } else {
                    sendError(exchange, "Prescription detail not found", 404);
                }
            }
            // DELETE /api/prescriptions/details/ID
            else if ("DELETE".equals(method)) {
                long id = parseId(path, "/api/prescriptions/details/");
                boolean deleted = dao.delete(id);
                if (deleted) {
                    sendJson(exchange, "{\"status\": \"deleted\"}", 200);
                } else {
                    sendError(exchange, "Prescription detail not found", 404);
                }
            } else {
                sendError(exchange, "Method not allowed", 405);
            }
        } catch (Exception e) {
            sendError(exchange, e.getMessage(), 500);
        }
    }

    private String toJson(PrescriptionDetail d) {
        if (d == null) return "null";
        return "{" +
            "\"id\": " + d.getId() + ", " +
            "\"prescriptionId\": " + d.getPrescriptionId() + ", " +
            "\"medicineId\": " + d.getMedicineId() + ", " +
            "\"quantity\": " + d.getQuantity() + ", " +
            "\"dosage\": \"" + escapeJson(d.getDosage()) + "\"" +
            "}";
    }

    private String listToJson(List details) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < details.size(); i++) {
            if (i > 0) sb.append(",");
            sb.append(toJson((PrescriptionDetail) details.get(i)));
        }
        sb.append("]");
        return sb.toString();
    }

    private boolean exists(String table, long id) throws Exception {
        if (id <= 0) return false;
        String sql = "SELECT 1 FROM " + table + " WHERE id = ?";
        Connection conn = ConnectionManager.getConnection();
        try {
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setLong(1, id);
            ResultSet rs = stmt.executeQuery();
            return rs.next();
        } finally {
            ConnectionManager.closeConnection(conn);
        }
    }

    private long extractLong(String json, String key) {
        String search = "\"" + key + "\":";
        int idx = json.indexOf(search);
        if (idx == -1) return 0;
        int end = json.indexOf(",", idx);
        if (end == -1) end = json.indexOf("}", idx);
        String num = json.substring(idx + search.length(), end).trim();
        try {
            return Long.parseLong(num);
        } catch (Exception e) {
            return 0;
        }
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
        return Long.parseLong(path.substring(prefix.length()));
    }

    private long parseQueryLong(String query, String key) {
        String[] parts = query.split("&");
        for (String part : parts) {
            String[] kv = part.split("=", 2);
            if (kv.length == 2 && key.equals(kv[0])) {
                return Long.parseLong(kv[1]);
            }
        }
        return 0;
    }
}
