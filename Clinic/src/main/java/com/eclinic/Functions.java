package com.eclinic;

import com.eclinic.dao.*;
import com.eclinic.models.*;
import java.sql.SQLException;
import java.math.BigDecimal;
import java.util.List;

/**
 * Facade class for easy access to all CRUD operations
 * Provides simple, high-level methods for interacting with the clinic database
 */
public class Functions {

    public static long CreateUser(String username, String passwordHash, String role, String status) throws SQLException {
        return createUser(username, passwordHash, role, status);
    }

    public static boolean EditUser(long userId, String role, String status) throws SQLException {
        return editUser(userId, role, status);
    }

    public static boolean EditUserUsername(long userId, String username) throws SQLException {
        return editUserUsername(userId, username);
    }

    public static boolean EditUserPassword(long userId, String passwordHash) throws SQLException {
        return editUserPassword(userId, passwordHash);
    }

    // ============ USER OPERATIONS ============

    public static long createUser(String username, String passwordHash, String role, String status) throws SQLException {
        UserDAO dao = new UserDAO();
        return dao.create(username, passwordHash, role, status);
    }

    public static User getUserById(long userId) throws SQLException {
        UserDAO dao = new UserDAO();
        return dao.findById(userId);
    }

    public static User getUserByUsername(String username) throws SQLException {
        UserDAO dao = new UserDAO();
        return dao.findByUsername(username);
    }

    public static List getAllUsers() throws SQLException {
        UserDAO dao = new UserDAO();
        return dao.findAll();
    }

    public static boolean editUser(long userId, String role, String status) throws SQLException {
        UserDAO dao = new UserDAO();
        return dao.update(userId, role, status);
    }

    public static boolean editUserUsername(long userId, String username) throws SQLException {
        UserDAO dao = new UserDAO();
        return dao.updateUsername(userId, username);
    }

    public static boolean editUserPassword(long userId, String passwordHash) throws SQLException {
        UserDAO dao = new UserDAO();
        return dao.updatePassword(userId, passwordHash);
    }

    public static boolean editUserUsernameAndPassword(long userId, String username, String passwordHash) throws SQLException {
        UserDAO dao = new UserDAO();
        return dao.updateUsernameAndPassword(userId, username, passwordHash);
    }

    public static boolean editUserEmail(long userId, String email) throws SQLException {
        UserDAO dao = new UserDAO();
        return dao.updateEmail(userId, email);
    }

    public static boolean editUserEmailAndPassword(long userId, String email, String passwordHash) throws SQLException {
        UserDAO dao = new UserDAO();
        return dao.updateEmailAndPassword(userId, email, passwordHash);
    }

    public static boolean deleteUser(long userId) throws SQLException {
        UserDAO dao = new UserDAO();
        return dao.delete(userId);
    }

    public static long CreateDoctor(Long userId, String username, String password, String fullName, String specialty, String phone, String email, String roomNumber) throws SQLException {
        Long resolvedUserId = userId;
        if (resolvedUserId == null || resolvedUserId.longValue() <= 0) {
            resolvedUserId = createUser(username, password, "DOCTOR", "ACTIVE");
        }
        return createDoctor(resolvedUserId.longValue(), fullName, specialty, phone, email, roomNumber);
    }

    public static boolean EditDoctor(long doctorId, String fullName, String specialty, String email, String roomNumber) throws SQLException {
        return editDoctor(doctorId, fullName, specialty, email, roomNumber);
    }

    public static boolean EditDoctorUsername(long doctorId, String username) throws SQLException {
        Long userId = getDoctorUserId(doctorId);
        if (userId == null) {
            return false;
        }
        return editUserUsername(userId.longValue(), username);
    }

    public static boolean EditDoctorPassword(long doctorId, String passwordHash) throws SQLException {
        Long userId = getDoctorUserId(doctorId);
        if (userId == null) {
            return false;
        }
        return editUserPassword(userId.longValue(), passwordHash);
    }

    public static boolean EditDoctorCredentials(long doctorId, String username, String passwordHash) throws SQLException {
        Long userId = getDoctorUserId(doctorId);
        if (userId == null) {
            return false;
        }
        return editUserUsernameAndPassword(userId.longValue(), username, passwordHash);
    }

    public static long CreatePatient(Long userId, String username, String password, String fullName, String dob, String gender, String phone, String address, String insuranceCode) throws SQLException {
        Long resolvedUserId = userId;
        if (resolvedUserId == null || resolvedUserId.longValue() <= 0) {
            resolvedUserId = createUser(username, password, "PATIENT", "ACTIVE");
        }
        return createPatient(resolvedUserId, fullName, dob, gender, phone, address, insuranceCode);
    }

    public static boolean EditPatient(long patientId, String fullName, String phone, String address) throws SQLException {
        return editPatient(patientId, fullName, phone, address);
    }

    public static boolean EditPatientUsername(long patientId, String username) throws SQLException {
        Long userId = getPatientUserId(patientId);
        if (userId == null) {
            return false;
        }
        return editUserUsername(userId.longValue(), username);
    }

    public static boolean EditPatientPassword(long patientId, String passwordHash) throws SQLException {
        Long userId = getPatientUserId(patientId);
        if (userId == null) {
            return false;
        }
        return editUserPassword(userId.longValue(), passwordHash);
    }

    public static boolean EditPatientCredentials(long patientId, String username, String passwordHash) throws SQLException {
        Long userId = getPatientUserId(patientId);
        if (userId == null) {
            return false;
        }
        return editUserUsernameAndPassword(userId.longValue(), username, passwordHash);
    }

    // ============ DOCTOR OPERATIONS ============

    public static long createDoctor(long userId, String fullName, String specialty, String phone, String email, String roomNumber) throws SQLException {
        DoctorDAO dao = new DoctorDAO();
        return dao.create(userId, fullName, specialty, phone, email, roomNumber);
    }

    public static Doctor getDoctorById(long doctorId) throws SQLException {
        DoctorDAO dao = new DoctorDAO();
        return dao.findById(doctorId);
    }

    public static List getAllDoctors() throws SQLException {
        DoctorDAO dao = new DoctorDAO();
        return dao.findAll();
    }

    public static boolean editDoctor(long doctorId, String fullName, String specialty, String email, String roomNumber) throws SQLException {
        DoctorDAO dao = new DoctorDAO();
        return dao.update(doctorId, fullName, specialty, email, roomNumber);
    }

    public static boolean editDoctorEmail(long doctorId, String email) throws SQLException {
        DoctorDAO dao = new DoctorDAO();
        return dao.updateEmail(doctorId, email);
    }

    public static Long getDoctorUserId(long doctorId) throws SQLException {
        DoctorDAO dao = new DoctorDAO();
        return dao.getUserIdForDoctor(doctorId);
    }

    public static boolean deleteDoctor(long doctorId) throws SQLException {
        DoctorDAO dao = new DoctorDAO();
        return dao.delete(doctorId);
    }

    // ============ PATIENT OPERATIONS ============

    public static long createPatient(Long userId, String fullName, String dob, String gender, String phone, String address, String insuranceCode) throws SQLException {
        PatientDAO dao = new PatientDAO();
        return dao.create(userId, fullName, dob, gender, phone, address, insuranceCode);
    }

    public static Patient getPatientById(long patientId) throws SQLException {
        PatientDAO dao = new PatientDAO();
        return dao.findById(patientId);
    }

    public static List getAllPatients() throws SQLException {
        PatientDAO dao = new PatientDAO();
        return dao.findAll();
    }

    public static boolean editPatient(long patientId, String fullName, String phone, String address) throws SQLException {
        PatientDAO dao = new PatientDAO();
        return dao.update(patientId, fullName, phone, address);
    }

    public static boolean editPatientEmail(long patientId, String email) throws SQLException {
        PatientDAO dao = new PatientDAO();
        return dao.updateEmail(patientId, email);
    }

    public static Long getPatientUserId(long patientId) throws SQLException {
        PatientDAO dao = new PatientDAO();
        return dao.getUserIdForPatient(patientId);
    }

    public static boolean deletePatient(long patientId) throws SQLException {
        PatientDAO dao = new PatientDAO();
        return dao.delete(patientId);
    }

    // ============ APPOINTMENT OPERATIONS ============

    public static long createAppointment(long doctorId, long patientId, String startDate, String endDate, String reason, String status) throws SQLException {
        AppointmentDAO dao = new AppointmentDAO();
        return dao.create(doctorId, patientId, startDate, endDate, reason, status);
    }

    public static Appointment getAppointmentById(long appointmentId) throws SQLException {
        AppointmentDAO dao = new AppointmentDAO();
        return dao.findById(appointmentId);
    }

    public static List getAllAppointments() throws SQLException {
        AppointmentDAO dao = new AppointmentDAO();
        return dao.findAll();
    }

    public static boolean editAppointmentStatus(long appointmentId, String newStatus) throws SQLException {
        AppointmentDAO dao = new AppointmentDAO();
        return dao.updateStatus(appointmentId, newStatus);
    }

    public static boolean deleteAppointment(long appointmentId) throws SQLException {
        AppointmentDAO dao = new AppointmentDAO();
        return dao.delete(appointmentId);
    }

    // ============ MEDICINE OPERATIONS ============

    public static long createMedicine(String name, String unit, BigDecimal price, int stockQuantity, String expiryDate) throws SQLException {
        MedicineDAO dao = new MedicineDAO();
        return dao.create(name, unit, price, stockQuantity, expiryDate);
    }

    public static Medicine getMedicineById(long medicineId) throws SQLException {
        MedicineDAO dao = new MedicineDAO();
        return dao.findById(medicineId);
    }

    public static List getAllMedicines() throws SQLException {
        MedicineDAO dao = new MedicineDAO();
        return dao.findAll();
    }

    public static boolean editMedicineStock(long medicineId, int stockQuantity) throws SQLException {
        MedicineDAO dao = new MedicineDAO();
        return dao.updateStock(medicineId, stockQuantity);
    }

    public static boolean deleteMedicine(long medicineId) throws SQLException {
        MedicineDAO dao = new MedicineDAO();
        return dao.delete(medicineId);
    }

    // ============ PRESCRIPTION OPERATIONS ============

    public static long createPrescription(long medicalRecordId, String notes, BigDecimal totalPrice) throws SQLException {
        PrescriptionDAO dao = new PrescriptionDAO();
        return dao.create(medicalRecordId, notes, totalPrice);
    }

    public static Prescription getPrescriptionById(long prescriptionId) throws SQLException {
        PrescriptionDAO dao = new PrescriptionDAO();
        return dao.findById(prescriptionId);
    }

    public static List getAllPrescriptions() throws SQLException {
        PrescriptionDAO dao = new PrescriptionDAO();
        return dao.findAll();
    }

    public static boolean editPrescription(long prescriptionId, String notes, BigDecimal totalPrice) throws SQLException {
        PrescriptionDAO dao = new PrescriptionDAO();
        return dao.update(prescriptionId, notes, totalPrice);
    }

    public static boolean deletePrescription(long prescriptionId) throws SQLException {
        PrescriptionDAO dao = new PrescriptionDAO();
        return dao.delete(prescriptionId);
    }

    // ============ PRESCRIPTION DETAIL OPERATIONS ============

    public static long createPrescriptionDetail(long prescriptionId, long medicineId, int quantity, String dosage) throws SQLException {
        PrescriptionDetailDAO dao = new PrescriptionDetailDAO();
        return dao.create(prescriptionId, medicineId, quantity, dosage);
    }

    public static PrescriptionDetail getPrescriptionDetailById(long detailId) throws SQLException {
        PrescriptionDetailDAO dao = new PrescriptionDetailDAO();
        return dao.findById(detailId);
    }

    public static List getAllPrescriptionDetails() throws SQLException {
        PrescriptionDetailDAO dao = new PrescriptionDetailDAO();
        return dao.findAll();
    }

    public static boolean editPrescriptionDetail(long detailId, int quantity, String dosage) throws SQLException {
        PrescriptionDetailDAO dao = new PrescriptionDetailDAO();
        return dao.update(detailId, quantity, dosage);
    }

    public static boolean deletePrescriptionDetail(long detailId) throws SQLException {
        PrescriptionDetailDAO dao = new PrescriptionDetailDAO();
        return dao.delete(detailId);
    }

    // ============ MEDICAL RECORD OPERATIONS ============

    public static long createMedicalRecord(long appointmentId, String symptoms, String diagnosis, String recordType, String treatmentPlan) throws SQLException {
        MedicalRecordDAO dao = new MedicalRecordDAO();
        return dao.create(appointmentId, symptoms, diagnosis, recordType, treatmentPlan);
    }

    public static MedicalRecord getMedicalRecordById(long recordId) throws SQLException {
        MedicalRecordDAO dao = new MedicalRecordDAO();
        return dao.findById(recordId);
    }

    public static List getAllMedicalRecords() throws SQLException {
        MedicalRecordDAO dao = new MedicalRecordDAO();
        return dao.findAll();
    }

    public static boolean editMedicalRecord(long recordId, String diagnosis, String treatmentPlan) throws SQLException {
        MedicalRecordDAO dao = new MedicalRecordDAO();
        return dao.update(recordId, diagnosis, treatmentPlan);
    }

    public static boolean deleteMedicalRecord(long recordId) throws SQLException {
        MedicalRecordDAO dao = new MedicalRecordDAO();
        return dao.delete(recordId);
    }
}
