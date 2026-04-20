package com.eclinic;

import com.eclinic.UserService.UserRegistrationRequest;

//Product class - Sử dụng chung
abstract class User {
    private final Database database;
    private final int id;
    private final String fullName;

    User(Database database, int id, String fullName) {
        this.database = database;
        this.id = id;
        this.fullName = fullName;
    }

    public int getId() {
        return id;
    }

    public String getFullName() {
        return fullName;
    }

    public abstract String getRole();

    public void saveProfile() {
        database.addUserRecord(new UserRecord(id, fullName, getRole()));
        saveRoleProfile(database);
    }

    protected abstract void saveRoleProfile(Database database);

    static final class UserRecord {
        private final int id;
        private final String fullName;
        private final String role;

        UserRecord(int id, String fullName, String role) {
            this.id = id;
            this.fullName = fullName;
            this.role = role;
        }

        int id() {
            return id;
        }

        String fullName() {
            return fullName;
        }

        String role() {
            return role;
        }

        @Override
        public String toString() {
            return id + " | " + fullName + " | " + role;
        }
    }
}

//Concrete Product cho Doctor
final class DoctorUser extends User {
    private final String specialty;

    DoctorUser(Database database, int id, String fullName, String specialty) {
        super(database, id, fullName);
        this.specialty = specialty;
    }

    public String getRole() {
        return "Doctor";
    }

    @Override
    protected void saveRoleProfile(Database database) {
        database.addDoctorRecord(new DoctorRecord(getId(), specialty));
        System.out.println("Saved to Users and Doctor tables.");
    }

    static final class DoctorRecord {
        private final int userId;
        private final String specialty;

        DoctorRecord(int userId, String specialty) {
            this.userId = userId;
            this.specialty = specialty;
        }

        int userId() {
            return userId;
        }

        String specialty() {
            return specialty;
        }

        @Override
        public String toString() {
            return userId + " | Specialty: " + specialty;
        }
    }
}

//Concrete Product cho Patient
final class PatientUser extends User {
    private final String medicalCondition;

    PatientUser(Database database, int id, String fullName, String medicalCondition) {
        super(database, id, fullName);
        this.medicalCondition = medicalCondition;
    }

    public String getRole() {
        return "Patient";
    }

    @Override
    protected void saveRoleProfile(Database database) {
        database.addPatientRecord(new PatientRecord(getId(), medicalCondition));
        System.out.println("Saved to Users and Patient tables.");
    }

    static final class PatientRecord {
        private final int userId;
        private final String medicalCondition;

        PatientRecord(int userId, String medicalCondition) {
            this.userId = userId;
            this.medicalCondition = medicalCondition;
        }

        int userId() {
            return userId;
        }

        String medicalCondition() {
            return medicalCondition;
        }

        @Override
        public String toString() {
            return userId + " | Condition: " + medicalCondition;
        }
    }
}

//Creator
public abstract class UserFactory {
    //Gọi hàm validate, create, save và audit theo flow chung.
    //User sẽ chỉ cần gọi hàm này
    public final User registerAccount(UserRegistrationRequest request) {
        validateRequest(request);
        User user = createUser(request);
        user.saveProfile();
        audit(user);
        return user;
    }

    //Lệnh createProduct() được thể hiện ở đây
    protected abstract User createUser(UserRegistrationRequest request);

    //Các business rule chung cho cả Doctor và Patient có thể đặt ở đây.
    protected void validateRequest(UserRegistrationRequest request) {
        if (isBlank(request.fullName())) {
            throw new IllegalArgumentException("Full name is required");
        }
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().length() == 0;
    }

    //Thông báo đã tạo 1 product mới
    protected void audit(User user) {
        System.out.println("Created account: " + user.getRole() + " - " + user.getFullName());
    }

    //Concrete Creator cho nhánh Doctor.
     
    public static final class DoctorCreator extends UserFactory {
        private final Database database;

        public DoctorCreator(Database database) {
            this.database = database;
        }

        @Override
        protected void validateRequest(UserRegistrationRequest request) {
            super.validateRequest(request);
            if (isBlank(request.specialtyOrCondition())) {
                throw new IllegalArgumentException("Specialty is required for doctor");
            }
        }

        @Override
        protected User createUser(UserRegistrationRequest request) {
            int id = database.nextUserId();
            return new DoctorUser(
                    database,
                    id,
                    request.fullName(),
                    request.specialtyOrCondition());
        }
    }

    //Concrete Creator cho nhánh Patient.

    public static final class PatientCreator extends UserFactory {
        private final Database database;

        public PatientCreator(Database database) {
            this.database = database;
        }

        @Override
        protected void validateRequest(UserRegistrationRequest request) {
            super.validateRequest(request);
            if (isBlank(request.specialtyOrCondition())) {
                throw new IllegalArgumentException("Medical condition is required for patient");
            }
        }

        @Override
        protected User createUser(UserRegistrationRequest request) {
            int id = database.nextUserId();
            return new PatientUser(
                    database,
                    id,
                    request.fullName(),
                    request.specialtyOrCondition());
        }
    }
}



