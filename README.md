# Clinic Backend + Supabase

This folder is the Clinic backend scaffold wired for Supabase PostgreSQL via JDBC.

## 1) Create schema in Supabase

1. Open Supabase project dashboard.
2. Go to SQL Editor.
3. Paste and run [data/supabase-schema.sql](data/supabase-schema.sql).

## 2) Get connection info from Supabase

In Supabase dashboard, open: Project Settings -> Database.

Use these values:
- Host
- Port
- Database name
- User
- Password

Do not use the REST API endpoint from Project Settings -> API for JDBC.

Build JDBC URL in this format:

```text
jdbc:postgresql://<host>:5432/postgres?sslmode=require
```

If your Supabase URL looks like `https://<project-ref>.supabase.co/rest/v1/`, the JDBC host is usually `db.<project-ref>.supabase.co`.

If Supabase gives you a connection string like `postgresql://postgres:<password>@db.<project-ref>.supabase.co:5432/postgres`, the JDBC version is:

```text
jdbc:postgresql://db.<project-ref>.supabase.co:5432/postgres?sslmode=require
```

## 3) Set environment variables (PowerShell)

Run these in terminal before starting app:

```powershell
$env:SUPABASE_DB_URL="jdbc:postgresql://<host>:5432/postgres?sslmode=require"
$env:SUPABASE_DB_USER="postgres"
$env:SUPABASE_DB_PASSWORD="<your-db-password>"
```

## 4) Compile and run Clinic app

From workspace root:

```powershell
javac -encoding UTF-8 -cp lib/postgresql-42.7.5.jar -d target/classes src/main/java/com/eclinic/*.java
java -cp "target/classes;lib/postgresql-42.7.5.jar" com.eclinic.App
```

If connected, app prints database name, user, and server time.

## Notes

- `App.java` supports env vars and JVM properties:
  - `SUPABASE_DB_URL` or `-Dsupabase.db.url`
  - `SUPABASE_DB_USER` or `-Dsupabase.db.user`
  - `SUPABASE_DB_PASSWORD` or `-Dsupabase.db.password`
- Supabase PostgreSQL requires SSL; keep `sslmode=require` in JDBC URL.
