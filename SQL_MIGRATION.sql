-- SQL Migration: Add RECEPTIONIST Role Support
-- Date: 2026-06-10
-- Purpose: Enable RECEPTIONIST role in database schema
-- Status: Ready to apply on staging environment
-- Rollback: See section at bottom

-- =============================================================================
-- MIGRATION SCRIPT: Add RECEPTIONIST to users.role constraint
-- =============================================================================

-- STEP 1: Update the CHECK constraint on users table
-- This adds 'RECEPTIONIST' to the allowed roles

ALTER TABLE users
DROP CONSTRAINT users_role_check;

ALTER TABLE users
ADD CONSTRAINT users_role_check 
CHECK (role IN ('ADMIN', 'DOCTOR', 'RECEPTIONIST', 'PATIENT'));

-- STEP 2: Verify constraint is applied
-- (This is just informational - helps team verify)
SELECT constraint_name, constraint_type
FROM information_schema.table_constraints
WHERE table_name = 'users' AND constraint_name LIKE '%role%';

-- Expected output:
-- constraint_name       | constraint_type
-- ---------------------+-----------------
-- users_role_check      | CHECK
-- (1 row)

-- =============================================================================
-- VERIFICATION QUERIES (Run after migration)
-- =============================================================================

-- Check 1: Verify constraint exists
SELECT 
  constraint_name,
  constraint_type
FROM information_schema.table_constraints
WHERE table_name = 'users' AND constraint_name = 'users_role_check';
-- Expected: Should return 1 row with constraint_type = 'CHECK'

-- Check 2: Insert test user with RECEPTIONIST role (verify constraint works)
BEGIN;

INSERT INTO users (username, password, role, status, created_at, updated_at)
VALUES (
  'test_receptionist_migration', 
  'hashed_password_here',
  'RECEPTIONIST',
  'ACTIVE',
  NOW(),
  NOW()
);

-- If no error, migration succeeded ✅
-- Rollback this test insert:
ROLLBACK;

-- Check 3: Show current users by role
SELECT 
  COUNT(*) as count,
  role
FROM users
GROUP BY role
ORDER BY role;
-- Expected: Should have 0 or more RECEPTIONIST entries after migration

-- Check 4: Verify existing users still valid
SELECT id, username, role, status
FROM users
WHERE role IN ('ADMIN', 'DOCTOR', 'PATIENT')
LIMIT 5;
-- Expected: All existing users unaffected

-- =============================================================================
-- TESTING SQL (Run manually to verify)
-- =============================================================================

-- Test 1: Try insert RECEPTIONIST (should succeed)
INSERT INTO users (username, password, role, status, created_at, updated_at)
VALUES (
  'test_rec_' || NOW()::text,
  'password123',
  'RECEPTIONIST',
  'ACTIVE',
  NOW(),
  NOW()
);
-- Expected: INSERT 0 1 (success)

-- Test 2: Try insert invalid role (should fail with constraint violation)
INSERT INTO users (username, password, role, status, created_at, updated_at)
VALUES (
  'test_invalid',
  'password123',
  'INVALID_ROLE',  -- ← Not allowed
  'ACTIVE',
  NOW(),
  NOW()
);
-- Expected: ERROR:  new row for relation "users" violates check constraint "users_role_check"

-- Test 3: Query receptionist users
SELECT id, username, role
FROM users
WHERE role = 'RECEPTIONIST'
ORDER BY created_at DESC;
-- Expected: List of receptionist users (if any exist)

-- =============================================================================
-- ROLLBACK SCRIPT (If migration fails - run this)
-- =============================================================================

-- ONLY RUN IF MIGRATION FAILS:

-- Rollback 1: Remove new constraint
ALTER TABLE users
DROP CONSTRAINT users_role_check;

-- Rollback 2: Restore old constraint (without RECEPTIONIST)
ALTER TABLE users
ADD CONSTRAINT users_role_check 
CHECK (role IN ('ADMIN', 'DOCTOR', 'PATIENT'));

-- Rollback 3: Delete any RECEPTIONIST users created (if they exist)
DELETE FROM users WHERE role = 'RECEPTIONIST';

-- Verify rollback succeeded
SELECT constraint_name FROM information_schema.table_constraints
WHERE table_name = 'users' AND constraint_name = 'users_role_check';
-- Expected: Should show constraint without RECEPTIONIST

-- =============================================================================
-- MIGRATION NOTES
-- =============================================================================

/*
WHAT CHANGED:
- users.role CHECK constraint expanded from 3 values to 4 values
- New allowed role: 'RECEPTIONIST'
- All existing data unaffected (ADMIN, DOCTOR, PATIENT users still valid)

BEFORE:
  CHECK (role IN ('ADMIN', 'DOCTOR', 'PATIENT'))

AFTER:
  CHECK (role IN ('ADMIN', 'DOCTOR', 'RECEPTIONIST', 'PATIENT'))

DEPENDENCIES AFTER THIS CHANGE:
1. Backend: AuthHandler must be updated to handle RECEPTIONIST role
2. Backend: UsersHandler must accept RECEPTIONIST in POST/PUT requests
3. Frontend: TS types (userService.ts, authService.ts) must include RECEPTIONIST
4. Frontend: user-dialog.tsx must show RECEPTIONIST option in role select

DEPLOYMENT STEPS:
1. Apply this SQL on STAGING first
2. Verify with Check queries (see above)
3. Test backend API with RECEPTIONIST user
4. If OK → apply on PRODUCTION
5. Update code on both frontend + backend after DB change

DATABASE BACKUP:
Before applying: pg_dump clinic_db > backup_before_migration_2026_06_10.sql

TIMING:
- Application time: < 1 second
- Risk: VERY LOW (only schema change, no data migration)
- Rollback time: < 1 second
- Testing time: 5-10 minutes

VERIFICATION CHECKLIST:
☐ Check migration applies without error
☐ Verify constraint exists (Check 1 query)
☐ Test insert RECEPTIONIST user (Check 2 query)
☐ Verify existing users unaffected (Check 3 query)
☐ Test API: POST /api/users with role=RECEPTIONIST
☐ Verify JWT token includes role=RECEPTIONIST on login
☐ Test frontend: Admin can select RECEPTIONIST in user creation form

*/

-- =============================================================================
-- END OF MIGRATION SCRIPT
-- =============================================================================

-- Script version: 1.0
-- Created: 2026-06-10
-- Author: Electronic Clinic Team
-- Status: ✅ Ready to apply
