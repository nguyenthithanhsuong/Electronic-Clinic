package com.eclinic.api;

import com.sun.net.httpserver.HttpExchange;
import com.eclinic.dao.DoctorDAO;
import com.eclinic.dao.UserDAO;
import com.eclinic.models.Doctor;
import com.eclinic.models.User;
import java.io.IOException;
import java.util.List;

public class DoctorsHandler extends BaseHandler {

    public void handle(HttpExchange exchange) throws IOException {
        String method = exchange.getRequestMethod();
        String path = exchange.getRequestURI().getPath();
        String query = exchange.getRequestURI().getQuery();

        if ("OPTIONS".equals(method)) {
            exchange.sendResponseHeaders(204, -1);
            return;
        }

        DoctorDAO dao = new DoctorDAO();

        try {
            if ("GET".equals(method)) {
                if (path.matches("/api/doctors/\\d+")) {
                    long id = Long.parseLong(path.substring(13));
                    Doctor doctor = dao.findById(id);
                    if (doctor != null) {
                        String json = toJson(doctor);
                        sendJson(exchange, json, 200);
                    } else {
                        sendError(exchange, "Doctor not found", 404);
                    }
                } else {
                    List allDoctors = dao.findAll();
                    String json = listToJson(allDoctors);
                    sendJson(exchange, json, 200);
                }
            } else if ("POST".equals(method)) {
                String body = readBody(exchange);
                Long requestedUserId = extractNullableLong(body, "userId");
                String username = extractString(body, "username");
                String password = extractString(body, "password");
                String fullName = extractString(body, "fullName");
                String specialty = extractString(body, "specialty");
                String phone = extractString(body, "phone");
                String email = extractString(body, "email");
                String roomNumber = extractString(body, "roomNumber");

                if (fullName.length() == 0 || specialty.length() == 0 || phone.length() == 0 || email.length() == 0 || roomNumber.length() == 0) {
                    sendError(exchange, "Invalid doctor payload", 400);
                    return;
                }

                long userId = resolveOrCreateDoctorUserId(requestedUserId, username, password, fullName, email);

                long id = dao.create(userId, fullName, specialty, phone, email, roomNumber);
                String json = "{\"id\": " + id + ", \"status\": \"created\"}";
                sendJson(exchange, json, 201);
            } else if ("PUT".equals(method)) {
                long id = Long.parseLong(path.substring(13));
                String body = readBody(exchange);
                String username = extractString(body, "username");
                String password = extractString(body, "password");
                String fullName = extractString(body, "fullName");
                String specialty = extractString(body, "specialty");
                String email = extractString(body, "email");
                String roomNumber = extractString(body, "roomNumber");

                DoctorDAO daoLocal = new DoctorDAO();
                Doctor doctor = daoLocal.findById(id);
                if (doctor == null) {
                    sendError(exchange, "Doctor not found", 404);
                    return;
                }

                boolean doctorUpdated = daoLocal.update(id, fullName, specialty, email, roomNumber);
                if (!doctorUpdated) {
                    sendError(exchange, "Failed to update doctor", 500);
                    return;
                }

                Long userId = daoLocal.getUserIdForDoctor(id);
                if (userId != null) {
                    UserDAO userDAO = new UserDAO();
                    if (username != null && username.length() > 0 && password != null && password.length() > 0) {
                        userDAO.updateUsernameAndPassword(userId, username, password);
                    } else if (username != null && username.length() > 0) {
                        userDAO.updateUsername(userId, username);
                    } else if (password != null && password.length() > 0) {
                        userDAO.updatePassword(userId, password);
                    }
                }

                sendJson(exchange, "{\"status\": \"updated\"}", 200);
            } else if ("DELETE".equals(method)) {
                long id = Long.parseLong(path.substring(13));
                boolean deleted = dao.delete(id);
                if (deleted) {
                    sendJson(exchange, "{\"status\": \"deleted\"}", 200);
                } else {
                    sendError(exchange, "Doctor not found", 404);
                }
            } else {
                sendError(exchange, "Method not allowed", 405);
            }
        } catch (IllegalArgumentException e) {
            sendError(exchange, e.getMessage(), 400);
        } catch (Exception e) {
            sendError(exchange, e.getMessage(), 500);
        }
    }

    private String toJson(Doctor d) {
        if (d == null) return "null";
        return "{" +
            "\"id\": " + d.getId() + ", " +
            "\"userId\": " + d.getUserId() + ", " +
            "\"username\": \"" + escapeJson(d.getUsername()) + "\", " +
            "\"fullName\": \"" + escapeJson(d.getFullName()) + "\", " +
            "\"specialty\": \"" + escapeJson(d.getSpecialty()) + "\", " +
            "\"phone\": \"" + escapeJson(d.getPhone()) + "\", " +
            "\"email\": \"" + escapeJson(d.getEmail()) + "\", " +
            "\"roomNumber\": \"" + escapeJson(d.getRoomNumber()) + "\", " +
            "\"createdAt\": \"" + escapeJson(d.getCreatedAt()) + "\"" +
            "}";
    }

    private String listToJson(List doctors) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < doctors.size(); i++) {
            if (i > 0) sb.append(",");
            sb.append(toJson((Doctor) doctors.get(i)));
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

    private long resolveOrCreateDoctorUserId(Long requestedUserId, String username, String password, String fullName, String email) throws Exception {
        UserDAO userDAO = new UserDAO();

        if (requestedUserId != null && requestedUserId.longValue() > 0) {
            User existing = userDAO.findById(requestedUserId.longValue());
            if (existing == null) {
                throw new IllegalArgumentException("userId does not exist");
            }
            if (!"DOCTOR".equals(existing.getRole())) {
                throw new IllegalArgumentException("userId must belong to a DOCTOR user");
            }
            return requestedUserId.longValue();
        }

        String resolvedUsername = isBlank(username) ? generateUsername("doctor", fullName, email) : username;
        String resolvedPassword = isBlank(password) ? "TEMP_HASH" : password;
        return userDAO.create(resolvedUsername, resolvedPassword, "DOCTOR", "ACTIVE");
    }

    private String generateUsername(String prefix, String fullName, String fallback) {
        String base = fallback;
        if (base == null || base.trim().length() == 0) {
            base = fullName;
        }
        if (base == null || base.trim().length() == 0) {
            base = prefix;
        }

        String normalized = base.toLowerCase();
        int at = normalized.indexOf('@');
        if (at > 0) {
            normalized = normalized.substring(0, at);
        }
        normalized = normalized.replaceAll("[^a-z0-9]", "");
        if (normalized.length() == 0) {
            normalized = prefix;
        }
        return prefix + "_" + normalized + "_" + System.currentTimeMillis();
    }

    private String extractString(String json, String key) {
        String search = "\"" + key + "\":\"";
        int idx = json.indexOf(search);
        if (idx == -1) return "";
        int start = idx + search.length();
        int end = json.indexOf("\"", start);
        return json.substring(start, end);
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().length() == 0;
    }
}
