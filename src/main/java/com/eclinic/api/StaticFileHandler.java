package com.eclinic.api;

import com.sun.net.httpserver.HttpExchange;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;

public class StaticFileHandler extends BaseHandler {

    /** Static files do NOT require authentication. */
    @Override
    protected boolean requiresAuth() {
        return false;
    }

    protected void handleRequest(HttpExchange exchange) throws IOException {
        String path = exchange.getRequestURI().getPath();
        
        if ("/".equals(path)) {
            path = "/index.html";
        }

        String filePath = "frontend-mock" + path;
        File file = new File(filePath);

        if (!file.exists()) {
            sendError(exchange, "File not found", 404);
            return;
        }

        if (file.isDirectory()) {
            sendError(exchange, "Directory listing not allowed", 403);
            return;
        }

        String mimeType = getMimeType(filePath);
        exchange.getResponseHeaders().set("Content-Type", mimeType);
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");

        byte[] fileBytes = readFile(file);
        exchange.sendResponseHeaders(200, fileBytes.length);
        OutputStream os = exchange.getResponseBody();
        os.write(fileBytes);
        os.close();
    }

    private String getMimeType(String filePath) {
        if (filePath.endsWith(".html")) return "text/html; charset=UTF-8";
        if (filePath.endsWith(".css")) return "text/css; charset=UTF-8";
        if (filePath.endsWith(".js")) return "application/javascript; charset=UTF-8";
        if (filePath.endsWith(".json")) return "application/json";
        return "text/plain; charset=UTF-8";
    }

    private byte[] readFile(File file) throws IOException {
        FileInputStream fis = new FileInputStream(file);
        byte[] data = new byte[(int) file.length()];
        fis.read(data);
        fis.close();
        return data;
    }
}
