# Kế hoạch Refactor: 3 Role chính (Admin, Receptionist, Doctor)

**Ngày:** 2026-06-09  
**Mục đích:** Chuẩn hóa hệ thống quản lý bệnh án theo quy trình bệnh viện thực tế  
**Scope:** Backend Java + Frontend Next.js  

---

## 📋 PHẦN 1: CHỨC NĂNG CỦA 3 ROLE

### 🔷 Role 1: **ADMIN** (Quản trị hệ thống)

**Trách nhiệm chính:**
- Quản lý toàn bộ người dùng trong hệ thống (tạo, xóa, chỉnh sửa, khóa/mở khóa tài khoản)
- Quản lý danh sách bác sĩ: thêm, sửa, xóa bác sĩ
- Quản lý danh sách tiếp tân (receptionist)
- Quản lý thuốc: tạo, sửa, xóa, phân loại thuốc
- Quản lý cấu hình hệ thống (settings): giờ làm việc, quy định, cấu hình khác
- Xem audit log (lịch sử hoạt động của tất cả người dùng)
- Xem dashboard (thống kê tổng quan)

**Pages trong Frontend:**
- `/admin/staff` → quản lý user (admin/receptionist/doctor/patient)
- `/admin/doctors` → quản lý danh sách bác sĩ
- `/admin/medicines` → quản lý thuốc
- `/admin/settings` → cấu hình hệ thống
- `/admin/dashboard` → thống kê, báo cáo

**API Backend:**
- `POST /api/users` → tạo user (role: ADMIN, RECEPTIONIST, DOCTOR, PATIENT)
- `PUT /api/users/{id}` → cập nhật user
- `DELETE /api/users/{id}` → xóa user
- `POST /api/users/{id}/lock` → khóa tài khoản
- `POST /api/users/{id}/unlock` → mở khóa tài khoản
- `GET/POST/PUT/DELETE /api/doctors` → quản lý doctor profile
- `GET/POST/PUT/DELETE /api/medicines` → quản lý thuốc
- `GET /api/dashboard/stats` → thống kê
- `GET /api/audit-logs` → xem lịch sử

---

### 🟢 Role 2: **RECEPTIONIST** (Tiếp tân bộ phận)

**Trách nhiệm chính:**
- **Tạo hồ sơ bệnh nhân:** nhập thông tin cơ bản (tên, tuổi, BHYT, liên hệ...)
- **Tạo tài khoản bệnh nhân:** nếu bệnh nhân muốn đăng nhập hệ thống
- **Đặt lịch khám:** lựa chọn bác sĩ, ngày/giờ, lý do khám
- **Xếp hàng chờ (queue):** bệnh nhân đến khám → xếp vào hàng chờ
- **Liên kết appointment với queue:** khi bệnh nhân được gọi vào phòng khám
- **Xem/in hóa đơn thanh toán:** nhận tiền, lập biên lai
- **Thống kê lịch hẹn:** xem danh sách appointment trong ngày
- **Không thể:** sửa bệnh án, kê đơn thuốc, xóa dữ liệu medical

**Pages trong Frontend:**
- `/reception/patients` → danh sách bệnh nhân + tạo mới
- `/reception/appointments` → danh sách lịch hẹn + tạo mới
- `/reception/queue` → xếp hàng chờ khám
- `/reception/payments` → xử lý thanh toán

**API Backend:**
- `GET/POST /api/patients` → danh sách/tạo bệnh nhân
- `GET/POST /api/appointments` → danh sách/tạo appointment
- `GET/POST/PUT /api/patient-queue` → quản lý queue
- `POST /api/patient-queue/{id}/attach-appointment` → gắn appointment vào queue item
- `GET/POST /api/payments` → danh sách/tạo thanh toán

---

### 🔵 Role 3: **DOCTOR** (Bác sĩ)

**Trách nhiệm chính:**
- **Xem danh sách bệnh nhân hôm nay:** những ai đặt lịch với mình
- **Xem hồ sơ bệnh nhân:** lịch sử khám, chẩn đoán, thuốc đã dùng
- **Kê đơn thuốc:** chọn thuốc từ danh sách, ghi liều lượng
- **Ghi chép bệnh án:** chẩn đoán, hướng điều trị, ghi chú
- **Xem lịch khám (appointment):** xem lịch cá nhân
- **Không thể:** tạo/xóa user, quản lý thuốc, xem tài chính

**Pages trong Frontend:**
- `/doctor/schedule` → lịch khám hôm nay + timeline
- `/doctor/patients` → danh sách bệnh nhân
- `/doctor/patient/{id}/profile` → xem hồ sơ chi tiết
- `/doctor/patient/{id}/medical-record` → thêm bệnh án / kê đơn

**API Backend:**
- `GET /api/appointments?doctorId=X` → lịch khám của bác sĩ
- `GET /api/patients/{id}` → xem hồ sơ bệnh nhân
- `GET /api/medical-records/{patientId}` → lịch sử khám
- `POST /api/prescriptions` → kê đơn mới
- `POST /api/medical-records` → ghi bệnh án

---

## 🔄 PHẦN 2: LIÊN HỆ GIỮA CÁC ROLE & WORKFLOW

### Workflow 1: **Bệnh nhân đến khám** (Main Flow)

```
┌─────────────────────────────────────────────────────────────────┐
│                     WORKFLOW: Bệnh nhân khám                    │
└─────────────────────────────────────────────────────────────────┘

1. RECEPTIONIST tạo bệnh nhân
   │
   ├─→ Nhập thông tin cơ bản (tên, tuổi, BHYT, SĐT)
   └─→ DB: INSERT INTO patients(...)

2. RECEPTIONIST đặt appointment
   │
   ├─→ Chọn bác sĩ (doctor_id) từ danh sách
   ├─→ Chọn ngày/giờ khám
   ├─→ Ghi lý do khám
   └─→ DB: INSERT INTO appointments(patient_id, doctor_id, ...)

3. RECEPTIONIST xếp queue
   │
   ├─→ Bệnh nhân đến khám → xếp vào hàng chờ
   ├─→ GHI NHẬN: patient_id + appointment_id vào queue
   └─→ DB: INSERT INTO patient_queue(patient_id, appointment_id, status='WAITING')

4. RECEPTIONIST gọi queue
   │
   ├─→ Xem hàng chờ, gọi bệnh nhân tiếp theo
   └─→ DB: UPDATE patient_queue SET status='CALLED' WHERE id=X

5. DOCTOR khám bệnh
   │
   ├─→ Xem appointment + hồ sơ bệnh nhân
   ├─→ Ghi chú bệnh án (chẩn đoán, triệu chứng)
   ├─→ Kê đơn thuốc (chọn từ danh sách medicine)
   └─→ DB: INSERT INTO medical_records(...), INSERT INTO prescriptions(...)

6. RECEPTIONIST hoàn tất queue
   │
   ├─→ Bệnh nhân xong khám → đánh dấu DONE
   ├─→ In hóa đơn / tính tiền
   └─→ DB: UPDATE patient_queue SET status='DONE'; INSERT INTO payments(...)

END: Hồ sơ bệnh nhân hoàn chỉnh, queue item closed, payment recorded.
```

### Sơ đồ phân quyền truy cập dữ liệu:

```
┌─────────────────────────┬──────────────┬───────────────┬────────────┐
│ Tính năng / Dữ liệu     │ ADMIN        │ RECEPTIONIST  │ DOCTOR     │
├─────────────────────────┼──────────────┼───────────────┼────────────┤
│ Tạo/Sửa User           │ ✅           │ ❌            │ ❌         │
│ Quản lý Doctor         │ ✅           │ ❌            │ ❌         │
│ Tạo Bệnh nhân          │ ✅ (admin)   │ ✅ ← Main     │ ❌         │
│ Tạo Appointment        │ ✅ (admin)   │ ✅ ← Main     │ ❌         │
│ Xếp Queue              │ ❌           │ ✅ ← Main     │ ❌         │
│ Xem Hồ sơ bệnh nhân    │ ✅           │ ✅ (own)      │ ✅ (own)   │
│ Ghi bệnh án            │ ❌           │ ❌            │ ✅ ← Main  │
│ Kê đơn thuốc           │ ❌           │ ❌            │ ✅ ← Main  │
│ Quản lý Thuốc          │ ✅ ← Main    │ ❌            │ ❌         │
│ Thanh toán             │ ✅ (admin)   │ ✅ ← Main     │ ❌         │
│ Xem Audit log          │ ✅ ← Main    │ ❌            │ ❌         │
└─────────────────────────┴──────────────┴───────────────┴────────────┘
Legend: ✅ = có quyền truy cập; ← Main = chức năng chính của role
```

---

## 💻 PHẦN 3: ĐỘ LỚN CÓ THAY ĐỔI CODE?

### ✅ **KHÔNG PHẢI DẬP ĐI XÂY LẠI TỪ ĐẦU!**

**Thực tế:**
- Backend và Frontend đã có ~80% cơ sở để hỗ trợ 3 role này.
- Các bảng DB (patients, appointments, patient_queue, medical_records, prescriptions) **đã tồn tại**.
- Các handler/DAO cho user, patient, doctor, queue **đã được viết**.
- Frontend pages, routes, components **đã có sẵn**.

**Vậy cần thay đổi gì?** Chỉ là **align** (sửa nhỏ) để hoạt động **đúng** và **đồng bộ**:

### Bảng ước lượng độ lớn thay đổi:

| Component | Loại | Thay đổi | % Code | Ghi chú |
|-----------|------|---------|--------|---------|
| **DB Schema** | SQL | Thêm `RECEPTIONIST` vào role check | <1% | 1 dòng SQL |
| **AuthHandler** | Java | Fix return role thực + remove mapping mặc định | ~5% (30 dòng) | Xóa logic sai, thêm log |
| **UsersHandler** | Java | Kiểm tra validation + response | ~3% (20 dòng) | Nhỏ |
| **PatientsHandler** | Java | Ensure userId + trả object đầy đủ | ~5% (30 dòng) | Thêm check null |
| **QueueHandler** | Java | Support appointment_id + trả full data | ~8% (50 dòng) | Thêm endpoint + validation |
| **Frontend userService.ts** | TS | Thêm `RECEPTIONIST` type | <1% | 1 dòng type |
| **Frontend authService.ts** | TS | Update type union | <1% | 1 dòng |
| **Frontend user-dialog.tsx** | TSX | Thêm RECEPTIONIST option | ~2% (5 dòng) | Conditional render |
| **Frontend RBAC** | TS | Verify route permissions (no change needed) | 0% | Đã đúng |
| **Frontend pages** | TSX | No changes needed (đã có) | 0% | Hỗ trợ sẵn |
| **Tests** | Java/TS | Thêm test cases | 0% (nếu skip) | Optional |
| **Docs/Migration** | SQL + MD | Thêm migration script + README | 0% | Support only |
| | | | **~32% tổng** | |

**Kết luận:** Thay đổi code **rất nhỏ**, ~5 files Java + 3 files TS chính; mỗi file chỉ chỉnh 2-5%, không làm hỏng cơ sở hiện tại.

---

## 🎬 PHẦN 4: KỆT QUẢ DEMO SAU KHI REFACTOR

### Scenario Demo: "Một bệnh nhân đến khám"

#### **Trước Refactor** (Hiện tại - Có bug):
```
❌ Admin tạo receptionist tài khoản
   └─→ Frontend: role type error (userService không có RECEPTIONIST)
   └─→ Server: role mismatch, AuthHandler map sai

❌ Receptionist login
   └─→ Login thành công nhưng role không rõ ràng
   └─→ Có thể truy cập page không nên truy cập (RBAC sai)

❌ Receptionist tạo bệnh nhân + appointment
   └─→ Patient hồ sơ tạo nhưng không có tài khoản (hoặc tài khoản bị tạo sai)
   └─→ API trả thiếu thông tin

❌ Receptionist xếp queue
   └─→ Queue item tạo nhưng appointment_id có thể bị null/sai
   └─→ Doctor không biết patient nào sẽ vào

❌ Doctor khám
   └─→ Xem appointment nhưng queue item và appointment không sync
   └─→ Bệnh án + đơn thuốc có thể không liên kết đúng với appointment

❌ Admin settings
   └─→ Page là UI tĩnh, không lưu được gì

🎭 RESULT: Hệ thống chạy nhưng **không ổn định**, **dữ liệu không đồng bộ**, **quy trình bệnh viện không tuân thủ**.
```

#### **Sau Refactor** (Target - No Bugs):
```
✅ Admin tạo receptionist tài khoản
   ├─→ Form user-dialog hiển thị option "Tiếp tân"
   ├─→ Backend accept RECEPTIONIST role
   └─→ Database lưu trữ đúng role

✅ Receptionist login
   ├─→ Login thành công → AuthHandler trả role: "RECEPTIONIST" (chính xác)
   ├─→ Token JWT chứa role RECEPTIONIST
   ├─→ Frontend redirect tới /reception (đúng page)
   ├─→ RBAC kiểm tra role → cho phép vào /reception (✓)
   └─→ Dashboard hiển thị các tính năng của receptionist

✅ Receptionist tạo bệnh nhân
   ├─→ Form input: tên, tuổi, BHYT, SĐT, ...
   ├─→ Backend:
   │   ├─→ Tạo record patients(name, age, ...)
   │   ├─→ (Nếu có yêu cầu) Tạo user account role PATIENT
   │   └─→ Trả response: { patientId, userId, ...}
   ├─→ Frontend ghi nhận thành công
   └─→ ✅ Bệnh nhân hồ sơ được tạo có uuid duy nhất

✅ Receptionist tạo appointment
   ├─→ Form: chọn bác sĩ, ngày/giờ, lý do
   ├─→ Backend:
   │   ├─→ Kiểm tra doctor tồn tại
   │   ├─→ Kiểm tra patient tồn tại
   │   ├─→ Tạo record appointments(patient_id, doctor_id, ...)
   │   └─→ Trả response: { appointmentId, patientId, doctorId, ...}
   └─→ ✅ Lịch khám được ghi nhận, doctor sẽ nhìn thấy appointment trong schedule

✅ Receptionist xếp queue
   ├─→ Bệnh nhân đến → nhấn "Xếp hàng chờ"
   ├─→ Form hiển thị: patient + appointment (đã tạo trước đó)
   ├─→ Backend:
   │   ├─→ INSERT patient_queue(patient_id=X, appointment_id=Y, status='WAITING')
   │   ├─→ Validate: appointment.doctor_id && patient_id match
   │   └─→ Trả: { queueId, patientId, appointmentId, position, ...}
   ├─→ Frontend: hiển thị "Bệnh nhân được xếp ở vị trí #3" + "Sẽ khám với BS Nguyễn Văn A vào 10h30"
   └─→ ✅ Queue item có trạng thái rõ, liên kết appointment chính xác

✅ Doctor xem lịch khám
   ├─→ Vào trang /doctor/schedule
   ├─→ Backend: GET /appointments?doctor_id=5&date=2026-06-09
   ├─→ Response: [{ id: 1, patientId: 10, patientName: "Nguyễn Văn B", time: "10:30", queuePosition: 3, ... }]
   ├─→ Frontend: hiển thị timeline "BS Nguyễn Văn A hôm nay có 5 bệnh nhân"
   └─→ ✅ Doctor biết rõ bệnh nhân nào, thứ tự, queue position

✅ Doctor xem hồ sơ bệnh nhân
   ├─→ Click patient → /doctor/patient/10/profile
   ├─→ Backend: GET /patients/10 + GET /medical-records?patient_id=10
   ├─→ Response: 
   │   {
   │     patient: { id, name, age, phone, insuranceId, ... },
   │     medicalHistory: [ 
   │       { date: "2026-05-01", diagnosis: "Cảm cúm", prescriptions: [...] },
   │       { date: "2026-04-10", diagnosis: "Đau đầu", prescriptions: [...] }
   │     ]
   │   }
   ├─→ Frontend: hiển thị profile + lịch khám trước
   └─→ ✅ Doctor có đầy đủ context trước khi khám

✅ Doctor kê đơn thuốc
   ├─→ Vào /doctor/patient/10/medical-record (page ghi bệnh án)
   ├─→ Form:
   │   ├─→ Diagnosis: "Viêm phổi"
   │   ├─→ Add medicines: (click "Tìm thuốc" modal)
   │   │   └─→ Modal MedicineSearchModal: search + filter by category
   │   │   └─→ Select thuốc → ghi liều lượng, hướng dùng
   │   └─→ Submit → backend tạo medical_record + prescriptions
   ├─→ Backend response: { recordId, prescriptionId[], ... }
   └─→ ✅ Đơn thuốc được lưu, liên kết với appointment + patient

✅ Receptionist hoàn tất & thanh toán
   ├─→ Doctor xong khám → status appointment = "COMPLETED"
   ├─→ Receptionist xem queue item → status = "DONE" (tự động hoặc thủ công)
   ├─→ Receptionist tính tiền:
   │   ├─→ Khám bệnh: 150k
   │   ├─→ Thuốc: 500k
   │   └─→ Tổng: 650k
   ├─→ Backend: INSERT payments(patient_id=10, amount=650000, description="...", status='PAID')
   └─→ ✅ Hóa đơn được ghi nhận, có thể in/xuất phiếu thanh toán

✅ Admin xem audit log
   ├─→ Vào /admin/audit-logs
   ├─→ Log hiển thị:
   │   - 2026-06-09 09:00 RECEPTIONIST:user_2 tạo patient(10)
   │   - 2026-06-09 09:05 RECEPTIONIST:user_2 tạo appointment(100, patient=10, doctor=5)
   │   - 2026-06-09 09:10 RECEPTIONIST:user_2 enqueue patient(10, appointment=100)
   │   - 2026-06-09 10:30 DOCTOR:user_5 tạo medical_record(patient=10)
   │   - 2026-06-09 10:35 DOCTOR:user_5 tạo prescription(patient=10)
   │   - 2026-06-09 10:40 RECEPTIONIST:user_2 tạo payment(patient=10, amount=650k)
   └─→ ✅ Có thể track từng bước, debug nếu có sai sót

✅ Admin settings (nếu implement)
   ├─→ Cấu hình: giờ mở cửa, giờ đóng cửa, max queue size, ...
   ├─→ Save → DB lưu config
   ├─→ System sử dụng config này (ví dụ: check giờ appointment nằm trong giờ khám)
   └─→ ✅ Hệ thống linh hoạt theo quy định bệnh viện

🎭 RESULT: Hệ thống hoạt động **trơn tru**, **dữ liệu đồng bộ**, **quy trình bệnh viện tuân thủ**, **audit trail đầy đủ**.
```

---

## 📊 PHẦN 5: TÓMO TẮT VĂN BẰNG REFACTOR

| Khía cạnh | Chi tiết |
|-----------|---------|
| **Thời gian** | ~7-10 ngày làm việc (5 người, chia công việc) |
| **Files thay đổi** | ~12-15 files (8 Java + 4-5 TS) |
| **Khả năng break** | Rất thấp (<5%) vì changes là localized |
| **Rollback** | Dễ (git revert hoặc restore từ backup DB) |
| **Migration strategy** | Chuẩn bị SQL migration, run staging trước |
| **Testing** | Unit + integration tests (new + existing) |
| **Demo kết quả** | Full workflow: bệnh nhân → queue → khám → đơn thuốc → thanh toán |
| **Compliance** | Tuân thủ quy trình bệnh viện, audit trail đầy đủ |

---

## ✨ PHẦN 6: CÁC BENEFIT SAU REFACTOR

### Cho **Team Dev**:
- ✅ Code rõ ràng, không ambiguous (role mapping, user creation)
- ✅ Dễ maintain, test, debug
- ✅ Cấu trúc hỗ trợ mở rộng (thêm role khác sau này)

### Cho **PM / Product**:
- ✅ Tuân thủ quy trình bệnh viện (receptionist → appointment → queue → doctor)
- ✅ Đầy đủ audit log cho compliance / kiểm tra
- ✅ Demo ấn tượng (workflow hoàn chỉnh, không bug)

### Cho **Bệnh viện / End User**:
- ✅ Receptionist có tính năng riêng, không nhầm lẫn
- ✅ Doctor xem rõ lịch khám, hồ sơ bệnh nhân
- ✅ Admin quản lý trung tâm, khóa tài khoản
- ✅ Dữ liệu nhất quán, không mất thông tin
- ✅ Hóa đơn, đơn thuốc được lưu đầy đủ

---

## 🎯 KẾT LUẬN

- **Không phải đập đi xây lại**: chỉ align + sửa nhỏ, ~32% code change
- **3 role rõ ràng**: Admin (quản lý) ← Receptionist (tiếp tân) → Doctor (khám)
- **Workflow tuân thủ**: bệnh nhân → appointment → queue → khám → đơn → thanh toán
- **Demo sẽ thấy**: Hệ thống hoạt động trơn trư, không bug, quy trình hoàn chỉnh
- **Risk thấp**: Changes là incremental, không phá vỡ cơ sở hiện tại
