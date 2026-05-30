package com.eclinic.api;

import com.sun.net.httpserver.HttpExchange;
import com.eclinic.dao.PrescriptionDAO;
import com.eclinic.dao.PrescriptionDetailDAO;
import com.eclinic.models.Prescription;
import com.eclinic.models.PrescriptionDetail;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;

public class PrescriptionHandler extends BaseHandler {

    protected void handleRequest(HttpExchange exchange) throws IOException {
        String method = exchange.getRequestMethod();
        String path = normalizePath(exchange.getRequestURI().getPath());
        String query = exchange.getRequestURI().getQuery();


        PrescriptionDAO dao = new PrescriptionDAO();
        PrescriptionDetailDAO detailDao = new PrescriptionDetailDAO();

        try {
            if ("GET".equals(method)) {
                // GET /api/prescriptions/ID
                if (path.startsWith("/api/prescriptions/")) {
                    long id = parseId(path, "/api/prescriptions/");
                    Prescription prescription = dao.findById(id);
                    if (prescription != null) {
                        // Include prescription details
                        List details = detailDao.findByPrescriptionId(id);
                        String json = toJsonWithDetails(prescription, details);
                        sendJson(exchange, json, 200);
                    } else {
                        sendError(exchange, "Prescription not found", 404);
                    }
                }
                // GET /api/prescriptions?medicalRecordId=X
                else if (query != null && query.contains("medicalRecordId=")) {
                    long medicalRecordId = parseQueryLong(query, "medicalRecordId");
                    Prescription prescription = dao.findByMedicalRecordId(medicalRecordId);
                    if (prescription != null) {
                        List details = detailDao.findByPrescriptionId(prescription.getId());
                        String json = toJsonWithDetails(prescription, details);
                        sendJson(exchange, json, 200);
                    } else {
                        sendJson(exchange, "null", 200);
                    }
                }
                // GET /api/prescriptions - all prescriptions
                else {
                    List prescriptions = dao.findAll();
                    String json = listToJson(prescriptions);
                    sendJson(exchange, json, 200);
                }
            }
            // POST /api/prescriptions - create prescription
            else if ("POST".equals(method)) {
                String body = readBody(exchange);
                long medicalRecordId = extractLong(body, "medicalRecordId");
                String notes = extractString(body, "notes");
                String totalPriceStr = extractString(body, "totalPrice");
                BigDecimal totalPrice = !totalPriceStr.isEmpty() ? new BigDecimal(totalPriceStr) : BigDecimal.ZERO;

                long id = dao.create(medicalRecordId, notes, totalPrice);
                String json = "{\"id\": " + id + ", \"status\": \"created\"}";
                sendJson(exchange, json, 201);
            }
            // PUT /api/prescriptions/ID - update prescription
            else if ("PUT".equals(method)) {
                long id = parseId(path, "/api/prescriptions/");
                String body = readBody(exchange);
                String notes = extractString(body, "notes");
                String totalPriceStr = extractString(body, "totalPrice");
                BigDecimal totalPrice = !totalPriceStr.isEmpty() ? new BigDecimal(totalPriceStr) : BigDecimal.ZERO;

                boolean updated = dao.update(id, notes, totalPrice);
                if (updated) {
                    sendJson(exchange, "{\"status\": \"updated\"}", 200);
                } else {
                    sendError(exchange, "Prescription not found", 404);
                }
            }
            // DELETE /api/prescriptions/ID
            else if ("DELETE".equals(method)) {
                long id = parseId(path, "/api/prescriptions/");
                boolean deleted = dao.delete(id);
                if (deleted) {
                    sendJson(exchange, "{\"status\": \"deleted\"}", 200);
                } else {
                    sendError(exchange, "Prescription not found", 404);
                }
            } else {
                sendError(exchange, "Method not allowed", 405);
            }
        } catch (Exception e) {
            sendError(exchange, e.getMessage(), 500);
        }
    }

    private String toJson(Prescription p) {
        if (p == null) return "null";
        return "{" +
            "\"id\": " + p.getId() + ", " +
            "\"medicalRecordId\": " + p.getMedicalRecordId() + ", " +
            "\"notes\": \"" + escapeJson(p.getNotes()) + "\", " +
            "\"totalPrice\": " + p.getTotalPrice() + ", " +
            "\"createdAt\": \"" + escapeJson(p.getCreatedAt()) + "\"" +
            "}";
    }

    private String toJsonWithDetails(Prescription p, List details) {
        if (p == null) return "null";
        StringBuilder sb = new StringBuilder("{");
        sb.append("\"id\": ").append(p.getId()).append(", ");
        sb.append("\"medicalRecordId\": ").append(p.getMedicalRecordId()).append(", ");
        sb.append("\"notes\": \"").append(escapeJson(p.getNotes())).append("\", ");
        sb.append("\"totalPrice\": ").append(p.getTotalPrice()).append(", ");
        sb.append("\"createdAt\": \"").append(escapeJson(p.getCreatedAt())).append("\", ");
        sb.append("\"details\": [");
        for (int i = 0; i < details.size(); i++) {
            if (i > 0) sb.append(",");
            sb.append(detailToJson((PrescriptionDetail) details.get(i)));
        }
        sb.append("]");
        sb.append("}");
        return sb.toString();
    }

    private String detailToJson(PrescriptionDetail d) {
        if (d == null) return "null";
        return "{" +
            "\"id\": " + d.getId() + ", " +
            "\"prescriptionId\": " + d.getPrescriptionId() + ", " +
            "\"medicineId\": " + d.getMedicineId() + ", " +
            "\"quantity\": " + d.getQuantity() + ", " +
            "\"dosage\": \"" + escapeJson(d.getDosage()) + "\"" +
            "}";
    }

    private String listToJson(List prescriptions) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < prescriptions.size(); i++) {
            if (i > 0) sb.append(",");
            sb.append(toJson((Prescription) prescriptions.get(i)));
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
