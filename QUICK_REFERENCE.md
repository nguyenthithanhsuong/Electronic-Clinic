# Quick Reference - Refactor 3 Roles
## Cheat Sheet cho Team

**In lại hoặc paste trên monitor để dễ tra cứu!**

---

## 🔷 FILE CHÍNH CẦN SỬA

### Backend
```
src/main/java/com/eclinic/api/
  ├─ AuthHandler.java          ← Fix role mapping
  ├─ UsersHandler.java         ← Add RECEPTIONIST validation
  └─ QueueHandler.java         ← Link appointment + JOIN doctor

data/
  └─ supabase-schema.sql       ← Add RECEPTIONIST to constraint
```

### Frontend
```
core/api/
  ├─ userService.ts            ← Add RECEPTIONIST to Role type
  └─ authService.ts            ← Add RECEPTIONIST to Role type

modules/admin/components/
  └─ user-dialog.tsx           ← Add <SelectItem value="RECEPTIONIST">

shared/lib/rbac/
  └─ allowed-roles-for-path.ts ← Verify routes permissions

context/
  └─ AuthContext.tsx           ← Check role type (if needed)
```

---

## 🚀 QUICK START COMMANDS

### Backend setup
```bash
# Test DB migration (on staging)
cd /path/to/Electronic-Clinic
psql -U postgres -d clinic_db -f data/supabase-schema.sql

# Compile
mvn clean compile

# Run tests
mvn test

# Run app
mvn spring-boot:run
# OR
java -cp target/classes com.eclinic.api.RestServer
```

### Frontend setup
```bash
cd /path/to/emr-system-frontend

# Install
npm install

# Type check
npm run build
# OR
npx tsc --noEmit

# Tests (if exists)
npm test

# Dev server
npm run dev
# Access: http://localhost:3000
```

---

## 📝 PROMPT TEMPLATE (Copy & Modify)

### For AI coding:
```
[Your request]

Files to modify: [list files]

Current [code/logic]:
[paste current code]

Should become:
[describe target]

Requirements:
1. [req1]
2. [req2]

Error handling:
- [error case 1] → [expected response]

Example:
[provide example input/output]

Please provide:
1. [specific output you want]
2. [specific output you want]
```

---

## ✅ VERIFICATION CHECKLIST BY STEP

### Bước 1: DB Migration
```
[ ] supabase-schema.sql has 'RECEPTIONIST' in role check
[ ] Can INSERT user with role='RECEPTIONIST' on staging
[ ] Migration script documented
```

### Bước 2: AuthHandler
```
[ ] AuthHandler.login() removes mapRole() call
[ ] JWT token contains exact role from DB (not mapped)
[ ] Test: Login receptionist → token has role='RECEPTIONIST'
[ ] Test: Login doctor → token has role='DOCTOR'
[ ] No null/exception on login
```

### Bước 3: UsersHandler
```
[ ] POST /api/users accepts role='RECEPTIONIST'
[ ] POST validates role in [ADMIN, DOCTOR, RECEPTIONIST, PATIENT]
[ ] PUT /api/users/{id} accepts all 4 roles
[ ] Response includes: id, username, role, status
[ ] Test: curl POST with RECEPTIONIST ✅
[ ] Test: curl POST with INVALID role → 400 ✅
```

### Bước 4: QueueHandler
```
[ ] POST /api/patient-queue accepts appointmentId
[ ] Validates: appointment exists + matches patient
[ ] Stores both patient_id and appointment_id in DB
[ ] GET /api/patient-queue returns:
    - patientId, patientName
    - appointmentId, appointmentTime
    - doctorId, doctorName
[ ] Test: curl POST with appointment ✅
[ ] Test: curl GET sees doctor details ✅
```

### Bước 5: TS Types
```
[ ] core/api/userService.ts has "RECEPTIONIST" in Role type
[ ] core/api/authService.ts has "RECEPTIONIST" in Role type
[ ] npm run build: 0 TypeScript errors
[ ] IDE autocomplete shows RECEPTIONIST option
```

### Bước 6: user-dialog.tsx
```
[ ] Form has 4 role options: Admin, Tiếp tân, Bác sĩ, Bệnh nhân
[ ] Can select "Tiếp tân" from dropdown
[ ] Submit sends role='RECEPTIONIST' to backend
[ ] Form edit shows RECEPTIONIST for existing receptionist user
```

### Bước 7: RBAC
```
[ ] /reception routes allow: RECEPTIONIST, ADMIN
[ ] /doctor routes allow: DOCTOR, ADMIN
[ ] /admin routes allow: ADMIN only
[ ] Test: Receptionist login → can access /reception ✅
[ ] Test: Receptionist login → cannot access /doctor (403/redirect) ✅
[ ] Test: Doctor login → can access /doctor ✅
[ ] Test: Doctor login → cannot access /admin (403/redirect) ✅
```

### Bước 8: Testing
```
[ ] Unit tests run: mvn test → all PASS
[ ] AuthHandler test: 3 roles return correct values
[ ] UsersHandler test: RECEPTIONIST create works
[ ] QueueHandler test: appointment linked correctly
[ ] Frontend tests: Role type valid
[ ] Manual workflow test: end-to-end scenario PASS
```

---

## 🔍 TESTING QUICK COMMANDS

### Create test user (curl)
```bash
# Create receptionist
curl -X POST http://localhost:8080/api/users \
  -H "Authorization: Bearer <admin_token>" \
  -H "Content-Type: application/json" \
  -d '{
    "username": "test_receptionist",
    "password": "TestPass123",
    "role": "RECEPTIONIST",
    "status": "ACTIVE"
  }'

# Expected response: 200 OK with user object containing role="RECEPTIONIST"
```

### Login test (curl)
```bash
# Login receptionist
curl -X POST http://localhost:8080/api/login \
  -H "Content-Type: application/json" \
  -d '{
    "username": "test_receptionist",
    "password": "TestPass123"
  }'

# Expected: JWT token with payload.role="RECEPTIONIST"
# Verify at jwt.io
```

### Get queue test (curl)
```bash
# Get queue items
curl -X GET http://localhost:8080/api/patient-queue?status=WAITING \
  -H "Authorization: Bearer <receptionist_token>"

# Expected: Array with fields:
# - patientId, patientName
# - appointmentId, doctorId, doctorName, appointmentTime
```

### Frontend build test
```bash
cd emr-system-frontend
npm run build 2>&1 | grep -i "error\|fail"
# Should show: 0 errors
```

---

## 🐛 COMMON BUGS & FIXES

| Bug | Solution |
|-----|----------|
| TypeScript error: Type "RECEPTIONIST" not assignable | Add "RECEPTIONIST" to Role type union |
| 400 Invalid role when creating RECEPTIONIST | Check UsersHandler validation logic |
| JWT token doesn't contain role | Check AuthHandler isn't mapping role |
| Queue item missing doctor name | Add JOIN appointments in QueueHandler query |
| Frontend can't access /reception | Check RBAC config + user.role |
| User can't login with RECEPTIONIST role | Check auth token generation + DB constraint |
| npm build error about React types | npm install → npm run build |

---

## 📊 ROLE MATRIX (Quick Reference)

```
┌──────────────┬────────┬──────┬────────────┐
│ Feature      │ Admin  │ Rec  │ Doctor     │
├──────────────┼────────┼──────┼────────────┤
│ Create user  │ ✅     │ ❌   │ ❌         │
│ Create patient│ ✅    │ ✅   │ ❌         │
│ Create appt  │ ✅     │ ✅   │ ❌         │
│ Queue patient│ ❌     │ ✅   │ ❌         │
│ Write record │ ❌     │ ❌   │ ✅         │
│ Prescribe    │ ❌     │ ❌   │ ✅         │
│ View audit   │ ✅     │ ❌   │ ❌         │
│ Settings     │ ✅     │ ❌   │ ❌         │
└──────────────┴────────┴──────┴────────────┘
Legend: ✅=can do, ❌=cannot
```

---

## 🎯 DAILY STANDUP TEMPLATE

**Each person say (1 min):**
```
✅ Done yesterday: [step/task]
🔄 Working today: [step/task]
🚧 Blocker: [if any] OR "None"
📈 Status: [% complete]
```

**Example:**
```
Backend Dev1:
✅ Done: DB migration tested on staging
🔄 Today: AuthHandler fix
🚧 Blocker: Need staging DB admin password
📈 Status: 15%
```

---

## 📞 ESCALATION CHECKLIST

**If stuck, check:**
1. [ ] Read implementation guide section again
2. [ ] Google error message
3. [ ] Ask teammate (pair program 5 min)
4. [ ] Ask Tech Lead
5. [ ] Ask on team Slack

**Provide tech lead:**
- Error message (full stack trace)
- What you're trying to do
- Code snippet (if applicable)
- Steps to reproduce

---

## 🚀 BEFORE YOU COMMIT

```bash
# Code style check (optional but good)
# For Java: run formatter (IDE: Ctrl+Alt+L)
# For TS: run prettier (IDE: Shift+Alt+F)

# Type check (Frontend)
npm run build 2>&1 | head -20

# Tests (Backend)
mvn test -DfailIfNoTests=false

# Manual verify
# [Follow "Cách verify" from implementation guide]

# Commit message
git commit -m "refactor: [Bước N] [short description]"

# Example:
git commit -m "refactor: Bước 2 - Fix AuthHandler role mapping"
```

---

## 📅 MILESTONE CHECKPOINTS

| Date | Milestone | Status |
|------|-----------|--------|
| T2 | DB + Auth done | ⭕ |
| T3 | UsersHandler + TS types | ⭕ |
| T4 | QueueHandler + RBAC | ⭕ |
| T5 | Code review + bug fix | ⭕ |
| T6 | Testing + merge staging | ⭕ |
| T7 | Demo ready ✅ | ⭕ |

---

## 🎬 LIVE DEMO CHECKLIST (T7)

Do this exact sequence in front of team:
```
[ ] 1. Logout all users
[ ] 2. Clear browser cache
[ ] 3. Admin login → create receptionist "demo_receptionist"
[ ] 4. Receptionist login ← SHOW ROLE IS CORRECT
[ ] 5. Create patient "Demo Patient" ← SHOW ID
[ ] 6. Create appointment with doctor "BS Demo" ← SHOW APPOINTMENT ID
[ ] 7. Queue patient ← SHOW DOCTOR NAME + TIME IN QUEUE ITEM
[ ] 8. Doctor login → view schedule ← SHOW PATIENT + TIME
[ ] 9. Doctor click patient → view history + kê đơn
[ ] 10. Receptionist mark DONE + payment
[ ] 11. Admin view audit log ← SHOW ALL ACTIONS RECORDED

If anything breaks: Have backup screenshot/video
```

---

## 📚 REFERENCE DOCS

**Read in order:**
1. REFACTOR_PLAN_3_ROLES.md (overview)
2. TEAM_BRIEFING.md (phân công)
3. IMPLEMENTATION_GUIDE_STEP_BY_STEP.md (chi tiết)
4. This file (quick lookup)

**When building:**
- Keep IMPLEMENTATION_GUIDE open
- Copy prompt from section → paste to AI
- Follow "Cách verify" section

---

**Last updated:** 2026-06-10  
**Version:** 1.0  
**Print & Post on Monitor!** 📌
