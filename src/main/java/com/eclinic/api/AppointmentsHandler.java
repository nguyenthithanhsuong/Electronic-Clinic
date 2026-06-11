package com.eclinic.api;

import com.sun.net.httpserver.HttpExchange;
import com.eclinic.dao.AppointmentDAO;
import com.eclinic.dao.AuditLogDAO;
import com.eclinic.dao.DoctorDAO;
import com.eclinic.dao.PatientDAO;
import com.eclinic.database.ConnectionManager;
import com.eclinic.models.Appointment;
import java.io.IOException;
import java.util.Map;
import java.util.List;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class AppointmentsHandler extends BaseHandler {

    protected void handleRequest(HttpExchange exchange) throws IOException {
        String method = exchange.getRequestMethod();
        String path = normalizePath(exchange.getRequestURI().getPath());
        String query = exchange.getRequestURI().getQuery();

        // RBAC: write operations require RECEPTIONIST or ADMIN
        if (!"GET".equals(method)) {
            if (!requireRole(exchange, "RECEPTIONIST", "ADMIN")) return;
        }

        AppointmentDAO dao = new AppointmentDAO();

        try {
            if ("GET".equals(method)) {
                if (path.startsWith("/api/appointments/")) {
                    long id = parseId(path, "/api/appointments/");
                    Appointment apt = dao.findById(id);
                    if (apt != null) {
                        String json = toJson(apt);
                        sendJson(exchange, json, 200);
                    } else {
                        sendError(exchange, "Appointment not found", 404);
                    }
                } else if (query != null && (query.contains("patientId=") || query.contains("doctorId=") || query.contains("doctor_id=") || query.contains("date="))) {
                    Map<String, String> params = parseQuery(query);
                    List appointments = findAppointments(params);
                    String json = listToJson(appointments);
                    sendJson(exchange, json, 200);
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
                if (status.length() == 0) status = "PENDING";

                if (doctorId <= 0 || new DoctorDAO().findById(doctorId) == null) {
                    sendError(exchange, "Doctor not found", 404);
                    return;
                }
                if (patientId <= 0 || new PatientDAO().findById(patientId) == null) {
                    sendError(exchange, "Patient not found", 404);
                    return;
                }
                if (!isValidStatus(status)) {
                    sendError(exchange, "Invalid appointment status", 400);
                    return;
                }

                long id = dao.create(doctorId, patientId, startDate, endDate, reason, status);
                logAudit(exchange, "CREATE_APPOINTMENT", "appointment #" + id + " patient #" + patientId + " doctor #" + doctorId);
                sendJson(exchange, toJson(dao.findById(id)), 201);
            } else if ("PUT".equals(method)) {
                long id = parseId(path, "/api/appointments/");
                String body = readBody(exchange);
                String status = extractString(body, "status");
                String startDate = extractString(body, "appointmentStartDate");
                String endDate = extractString(body, "appointmentEndDate");

                boolean updated;
                if (startDate.length() > 0) {
                    // Reschedule: update dates and optionally status
                    String effectiveEnd = endDate.length() > 0 ? endDate : startDate;
                    String effectiveStatus = status.length() > 0 ? status.toUpperCase() : "PENDING";
                    if (!isValidStatus(effectiveStatus)) {
                        sendError(exchange, "Invalid appointment status", 400);
                        return;
                    }
                    updated = dao.reschedule(id, startDate, effectiveEnd, effectiveStatus);
                } else if (status.length() > 0) {
                    status = status.toUpperCase();
                    if (!isValidStatus(status)) {
                        sendError(exchange, "Invalid appointment status", 400);
                        return;
                    }
                    updated = dao.updateStatus(id, status);
                } else {
                    sendError(exchange, "Nothing to update", 400);
                    return;
                }
                if (updated) {
                    sendJson(exchange, toJson(dao.findById(id)), 200);
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

    private String toJson(Appointment a) throws Exception {
        if (a == null) return "null";
        AppointmentDetails details = getAppointmentDetails(a.getId());
        return "{" +
            "\"id\": " + a.getId() + ", " +
            "\"doctorId\": " + a.getDoctorId() + ", " +
            "\"patientId\": " + a.getPatientId() + ", " +
            "\"doctorName\": \"" + escapeJson(details.doctorName) + "\", " +
            "\"patientName\": \"" + escapeJson(details.patientName) + "\", " +
            "\"queueId\": " + (details.queueId != null ? details.queueId.toString() : "null") + ", " +
            "\"queuePosition\": " + (details.queuePosition != null ? details.queuePosition.toString() : "null") + ", " +
            "\"appointmentStartDate\": \"" + escapeJson(a.getAppointmentStartDate()) + "\", " +
            "\"appointmentEndDate\": \"" + escapeJson(a.getAppointmentEndDate()) + "\", " +
            "\"reason\": \"" + escapeJson(a.getReason()) + "\", " +
            "\"status\": \"" + escapeJson(a.getStatus()) + "\", " +
            "\"createdAt\": \"" + escapeJson(a.getCreatedAt()) + "\"" +
            "}";
    }

    private String listToJson(List appointments) throws Exception {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < appointments.size(); i++) {
            if (i > 0) sb.append(",");
            sb.append(toJson((Appointment) appointments.get(i)));
        }
        sb.append("]");
        return sb.toString();
    }

    private List findAppointments(Map<String, String> params) throws Exception {
        if (params.containsKey("patientId")) {
            return new AppointmentDAO().findByPatientId(Long.parseLong(params.get("patientId")));
        }

        String doctorValue = params.containsKey("doctorId") ? params.get("doctorId") : params.get("doctor_id");
        String date = params.get("date");
        StringBuilder sql = new StringBuilder("SELECT id, doctor_id, patient_id, appointment_start_date, appointment_end_date, reason, status, created_at FROM appointments WHERE 1=1");
        java.util.List<Object> values = new java.util.ArrayList<Object>();
        if (doctorValue != null && doctorValue.length() > 0) {
            sql.append(" AND doctor_id = ?");
            values.add(Long.valueOf(Long.parseLong(doctorValue)));
        }
        if (date != null && date.length() > 0) {
            sql.append(" AND appointment_start_date::date = ?::date");
            values.add(date);
        }
        sql.append(" ORDER BY appointment_start_date ASC");

        Connection conn = ConnectionManager.getConnection();
        java.util.List appointments = new java.util.ArrayList();
        try {
            PreparedStatement stmt = conn.prepareStatement(sql.toString());
            for (int i = 0; i < values.size(); i++) {
                Object value = values.get(i);
                if (value instanceof Long) stmt.setLong(i + 1, ((Long) value).longValue());
                else stmt.setString(i + 1, value.toString());
            }
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                appointments.add(new Appointment(
                    rs.getLong("id"),
                    rs.getLong("doctor_id"),
                    rs.getLong("patient_id"),
                    rs.getString("appointment_start_date"),
                    rs.getString("appointment_end_date"),
                    rs.getString("reason"),
                    rs.getString("status"),
                    rs.getString("created_at")
                ));
            }
            return appointments;
        } finally {
            ConnectionManager.closeConnection(conn);
        }
    }

    private AppointmentDetails getAppointmentDetails(long appointmentId) throws Exception {
        String sql = "SELECT p.full_name AS patient_name, d.full_name AS doctor_name, pq.id AS queue_id, " +
            "(SELECT COUNT(*) FROM patient_queue q2 WHERE q2.status = 'WAITING' AND q2.enqueued_at <= pq.enqueued_at) AS queue_position " +
            "FROM appointments a " +
            "JOIN patients p ON a.patient_id = p.id " +
            "JOIN doctors d ON a.doctor_id = d.id " +
            "LEFT JOIN patient_queue pq ON pq.appointment_id = a.id AND pq.status = 'WAITING' " +
            "WHERE a.id = ?";
        Connection conn = ConnectionManager.getConnection();
        try {
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setLong(1, appointmentId);
            ResultSet rs = stmt.executeQuery();
            AppointmentDetails details = new AppointmentDetails();
            if (rs.next()) {
                details.patientName = rs.getString("patient_name");
                details.doctorName = rs.getString("doctor_name");
                long queueId = rs.getLong("queue_id");
                if (!rs.wasNull()) details.queueId = Long.valueOf(queueId);
                long position = rs.getLong("queue_position");
                if (!rs.wasNull()) details.queuePosition = Long.valueOf(position);
            }
            return details;
        } finally {
            ConnectionManager.closeConnection(conn);
        }
    }

    private boolean isValidStatus(String status) {
        return "PENDING".equals(status) || "CONFIRMED".equals(status) || "COMPLETED".equals(status) || "CANCELLED".equals(status);
    }

    private static class AppointmentDetails {
        String patientName = "";
        String doctorName = "";
        Long queueId;
        Long queuePosition;
    }

    private void logAudit(HttpExchange exchange, String action, String target) {
        try {
            String actor = getAuthRole(exchange) + ":" + getAuthUserId(exchange);
            new AuditLogDAO().log(action, actor, target);
        } catch (Exception ignored) {}
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
