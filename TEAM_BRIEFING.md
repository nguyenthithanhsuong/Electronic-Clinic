# Team Briefing: Refactor 3 Roles - Receptionist, Doctor, Admin

**Ngày:** 2026-06-10  
**Người tham gia:** [Backend team: 2-3 người, Frontend team: 2 người, QA/Tester: 1 người]  
**Deadline:** 7-10 ngày  
**Status:** 🎯 Ready to start  

---

## 🎯 MỤC TIÊU CHÍNH

Refactor hệ thống **Electronic Clinic** để hỗ trợ **3 role chính**:
- **ADMIN** (Quản trị hệ thống)
- **RECEPTIONIST** (Tiếp tân bộ phận) ← **NEW & MAIN ROLE**
- **DOCTOR** (Bác sĩ)

**Kết quả:** Hệ thống hoạt động **trơn trư**, **dữ liệu đồng bộ**, **quy trình bệnh viện tuân thủ**, **demo ấn tượng**.

---

## 📝 TẠI SAO REFACTOR?

### Vấn đề hiện tại:
```
❌ RECEPTIONIST role không tồn tại trong database (chỉ ADMIN, DOCTOR, PATIENT)
❌ Frontend dùng RECEPTIONIST nhưng backend không hỗ trợ → role mismatch
❌ AuthHandler map role sai → user login nhưng role không đúng
❌ Queue item tạo nhưng không liên kết appointment → doctor không biết bệnh nhân nào
❌ User creation không rõ ràng → có thể tạo user mà không có profile
❌ Admin settings page tĩnh → cấu hình không lưu
```

### Giải pháp:
- Thêm `RECEPTIONIST` vào database
- Fix backend auth, user, queue handlers
- Align frontend types + UI
- Test toàn bộ workflow

---

## 🚀 WORKFLOW RECEPTIONIST (Quy trình bệnh viện)

### "Một bệnh nhân đến khám"

```
1. RECEPTIONIST đăng nhập
   └─→ Role: RECEPTIONIST
   └─→ Page: /reception

2. RECEPTIONIST tạo hồ sơ bệnh nhân
   └─→ Tên, tuổi, BHYT, SĐT
   └─→ Saved: patients(id=10)

3. RECEPTIONIST đặt lịch khám
   └─→ Chọn bác sĩ (BS Nguyễn Thị B)
   └─→ Chọn ngày/giờ (2026-06-10 10:30)
   └─→ Saved: appointments(id=100, patient_id=10, doctor_id=5)

4. RECEPTIONIST xếp hàng chờ
   └─→ Bệnh nhân đến → xếp queue
   └─→ Saved: patient_queue(patient_id=10, appointment_id=100, status='WAITING')
   └─→ ⚡ QUAN TRỌNG: queue item gắn với appointment & doctor

5. DOCTOR khám bệnh
   └─→ Xem schedule: "10:30 - Nguyễn Văn A (lý do: Khám tổng quát)"
   └─→ Click bệnh nhân → xem hồ sơ (lịch sử khám trước)
   └─→ Kê đơn thuốc (search/filter category)
   └─→ Ghi bệnh án (chẩn đoán, hướng điều trị)
   └─→ Saved: medical_records + prescriptions

6. RECEPTIONIST hoàn tất
   └─→ Mark queue item DONE
   └─→ Tính tiền + in hóa đơn
   └─→ Saved: payments(patient_id=10, amount=650k)

✅ KẾT QUẢ: Hồ sơ bệnh nhân hoàn chỉnh, audit trail đầy đủ
```

---

## 📊 PHÂN CÔNG & TIMELINE

### Tuần 1 (7-10 ngày)

| Ngày | Backend | Frontend | QA |
|------|---------|----------|-----|
| T2 (6/10) | **Bước 1:** DB migration<br/>**Bước 2:** AuthHandler fix | **Bước 5:** TS types | Prepare test plan |
| T3 (6/11) | **Bước 3:** UsersHandler | **Bước 6:** user-dialog | Prepare test data |
| T4 (6/12) | **Bước 4:** QueueHandler | **Bước 7:** RBAC check | Setup staging |
| T5 (6/13) | Peer review + bug fix | Testing | Testing |
| T6 (6/14) | **Bước 8:** Unit tests | Manual tests | Manual tests |
| T7 (6/15) | Staging deployment + monitoring | Demo prep | Demo |

### Người phụ trách:
- **Backend:** Dev1 (senior) + Dev2 (middle) [~4-5 giờ/ngày]
- **Frontend:** Dev3 (middle) + Dev4 (junior) [~2-3 giờ/ngày]
- **QA:** Tester1 [~2-3 giờ/ngày]
- **Review:** Tech Lead [~1 tiếng/ngày]

---

## 📚 DOCUMENTS BẠN SẼ CẦN

### 1. [REFACTOR_PLAN_3_ROLES.md](REFACTOR_PLAN_3_ROLES.md)
**Đọc trước tiên!**
- 📖 Overview: 3 role chức năng, workflow, liên hệ
- 📊 Bảng so sánh quyền truy cập
- 💡 Độ lớn code change (~32%, không đập đi xây lại)
- 🎬 Demo scenario trước/sau

### 2. [IMPLEMENTATION_GUIDE_STEP_BY_STEP.md](IMPLEMENTATION_GUIDE_STEP_BY_STEP.md)
**Đây là document chính để làm việc!**
- 🔧 8 bước chi tiết (DB → Backend → Frontend → Testing)
- 💬 **Prompt AI chặt chẽ** cho mỗi bước (copy/paste vào Copilot)
- ✅ Kết quả mong đợi + cách verify
- 📋 Checklist done criteria

### 3. Này file (TEAM_BRIEFING.md)
**Đọc trước tiên để hiểu overview!**
- 🎯 Mục tiêu chính
- 🚀 Workflow quy trình bệnh viện
- 📊 Phân công & timeline
- 📝 Hướng dẫn làm việc

---

## 🎬 CÁCH THỰC HIỆN CÔNG VIỆC

### Với **Backend developers:**
1. Đọc [REFACTOR_PLAN_3_ROLES.md](REFACTOR_PLAN_3_ROLES.md) → hiểu overview (20 phút)
2. Đọc [IMPLEMENTATION_GUIDE_STEP_BY_STEP.md](IMPLEMENTATION_GUIDE_STEP_BY_STEP.md) → làm bước 1-4 (6-7 giờ)
3. Mỗi bước:
   - Copy **Prompt AI** → paste vào **Copilot / Claude**
   - Copilot tạo code
   - Bạn review + integrate vào project
   - Chạy **Cách verify** để kiểm tra
   - Commit git

### Với **Frontend developers:**
1. Đọc [REFACTOR_PLAN_3_ROLES.md](REFACTOR_PLAN_3_ROLES.md) → hiểu overview (20 phút)
2. Đọc [IMPLEMENTATION_GUIDE_STEP_BY_STEP.md](IMPLEMENTATION_GUIDE_STEP_BY_STEP.md) → làm bước 5-7 (1-1.5 giờ)
3. Tương tự: Prompt → Copilot → Review → Verify → Commit

### Với **QA/Tester:**
1. Đọc [REFACTOR_PLAN_3_ROLES.md](REFACTOR_PLAN_3_ROLES.md) → hiểu workflow (20 phút)
2. Chuẩn bị test data: 4 users (admin, receptionist, doctor, patient)
3. Chuẩn bị checklist manual test (dựa trên "Cách verify" trong guide)
4. Chạy unit tests + manual smoke tests (bước 8)
5. Ghi lại kết quả, report bugs

---

## ⚡ QUICK START

### 1️⃣ Backend Dev (Dev1 - Senior)
**Ngày T2 sáng:**
```
[ ] Đọc REFACTOR_PLAN_3_ROLES.md (20 phút)
[ ] Đọc IMPLEMENTATION_GUIDE bước 1-2 (20 phút)
[ ] Bước 1: Copy prompt SQL → Copilot → apply schema
[ ] Verify: DB insert test ✅
[ ] Bước 2: Copy prompt AuthHandler → Copilot → review code
[ ] Verify: Login test với 3 role ✅
[ ] Push PR + code review
```
**Effort:** ~2.5 giờ

### 2️⃣ Backend Dev (Dev2 - Middle)
**Ngày T2-T3:**
```
[ ] Đọc REFACTOR_PLAN_3_ROLES.md + guide bước 3-4
[ ] Bước 3: Copy prompt UsersHandler → Copilot → integrate
[ ] Verify: curl POST /api/users role=RECEPTIONIST ✅
[ ] Bước 4: Copy prompt QueueHandler → Copilot → integrate
[ ] Verify: GET queue with doctor details ✅
[ ] Push PR + code review
```
**Effort:** ~4 giờ

### 3️⃣ Frontend Dev (Dev3 - Middle)
**Ngày T2-T3:**
```
[ ] Đọc REFACTOR_PLAN_3_ROLES.md + guide bước 5-7
[ ] Bước 5: Update TS types RECEPTIONIST
[ ] Verify: npm run build ✅
[ ] Bước 6: Add RECEPTIONIST option user-dialog
[ ] Verify: Visual test form ✅
[ ] Bước 7: Check RBAC routes
[ ] Push PR + code review
```
**Effort:** ~1.5 giờ

### 4️⃣ Frontend Junior (Dev4)
**Hỗ trợ:**
```
[ ] Pair programming với Dev3
[ ] Chạy npm run build verify
[ ] Visual testing UI
```

### 5️⃣ QA (Tester1)
**Ngày T4-T7:**
```
[ ] Setup staging database
[ ] Prepare 4 test users + test data
[ ] Bước 8: Run unit tests (mvn test)
[ ] Manual smoke test checklist (30 phút)
[ ] Report bugs / sign off
```

---

## ✅ DONE CRITERIA

Khi hoàn tất, hệ thống phải:

✅ **Database:** `RECEPTIONIST` role được lưu trữ  
✅ **Backend:**
  - [ ] AuthHandler trả role chính xác (test với 3 role)
  - [ ] RECEPTIONIST user có thể được tạo bởi admin
  - [ ] Queue item liên kết appointment + doctor name
  - [ ] Unit tests PASS 100%

✅ **Frontend:**
  - [ ] TypeScript compile không lỗi
  - [ ] Admin form hiển thị 4 role options
  - [ ] RECEPTIONIST login → /reception page
  - [ ] DOCTOR login → /doctor page
  - [ ] RBAC bảo vệ routes đúng

✅ **Manual Testing:**
  - [ ] Full workflow: receptionist → appointment → queue → doctor khám → đơn → thanh toán
  - [ ] All role logins work
  - [ ] Audit log records actions

---

## 🚨 RISK & MITIGATION

| Risk | Likelihood | Impact | Mitigation |
|------|-----------|--------|-----------|
| Git conflict | Low | Medium | Merge early + small PRs |
| DB migration fail | Very low | High | Test on staging first |
| Role type mismatch | Low | Medium | All tests PASS before deploy |
| Frontend RBAC bug | Low | High | Manual test all route transitions |
| Data loss | Very low | Critical | Backup before migration |

---

## 📞 SUPPORT & COMMUNICATION

### Daily Standup (15 phút):
- 09:00 AM: T2-T6
- **Mỗi người:** 1 câu status + blockers

### Questions?
- Tech Lead: [name] (slack/email)
- For AI coding help: Paste code + prompt vào Claude/Copilot

### Merge Strategy:
- Small PRs (1 bước = 1 PR)
- Code review từ Tech Lead
- Merge → staging deploy
- QA verify → mark done

---

## 📹 DEMO PLAN (T7)

### Demo Script (10 phút):
```
1. Admin login → Tạo receptionist user ✅
2. Receptionist login → Tạo bệnh nhân "Nguyễn Văn A" ✅
3. Receptionist tạo appointment → BS Nguyễn Thị B, 10h30 ✅
4. Receptionist xếp queue → Show "Position #2" + "BS Nguyễn Thị B" ✅
5. Doctor login → View schedule "Nguyễn Văn A 10:30" ✅
6. Doctor click patient → View history + kê đơn ✅
7. Receptionist thanh toán → Show payment record ✅
8. Admin view audit log → Show tất cả actions ✅

🎬 RESULT: Hệ thống hoạt động complete, no bugs ✨
```

---

## 📋 NEXT STEPS

1. **Assign tasks** theo phân công
2. **Read documents** (20 phút/người)
3. **Start Bước 1** (DB migration)
4. **Daily standup** + progress tracking
5. **Daily code review** + merge staging
6. **QA testing** nội bộ
7. **Demo + sign-off** T7

---

## 🎯 SUCCESS METRIC

- ✅ 100% tests PASS
- ✅ 0 bugs critical/high severity
- ✅ Demo workflow hoàn chỉnh (no skips)
- ✅ Audit log đầy đủ
- ✅ Team confident to deploy

---

**Status:** 🎯 Ready to go!  
**Last updated:** 2026-06-10  
**Version:** 1.0

---

## 📌 CHECKLIST BEFORE START

Team lead, kiểm tra:
- [ ] Tất cả 5 người có read docs?
- [ ] Staging environment setup sẵn?
- [ ] Test data prepared (4 users)?
- [ ] AI tool access (Copilot/Claude)?
- [ ] Git branch strategy agreed?
- [ ] Daily standup time confirmed?

✅ All checked → **Ready to start Bước 1!** 🚀
