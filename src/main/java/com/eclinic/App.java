package com.eclinic;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URLDecoder;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class App {
    private static final int PORT = 8080;

    public static void main(String[] args) throws IOException {
        final Database database = new Database();
        final Client client = new Client(new UserService(database));

        final ExecutorService executor = Executors.newFixedThreadPool(4);
        ServerSocket serverSocket = new ServerSocket(PORT);

        System.out.println("Electronic Clinic API dang chay tao http://localhost:" + PORT);
        System.out.println("Mo frontend/index.html va no se goi API nay.");

        while (true) {
            final Socket socket = serverSocket.accept();
            executor.execute(new Runnable() {
                public void run() {
                    handleClient(socket, client, database);
                }
            });
        }
    }

    private static void handleClient(Socket socket, Client client, Database database) {
        try {
            HttpRequest request = readRequest(socket);
            if (request == null) {
                return;
            }

            if ("/api/register".equals(request.path)) {
                handleRegister(request, socket, client);
                return;
            }

            if ("/api/snapshot".equals(request.path)) {
                handleSnapshot(request, socket, database);
                return;
            }

            if ("/api/health".equals(request.path)) {
                handleHealth(request, socket);
                return;
            }

            sendJson(socket, 404, "{\"error\":\"Not found\"}");
        } catch (Exception e) {
            try {
                sendJson(socket, 500, "{\"error\":\"Internal server error\"}");
            } catch (IOException ignored) {
                // Ignore write failure on already broken socket.
            }
        } finally {
            try {
                socket.close();
            } catch (IOException ignored) {
                // Ignore close error.
            }
        }
    }

    private static HttpRequest readRequest(Socket socket) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream(), "UTF-8"));
        String requestLine = reader.readLine();
        if (requestLine == null || requestLine.length() == 0) {
            return null;
        }

        String[] parts = requestLine.split(" ");
        if (parts.length < 2) {
            return null;
        }

        String method = parts[0].trim();
        String rawPath = parts[1].trim();
        String path = rawPath;
        int queryIdx = rawPath.indexOf('?');
        if (queryIdx >= 0) {
            path = rawPath.substring(0, queryIdx);
        }

        Map<String, String> headers = new HashMap<String, String>();
        String line;
        while ((line = reader.readLine()) != null) {
            if (line.length() == 0) {
                break;
            }
            int colonIndex = line.indexOf(':');
            if (colonIndex > 0) {
                String key = line.substring(0, colonIndex).trim().toLowerCase();
                String value = line.substring(colonIndex + 1).trim();
                headers.put(key, value);
            }
        }

        int contentLength = 0;
        if (headers.containsKey("content-length")) {
            try {
                contentLength = Integer.parseInt(headers.get("content-length"));
            } catch (NumberFormatException ignored) {
                contentLength = 0;
            }
        }

        String body = "";
        if (contentLength > 0) {
            char[] bodyChars = new char[contentLength];
            int totalRead = 0;
            while (totalRead < contentLength) {
                int read = reader.read(bodyChars, totalRead, contentLength - totalRead);
                if (read == -1) {
                    break;
                }
                totalRead += read;
            }
            body = new String(bodyChars, 0, totalRead);
        }

        return new HttpRequest(method, path, headers, body);
    }

    private static void handleRegister(HttpRequest request, Socket socket, Client client) throws IOException {
        if ("OPTIONS".equalsIgnoreCase(request.method)) {
            sendResponse(socket, 204, "", "text/plain; charset=UTF-8");
            return;
        }

        if (!"POST".equalsIgnoreCase(request.method)) {
            sendJson(socket, 405, "{\"error\":\"Method not allowed\"}");
            return;
        }

        Map<String, String> form = parseFormBody(new ByteArrayInputStream(request.body.getBytes("UTF-8")));
        String userType = getOrEmpty(form, "userType");
        String fullName = getOrEmpty(form, "fullName");
        String specialtyOrCondition = getOrEmpty(form, "specialtyOrCondition");

        try {
            User user = client.createNewAccount(userType, fullName, specialtyOrCondition);
            String json = "{"
                    + "\"id\":" + user.getId() + ","
                    + "\"fullName\":\"" + escapeJson(user.getFullName()) + "\"," 
                    + "\"role\":\"" + escapeJson(user.getRole()) + "\""
                    + "}";
            sendJson(socket, 200, json);
        } catch (IllegalArgumentException e) {
            sendJson(socket, 400, "{\"error\":\"" + escapeJson(e.getMessage()) + "\"}");
        } catch (Exception e) {
            sendJson(socket, 500, "{\"error\":\"Internal server error\"}");
        }
    }

    private static void handleSnapshot(HttpRequest request, Socket socket, Database database) throws IOException {
        if ("OPTIONS".equalsIgnoreCase(request.method)) {
            sendResponse(socket, 204, "", "text/plain; charset=UTF-8");
            return;
        }

        if (!"GET".equalsIgnoreCase(request.method)) {
            sendJson(socket, 405, "{\"error\":\"Method not allowed\"}");
            return;
        }

        StringBuilder usersJson = new StringBuilder("[");
        for (int i = 0; i < database.getUsers().size(); i++) {
            User.UserRecord user = database.getUsers().get(i);
            usersJson.append("{")
                    .append("\"id\":").append(user.id()).append(",")
                    .append("\"fullName\":\"").append(escapeJson(user.fullName())).append("\",")
                    .append("\"role\":\"").append(escapeJson(user.role())).append("\"")
                    .append("}");
            if (i < database.getUsers().size() - 1) {
                usersJson.append(",");
            }
        }
        usersJson.append("]");

        StringBuilder detailsJson = new StringBuilder("[");
        boolean hasPrevious = false;
        for (DoctorUser.DoctorRecord doctor : database.getDoctors()) {
            if (hasPrevious) {
                detailsJson.append(",");
            }
            detailsJson.append("{")
                    .append("\"id\":").append(doctor.userId()).append(",")
                    .append("\"type\":\"Doctor\",")
                    .append("\"detail\":\"").append(escapeJson(doctor.specialty())).append("\"")
                    .append("}");
            hasPrevious = true;
        }
        for (PatientUser.PatientRecord patient : database.getPatients()) {
            if (hasPrevious) {
                detailsJson.append(",");
            }
            detailsJson.append("{")
                    .append("\"id\":").append(patient.userId()).append(",")
                    .append("\"type\":\"Patient\",")
                    .append("\"detail\":\"").append(escapeJson(patient.medicalCondition())).append("\"")
                    .append("}");
            hasPrevious = true;
        }
        detailsJson.append("]");

        String json = "{"
                + "\"users\":" + usersJson + ","
                + "\"details\":" + detailsJson
                + "}";
        sendJson(socket, 200, json);
    }

    private static void handleHealth(HttpRequest request, Socket socket) throws IOException {
        if (!"GET".equalsIgnoreCase(request.method)) {
            sendJson(socket, 405, "{\"error\":\"Method not allowed\"}");
            return;
        }
        sendJson(socket, 200, "{\"status\":\"ok\"}");
    }

    private static Map<String, String> parseFormBody(ByteArrayInputStream inputStream) throws IOException {
        String body = readUtf8Body(inputStream);
        Map<String, String> data = new HashMap<String, String>();
        if (isBlank(body)) {
            return data;
        }

        String[] pairs = body.split("&");
        for (int i = 0; i < pairs.length; i++) {
            String pair = pairs[i];
            String[] keyValue = pair.split("=", 2);
            String key = URLDecoder.decode(keyValue[0], "UTF-8");
            String value = keyValue.length > 1
                    ? URLDecoder.decode(keyValue[1], "UTF-8")
                    : "";
            data.put(key, value);
        }
        return data;
    }

    private static String readUtf8Body(ByteArrayInputStream inputStream) throws IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        byte[] chunk = new byte[1024];
        int read;
        while ((read = inputStream.read(chunk)) != -1) {
            buffer.write(chunk, 0, read);
        }
        return new String(buffer.toByteArray(), "UTF-8");
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().length() == 0;
    }

    private static String getOrEmpty(Map<String, String> map, String key) {
        String value = map.get(key);
        return value == null ? "" : value;
    }

    private static String escapeJson(String value) {
        return value
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r");
    }

    private static void sendJson(Socket socket, int statusCode, String jsonBody) throws IOException {
        sendResponse(socket, statusCode, jsonBody, "application/json; charset=UTF-8");
    }

    private static void sendResponse(Socket socket, int statusCode, String body, String contentType) throws IOException {
        byte[] bodyBytes = body.getBytes("UTF-8");
        OutputStream raw = socket.getOutputStream();
        PrintWriter writer = new PrintWriter(new OutputStreamWriter(raw, "UTF-8"));

        writer.print("HTTP/1.1 " + statusCode + " " + statusText(statusCode) + "\r\n");
        writer.print("Content-Type: " + contentType + "\r\n");
        writer.print("Access-Control-Allow-Origin: *\r\n");
        writer.print("Access-Control-Allow-Methods: GET,POST,OPTIONS\r\n");
        writer.print("Access-Control-Allow-Headers: Content-Type\r\n");
        writer.print("Content-Length: " + bodyBytes.length + "\r\n");
        writer.print("Connection: close\r\n");
        writer.print("\r\n");
        writer.flush();
        raw.write(bodyBytes);
        raw.flush();
    }

    private static String statusText(int statusCode) {
        if (statusCode == 200) {
            return "OK";
        }
        if (statusCode == 204) {
            return "No Content";
        }
        if (statusCode == 400) {
            return "Bad Request";
        }
        if (statusCode == 404) {
            return "Not Found";
        }
        if (statusCode == 405) {
            return "Method Not Allowed";
        }
        if (statusCode == 500) {
            return "Internal Server Error";
        }
        return "OK";
    }

    private static final class HttpRequest {
        private final String method;
        private final String path;
        private final Map<String, String> headers;
        private final String body;

        HttpRequest(String method, String path, Map<String, String> headers, String body) {
            this.method = method;
            this.path = path;
            this.headers = headers;
            this.body = body;
        }
    }
}
