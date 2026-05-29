package com.eclinic;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.InetAddress;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;

public class App {
    public static void main(String[] args) {
        Map<String, String> dotenv = loadDotEnv();

        String dbUrl = getConfig(dotenv, "SUPABASE_DB_URL", "supabase.db.url");
        String dbUser = getConfig(dotenv, "SUPABASE_DB_USER", "supabase.db.user");
        String dbPassword = getConfig(dotenv, "SUPABASE_DB_PASSWORD", "supabase.db.password");

        if (isBlank(dbUrl) || isBlank(dbUser) || isBlank(dbPassword)) {
            System.err.println("Missing Supabase DB config.");
            System.err.println("Set env vars SUPABASE_DB_URL, SUPABASE_DB_USER, SUPABASE_DB_PASSWORD");
            System.err.println("or pass JVM properties -Dsupabase.db.url=... -Dsupabase.db.user=... -Dsupabase.db.password=...");
            return;
        }

        System.out.println("Loaded DB config from .env/env/system properties.");
        System.out.println("DB URL: " + maskUrl(dbUrl));
        System.out.println("DB user: " + dbUser);

        if (!dbUrl.startsWith("jdbc:postgresql://")) {
            System.err.println("SUPABASE_DB_URL must be the PostgreSQL JDBC connection string, not the Supabase HTTP API URL.");
            System.err.println("Expected format: jdbc:postgresql://<host>:5432/postgres?sslmode=require");
            System.err.println("Check your .env file and replace any https://...supabase.co/rest/v1/... value.");
            return;
        }

        String host = extractHost(dbUrl);
        if (!isBlank(host)) {
            try {
                InetAddress[] addresses = InetAddress.getAllByName(host);
                System.out.println("Resolved host: " + host + " (" + addresses.length + " address(es))");
                for (int i = 0; i < addresses.length; i++) {
                    System.out.println("  - " + addresses[i].getHostAddress());
                }
                if (addresses.length > 0 && addresses[0].getHostAddress().indexOf(':') >= 0) {
                    System.out.println("Note: the DB host resolved to IPv6 only. If your network blocks IPv6, use Supabase's pooler connection string instead.");
                }
            } catch (Exception e) {
                System.err.println("Could not resolve DB host: " + host + " - " + e.getMessage());
                System.err.println("Use the exact database host from Supabase Dashboard -> Project Settings -> Database, or the pooler endpoint.");
                return;
            }
        }

        Connection connection = null;
        Statement statement = null;
        ResultSet rs = null;
        try {
            connection = DriverManager.getConnection(dbUrl, dbUser, dbPassword);
            statement = connection.createStatement();
            rs = statement.executeQuery("SELECT current_database(), current_user, NOW()");

            if (rs.next()) {
                System.out.println("Connected to Supabase PostgreSQL successfully.");
                System.out.println("Database: " + rs.getString(1));
                System.out.println("User: " + rs.getString(2));
                System.out.println("Server time: " + rs.getString(3));
            }
        } catch (Exception e) {
            System.err.println("Failed to connect to Supabase PostgreSQL: " + e.getMessage());
            e.printStackTrace();
        } finally {
            closeQuietlyResultSet(rs);
            closeQuietlyStatement(statement);
            closeQuietlyConnection(connection);
        }
    }

    private static String getConfig(Map<String, String> dotenv, String envKey, String systemPropertyKey) {
        String fromSystemProperty = System.getProperty(systemPropertyKey);
        if (!isBlank(fromSystemProperty)) {
            return fromSystemProperty;
        }
        String fromProcessEnv = System.getenv(envKey);
        if (!isBlank(fromProcessEnv)) {
            return fromProcessEnv;
        }
        Object fromDotEnv = dotenv.get(envKey);
        if (fromDotEnv == null) {
            return null;
        }
        return String.valueOf(fromDotEnv);
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().length() == 0;
    }

    private static String maskUrl(String url) {
        if (isBlank(url)) {
            return "";
        }

        int atIndex = url.indexOf('@');
        if (atIndex < 0) {
            return url;
        }

        int schemeIndex = url.indexOf("//");
        if (schemeIndex < 0 || schemeIndex + 2 >= atIndex) {
            return url;
        }

        return url.substring(0, schemeIndex + 2) + "***:***@" + url.substring(atIndex + 1);
    }

    private static String extractHost(String jdbcUrl) {
        int schemeIndex = jdbcUrl.indexOf("//");
        if (schemeIndex < 0) {
            return null;
        }

        int hostStart = schemeIndex + 2;
        int hostEnd = jdbcUrl.indexOf(':', hostStart);
        if (hostEnd < 0) {
            hostEnd = jdbcUrl.indexOf('/', hostStart);
        }
        if (hostEnd < 0 || hostEnd <= hostStart) {
            return null;
        }
        return jdbcUrl.substring(hostStart, hostEnd);
    }

    private static Map<String, String> loadDotEnv() {
        Map<String, String> values = new HashMap<String, String>();
        File[] candidates = new File[] {
            new File(".env")
        };

        for (int i = 0; i < candidates.length; i++) {
            File candidate = candidates[i];
            if (!candidate.exists() || !candidate.isFile()) {
                continue;
            }

            BufferedReader reader = null;
            try {
                reader = new BufferedReader(new FileReader(candidate));
                String line;
                while ((line = reader.readLine()) != null) {
                    line = line.trim();
                    if (line.length() == 0 || line.startsWith("#")) {
                        continue;
                    }

                    int equalsIndex = line.indexOf('=');
                    if (equalsIndex <= 0) {
                        continue;
                    }

                    String key = line.substring(0, equalsIndex).trim();
                    String value = line.substring(equalsIndex + 1).trim();

                    if (value.length() >= 2) {
                        boolean quotedWithDouble = value.startsWith("\"") && value.endsWith("\"");
                        boolean quotedWithSingle = value.startsWith("'") && value.endsWith("'");
                        if (quotedWithDouble || quotedWithSingle) {
                            value = value.substring(1, value.length() - 1);
                        }
                    }

                    values.put(key, value);
                }
            } catch (IOException e) {
                System.err.println("Failed to read .env file: " + candidate.getPath() + " - " + e.getMessage());
            } finally {
                if (reader != null) {
                    try {
                        reader.close();
                    } catch (IOException ignored) {
                        // Ignore close exceptions from .env reader.
                    }
                }
            }

            if (!values.isEmpty()) {
                break;
            }
        }

        return values;
    }

    private static void closeQuietlyResultSet(ResultSet rs) {
        if (rs == null) {
            return;
        }
        try {
            rs.close();
        } catch (Exception ignored) {
            // Ignore close exceptions on shutdown path.
        }
    }

    private static void closeQuietlyStatement(Statement statement) {
        if (statement == null) {
            return;
        }
        try {
            statement.close();
        } catch (Exception ignored) {
            // Ignore close exceptions on shutdown path.
        }
    }

    private static void closeQuietlyConnection(Connection connection) {
        if (connection == null) {
            return;
        }
        try {
            connection.close();
        } catch (Exception ignored) {
            // Ignore close exceptions on shutdown path.
        }
    }
}
