package com.eclinic.api;

import com.sun.net.httpserver.HttpExchange;
import com.eclinic.dao.UserDAO;
import com.eclinic.dao.DoctorDAO;
import com.eclinic.dao.AppointmentDAO;
import com.eclinic.models.User;
import java.io.IOException;
import java.util.List;

public class DashboardHandler extends BaseHandler {

    protected void handleRequest(HttpExchange exchange) throws IOException {
        String method = exchange.getRequestMethod();


        if (!"GET".equals(method)) {
            sendError(exchange, "Method not allowed", 405);
            return;
        }

        try {
            UserDAO userDAO = new UserDAO();
            DoctorDAO doctorDAO = new DoctorDAO();
            AppointmentDAO aptDAO = new AppointmentDAO();

            List allUsers = userDAO.findAll();
            List allDoctors = doctorDAO.findAll();

            int totalUsers = allUsers.size();
            int totalDoctors = allDoctors.size();
            int totalPatients = 0;
            for (int i = 0; i < allUsers.size(); i++) {
                User u = (User) allUsers.get(i);
                if ("PATIENT".equals(u.getRole())) {
                    totalPatients++;
                }
            }

            String json = "{" +
                "\"totalUsers\": " + totalUsers + ", " +
                "\"totalDoctors\": " + totalDoctors + ", " +
                "\"totalPatients\": " + totalPatients + ", " +
                "\"todayAppointments\": 0, " +
                "\"revenue\": 0" +
                "}";

            sendJson(exchange, json, 200);
        } catch (Exception e) {
            sendError(exchange, e.getMessage(), 500);
        }
    }
}
