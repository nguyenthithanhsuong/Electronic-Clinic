package com.eclinic.api;

import com.sun.net.httpserver.HttpExchange;
import com.eclinic.dao.AuditLogDAO;
import java.io.IOException;

public class EmailHandler extends BaseHandler {

    protected void handleRequest(HttpExchange exchange) throws IOException {
        String method = exchange.getRequestMethod();

        if (!"POST".equals(method)) {
            sendError(exchange, "Method not allowed", 405);
            return;
        }

        try {
            String body = readBody(exchange);
            String to = extractString(body, "to");
            String subject = extractString(body, "subject");
            String content = extractString(body, "content");
            String type = extractString(body, "type");

            if (to.isEmpty() || subject.isEmpty()) {
                sendError(exchange, "Missing 'to' or 'subject'", 400);
                return;
            }

            // In production: connect to SMTP / Resend / SendGrid here
            // For now: log the email as an audit entry
            System.out.println("[EMAIL] To: " + to + " | Subject: " + subject);

            try {
                AuditLogDAO auditDAO = new AuditLogDAO();
                auditDAO.log("SEND_EMAIL", type.isEmpty() ? "system" : type, to + " — " + subject);
            } catch (Exception ignored) {}

            String json = "{\"success\": true, \"message\": \"Email queued\", \"to\": \"" + escapeJson(to) + "\", \"subject\": \"" + escapeJson(subject) + "\"}";
            sendJson(exchange, json, 200);
        } catch (Exception e) {
            sendError(exchange, e.getMessage(), 500);
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
