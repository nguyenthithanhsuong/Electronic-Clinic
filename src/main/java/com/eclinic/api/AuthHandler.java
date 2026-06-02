package com.eclinic.api;

import com.sun.net.httpserver.HttpExchange;
import com.eclinic.dao.UserDAO;
import com.eclinic.dao.AuditLogDAO;
import com.eclinic.models.User;
import com.eclinic.util.PasswordUtil;
import com.eclinic.util.JwtUtil;
import java.io.IOException;

public class AuthHandler extends BaseHandler {

    /** Login endpoint does NOT require authentication. */
    @Override
    protected boolean requiresAuth() {
        return false;
    }

    protected void handleRequest(HttpExchange exchange) throws IOException {
        String method = exchange.getRequestMethod();
        String path = exchange.getRequestURI().getPath();


        if ("POST".equals(method) && path.endsWith("/login")) {
            handleLogin(exchange);
        } else {
            sendError(exchange, "Method not allowed", 405);
        }
    }

    private void handleLogin(HttpExchange exchange) throws IOException {
        try {
            String body = readBody(exchange);
            String username = extractString(body, "username");
            String password = extractString(body, "password");

            if (username.isEmpty() || password.isEmpty()) {
                sendJson(exchange, "{\"success\": false, \"message\": \"Username and password required\"}", 400);
                return;
            }

            UserDAO userDAO = new UserDAO();
            User user = userDAO.findByUsername(username);

            if (user == null) {
                sendJson(exchange, "{\"success\": false, \"message\": \"Tài khoản không tồn tại\"}", 401);
                return;
            }

            // Use bcrypt-aware password check (with legacy plaintext fallback)
            if (!PasswordUtil.check(password, user.getPasswordHash())) {
                sendJson(exchange, "{\"success\": false, \"message\": \"Mật khẩu không đúng\"}", 401);
                return;
            }

            if ("LOCKED".equals(user.getStatus())) {
                sendJson(exchange, "{\"success\": false, \"message\": \"Tài khoản đã bị khóa\"}", 403);
                return;
            }

            // Migrate legacy plaintext password to bcrypt on successful login
            if (!PasswordUtil.isBcrypt(user.getPasswordHash())) {
                try {
                    String bcryptHash = PasswordUtil.hash(password);
                    userDAO.updatePassword(user.getId(), bcryptHash);
                } catch (Exception ignored) {}
            }

            // Generate proper JWT token instead of Base64
            String token = JwtUtil.generateToken(user.getId(), user.getUsername(), user.getRole());

            String roleForFrontend = mapRole(user.getRole());

            String json = "{" +
                "\"success\": true, " +
                "\"message\": \"Đăng nhập thành công\", " +
                "\"user\": {" +
                    "\"id\": \"" + user.getId() + "\", " +
                    "\"username\": \"" + escapeJson(user.getUsername()) + "\", " +
                    "\"fullName\": \"" + escapeJson(user.getUsername()) + "\", " +
                    "\"email\": \"\", " +
                    "\"role\": \"" + roleForFrontend + "\", " +
                    "\"createdAt\": \"" + escapeJson(user.getCreatedAt()) + "\"" +
                "}, " +
                "\"token\": \"" + token + "\"" +
                "}";

            sendJson(exchange, json, 200);

            // Log the login
            try {
                AuditLogDAO auditDAO = new AuditLogDAO();
                auditDAO.log("LOGIN", username, "system");
            } catch (Exception ignored) {}

        } catch (Exception e) {
            sendError(exchange, e.getMessage(), 500);
        }
    }

    private String mapRole(String dbRole) {
        if (dbRole == null) return "RECEPTIONIST";
        switch (dbRole.toUpperCase()) {
            case "ADMIN": return "ADMIN";
            case "DOCTOR": return "DOCTOR";
            case "RECEPTIONIST": return "RECEPTIONIST";
            default: return "RECEPTIONIST";
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
}
