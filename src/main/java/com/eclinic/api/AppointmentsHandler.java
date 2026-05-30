package com.eclinic.api;

import com.sun.net.httpserver.HttpExchange;
import com.eclinic.dao.AppointmentDAO;
import com.eclinic.models.Appointment;
import java.io.IOException;
import java.util.Map;
import java.util.List;

public class AppointmentsHandler extends BaseHandler {

    protected void handleRequest(HttpExchange exchange) throws IOException {
        String method = exchange.getRequestMethod();
        String path = normalizePath(exchange.getRequestURI().getPath());
        String query = exchange.getRequestURI().getQuery();


        AppointmentDAO dao = new AppointmentDAO();

        try {
            if ("GET".equals(method)) {
                if (query != null && query.contains("patientId=")) {
                    Map<String, String> params = parseQuery(query);
                    long patientId = Long.parseLong(params.get("patientId"));
                    List appointments = dao.findByPatientId(patientId);
                    String json = listToJson(appointments);
                    sendJson(exchange, json, 200);
                } else if (path.startsWith("/api/appointments/")) {
                    long id = parseId(path, "/api/appointments/");
                    Appointment apt = dao.findById(id);
                    if (apt != null) {
                        String json = toJson(apt);
                        sendJson(exchange, json, 200);
                    } else {
                        sendError(exchange, "Appointment not found", 404);
                    }
                } else {
                    List appointments = dao.findAll();
                    String json = listToJson(appointments);
                    sendJson(exchange, json, 200);
                }
            } else if ("POST".equals(method)) {
                String body = readBody(exchange);
                long doctorId = extractLong(body, "doctorId");
                long patientId = extractLong(body, "patientId");
                String startDate = extractString(body, "appointmentStartDate");
                String endDate = extractString(body, "appointmentEndDate");
                String reason = extractString(body, "reason");
                String status = extractString(body, "status");

                long id = dao.create(doctorId, patientId, startDate, endDate, reason, status);
                String json = "{\"id\": " + id + ", \"status\": \"created\"}";
                sendJson(exchange, json, 201);
            } else if ("PUT".equals(method)) {
                long id = parseId(path, "/api/appointments/");
                String body = readBody(exchange);
                String status = extractString(body, "status");

                boolean updated = dao.updateStatus(id, status);
                if (updated) {
                    sendJson(exchange, "{\"status\": \"updated\"}", 200);
                } else {
                    sendError(exchange, "Appointment not found", 404);
                }
            } else if ("DELETE".equals(method)) {
                long id = parseId(path, "/api/appointments/");
                boolean deleted = dao.delete(id);
                if (deleted) {
                    sendJson(exchange, "{\"status\": \"deleted\"}", 200);
                } else {
                    sendError(exchange, "Appointment not found", 404);
                }
            } else {
                sendError(exchange, "Method not allowed", 405);
            }
        } catch (Exception e) {
            sendError(exchange, e.getMessage(), 500);
        }
    }

    private String toJson(Appointment a) {
        if (a == null) return "null";
        return "{" +
            "\"id\": " + a.getId() + ", " +
            "\"doctorId\": " + a.getDoctorId() + ", " +
            "\"patientId\": " + a.getPatientId() + ", " +
            "\"appointmentStartDate\": \"" + escapeJson(a.getAppointmentStartDate()) + "\", " +
            "\"appointmentEndDate\": \"" + escapeJson(a.getAppointmentEndDate()) + "\", " +
            "\"reason\": \"" + escapeJson(a.getReason()) + "\", " +
            "\"status\": \"" + escapeJson(a.getStatus()) + "\", " +
            "\"createdAt\": \"" + escapeJson(a.getCreatedAt()) + "\"" +
            "}";
    }

    private String listToJson(List appointments) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < appointments.size(); i++) {
            if (i > 0) sb.append(",");
            sb.append(toJson((Appointment) appointments.get(i)));
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

    private Map<String, String> parseQuery(String query) {
        Map<String, String> params = new java.util.HashMap<String, String>();
        String[] pairs = query.split("&");
        for (String pair : pairs) {
            String[] kv = pair.split("=", 2);
            if (kv.length == 2) {
                params.put(kv[0], kv[1]);
            }
        }
        return params;
    }
}
