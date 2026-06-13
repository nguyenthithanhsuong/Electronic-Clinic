package com.eclinic.api;

import com.sun.net.httpserver.HttpExchange;
import com.eclinic.dao.PatientDAO;
import com.eclinic.dao.UserDAO;
import com.eclinic.dao.AuditLogDAO;
import com.eclinic.models.Patient;
import com.eclinic.models.User;
import com.eclinic.util.PasswordUtil;
import java.io.IOException;
import java.util.List;

public class PatientsHandler extends BaseHandler {

    protected void handleRequest(HttpExchange exchange) throws IOException {
        String method = exchange.getRequestMethod();
        String path = exchange.getRequestURI().getPath();

        // RBAC: reception owns patient intake; admin may maintain records.
        if ("POST".equals(method) || "PUT".equals(method)) {
            if (!requireRole(exchange, "RECEPTIONIST", "ADMIN")) return;
        } else if ("DELETE".equals(method)) {
            if (!requireRole(exchange, "ADMIN")) return;
        }

        PatientDAO dao = new PatientDAO();

        try {
            if ("GET".equals(method)) {
                if (path.matches("/api/patients/\\d+")) {
                    long id = Long.parseLong(path.substring(14));
                    Patient patient = dao.findById(id);
                    if (patient != null) {
                        String json = toJson(patient);
                        sendJson(exchange, json, 200);
                    } else {
                        sendError(exchange, "Patient not found", 404);
                    }
                } else {
                    List<Patient> allPatients = dao.findAll();
                    String json = listToJson(allPatients);
                    sendJson(exchange, json, 200);
                }
            } else if ("POST".equals(method)) {
                String body = readBody(exchange);
                Long requestedUserId = extractNullableLong(body, "userId");
                String username = extractString(body, "username");
                String password = extractString(body, "password");
                String fullName = extractString(body, "fullName");
                String dob = extractString(body, "dob");
                String gender = extractString(body, "gender");
                String phone = extractString(body, "phone");
                String address = extractString(body, "address");
                String insuranceCode = extractString(body, "insuranceCode");

                if (fullName.length() == 0 || dob.length() == 0 || gender.length() == 0 || phone.length() == 0) {
                    sendError(exchange, "Invalid patient payload", 400);
                    return;
                }

                Long userId = resolveOrCreatePatientUserId(requestedUserId, username, password, fullName, phone);

                long id = dao.create(userId, fullName, dob, gender, phone, address, insuranceCode);
                logAudit(exchange, "CREATE_PATIENT", "patient #" + id);
                sendJson(exchange, toJson(dao.findById(id)), 201);
            } else if ("PUT".equals(method)) {
                long id = Long.parseLong(path.substring(14));
                String body = readBody(exchange);
                String username = extractString(body, "username");
                String password = extractString(body, "password");
                String fullName = extractString(body, "fullName");
                String phone = extractString(body, "phone");
                String address = extractString(body, "address");

                PatientDAO daoLocal = new PatientDAO();
                Patient patient = daoLocal.findById(id);
                if (patient == null) {
                    sendError(exchange, "Patient not found", 404);
                    return;
                }

                boolean patientUpdated = daoLocal.update(id, fullName, phone, address);
                if (!patientUpdated) {
                    sendError(exchange, "Failed to update patient", 500);
                    return;
                }

                Long userId = daoLocal.getUserIdForPatient(id);
                if (userId != null) {
                    UserDAO userDAO = new UserDAO();
                    if (username != null && username.length() > 0 && password != null && password.length() > 0) {
                        // Hash password before updating
                        String hashedPassword = PasswordUtil.hash(password);
                        userDAO.updateUsernameAndPassword(userId, username, hashedPassword);
                    } else if (username != null && username.length() > 0) {
                        userDAO.updateUsername(userId, username);
                    } else if (password != null && password.length() > 0) {
                        String hashedPassword = PasswordUtil.hash(password);
                        userDAO.updatePassword(userId, hashedPassword);
                    }
                }

                sendJson(exchange, "{\"status\": \"updated\"}", 200);
            } else if ("DELETE".equals(method)) {
                long id = Long.parseLong(path.substring(14));
                boolean deleted = dao.delete(id);
                if (deleted) {
                    sendJson(exchange, "{\"status\": \"deleted\"}", 200);
                } else {
                    sendError(exchange, "Patient not found", 404);
                }
            } else {
                sendError(exchange, "Method not allowed", 405);
            }
        } catch (IllegalArgumentException e) {
            sendError(exchange, e.getMessage(), 400);
        } catch (java.sql.SQLException e) {
            String msg = e.getMessage();
            if (msg != null && msg.contains("patients_phone_key")) {
                sendError(exchange, "Số điện thoại này đã được sử dụng cho một bệnh nhân khác. Vui lòng kiểm tra lại.", 400);
            } else if (msg != null && msg.contains("patients_insurance_code_key")) {
                sendError(exchange, "Mã bảo hiểm y tế này đã tồn tại trên hệ thống.", 400);
            } else {
                sendError(exchange, "Lỗi cơ sở dữ liệu: " + msg, 500);
            }
        } catch (Exception e) {
            sendError(exchange, e.getMessage(), 500);
        }
    }

    private String toJson(Patient p) {
        if (p == null) return "null";
        return "{" +
            "\"id\": " + p.getId() + ", " +
            "\"userId\": " + (p.getUserId() != null ? p.getUserId() : "null") + ", " +
            "\"username\": \"" + escapeJson(p.getUsername()) + "\", " +
            "\"fullName\": \"" + escapeJson(p.getFullName()) + "\", " +
            "\"dob\": \"" + escapeJson(p.getDob()) + "\", " +
            "\"gender\": \"" + escapeJson(p.getGender()) + "\", " +
            "\"phone\": \"" + escapeJson(p.getPhone()) + "\", " +
            "\"address\": \"" + escapeJson(p.getAddress()) + "\", " +
            "\"insuranceCode\": \"" + escapeJson(p.getInsuranceCode()) + "\", " +
            "\"createdAt\": \"" + escapeJson(p.getCreatedAt()) + "\"" +
            "}";
    }

    private String listToJson(List<Patient> patients) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < patients.size(); i++) {
            if (i > 0) sb.append(",");
            sb.append(toJson(patients.get(i)));
        }
        sb.append("]");
        return sb.toString();
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

    private Long extractNullableLong(String json, String key) {
        String search = "\"" + key + "\":";
        int idx = json.indexOf(search);
        if (idx == -1) return null;
        int start = idx + search.length();
        int end = json.indexOf(",", start);
        if (end == -1) end = json.indexOf("}", start);
        if (end == -1) throw new IllegalArgumentException("Invalid JSON payload");

        String raw = json.substring(start, end).trim();
        if (raw.length() == 0 || "null".equals(raw)) {
            return null;
        }
        if (raw.startsWith("\"") && raw.endsWith("\"") && raw.length() >= 2) {
            raw = raw.substring(1, raw.length() - 1).trim();
        }
        if (raw.length() == 0) {
            return null;
        }
        try {
            return Long.valueOf(Long.parseLong(raw));
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid userId");
        }
    }

    private Long resolveOrCreatePatientUserId(Long requestedUserId, String username, String password, String fullName, String phone) throws Exception {
        UserDAO userDAO = new UserDAO();

        if (requestedUserId != null && requestedUserId.longValue() > 0) {
            User existing = userDAO.findById(requestedUserId.longValue());
            if (existing == null) {
                throw new IllegalArgumentException("userId does not exist");
            }
            if (!"PATIENT".equals(existing.getRole())) {
                throw new IllegalArgumentException("userId must belong to a PATIENT user");
            }
            return requestedUserId;
        }

        if (isBlank(username) && isBlank(password)) {
            return null;
        }
        if (isBlank(username) || isBlank(password)) {
            throw new IllegalArgumentException("username and password are required to create a patient account");
        }

        String resolvedUsername = username;
        String resolvedPassword = PasswordUtil.hash(password);
        long createdUserId = userDAO.create(resolvedUsername, resolvedPassword, "PATIENT", "ACTIVE");
        return Long.valueOf(createdUserId);
    }

    /** Generate a secure random temporary password. */
    private String generateRandomPassword() {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
        StringBuilder sb = new StringBuilder();
        java.security.SecureRandom rng = new java.security.SecureRandom();
        for (int i = 0; i < 12; i++) {
            sb.append(chars.charAt(rng.nextInt(chars.length())));
        }
        return sb.toString();
    }

    private String generateUsername(String prefix, String fullName, String fallback) {
        String base = fallback;
        if (base == null || base.trim().length() == 0) {
            base = fullName;
        }
        if (base == null || base.trim().length() == 0) {
            base = prefix;
        }

        String normalized = base.toLowerCase().replaceAll("[^a-z0-9]", "");
        if (normalized.length() == 0) {
            normalized = prefix;
        }
        return prefix + "_" + normalized + "_" + System.currentTimeMillis();
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().length() == 0;
    }

    private void logAudit(HttpExchange exchange, String action, String target) {
        try {
            String actor = getAuthRole(exchange) + ":" + getAuthUserId(exchange);
            new AuditLogDAO().log(action, actor, target);
        } catch (Exception ignored) {}
    }
}
