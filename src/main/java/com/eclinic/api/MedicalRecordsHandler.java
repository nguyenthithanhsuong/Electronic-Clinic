package com.eclinic.api;

import com.sun.net.httpserver.HttpExchange;
import com.eclinic.dao.MedicalRecordDAO;
import com.eclinic.models.MedicalRecord;
import java.io.IOException;
import java.util.List;

public class MedicalRecordsHandler extends BaseHandler {

    protected void handleRequest(HttpExchange exchange) throws IOException {
        String method = exchange.getRequestMethod();
        String path = normalizePath(exchange.getRequestURI().getPath());
        String query = exchange.getRequestURI().getQuery();


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
                long appointmentId = extractLong(body, "appointmentId");
                String symptoms = extractString(body, "symptoms");
                String diagnosis = extractString(body, "diagnosis");
                String recordType = extractString(body, "recordType");
                String treatmentPlan = extractString(body, "treatmentPlan");

                long id = dao.create(appointmentId, symptoms, diagnosis, recordType, treatmentPlan);
                String json = "{\"id\": " + id + ", \"status\": \"created\"}";
                sendJson(exchange, json, 201);
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
