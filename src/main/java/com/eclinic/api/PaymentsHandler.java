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
        String sql = "SELECT p.id, p.medical_record_id, p.total_price, p.payment_status, p.paid_at, p.created_at, " +
            "pat.full_name as patient_name, d.full_name as doctor_name " +
            "FROM prescriptions p " +
            "LEFT JOIN medical_records mr ON p.medical_record_id = mr.id " +
            "LEFT JOIN appointments a ON mr.appointment_id = a.id " +
            "LEFT JOIN patients pat ON a.patient_id = pat.id " +
            "LEFT JOIN doctors d ON a.doctor_id = d.id " +
            "ORDER BY p.created_at DESC";

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

                long id = rs.getLong("id");
                String patientName = rs.getString("patient_name");
                String doctorName = rs.getString("doctor_name");
                double totalPrice = rs.getDouble("total_price");
                String status = rs.getString("payment_status");
                String paidAt = rs.getString("paid_at");
                String createdAt = rs.getString("created_at");

                if (patientName == null) patientName = "N/A";
                if (doctorName == null) doctorName = "N/A";
                if (status == null) status = "UNPAID";

                List details = detailDao.findByPrescriptionId(id);
                String itemsJson = buildItemsJson(details, medDao);

                sb.append("{");
                sb.append("\"id\": ").append(id).append(", ");
                sb.append("\"prescriptionId\": ").append(id).append(", ");
                sb.append("\"patientName\": \"").append(escapeJson(patientName)).append("\", ");
                sb.append("\"doctorName\": \"").append(escapeJson(doctorName)).append("\", ");
                sb.append("\"totalPrice\": ").append(totalPrice).append(", ");
                sb.append("\"status\": \"").append(status).append("\", ");
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
        String sql = "UPDATE prescriptions SET payment_status = 'PAID', paid_at = NOW() WHERE id = ? AND (payment_status IS NULL OR payment_status = 'UNPAID')";
        Connection conn = ConnectionManager.getConnection();
        try {
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setLong(1, id);
            int rows = stmt.executeUpdate();
            if (rows > 0) {
                try {
                    AuditLogDAO auditDAO = new AuditLogDAO();
                    auditDAO.log("CONFIRM_PAYMENT", "admin", "Hóa đơn #" + id);
                } catch (Exception ignored) {}
                sendJson(exchange, "{\"status\": \"confirmed\"}", 200);
            } else {
                sendError(exchange, "Payment not found or already paid", 404);
            }
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
