# Clinic Electronic Health Record System - Complete Setup Guide

## System Architecture

```
┌─────────────────────────────────────────────────────────────┐
│  Frontend (HTML/JavaScript)                                  │
│  - Dashboard                                                 │
│  - Doctors Management (CRUD)                                │
│  - Patients Management (CRUD + Medical History)             │
│  - Appointments (Calendar, Filters)                         │
│  - Medicine Inventory Management                            │
└──────────────────────┬──────────────────────────────────────┘
                       │ HTTP REST (JSON)
                       │
┌──────────────────────▼──────────────────────────────────────┐
│  REST API Server (Java - SimpleRestServer)                  │
│  - Port 3001                                                 │
│  - Routes: /api/dashboard, /api/doctors, /api/patients, etc  │
└──────────────────────┬──────────────────────────────────────┘
                       │ JDBC/SQL
                       │
┌──────────────────────▼──────────────────────────────────────┐
│  Data Access Layer (DAOs)                                    │
│  - UserDAO, DoctorDAO, PatientDAO, AppointmentDAO           │
│  - MedicineDAO, MedicalRecordDAO, PrescriptionDAO           │
└──────────────────────┬──────────────────────────────────────┘
                       │ PostgreSQL JDBC
                       │
┌──────────────────────▼──────────────────────────────────────┐
│  Supabase PostgreSQL Database                               │
│  - 8 tables: users, doctors, patients, appointments, etc     │
│  - Session Pooler: aws-1-ap-south-1.pooler.supabase.com     │
└─────────────────────────────────────────────────────────────┘
```

## Quick Start

### 1. Prerequisites
- Java 1.8+ installed
- Supabase account with database schema initialized
- PostgreSQL JDBC driver (included: `lib/postgresql-42.7.5.jar`)

### 2. Start the REST API Server

From the workspace root:
```powershell
java -cp "target/classes;lib/postgresql-42.7.5.jar" com.eclinic.api.SimpleRestServer
```

Expected output:
```
Initializing database connection...
Database connection initialized.
Starting REST API server on http://localhost:3001
REST API server started. Open http://localhost:3001 in browser.
```

### 3. Access the Frontend

Open browser to: **http://localhost:3001**

Available pages:
- **Dashboard** (`/`) - System statistics and test suite
- **Doctors** (`/doctors.html`) - Doctor CRUD operations
- **Patients** (`/patients.html`) - Patient management with medical history
- **Appointments** (`/appointments.html`) - Schedule/manage appointments
- **Medicines** (`/medicines.html`) - Pharmacy inventory

### 4. Run CRUD Tests

Each page has a "Run CRUD Tests" button that:
1. Creates a test record
2. Reads it back
3. Updates it
4. Lists all records
5. Deletes it

Results are logged in the test panel on each page.

## REST API Endpoints

### Dashboard
```
GET /api/dashboard
Returns: { totalUsers, totalDoctors, totalPatients, todayAppointments, revenue }
```

### Doctors (CRUD)
```
GET  /api/doctors              → List all doctors
POST /api/doctors              → Create doctor
GET  /api/doctors/{id}         → Get doctor by ID
PUT  /api/doctors/{id}         → Update doctor
DELETE /api/doctors/{id}       → Delete doctor
```

Example POST body:
```json
{
  "userId": 1,
  "fullName": "Dr. John Smith",
  "specialty": "Cardiology",
  "phone": "555-0001",
  "email": "john@clinic.com",
  "roomNumber": "Room 101"
}
```

### Patients (CRUD + History)
```
GET  /api/patients             → List all patients
POST /api/patients             → Create patient
GET  /api/patients/{id}        → Get patient by ID
PUT  /api/patients/{id}        → Update patient
DELETE /api/patients/{id}      → Delete patient
GET  /api/appointments?patientId={id} → Get patient's appointments
```

Example POST body:
```json
{
  "userId": null,
  "fullName": "Jane Doe",
  "dob": "1990-05-15",
  "gender": "FEMALE",
  "phone": "555-5678",
  "address": "123 Main St",
  "insuranceCode": "INS12345"
}
```

### Appointments (CRUD + Filters)
```
GET  /api/appointments         → List all appointments
POST /api/appointments         → Schedule appointment
GET  /api/appointments/{id}    → Get appointment by ID
PUT  /api/appointments/{id}    → Update appointment status
DELETE /api/appointments/{id}  → Cancel appointment
GET  /api/appointments?patientId={id} → Filter by patient
```

Example POST body:
```json
{
  "doctorId": 1,
  "patientId": 1,
  "appointmentStartDate": "2026-05-15 10:00:00",
  "appointmentEndDate": "2026-05-15 11:00:00",
  "reason": "Annual checkup",
  "status": "PENDING"
}
```

### Medicines (CRUD + Stock)
```
GET  /api/medicines            → List all medicines
POST /api/medicines            → Add medicine
GET  /api/medicines/{id}       → Get medicine by ID
PUT  /api/medicines/{id}       → Update stock
DELETE /api/medicines/{id}     → Delete medicine
```

Example POST body:
```json
{
  "name": "Aspirin",
  "unit": "tablets",
  "price": 5.99,
  "stockQuantity": 100,
  "expiryDate": "2026-12-31"
}
```

## Testing with PowerShell

### Get Dashboard Stats
```powershell
Invoke-WebRequest -Uri "http://localhost:3001/api/dashboard" -Method GET | Select-Object -ExpandProperty Content
```

### Create Doctor
```powershell
$body = @"
{
  "userId": 1,
  "fullName": "Dr. Test",
  "specialty": "General",
  "phone": "555-0001",
  "email": "test@clinic.com",
  "roomNumber": "Room 1"
}
"@
Invoke-WebRequest -Uri "http://localhost:3001/api/doctors" -Method POST -ContentType "application/json" -Body $body -UseBasicParsing | Select-Object -ExpandProperty Content
```

### List Patients
```powershell
Invoke-WebRequest -Uri "http://localhost:3001/api/patients" -Method GET -UseBasicParsing | Select-Object -ExpandProperty Content | ConvertFrom-Json
```

## File Structure

```
src/main/java/com/eclinic/
│   ├── App.java                    # Connection test entry point
│   ├── api/
│   │   └── SimpleRestServer.java   # REST API server (port 3001)
│   ├── models/                     # Data classes (8 POJO models)
│   ├── dao/                        # Data access objects (CRUD)
│   └── database/
│       └── ConnectionManager.java  # JDBC connection management
├── frontend-mock/
│   ├── index.html                  # Dashboard
│   ├── doctors.html                # Doctor CRUD
│   ├── patients.html               # Patient management
│   ├── appointments.html           # Appointments
│   ├── medicines.html              # Pharmacy inventory
│   ├── common.js                   # Shared API client functions
│   └── styles.css                  # Basic styling
├── lib/
│   └── postgresql-42.7.5.jar       # PostgreSQL JDBC driver
├── target/classes/                 # Compiled .class files
└── data/
    └── supabase-schema.sql         # Database schema (run in Supabase)
```

## Compilation

Compile everything (backend + REST API):
```powershell
javac -encoding UTF-8 -cp lib/postgresql-42.7.5.jar -d target/classes `
  src/main/java/com/eclinic/*.java `
  src/main/java/com/eclinic/models/*.java `
  src/main/java/com/eclinic/database/*.java `
  src/main/java/com/eclinic/dao/*.java `
  src/main/java/com/eclinic/api/SimpleRestServer.java
```

## Database Schema

8 tables with relationships:
- **users** - Authentication & roles (ADMIN, DOCTOR, PATIENT)
- **doctors** - Doctor profiles (linked to users)
- **patients** - Patient records (with insurance info)
- **appointments** - Appointment scheduling
- **medical_records** - Clinical notes & diagnoses
- **medicines** - Pharmacy inventory
- **prescriptions** - Prescription headers
- **prescription_details** - Line items for prescriptions

To initialize the database, run the SQL from `data/supabase-schema.sql` in your Supabase SQL Editor.

## Example Workflow

1. **Create a Doctor User**:
   ```
   POST /api/doctors
   { userId: 1, fullName: "Dr. Smith", ... }
   → Returns: { id: 123, status: "created" }
   ```

2. **Create a Patient**:
   ```
   POST /api/patients
   { fullName: "John Patient", ... }
   → Returns: { id: 456, status: "created" }
   ```

3. **Schedule an Appointment**:
   ```
   POST /api/appointments
   { doctorId: 123, patientId: 456, ... }
   → Returns: { id: 789, status: "created" }
   ```

4. **View Patient Medical History**:
   ```
   GET /api/appointments?patientId=456
   → Returns: [{ id: 789, reason: "...", status: "COMPLETED", ... }]
   ```

## Troubleshooting

### REST Server won't start
- Ensure port 3001 is not in use: `netstat -an | findstr :3001`
- Check `.env` file has correct Supabase credentials
- Verify PostgreSQL JDBC driver is in `lib/`

### Frontend pages don't load
- REST server must be running on localhost:3001
- Check browser console for CORS errors
- Verify REST server is listening: `curl http://localhost:3001/`

### Database operations fail
- Run `data/supabase-schema.sql` in Supabase SQL Editor first
- Verify Supabase credentials in `.env`
- Check Supabase session pooler is configured (not direct connection)

## Features Implemented

✓ Complete REST API with 8 endpoints
✓ Full CRUD operations for 5 main entities
✓ JSON request/response handling
✓ Database connection pooling via ConnectionManager
✓ Frontend pages with forms and tables
✓ Integrated test suites on each page
✓ Filter capabilities (by patient, status, date)
✓ Medical history view for patients
✓ Appointment scheduling
✓ Medicine inventory management

## Next Steps

1. Add authentication (login page)
2. Implement business logic layer (services)
3. Add more complex queries (by date range, doctor workload, etc)
4. Create reports (revenue, occupancy, etc)
5. Add real-time notifications
6. Deploy to production server
