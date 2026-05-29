# Clinic Backend - Complete Data Access Layer

This folder contains the complete Clinic electronic health record backend with JDBC connectivity to Supabase PostgreSQL and a full data access object (DAO) layer.

## Folder Structure

```
src/main/java/com/eclinic/
├── App.java                          # Main entry point with JDBC setup
├── models/                           # Data model classes (8 tables)
│   ├── User.java                     # Users table model
│   ├── Doctor.java                   # Doctors table model
│   ├── Patient.java                  # Patients table model
│   ├── Appointment.java              # Appointments table model
│   ├── MedicalRecord.java            # Medical_Records table model
│   ├── Medicine.java                 # Medicines table model
│   ├── Prescription.java             # Prescriptions table model
│   └── PrescriptionDetail.java       # Prescription_Details table model
├── dao/                              # Data access objects (CRUD operations)
│   ├── UserDAO.java                  # User CRUD, find by username, list all
│   ├── DoctorDAO.java                # Doctor CRUD, find by ID, list all
│   ├── PatientDAO.java               # Patient CRUD, find by ID, list all
│   ├── AppointmentDAO.java           # Appointment CRUD, find by patient, update status
│   ├── MedicineDAO.java              # Medicine CRUD, update stock
│   ├── MedicalRecordDAO.java         # Medical record CRUD, find by appointment
│   ├── PrescriptionDAO.java          # Prescription CRUD, find by medical record
│   └── PrescriptionDetailDAO.java    # Prescription detail CRUD, find by prescription
└── database/
    └── ConnectionManager.java        # Centralized JDBC connection pool init
```

## Database Schema (8 Tables)

All tables are auto-created via SQL in `data/supabase-schema.sql`. Run this SQL in Supabase before using the backend.

| Table | Columns | Purpose |
|-------|---------|---------|
| **users** | id, username, password_hash, role, status, created_at | Core user authentication (ADMIN, DOCTOR, PATIENT) |
| **doctors** | id, user_id, full_name, specialty, phone, email, room_number, created_at | Doctor profile linked to users |
| **patients** | id, user_id, full_name, dob, gender, phone, address, insurance_code, created_at | Patient profile |
| **appointments** | id, doctor_id, patient_id, appointment_start_date, appointment_end_date, reason, status, created_at | Schedule appointments (PENDING, CONFIRMED, COMPLETED, CANCELLED) |
| **medical_records** | id, appointment_id, symptoms, diagnosis, record_type, treatment_plan, created_at | Medical notes (GENERAL, EMERGENCY, DENTAL) |
| **medicines** | id, name, unit, price, stock_quantity, expiry_date | Pharmacy inventory |
| **prescriptions** | id, medical_record_id, notes, total_price, created_at | Prescription header |
| **prescription_details** | id, prescription_id, medicine_id, quantity, dosage | Prescription line items |

## How to Use the Backend

### 1) Initialize Connection

```java
import com.eclinic.database.ConnectionManager;

ConnectionManager.init(
    "jdbc:postgresql://aws-1-ap-south-1.pooler.supabase.com:5432/postgres?sslmode=require",
    "postgres.jmzhakwlvnuqbqslgyzg",
    "your-password"
);
```

### 2) Use Any DAO

```java
import com.eclinic.dao.UserDAO;
import com.eclinic.dao.DoctorDAO;
import com.eclinic.dao.PatientDAO;
import com.eclinic.models.User;
import com.eclinic.models.Doctor;

// Create a user
UserDAO userDAO = new UserDAO();
long userId = userDAO.create("dr_smith", "hashed_pwd", "DOCTOR", "ACTIVE");

// Find by ID
User user = userDAO.findById(userId);

// Find by username
User foundUser = userDAO.findByUsername("dr_smith");

// List all
java.util.List allUsers = userDAO.findAll();

// Update
userDAO.update(userId, "DOCTOR", "INACTIVE");

// Delete
userDAO.delete(userId);
```

### 3) Full Workflow Example: Create a Doctor + Appointment

```java
UserDAO userDAO = new UserDAO();
DoctorDAO doctorDAO = new DoctorDAO();
PatientDAO patientDAO = new PatientDAO();
AppointmentDAO appointmentDAO = new AppointmentDAO();

// 1. Create doctor user
long doctorUserId = userDAO.create("dr_johnson", "pwd123", "DOCTOR", "ACTIVE");

// 2. Create doctor profile
long doctorId = doctorDAO.create(
    doctorUserId, 
    "Dr. Robert Johnson", 
    "Internal Medicine",
    "555-0001",
    "johnson@hospital.com",
    "Room 105"
);

// 3. Create patient
long patientId = patientDAO.create(
    null,
    "John Patient",
    "1980-01-01",
    "MALE",
    "555-9999",
    "456 Oak Ave",
    "INS98765"
);

// 4. Schedule appointment
long appointmentId = appointmentDAO.create(
    doctorId,
    patientId,
    "2026-05-10 14:00:00",
    "2026-05-10 15:00:00",
    "Annual checkup",
    "PENDING"
);

// 5. Confirm appointment
appointmentDAO.updateStatus(appointmentId, "CONFIRMED");
```

## Compilation

From workspace root:

```powershell
javac -encoding UTF-8 -cp lib/postgresql-42.7.5.jar -d target/classes `
    src/main/java/com/eclinic/*.java `
    src/main/java/com/eclinic/models/*.java `
    src/main/java/com/eclinic/dao/*.java `
    src/main/java/com/eclinic/database/*.java
```

## Run

```powershell
java -cp "target/classes;lib/postgresql-42.7.5.jar" com.eclinic.App
```

## Key Design Patterns

1. **DAO Pattern**: Each table has its own DAO class with CRUD methods.
2. **Model Classes**: Plain Java objects matching table schemas, with getters/setters.
3. **Centralized Connection**: `ConnectionManager` manages the JDBC pool initialization.
4. **Query Methods**: Each DAO supports find by ID, find all, create, update, delete, and specialized queries (findByUsername, findByAppointmentId, etc.).
5. **Exception Handling**: DAOs throw `SQLException` for caller handling.

## Environment Variables

The App reads from [.env](.env):

```
SUPABASE_DB_URL=jdbc:postgresql://aws-1-ap-south-1.pooler.supabase.com:5432/postgres?sslmode=require
SUPABASE_DB_USER=postgres.jmzhakwlvnuqbqslgyzg
SUPABASE_DB_PASSWORD=your-db-password
```

## Next Steps

1. Extend DAOs with business logic (e.g., `findAppointmentsByDoctor()`)
2. Add service layer for complex workflows
3. Implement REST API endpoints wrapping DAOs
4. Add caching or connection pooling for production use
5. Integrate with frontend via REST endpoints
