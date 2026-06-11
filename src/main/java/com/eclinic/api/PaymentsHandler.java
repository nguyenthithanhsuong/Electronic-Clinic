package com.eclinic.api;

import com.sun.net.httpserver.HttpExchange;
import com.eclinic.dao.PrescriptionDAO;
import com.eclinic.dao.PrescriptionDetailDAO;
import com.eclinic.dao.AuditLogDAO;
import com.eclinic.dao.MedicineDAO;
import com.eclinic.models.Prescription;
import com.eclinic.models.PrescriptionDetail;
import com.eclinic.models.Medicine;
import com.eclinic.database.ConnectionManager;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.List;

public class PaymentsHandler extends BaseHandler {

    protected void handleRequest(HttpExchange exchange) throws IOException {
        String method = exchange.getRequestMethod();
        String path = exchange.getRequestURI().getPath();

        // RBAC: payments — ADMIN for confirm, any authenticated role can view
        if ("POST".equals(method)) {
            if (!requireRole(exchange, "ADMIN", "RECEPTIONIST")) return;
        }

        try {
            if ("GET".equals(method) && "/api/payments".equals(path)) {
                handleGetAll(exchange);
            } else if ("POST".equals(method) && path.matches("/api/payments/\\d+/confirm")) {
                long id = extractIdFromConfirmPath(path);
                handleConfirm(exchange, id);
            } else {
                sendError(exchange, "Method not allowed", 405);
            }
        } catch (Exception e) {
            sendError(exchange, e.getMessage(), 500);
        }
    }

    private void handleGetAll(HttpExchange exchange) throws Exception {
        String sql = "SELECT pay.id AS payment_id, COALESCE(pay.amount, pres.total_price) AS amount, " +
            "COALESCE(pay.payment_status, 'PENDING') AS payment_status, COALESCE(pay.payment_method, 'CASH') AS payment_method, " +
            "pay.paid_at, COALESCE(pay.created_at, pres.created_at) AS payment_created_at, " +
            "pres.id as prescription_id, pres.medical_record_id, pres.total_price, " +
            "pat.full_name as patient_name, d.full_name as doctor_name " +
            "FROM prescriptions pres " +
            "LEFT JOIN payments pay ON pay.prescription_id = pres.id " +
            "LEFT JOIN medical_records mr ON pres.medical_record_id = mr.id " +
            "LEFT JOIN appointments a ON mr.appointment_id = a.id " +
            "LEFT JOIN patients pat ON a.patient_id = pat.id " +
            "LEFT JOIN doctors d ON a.doctor_id = d.id " +
            "ORDER BY COALESCE(pay.created_at, pres.created_at) DESC";

        Connection conn = ConnectionManager.getConnection();
        try {
            PreparedStatement stmt = conn.prepareStatement(sql);
            ResultSet rs = stmt.executeQuery();

            PrescriptionDetailDAO detailDao = new PrescriptionDetailDAO();
            MedicineDAO medDao = new MedicineDAO();

            StringBuilder sb = new StringBuilder("[");
            boolean first = true;
            while (rs.next()) {
                if (!first) sb.append(",");
                first = false;

                long prescriptionId = rs.getLong("prescription_id");
                long paymentId = rs.getLong("payment_id");
                if (rs.wasNull()) paymentId = prescriptionId;
                String patientName = rs.getString("patient_name");
                String doctorName = rs.getString("doctor_name");
                double totalPrice = rs.getDouble("total_price");
                double amount = rs.getDouble("amount");
                String status = rs.getString("payment_status");
                String paymentMethod = rs.getString("payment_method");
                String paidAt = rs.getString("paid_at");
                String createdAt = rs.getString("payment_created_at");

                if (patientName == null) patientName = "N/A";
                if (doctorName == null) doctorName = "N/A";
                if (status == null) status = "PENDING";

                List details = detailDao.findByPrescriptionId(prescriptionId);
                String itemsJson = buildItemsJson(details, medDao);

                sb.append("{");
                sb.append("\"id\": ").append(paymentId).append(", ");
                sb.append("\"prescriptionId\": ").append(prescriptionId).append(", ");
                sb.append("\"patientName\": \"").append(escapeJson(patientName)).append("\", ");
                sb.append("\"doctorName\": \"").append(escapeJson(doctorName)).append("\", ");
                sb.append("\"totalPrice\": ").append(totalPrice).append(", ");
                sb.append("\"amount\": ").append(amount).append(", ");
                sb.append("\"status\": \"").append(status).append("\", ");
                sb.append("\"paymentMethod\": \"").append(paymentMethod != null ? paymentMethod : "CASH").append("\", ");
                sb.append("\"createdAt\": \"").append(escapeJson(createdAt)).append("\", ");
                if (paidAt != null) {
                    sb.append("\"paidAt\": \"").append(escapeJson(paidAt)).append("\", ");
                }
                sb.append("\"items\": ").append(itemsJson);
                sb.append("}");
            }
            sb.append("]");

            sendJson(exchange, sb.toString(), 200);
        } finally {
            ConnectionManager.closeConnection(conn);
        }
    }

    private void handleConfirm(HttpExchange exchange, long id) throws Exception {
        // id is the prescription_id — upsert into payments table
        String checkSql = "SELECT id, payment_status FROM payments WHERE prescription_id = ?";
        Connection conn = ConnectionManager.getConnection();
        try {
            PreparedStatement checkStmt = conn.prepareStatement(checkSql);
            checkStmt.setLong(1, id);
            ResultSet rs = checkStmt.executeQuery();

            if (rs.next()) {
                // Payment record exists — update if not already confirmed
                long payId = rs.getLong("id");
                String currentStatus = rs.getString("payment_status");
                if ("CONFIRMED".equals(currentStatus)) {
                    sendError(exchange, "Payment already confirmed", 409);
                    return;
                }
                String updateSql = "UPDATE payments SET payment_status = 'CONFIRMED', paid_at = NOW() WHERE id = ?";
                PreparedStatement updateStmt = conn.prepareStatement(updateSql);
                updateStmt.setLong(1, payId);
                updateStmt.executeUpdate();
            } else {
                // No payment record yet — create one as CONFIRMED
                String insertSql = "INSERT INTO payments (prescription_id, amount, payment_status, paid_at) " +
                    "SELECT id, total_price, 'CONFIRMED', NOW() FROM prescriptions WHERE id = ?";
                PreparedStatement insertStmt = conn.prepareStatement(insertSql);
                insertStmt.setLong(1, id);
                int rows = insertStmt.executeUpdate();
                if (rows == 0) {
                    sendError(exchange, "Prescription not found", 404);
                    return;
                }
            }

            try {
                AuditLogDAO auditDAO = new AuditLogDAO();
                auditDAO.log("CONFIRM_PAYMENT", "admin", "Hóa đơn thuốc #" + id);
            } catch (Exception ignored) {}
            sendJson(exchange, "{\"status\": \"confirmed\"}", 200);
        } finally {
            ConnectionManager.closeConnection(conn);
        }
    }

    private String buildItemsJson(List details, MedicineDAO medDao) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < details.size(); i++) {
            if (i > 0) sb.append(",");
            PrescriptionDetail d = (PrescriptionDetail) details.get(i);
            String medName = "Unknown";
            double unitPrice = 0;
            try {
                Medicine med = medDao.findById(d.getMedicineId());
                if (med != null) {
                    medName = med.getName();
                    unitPrice = med.getPrice().doubleValue();
                }
            } catch (Exception ignored) {}

            double subtotal = unitPrice * d.getQuantity();
            sb.append("{");
            sb.append("\"medicineName\": \"").append(escapeJson(medName)).append("\", ");
            sb.append("\"quantity\": ").append(d.getQuantity()).append(", ");
            sb.append("\"unitPrice\": ").append(unitPrice).append(", ");
            sb.append("\"subtotal\": ").append(subtotal);
            sb.append("}");
        }
        sb.append("]");
        return sb.toString();
    }

    private long extractIdFromConfirmPath(String path) {
        // /api/payments/123/confirm
        String[] parts = path.split("/");
        return Long.parseLong(parts[3]);
    }
}
