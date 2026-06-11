# DEMO SCRIPT - Ngày T7 (Demo Day)
## Exact steps + Expected outputs

**Duration:** 10-15 phút  
**Audience:** Team + Stakeholders  
**Objective:** Show complete workflow: Receptionist → Doctor → Payment → Audit Log  

---

## 🎬 SETUP TRƯỚC DEMO (30 phút)

### 1. Staging Environment
```bash
# Ensure staging is clean and running
[ ] DB migrated to RECEPTIONIST role
[ ] Backend server running (port 8080)
[ ] Frontend running (port 3000)
[ ] Test data cleared (or use specific test DB)
```

### 2. Test Data Setup
```bash
# Create 4 base users (via direct DB or API)
User 1: admin / admin123
  - role: ADMIN
  - status: ACTIVE

User 2: receptionist / rec123
  - role: RECEPTIONIST
  - status: ACTIVE

User 3: doctor / doc123
  - role: DOCTOR
  - status: ACTIVE
  
User 4: patient / pat123
  - role: PATIENT
  - status: ACTIVE

# Create 1 sample doctor profile
Doctor 1:
  - name: "BS Nguyễn Thị Hoa"
  - specialization: "Khám tổng quát"
  - user_id: 3 (doctor user)
```

### 3. Browser Preparation
```
[ ] Open 3 browser windows side-by-side:
    - Window 1: Admin UI (for capturing steps 1-2)
    - Window 2: Receptionist UI (for capturing steps 3-7)
    - Window 3: Doctor UI (for capturing steps 8-9)
[ ] Clear cache / private mode
[ ] Test URLs:
    - Admin: http://localhost:3000/admin/staff
    - Receptionist: http://localhost:3000/reception/patients
    - Doctor: http://localhost:3000/doctor/schedule
```

### 4. Recording Setup (Optional)
```bash
# If capturing demo video:
# Use OBS / screen recorder
# Bitrate: 2Mbps, 1080p, 30fps
# Output: demo_video.mp4
```

---

## 🎯 DEMO FLOW (10 phases)

### PHASE 1: Admin Creates Receptionist (1 min)

**Setup:** Already logged in as ADMIN

**Steps:**
```
1. Click "Tạo người dùng" button
   └─ Expected: Dialog appears with title "Tạo người dùng mới"

2. Fill form:
   - Username: "receptionist_demo"
   - Password: "DemoPass123"
   - Role: Select "Tiếp tân"  ← CLICK HERE (SHOW RECEPTIONIST OPTION)
   - Status: "Hoạt động"
   
3. Click "Tạo" button
   └─ Expected: Toast notification "Tạo người dùng thành công"
   └─ Receptionist appears in table with role="Tiếp tân"

4. Take screenshot
   CAPTION: "✅ RECEPTIONIST role supported in database + UI"
```

**Expected Output:**
```json
{
  "id": 10,
  "username": "receptionist_demo",
  "role": "RECEPTIONIST",
  "status": "ACTIVE",
  "created_at": "2026-06-14T09:00:00Z"
}
```

**Talking Point:**
> "Trước đây, hệ thống không có role RECEPTIONIST. Bây giờ admin có thể tạo receptionist chính xác, và role được lưu trữ trong database."

---

### PHASE 2: Admin Logs Out, Receptionist Logs In (0.5 min)

**Steps:**
```
1. Click user menu (top right) → "Đăng xuất"
   └─ Expected: Redirect to /login

2. Clear form, input:
   - Username: "receptionist_demo"
   - Password: "DemoPass123"
   
3. Click "Đăng nhập"
   └─ Expected: 
      - JWT token contains role="RECEPTIONIST"
      - Redirect to /reception/patients
      - Show receptionist dashboard (NOT admin, NOT doctor)

4. Take screenshot
   CAPTION: "✅ Receptionist role correctly returned from backend"
```

**Expected UI Elements:**
```
- Navigation shows "/reception" routes active
- Sidebar: Bệnh nhân | Lịch khám | Xếp hàng | Thanh toán
- No access to /admin or /doctor routes
```

**Talking Point:**
> "AuthHandler giờ trả role chính xác từ database. Token JWT chứa role RECEPTIONIST, không còn map sai."

---

### PHASE 3: Receptionist Creates Patient (1.5 min)

**Steps:**
```
1. Go to "/reception/patients"
   └─ Expected: List of patients page

2. Click "Thêm bệnh nhân" button
   └─ Expected: Form dialog appears

3. Fill patient form:
   - Full Name: "Trần Văn An"
   - Age: "35"
   - Gender: "Nam"
   - Insurance ID: "BHYT123456"
   - Phone: "0912345678"
   - Address: "123 Đường Nguyễn"

4. Click "Tạo bệnh nhân"
   └─ Expected:
      - Toast: "Bệnh nhân đã được tạo"
      - New patient appears in list with ID (e.g., "ID: 50")

5. Take screenshot
   CAPTION: "✅ Patient profile created successfully"
```

**Database Verification:**
```sql
-- Check patient record
SELECT id, full_name, age, phone FROM patients 
WHERE full_name = 'Trần Văn An';
-- Expected: id=50, full_name=Trần Văn An, age=35, phone=0912345678
```

**Talking Point:**
> "Receptionist tạo hồ sơ bệnh nhân - đây là tính năng chính của tiếp tân."

---

### PHASE 4: Receptionist Creates Appointment (1.5 min)

**Steps:**
```
1. Still on /reception/patients
   OR navigate to /reception/appointments

2. Click "Tạo lịch khám" button
   └─ Expected: Appointment form dialog

3. Fill form:
   - Select Patient: "Trần Văn An" (ID: 50)
   - Select Doctor: "BS Nguyễn Thị Hoa"
   - Date: "2026-06-14"
   - Time: "14:00"
   - Reason: "Khám sức khỏe tổng quát"

4. Click "Tạo lịch khám"
   └─ Expected:
      - Toast: "Lịch khám đã được tạo"
      - Appointment appears in list with:
        * Patient: "Trần Văn An"
        * Doctor: "BS Nguyễn Thị Hoa"
        * Time: "2026-06-14 14:00"
        * Status: "PENDING"

5. Note appointment ID from response (e.g., "APT_100")

6. Take screenshot
   CAPTION: "✅ Appointment scheduled with Doctor + Patient link"
```

**API Verification:**
```bash
curl -X GET http://localhost:8080/api/appointments \
  -H "Authorization: Bearer <receptionist_token>"

# Expected response includes:
# {
#   "id": 100,
#   "patientId": 50,
#   "patientName": "Trần Văn An",
#   "doctorId": 5,
#   "doctorName": "BS Nguyễn Thị Hoa",
#   "appointmentTime": "2026-06-14 14:00:00",
#   "status": "PENDING"
# }
```

**Talking Point:**
> "Lịch khám liên kết bệnh nhân + bác sĩ. Mỗi appointment có ID duy nhất."

---

### PHASE 5: Receptionist Queues Patient (1 min)

**Steps:**
```
1. Navigate to /reception/queue (or "Xếp hàng" in sidebar)
   └─ Expected: Queue list page

2. Click "Xếp bệnh nhân" button
   └─ Expected: Queue form dialog

3. Fill queue form:
   - Select Patient: "Trần Văn An" (ID: 50)
   - Select Appointment: (should auto-match or select from list)
      └─ Show appointment ID "APT_100" + "BS Nguyễn Thị Hoa 14:00"
   - Status: "WAITING" (default)

4. Click "Xếp hàng"
   └─ Expected:
      - Toast: "Bệnh nhân đã được xếp hàng"
      - Queue item appears with:
        * Patient: "Trần Văn An"
        * Doctor: "BS Nguyễn Thị Hoa"  ← KEY FIELD
        * Time: "14:00"  ← KEY FIELD
        * Position: "#1" (or next available)

5. Take screenshot
   CAPTION: "✅ Queue item linked with Appointment + Doctor info"
```

**⚡ CRITICAL TEST POINT:**
```
Show that queue item includes:
✅ patient_id = 50
✅ appointment_id = 100 ← IMPORTANT!
✅ doctor_name = "BS Nguyễn Thị Hoa" ← FROM APPOINTMENT JOIN
✅ appointment_time = "14:00" ← FROM APPOINTMENT JOIN
```

**Database Verification:**
```sql
-- Check queue with appointment
SELECT 
  pq.id, pq.patient_id, pq.appointment_id, pq.status,
  p.full_name as patient_name,
  a.id as appt_id, a.doctor_id,
  u.username as doctor_name
FROM patient_queue pq
JOIN patients p ON pq.patient_id = p.id
JOIN appointments a ON pq.appointment_id = a.id
JOIN users u ON a.doctor_id = u.id
WHERE pq.patient_id = 50;

-- Expected: Shows both patient_id AND appointment_id ✅
```

**Talking Point:**
> "Queue item giờ được liên kết chặt chẽ với appointment. Doctor biết chính xác bệnh nhân nào sẽ khám, mấy giờ, và lý do khám."

---

### PHASE 6: Receptionist Logs Out, Doctor Logs In (0.5 min)

**Steps:**
```
1. Click user menu → "Đăng xuất"
2. Login as doctor:
   - Username: "doctor_demo" (or "doctor" if exists)
   - Password: "DemoPass123"
   
3. Redirect should go to /doctor/schedule
   └─ Expected: Doctor dashboard with schedule view

4. Take screenshot
   CAPTION: "✅ Doctor role routing works correctly"
```

---

### PHASE 7: Doctor Views Schedule & Patient (1.5 min)

**Steps:**
```
1. On /doctor/schedule
   └─ Expected: Timeline or table showing today's appointments

2. Should see:
   - "14:00 - Trần Văn An"
   - Subtitle: "Khám sức khỏe tổng quát"

3. Click on patient appointment
   └─ Expected: Modal or page with:
      * Patient name: "Trần Văn An"
      * Age: 35
      * Phone: "0912345678"
      * Insurance: "BHYT123456"
      * Appointment time: "2026-06-14 14:00"
      * Queue position: "#1"
      * Previous history: [if any]

4. Take screenshot
   CAPTION: "✅ Doctor sees complete patient info + appointment"
```

**Talking Point:**
> "Doctor có đầy đủ thông tin bệnh nhân: hồ sơ, lịch khám, vị trí xếp hàng. Không cần hỏi receptionist."

---

### PHASE 8: Doctor Writes Prescription (1.5 min)

**Steps:**
```
1. Click "Kê đơn" or "Ghi bệnh án"
   └─ Expected: Medical record form

2. Fill form:
   - Diagnosis: "Cảm cúm thông thường"
   - Symptoms: "Sốt, ho, mệt mỏi"
   
3. Click "Thêm thuốc"
   └─ Expected: Medicine search modal appears

4. Type medicine name: "Aspirin" (or search by category)
   - Show category filter (if available)
   - Select from results

5. Fill medicine details:
   - Quantity: "30"
   - Dosage: "500mg"
   - Usage: "2x/day, 3 days"

6. Click "Lưu bệnh án"
   └─ Expected:
      - Toast: "Bệnh án đã được ghi"
      - Medical record appears in patient history

7. Take screenshot
   CAPTION: "✅ Prescription saved with patient record"
```

**Data Verification:**
```sql
SELECT 
  mr.id, mr.patient_id, mr.diagnosis, mr.created_at,
  p.prescription_id, p.medicine_id, p.quantity, p.dosage
FROM medical_records mr
LEFT JOIN prescriptions p ON mr.id = p.medical_record_id
WHERE mr.patient_id = 50
ORDER BY mr.created_at DESC;

-- Expected: Medical record + prescriptions linked ✅
```

**Talking Point:**
> "Bệnh án + đơn thuốc được lưu trữ chặt chẽ với appointment. Không có dữ liệu loose/orphaned."

---

### PHASE 9: Receptionist Marks Complete & Payment (1 min)

**Steps:**
```
1. Switch back to Receptionist browser window
2. Navigate to /reception/queue
3. Find "Trần Văn An" queue item
4. Click "Hoàn tất" button
   └─ Expected: Queue status changes from "WAITING" → "DONE"

5. Navigate to /reception/payments
6. Click "Thêm thanh toán"
   └─ Payment form appears

7. Fill:
   - Patient: "Trần Văn An"
   - Khám: 150,000 VND
   - Thuốc: 250,000 VND
   - Total: 400,000 VND
   - Status: "PAID"

8. Click "Lưu"
   └─ Expected: Payment record created

9. Take screenshot
   CAPTION: "✅ Complete workflow: Queue → Payment"
```

---

### PHASE 10: Admin Views Audit Log (1 min)

**Steps:**
```
1. Switch to Admin browser window
2. Navigate to /admin/audit-logs (or dashboard)
3. Should see complete action history:

   2026-06-14 09:00 ADMIN created user receptionist_demo role=RECEPTIONIST
   2026-06-14 09:05 RECEPTIONIST created patient(50) Trần Văn An
   2026-06-14 09:10 RECEPTIONIST created appointment(100) patient=50 doctor=5
   2026-06-14 09:15 RECEPTIONIST queued patient(50) appointment=100
   2026-06-14 14:00 DOCTOR created medical_record(patient=50) diagnosis=Cảm cúm
   2026-06-14 14:05 DOCTOR created prescription(medicine_id=5) patient=50
   2026-06-14 14:10 RECEPTIONIST updated queue status=DONE
   2026-06-14 14:15 RECEPTIONIST created payment(patient=50) amount=400000

4. Take screenshot
   CAPTION: "✅ Complete audit trail for compliance"
```

**Talking Point:**
> "Tất cả hành động của mỗi role được ghi lại. Có thể tracking từng bước cho mục đích kiểm tra, tuân thủ."

---

## 📊 DEMO SUMMARY SLIDE (Show at end)

```
🎯 REFACTOR RESULTS - ELECTRONIC CLINIC v2.0

✅ 3 Roles đầy đủ chức năng:
   • ADMIN: Quản lý toàn hệ thống
   • RECEPTIONIST: Tiếp tân + lập lịch + xếp hàng (NEW)
   • DOCTOR: Khám bệnh + kê đơn

✅ Core Workflow hoạt động đúng:
   • Patient creation ✅
   • Appointment scheduling ✅
   • Queue management ✅
   • Prescription handling ✅
   • Payment tracking ✅

✅ Data Integrity:
   • Queue ↔ Appointment linking ✅
   • Patient ↔ Medical record ✅
   • Prescription ↔ Patient ✅
   • Audit trail (complete) ✅

✅ Role-based Access Control:
   • Admin → /admin ✅
   • Receptionist → /reception ✅
   • Doctor → /doctor ✅
   • No cross-role access ✅

✅ Database:
   • RECEPTIONIST role supported ✅
   • Migration successful ✅
   • 0 data loss ✅

📈 Code Quality:
   • Unit tests: 100% PASS ✅
   • No TypeScript errors ✅
   • Audit logs: Complete ✅

🎬 READY FOR PRODUCTION ✅
```

---

## 🚨 CONTINGENCY PLAN (If something breaks)

### If Login fails:
```bash
# Check backend is running
curl -X POST http://localhost:8080/api/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin123"}'

# If 500 error: restart backend
# If connection refused: check if app crashed
```

### If Role not showing in form:
```bash
# Refresh page: Ctrl+Shift+R
# Check browser console: F12 → Console
# Check TypeScript build: npm run build
```

### If Queue appointment not linked:
```bash
# Check database directly
SELECT * FROM patient_queue WHERE patient_id=50;
# Should show appointment_id not null

# Check API response
curl -X GET http://localhost:8080/api/patient-queue \
  -H "Authorization: Bearer <token>"
```

### Backup Plan:
```
If critical issue occurs:
1. Use pre-recorded demo video (from earlier run)
2. Show database screenshots (prove data consistency)
3. Show unit test results (prove code works)
4. Explain what happened + fix plan
5. Schedule follow-up demo after fix
```

---

## ✅ DEMO CHECKLIST

**Before starting:**
- [ ] Database migrated
- [ ] All servers running
- [ ] Test data loaded
- [ ] Browsers cleared/ready
- [ ] Screenshot tool ready
- [ ] Note notepad ready (for timings)

**During demo:**
- [ ] Follow exact steps
- [ ] Show database/API when needed
- [ ] Speak clearly (explain each step)
- [ ] Pause for questions between phases
- [ ] Take screenshots at key points

**After demo:**
- [ ] Ask for feedback
- [ ] Collect questions
- [ ] Schedule action items
- [ ] Archive screenshots/recording

---

**Total demo time:** 10-15 minutes  
**Buffer time:** 5 minutes (for questions/troubleshooting)  
**Total session:** 20 minutes  

**Last updated:** 2026-06-10  
**Version:** 1.0
