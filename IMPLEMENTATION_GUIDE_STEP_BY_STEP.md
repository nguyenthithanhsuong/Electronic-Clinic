# IMPLEMENTATION GUIDE: Refactor 3 Roles (Admin, Receptionist, Doctor)
## Chi tiết từng bước + Prompt AI cho team

**Dự án:** Electronic Clinic - Quản lý bệnh án  
**Phiên bản:** v2.0 (Refactor roles)  
**Ngày:** 2026-06-10  
**Tác giả:** Tech Lead  

---

## 📋 MỤC LỤC
1. [Bước 1: DB Migration - Thêm RECEPTIONIST role](#bước-1-db-migration)
2. [Bước 2: Backend Auth Fix - AuthHandler](#bước-2-backend-auth-fix)
3. [Bước 3: Backend UsersHandler - API create/update user](#bước-3-backend-usershandler)
4. [Bước 4: Backend QueueHandler - Liên kết appointment](#bước-4-backend-queuehandler)
5. [Bước 5: Frontend Types & Services - Đồng bộ role](#bước-5-frontend-types)
6. [Bước 6: Frontend user-dialog.tsx - Thêm RECEPTIONIST option](#bước-6-frontend-user-dialog)
7. [Bước 7: Frontend RBAC & Routes - Verify permissions](#bước-7-frontend-rbac)
8. [Bước 8: Testing & Verification - Unit + smoke tests](#bước-8-testing)

---

<a name="bước-1-db-migration"></a>
## 🔷 BƯỚC 1: DB Migration - Thêm RECEPTIONIST Role

### Mục đích
Thêm role `RECEPTIONIST` vào constraint của bảng `users`, cho phép database lưu trữ tài khoản tiếp tân.

### Hành động
1. Mở file: `data/supabase-schema.sql`
2. Tìm dòng chứa constraint `users_role_check` (khoảng dòng 20-30)
3. Thay đổi constraint từ `('ADMIN','DOCTOR','PATIENT')` thành `('ADMIN','DOCTOR','RECEPTIONIST','PATIENT')`

### 📝 Prompt AI cho team (Copy/Paste vào Copilot):
```
I need to modify the users table role constraint in supabase-schema.sql to support RECEPTIONIST role.

File: data/supabase-schema.sql

Current constraint:
  CONSTRAINT users_role_check CHECK (role IN ('ADMIN','DOCTOR','PATIENT'))

Change to:
  CONSTRAINT users_role_check CHECK (role IN ('ADMIN','DOCTOR','RECEPTIONIST','PATIENT'))

This is a simple one-line change to add RECEPTIONIST to the allowed roles.
Please provide the exact change only.
```

### Kết quả mong đợi
- File `data/supabase-schema.sql` có constraint mới bao gồm `'RECEPTIONIST'`
- Có thể verify bằng cách grep/search string `'RECEPTIONIST'` trong file

### Files thay đổi
- `data/supabase-schema.sql` (1 dòng)

### Effort
- **Thời gian:** <5 phút
- **Người phụ trách:** DBA / Backend lead
- **Yêu cầu:** Có quyền edit file schema

### Cách verify
```sql
-- Chạy trên DB (staging):
ALTER TABLE users DROP CONSTRAINT users_role_check;
ALTER TABLE users ADD CONSTRAINT users_role_check 
  CHECK (role IN ('ADMIN','DOCTOR','RECEPTIONIST','PATIENT'));

-- Test insert:
INSERT INTO users(username, password, role, status) 
VALUES('test_receptionist', 'hashed_pwd', 'RECEPTIONIST', 'ACTIVE');
-- Kết quả: INSERT thành công ✅
```

---

<a name="bước-2-backend-auth-fix"></a>
## 🔷 BƯỚC 2: Backend Auth Fix - AuthHandler

### Mục đích
Sửa `AuthHandler` để trả role **chính xác từ DB** (không map mặc định thành RECEPTIONIST).  
Hiện tại backend có logic sai: nếu role không phải ADMIN/DOCTOR thì map thành RECEPTIONIST.  
Sau refactor: return role từ DB như-là.

### Hành động
1. Mở file: `src/main/java/com/eclinic/api/AuthHandler.java`
2. Tìm hàm `login()` (POST endpoint `/api/login`)
3. Tìm đoạn code xử lý mapping role (tìm chuỗi "mapRole" hoặc "RECEPTIONIST")
4. **Xóa** logic mapping mặc định
5. **Thay thế** bằng: return role từ DB trực tiếp

### 📝 Prompt AI cho team (Copy/Paste vào Copilot):

```
I'm refactoring the AuthHandler in Electronic Clinic backend to fix role mapping.

File: src/main/java/com/eclinic/api/AuthHandler.java

Current problem: 
- The login() method has a mapRole() function that maps unknown roles to RECEPTIONIST by default
- This is wrong: we should return the exact role from the database

Task:
1. Find the login() POST endpoint handler
2. Find the mapRole() helper function (if exists) that maps role to RECEPTIONIST
3. Remove the mapRole() function entirely
4. In the login response, use the role from the database directly: user.getRole()
5. Make sure the JWT token includes the exact role from DB (not mapped)

Expected behavior after fix:
- User with role='RECEPTIONIST' in DB → token contains role='RECEPTIONIST'
- User with role='DOCTOR' in DB → token contains role='DOCTOR'
- User with role='ADMIN' in DB → token contains role='ADMIN'
- No more default mapping to RECEPTIONIST

Please provide:
1. The updated login() method (showing role handling)
2. Confirm mapRole() is removed
3. Show how role is now extracted from user object and put in JWT
```

### Kết quả mong đợi
- `AuthHandler.login()` trả role chính xác từ DB
- JWT token chứa role như trong DB (không mapping)
- Test login với user role RECEPTIONIST → token role='RECEPTIONIST' ✅
- Test login với user role DOCTOR → token role='DOCTOR' ✅

### Files thay đổi
- `src/main/java/com/eclinic/api/AuthHandler.java` (~20-30 dòng)

### Effort
- **Thời gian:** 30-45 phút
- **Người phụ trách:** Backend dev (senior) - ai quen JWT/auth
- **Yêu cầu:** Hiểu JWT, role concept

### Cách verify
```java
// Test case:
POST /api/login
{
  "username": "receptionist_user",
  "password": "correct_password"
}

Expected response:
{
  "token": "eyJhbGc...", // JWT containing {"role": "RECEPTIONIST", ...}
  "user": {
    "id": 2,
    "username": "receptionist_user",
    "role": "RECEPTIONIST", // ← Now matches DB ✅
    "status": "ACTIVE"
  }
}

// Decode JWT:
jwt.io → Paste token → Check payload.role = "RECEPTIONIST" ✅
```

---

<a name="bước-3-backend-usershandler"></a>
## 🔷 BƯỚC 3: Backend UsersHandler - Create/Update User API

### Mục đích
Sửa `UsersHandler` để:
1. Accept role `RECEPTIONIST` trong POST/PUT requests
2. Validate role values (chỉ accept ADMIN, DOCTOR, RECEPTIONIST, PATIENT)
3. Trả response đầy đủ (id, username, role, status)

### Hành động
1. Mở file: `src/main/java/com/eclinic/api/UsersHandler.java`
2. Tìm hàm `handleCreate()` (xử lý POST /api/users)
3. Tìm hàm `handleUpdate()` (xử lý PUT /api/users/{id})
4. Thêm validation: role phải nằm trong set {ADMIN, DOCTOR, RECEPTIONIST, PATIENT}
5. Sửa response JSON để include id, username, role, status

### 📝 Prompt AI cho team:

```
I need to update UsersHandler in Electronic Clinic backend to support RECEPTIONIST role properly.

File: src/main/java/com/eclinic/api/UsersHandler.java

Requirements:
1. The POST /api/users endpoint should accept role values: ADMIN, DOCTOR, RECEPTIONIST, PATIENT
2. The PUT /api/users/{id} endpoint should also accept all 4 role values
3. Validate incoming role: if not in [ADMIN, DOCTOR, RECEPTIONIST, PATIENT], return error 400
4. Response JSON should include: { id, username, role, status, created_at, updated_at }
5. Add null checks: if username is null/empty, return error

Task:
1. Add a validation method: isValidRole(String role) that checks if role is one of the 4 values
2. In handleCreate():
   - Parse request body to get { username, password, role, status }
   - Validate: username not empty, role is valid, password provided
   - Call userDAO.create() with all 4 parameters
   - Return response with id, username, role, status
3. In handleUpdate():
   - Parse request body to get { username, password, role, status } (all optional)
   - If role provided, validate it
   - Call userDAO.update() with non-null fields
   - Return updated user object with id, username, role, status

Error cases:
- Invalid role: return 400 "Invalid role: must be one of [ADMIN, DOCTOR, RECEPTIONIST, PATIENT]"
- Empty username: return 400 "Username cannot be empty"
- User not found: return 404 "User not found"

Please provide:
1. The updated handleCreate() method
2. The updated handleUpdate() method  
3. The isValidRole() validation helper
4. Example response JSON format
```

### Kết quả mong đợi
- POST /api/users với role='RECEPTIONIST' → 200 OK ✅
- PUT /api/users/5 với role='RECEPTIONIST' → 200 OK ✅
- POST /api/users với role='INVALID_ROLE' → 400 Bad Request ✅
- Response JSON gồm: id, username, role, status ✅

### Files thay đổi
- `src/main/java/com/eclinic/api/UsersHandler.java` (~40-50 dòng)

### Effort
- **Thời gian:** 45 phút - 1 tiếng
- **Người phụ trách:** Backend dev
- **Yêu cầu:** Hiểu request/response handling, validation

### Cách verify
```bash
# Test 1: Create RECEPTIONIST user
curl -X POST http://localhost:8080/api/users \
  -H "Authorization: Bearer <admin_token>" \
  -H "Content-Type: application/json" \
  -d '{
    "username": "receptionist_01",
    "password": "secure_pwd",
    "role": "RECEPTIONIST",
    "status": "ACTIVE"
  }'

# Expected response:
# {
#   "id": 10,
#   "username": "receptionist_01",
#   "role": "RECEPTIONIST",
#   "status": "ACTIVE"
# }
# Status: 200 ✅

# Test 2: Invalid role
curl -X POST http://localhost:8080/api/users \
  -H "Authorization: Bearer <admin_token>" \
  -H "Content-Type: application/json" \
  -d '{
    "username": "test",
    "password": "pwd",
    "role": "SUPERVISOR",
    "status": "ACTIVE"
  }'

# Expected: 400 "Invalid role: must be one of [ADMIN, DOCTOR, RECEPTIONIST, PATIENT]"
```

---

<a name="bước-4-backend-queuehandler"></a>
## 🔷 BƯỚC 4: Backend QueueHandler - Queue ↔ Appointment Linking

### Mục đích
Sửa `QueueHandler` để:
1. Khi tạo queue item: **phải gắn appointment_id** (nếu có appointment)
2. Khi GET queue items: trả kèm appointment info (doctor name, time)
3. Validate: appointment_id tồn tại + patient_id match + appointment.doctor_id không null

### Hành động
1. Mở file: `src/main/java/com/eclinic/api/QueueHandler.java`
2. Tìm hàm `handleCreate()` (xử lý POST /api/patient-queue)
3. Sửa logic:
   - Parse request: patient_id (required), appointment_id (required nếu có appointment)
   - Validate: patient tồn tại, appointment tồn tại, appointment.patient_id == patient_id
   - INSERT vào patient_queue với cả patient_id và appointment_id
4. Tìm hàm `handleGetAll()` (xử lý GET /api/patient-queue)
5. Sửa query: SELECT từ patient_queue **JOIN appointments** để lấy doctor info

### 📝 Prompt AI cho team:

```
I need to update QueueHandler in Electronic Clinic backend to properly link queue items with appointments.

File: src/main/java/com/eclinic/api/QueueHandler.java

Current issue:
- POST /api/patient-queue creates queue items but appointment_id might be null
- GET /api/patient-queue doesn't return appointment details (doctor name, time)
- No validation that appointment matches patient

Requirements:
1. POST /api/patient-queue:
   - Request: { "patientId": 10, "appointmentId": 100 }
   - Validate: 
     * patientId exists in patients table
     * appointmentId exists in appointments table
     * appointments.patient_id == patientId (they must match)
   - Insert into patient_queue with status='WAITING'
   - Return: { "id": 1, "patientId": 10, "appointmentId": 100, "status": "WAITING", "position": 3 }

2. GET /api/patient-queue?status=WAITING:
   - Should return queue items with appointment details
   - Response should include:
     * id, patientId, appointmentId, status, position
     * patient info: patientName, patientAge
     * appointment info: appointmentTime, doctorId, doctorName, reason
   - Query should JOIN appointments and users (for doctor name)

3. Error handling:
   - appointmentId provided but appointment doesn't exist: return 404 "Appointment not found"
   - appointmentId doesn't match patientId: return 400 "Appointment does not belong to this patient"
   - patientId doesn't exist: return 404 "Patient not found"

Example response for GET:
[
  {
    "id": 1,
    "patientId": 10,
    "patientName": "Nguyễn Văn A",
    "appointmentId": 100,
    "appointmentTime": "2026-06-10 10:30:00",
    "doctorId": 5,
    "doctorName": "Nguyễn Thị B",
    "reason": "Khám tổng quát",
    "status": "WAITING",
    "position": 2
  }
]

Please provide:
1. Updated handleCreate() method with validation
2. Updated handleGetAll() method with JOIN logic
3. Show the SQL query used for GET
4. Error response format
```

### Kết quả mong đợi
- POST queue với appointment_id → lưu appointment_id vào DB ✅
- GET queue trả kèm doctor name + appointment time ✅
- Validate appointment match patient → 400 error nếu sai ✅
- Xem queue item có thể biết: bệnh nhân nào, BS nào, mấy giờ khám ✅

### Files thay đổi
- `src/main/java/com/eclinic/api/QueueHandler.java` (~50-70 dòng)

### Effort
- **Thời gian:** 1-1.5 tiếng
- **Người phụ trách:** Backend dev (quen SQL/JOIN)
- **Yêu cầu:** Hiểu JOINs, validation logic

### Cách verify
```bash
# Setup: Có doctor, patient, appointment trước
# Doctor ID: 5, Doctor name: "BS Nguyễn Thị B"
# Patient ID: 10, Patient name: "Nguyễn Văn A"
# Appointment ID: 100, Time: "2026-06-10 10:30"

# Test 1: Create queue with appointment
curl -X POST http://localhost:8080/api/patient-queue \
  -H "Authorization: Bearer <receptionist_token>" \
  -H "Content-Type: application/json" \
  -d '{
    "patientId": 10,
    "appointmentId": 100
  }'

# Expected:
# {
#   "id": 1,
#   "patientId": 10,
#   "appointmentId": 100,
#   "status": "WAITING",
#   "position": 1
# }
# Status: 200 ✅

# Test 2: Get queue with full details
curl -X GET http://localhost:8080/api/patient-queue?status=WAITING \
  -H "Authorization: Bearer <receptionist_token>"

# Expected response includes:
# {
#   "id": 1,
#   "patientId": 10,
#   "patientName": "Nguyễn Văn A",
#   "appointmentId": 100,
#   "appointmentTime": "2026-06-10 10:30:00",
#   "doctorId": 5,
#   "doctorName": "BS Nguyễn Thị B",
#   "reason": "Khám tổng quát",
#   "status": "WAITING",
#   "position": 1
# } ✅

# Test 3: Invalid appointment
curl -X POST http://localhost:8080/api/patient-queue \
  -H "Authorization: Bearer <receptionist_token>" \
  -H "Content-Type: application/json" \
  -d '{
    "patientId": 10,
    "appointmentId": 999
  }'

# Expected: 404 "Appointment not found" ✅
```

---

<a name="bước-5-frontend-types"></a>
## 🔷 BƯỚC 5: Frontend Types & Services - Đồng bộ Role

### Mục đích
Cập nhật TypeScript types trong frontend để **include `RECEPTIONIST`** role.  
File cần thay đổi:
- `core/api/userService.ts` → role type union
- `core/api/authService.ts` → role type union

### Hành động
1. Mở file: `core/api/userService.ts`
2. Tìm type `Role` hoặc union type role (hiện: `"ADMIN" | "DOCTOR" | "PATIENT"`)
3. Thêm `"RECEPTIONIST"` vào union
4. Lặp lại với `core/api/authService.ts`

### 📝 Prompt AI cho team:

```
I need to update TypeScript types in Electronic Clinic frontend to support RECEPTIONIST role.

Files to update:
1. core/api/userService.ts
2. core/api/authService.ts
3. context/AuthContext.tsx (if it has role type)

Task:
1. Find all role type definitions (look for type alias or union like: "ADMIN" | "DOCTOR" | "PATIENT")
2. Add "RECEPTIONIST" to each role union/type
3. Make sure interfaces/types that include role are updated:
   - User interface
   - AuthResponse interface
   - Any enum or const defining roles

Current pattern (example):
export type Role = "ADMIN" | "DOCTOR" | "PATIENT"
export interface User {
  id: number
  username: string
  role: Role
  status: "ACTIVE" | "INACTIVE" | "BLOCKED"
}

Should become:
export type Role = "ADMIN" | "DOCTOR" | "RECEPTIONIST" | "PATIENT"
export interface User {
  id: number
  username: string
  role: Role
  status: "ACTIVE" | "INACTIVE" | "BLOCKED"
}

Please provide:
1. Updated role type definitions for each file
2. Show where Role type is used (search results)
3. Confirm all interfaces using Role are checked
4. No breaking changes to existing code
```

### Kết quả mong đợi
- `core/api/userService.ts` có role = "RECEPTIONIST" ✅
- `core/api/authService.ts` có role = "RECEPTIONIST" ✅
- TypeScript compiler không lỗi ✅
- IDE autocomplete hiển thị "RECEPTIONIST" ✅

### Files thay đổi
- `core/api/userService.ts` (1-2 dòng)
- `core/api/authService.ts` (1-2 dòng)
- `context/AuthContext.tsx` (nếu có role type, 1-2 dòng)

### Effort
- **Thời gian:** 15-20 phút
- **Người phụ trách:** Frontend dev (junior có thể)
- **Yêu cầu:** Hiểu TypeScript types

### Cách verify
```bash
# In project root:
npm run build
# Should pass TypeScript compilation ✅

# OR:
npx tsc --noEmit
# Should show no errors ✅

# Verify in code:
grep -r "type Role" core/api/
# Output should show "RECEPTIONIST" in all role definitions ✅
```

---

<a name="bước-6-frontend-user-dialog"></a>
## 🔷 BƯỚC 6: Frontend user-dialog.tsx - Thêm RECEPTIONIST Option

### Mục đích
Sửa form tạo/sửa user (admin UI) để **cho phép chọn role RECEPTIONIST**.  
Hiện tại form chỉ có: ADMIN, DOCTOR, PATIENT.  
Thêm: RECEPTIONIST.

### Hành động
1. Mở file: `modules/admin/components/user-dialog.tsx`
2. Tìm SelectItem hoặc radio button/dropdown cho role selection
3. Thêm option mới:
   ```tsx
   <SelectItem value="RECEPTIONIST">Tiếp tân</SelectItem>
   ```
4. Sắp xếp thứ tự (gợi ý: ADMIN, RECEPTIONIST, DOCTOR, PATIENT)

### 📝 Prompt AI cho team:

```
I need to update the user creation/edit dialog in Electronic Clinic frontend to include RECEPTIONIST role option.

File: modules/admin/components/user-dialog.tsx

Task:
1. Find the role selection input (should be a Select, Radio, or Dropdown component)
2. Current options are likely: ADMIN, DOCTOR, PATIENT
3. Add RECEPTIONIST option between ADMIN and DOCTOR
4. The label should be: "Tiếp tân" (Vietnamese for Receptionist)
5. Keep the value as "RECEPTIONIST" to match backend

Current example structure:
<Select value={role} onValueChange={setRole}>
  <SelectItem value="ADMIN">Quản trị</SelectItem>
  <SelectItem value="DOCTOR">Bác sĩ</SelectItem>
  <SelectItem value="PATIENT">Bệnh nhân</SelectItem>
</Select>

Should become:
<Select value={role} onValueChange={setRole}>
  <SelectItem value="ADMIN">Quản trị</SelectItem>
  <SelectItem value="RECEPTIONIST">Tiếp tân</SelectItem>
  <SelectItem value="DOCTOR">Bác sĩ</SelectItem>
  <SelectItem value="PATIENT">Bệnh nhân</SelectItem>
</Select>

Please provide:
1. The updated role selection code
2. Show the exact location (line numbers if possible)
3. Confirm no other changes needed in the dialog
4. Show example of how form submission handles RECEPTIONIST
```

### Kết quả mong đợi
- Form tạo user hiển thị 4 option: Admin, Tiếp tân, Bác sĩ, Bệnh nhân ✅
- Chọn "Tiếp tân" → gửi role='RECEPTIONIST' tới backend ✅
- Form sửa user cũng hiển thị option RECEPTIONIST ✅

### Files thay đổi
- `modules/admin/components/user-dialog.tsx` (~3-5 dòng)

### Effort
- **Thời gian:** 15 phút
- **Người phụ trách:** Frontend dev (junior có thể)
- **Yêu cầu:** Hiểu React Select component

### Cách verify
```bash
# 1. Visual test: Open admin staff page in browser
# Navigate to /admin/staff → "Tạo người dùng" button → Form should show 4 role options ✅

# 2. Select receptionist and submit
# - Select "Tiếp tân"
# - Fill username, password, status
# - Submit
# - Should get success response from backend ✅

# 3. Check form edit
# - Open an existing user
# - Form should show "Tiếp tân" as current role if user.role === "RECEPTIONIST" ✅
```

---

<a name="bước-7-frontend-rbac"></a>
## 🔷 BƯỚC 7: Frontend RBAC & Routes - Verify Permissions

### Mục đích
Kiểm tra **route permissions (RBAC)** để đảm bảo:
1. RECEPTIONIST có thể vào `/reception` routes
2. DOCTOR có thể vào `/doctor` routes
3. ADMIN có thể vào `/admin` routes
4. PATIENT có thể vào `/patient` routes (nếu có)
5. Không có route nào sai config

### Hành động
1. Mở file: `shared/lib/rbac/allowed-roles-for-path.ts`
2. Kiểm tra route `/reception` → phải gồm `RECEPTIONIST` + `ADMIN`
3. Kiểm tra route `/doctor` → phải gồm `DOCTOR` + `ADMIN`
4. Kiểm tra route `/admin` → phải gồm chỉ `ADMIN`
5. Nếu có sai, sửa lại

### 📝 Prompt AI cho team:

```
I need to verify and update the RBAC (Role-Based Access Control) configuration in Electronic Clinic frontend.

File: shared/lib/rbac/allowed-roles-for-path.ts

Task:
1. Find the function/object that maps routes to allowed roles
2. Verify these routes have correct role permissions:
   - /reception → should allow: RECEPTIONIST, ADMIN
   - /reception/* → should allow: RECEPTIONIST, ADMIN
   - /doctor → should allow: DOCTOR, ADMIN
   - /doctor/* → should allow: DOCTOR, ADMIN
   - /admin → should allow: ADMIN only
   - /admin/* → should allow: ADMIN only

3. If not present, add RECEPTIONIST to /reception routes
4. Verify that:
   - No route allows PATIENT to access admin or reception (unless explicitly designed)
   - No route allows RECEPTIONIST to access doctor pages
   - No route allows DOCTOR to access admin pages

Current pattern (example):
export const allowedRolesForPath = {
  '/reception': ['RECEPTIONIST', 'ADMIN'],
  '/reception/patients': ['RECEPTIONIST', 'ADMIN'],
  '/reception/appointments': ['RECEPTIONIST', 'ADMIN'],
  '/doctor': ['DOCTOR', 'ADMIN'],
  '/doctor/schedule': ['DOCTOR', 'ADMIN'],
  '/admin': ['ADMIN'],
}

Or as a function:
export function getAllowedRoles(path: string): Role[] {
  if (path.startsWith('/reception')) return ['RECEPTIONIST', 'ADMIN']
  if (path.startsWith('/doctor')) return ['DOCTOR', 'ADMIN']
  if (path.startsWith('/admin')) return ['ADMIN']
  return []
}

Please provide:
1. Current RBAC configuration
2. Any changes needed
3. Confirm all routes are protected correctly
4. Show example of how ProtectedRoute component uses this
```

### Kết quả mong đợi
- `/reception` routes chỉ cho RECEPTIONIST + ADMIN ✅
- `/doctor` routes chỉ cho DOCTOR + ADMIN ✅
- `/admin` routes chỉ cho ADMIN ✅
- RECEPTIONIST không thể vào `/doctor` (403 redirect) ✅
- DOCTOR không thể vào `/admin` (403 redirect) ✅

### Files thay đổi
- `shared/lib/rbac/allowed-roles-for-path.ts` (0-3 dòng nếu cần sửa)

### Effort
- **Thời gian:** 20-30 phút (nếu cần sửa), 10 phút (nếu đã đúng)
- **Người phụ trách:** Frontend dev (quen RBAC)
- **Yêu cầu:** Hiểu route protection, authentication flow

### Cách verify
```bash
# Test 1: Receptionist login + visit /doctor page
# - Login as RECEPTIONIST
# - Navigate to /doctor/schedule
# - Should redirect to /reception or show 403 error ✅

# Test 2: Doctor login + visit /admin page
# - Login as DOCTOR
# - Navigate to /admin/staff
# - Should redirect to /doctor or show 403 error ✅

# Test 3: Admin login + visit all pages
# - Login as ADMIN
# - Should be able to access /admin, /reception, /doctor pages ✅
# - Should be able to view all data in dashboard ✅

# Test 4: Patient login (if applicable)
# - Should only see /patient routes ✅
```

---

<a name="bước-8-testing"></a>
## 🔷 BƯỚC 8: Testing & Verification - Unit + Smoke Tests

### Mục đích
Đảm bảo tất cả refactor hoạt động đúng bằng cách chạy tests tự động + manual verification.

### Hành động
1. **Unit Tests:**
   - Test `AuthHandler.login()` trả role chính xác
   - Test `UsersHandler.create()` với role RECEPTIONIST
   - Test `QueueHandler` validate appointment match patient
   
2. **Integration Tests:**
   - Test workflow: create receptionist → login → tạo bệnh nhân → tạo appointment → queue

3. **Manual Smoke Tests:**
   - Chạy application locally
   - Thực hiện toàn bộ workflow bằng tay
   - Ghi lại kết quả

### 📝 Prompt AI cho team:

```
I need to create and run tests for the refactored role system in Electronic Clinic.

Test files to update/create:
1. src/test/java/com/eclinic/api/AuthHandlerTest.java
2. src/test/java/com/eclinic/api/UsersHandlerTest.java
3. emr-system-frontend/tests/authService.test.ts (if exists, or create new)

Test cases to implement:

BACKEND - AuthHandlerTest:
1. testLoginReturnsCorrectRole_Receptionist()
   - Create user with role='RECEPTIONIST'
   - Call login endpoint
   - Assert response.role == 'RECEPTIONIST'
   - Assert JWT token contains role='RECEPTIONIST'

2. testLoginReturnsCorrectRole_Doctor()
   - Create user with role='DOCTOR'
   - Call login endpoint
   - Assert response.role == 'DOCTOR'
   - Assert JWT token contains role='DOCTOR'

3. testLoginReturnsCorrectRole_Admin()
   - Create user with role='ADMIN'
   - Call login endpoint
   - Assert response.role == 'ADMIN'

BACKEND - UsersHandlerTest:
1. testCreateReceptionist_Success()
   - Call POST /api/users with role='RECEPTIONIST'
   - Assert status 200
   - Assert response.role == 'RECEPTIONIST'
   - Assert user created in DB

2. testCreateUser_InvalidRole_Returns400()
   - Call POST /api/users with role='INVALID'
   - Assert status 400
   - Assert error message contains "Invalid role"

3. testUpdateUserRole_Success()
   - Create user with role='ADMIN'
   - Update to role='RECEPTIONIST'
   - Assert update successful
   - Assert DB shows new role

BACKEND - QueueHandlerTest:
1. testCreateQueue_WithAppointment_Success()
   - Create patient, appointment, doctor
   - Call POST /api/patient-queue with patientId and appointmentId
   - Assert status 200
   - Assert response includes appointmentId
   - Assert DB shows both patient_id and appointment_id

2. testCreateQueue_AppointmentNotMatch_Returns400()
   - Create patient A, patient B, appointment for A
   - Try to queue patient B with appointment for A
   - Assert status 400
   - Assert error message "Appointment does not belong to this patient"

3. testGetQueue_IncludesAppointmentDetails()
   - Create queue item with appointment
   - Call GET /api/patient-queue
   - Assert response includes doctorId, doctorName, appointmentTime

FRONTEND:
1. testRoleTypeIncludesReceptionist()
   - Import Role type from userService
   - Assert 'RECEPTIONIST' is valid type
   - Assert TypeScript compiles without errors

2. testUserDialogShowsReceptionistOption()
   - Render UserDialog component
   - Find role select dropdown
   - Assert RECEPTIONIST option exists
   - Assert can select RECEPTIONIST and submit form

Please provide:
1. Test code for each test case
2. Setup/teardown methods (test data, mocks)
3. How to run tests locally
4. Expected test output (all should PASS ✅)
```

### Kết quả mong đợi
- Tất cả unit tests PASS ✅
- Tất cả integration tests PASS ✅
- Manual smoke test workflow hoàn tất thành công ✅
- Không có lỗi TypeScript ✅
- Log output ghi nhận mỗi step ✅

### Files thay đổi / tạo mới
- `src/test/java/com/eclinic/api/AuthHandlerTest.java` (nếu chưa có)
- `src/test/java/com/eclinic/api/UsersHandlerTest.java` (nếu chưa có)
- `src/test/java/com/eclinic/api/QueueHandlerTest.java` (nếu chưa có)

### Effort
- **Thời gian:** 2-3 tiếng
- **Người phụ trách:** QA/Tester + Backend dev (kiểm tra unit tests)
- **Yêu cầu:** Hiểu JUnit, mocking, test frameworks

### Cách verify
```bash
# Backend tests:
cd /path/to/Electronic-Clinic
mvn test
# Output should show: Tests run: X, Failures: 0, Errors: 0 ✅

# Frontend tests (if applicable):
cd /path/to/emr-system-frontend
npm test
# Output should show: Tests passed ✅

# Manual smoke test checklist:
# [ ] Admin login → /admin ✅
# [ ] Admin create receptionist user ✅
# [ ] Admin create doctor user ✅
# [ ] Receptionist login ✅
# [ ] Receptionist create patient ✅
# [ ] Receptionist create appointment ✅
# [ ] Receptionist queue patient ✅
# [ ] Doctor login ✅
# [ ] Doctor view schedule ✅
# [ ] Doctor view patient ✅
# [ ] Doctor write prescription ✅
# [ ] Admin view audit log ✅
# [ ] Check DB: user roles, queue with appointment_id ✅
```

---

## 📊 TÓMO TẮT TOÀN BỘ IMPLEMENTATION

| Bước | Tên | File chính | Effort | Người phụ trách | Verify method |
|------|-----|-----------|--------|-----------------|---------------|
| 1 | DB Migration | `data/supabase-schema.sql` | <5 phút | DBA | SQL INSERT test |
| 2 | Auth Fix | `src/main/.../AuthHandler.java` | 45 phút | Backend senior | JWT token decode |
| 3 | UsersHandler | `src/main/.../UsersHandler.java` | 1 tiếng | Backend dev | POST/PUT curl test |
| 4 | QueueHandler | `src/main/.../QueueHandler.java` | 1.5 tiếng | Backend dev | GET with JOINs |
| 5 | TS Types | `core/api/*.ts` | 15 phút | Frontend junior | npm run build |
| 6 | User Dialog | `modules/admin/.../user-dialog.tsx` | 15 phút | Frontend junior | Visual test |
| 7 | RBAC Check | `shared/lib/rbac/*.ts` | 30 phút | Frontend dev | Route redirect test |
| 8 | Testing | `src/test/...`, manual | 3 tiếng | QA + Backend | Test report |
| | **TOTAL** | | **~7-8 giờ** | 5 người | ✅ All pass |

---

## ✅ DONE CRITERIA

Khi tất cả 8 bước hoàn tất thành công:

✅ **Database:** role `RECEPTIONIST` được lưu trữ  
✅ **Backend:** Auth trả role chính xác, RECEPTIONIST user có thể được tạo  
✅ **Backend:** Queue item liên kết appointment, GET trả appointment details  
✅ **Frontend:** TypeScript không lỗi, type hỗ trợ RECEPTIONIST  
✅ **Frontend:** Admin form hiển thị RECEPTIONIST option  
✅ **Frontend:** RBAC bảo vệ routes đúng  
✅ **Tests:** Unit + integration tests PASS  
✅ **Manual:** Toàn bộ workflow receptionist → doctor → admin chạy đúng  

---

## 🎬 KỈ TIẾP THEO

**Sau khi hoàn tất 8 bước:**
1. Deploy lên staging environment
2. Chạy full manual QA checklist
3. Capture screenshots / video demo
4. Chuẩn bị demo slide cho team
5. Deploy lên production (nếu ok)
6. Giám sát logs (auth failures, queue issues)
7. Gather feedback từ users

---

**Ngày cập nhật:** 2026-06-10  
**Phiên bản:** 1.0
