package com.eclinic.api;

import com.sun.net.httpserver.HttpExchange;
import com.eclinic.dao.AuditLogDAO;
import com.eclinic.dao.MedicalRecordDAO;
import com.eclinic.database.ConnectionManager;
import com.eclinic.models.MedicalRecord;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.List;

public class MedicalRecordsHandler extends BaseHandler {

    protected void handleRequest(HttpExchange exchange) throws IOException {
        String method = exchange.getRequestMethod();
        String path = normalizePath(exchange.getRequestURI().getPath());
        String query = exchange.getRequestURI().getQuery();

        // RBAC: only DOCTOR and ADMIN can manage medical records
        if (!requireRole(exchange, "DOCTOR", "ADMIN")) return;

        MedicalRecordDAO dao = new MedicalRecordDAO();

        try {
            if ("GET".equals(method)) {
                if (query != null && query.contains("appointmentId=")) {
                    long appointmentId = parseQueryLong(query, "appointmentId");
                    MedicalRecord record = dao.findByAppointmentId(appointmentId);
                    if (record != null) {
                        String json = toJson(record);
                        sendJson(exchange, json, 200);
                    } else {
                        sendJson(exchange, "null", 200);
                    }
                } else if (path.startsWith("/api/patients/medical-records/")) {
                    long id = parseId(path, "/api/patients/medical-records/");
                    MedicalRecord record = dao.findById(id);
                    if (record != null) {
                        String json = toJson(record);
                        sendJson(exchange, json, 200);
                    } else {
                        sendError(exchange, "Medical record not found", 404);
                    }
                } else {
                    List records = dao.findAll();
                    String json = listToJson(records);
                    sendJson(exchange, json, 200);
                }
            } else if ("POST".equals(method)) {
                String body = readBody(exchange);
                Long appointmentId = extractNullableLong(body, "appointmentId");
                String symptoms = extractString(body, "symptoms");
                String diagnosis = extractString(body, "diagnosis");
                String recordType = extractString(body, "recordType");
                String treatmentPlan = extractString(body, "treatmentPlan");
                if (symptoms.length() == 0 || diagnosis.length() == 0) {
                    sendError(exchange, "symptoms and diagnosis are required", 400);
                    return;
                }
                if (recordType.length() == 0) recordType = "GENERAL";
                if (appointmentId != null && !appointmentExists(appointmentId.longValue())) {
                    sendError(exchange, "Appointment not found", 404);
                    return;
                }
                if (appointmentId != null) {
                    MedicalRecord existing = dao.findByAppointmentId(appointmentId.longValue());
                    if (existing != null) {
                        sendJson(exchange, toJson(existing), 200);
                        return;
                    }
                }

                long id = dao.create(appointmentId, symptoms, diagnosis, recordType, treatmentPlan);
                logAudit(exchange, "CREATE_MEDICAL_RECORD", "medical_record #" + id + " appointment #" + appointmentId);
                sendJson(exchange, toJson(dao.findById(id)), 201);
            } else if ("PUT".equals(method)) {
                long id = parseId(path, "/api/patients/medical-records/");
                String body = readBody(exchange);
                String diagnosis = extractString(body, "diagnosis");
                String treatmentPlan = extractString(body, "treatmentPlan");

                boolean updated = dao.update(id, diagnosis, treatmentPlan);
                if (updated) {
                    sendJson(exchange, "{\"status\": \"updated\"}", 200);
                } else {
                    sendError(exchange, "Medical record not found", 404);
                }
            } else if ("DELETE".equals(method)) {
                long id = parseId(path, "/api/patients/medical-records/");
                boolean deleted = dao.delete(id);
                if (deleted) {
                    sendJson(exchange, "{\"status\": \"deleted\"}", 200);
                } else {
                    sendError(exchange, "Medical record not found", 404);
                }
            } else {
                sendError(exchange, "Method not allowed", 405);
            }
        } catch (Exception e) {
            sendError(exchange, e.getMessage(), 500);
        }
    }

    private String toJson(MedicalRecord m) {
        if (m == null) return "null";
        return "{" +
            "\"id\": " + m.getId() + ", " +
            "\"appointmentId\": " + m.getAppointmentId() + ", " +
            "\"symptoms\": \"" + escapeJson(m.getSymptoms()) + "\", " +
            "\"diagnosis\": \"" + escapeJson(m.getDiagnosis()) + "\", " +
            "\"recordType\": \"" + escapeJson(m.getRecordType()) + "\", " +
            "\"treatmentPlan\": \"" + escapeJson(m.getTreatmentPlan()) + "\", " +
            "\"createdAt\": \"" + escapeJson(m.getCreatedAt()) + "\"" +
            "}";
    }

    private String listToJson(List records) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < records.size(); i++) {
            if (i > 0) sb.append(",");
            sb.append(toJson((MedicalRecord) records.get(i)));
        }
        sb.append("]");
        return sb.toString();
    }

    private boolean appointmentExists(long appointmentId) throws Exception {
        String sql = "SELECT 1 FROM appointments WHERE id = ?";
        Connection conn = ConnectionManager.getConnection();
        try {
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setLong(1, appointmentId);
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

    /** Extract a Long that may be null (returns null instead of 0). */
    private Long extractNullableLong(String json, String key) {
        String search = "\"" + key + "\":";
        int idx = json.indexOf(search);
        if (idx == -1) return null;
        int start = idx + search.length();
        int end = json.indexOf(",", start);
        if (end == -1) end = json.indexOf("}", start);
        String raw = json.substring(start, end).trim();
        if (raw.equals("null") || raw.isEmpty()) return null;
        // Strip quotes if present
        if (raw.startsWith("\"") && raw.endsWith("\"")) {
            raw = raw.substring(1, raw.length() - 1);
            if (raw.isEmpty()) return null;
        }
        try {
            long val = Long.parseLong(raw);
            return val > 0 ? val : null;
        } catch (Exception e) {
            return null;
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

    private void logAudit(HttpExchange exchange, String action, String target) {
        try {
            String actor = getAuthRole(exchange) + ":" + getAuthUserId(exchange);
            new AuditLogDAO().log(action, actor, target);
        } catch (Exception ignored) {}
    }
}
