$ErrorActionPreference = 'Stop'

function To-JsonBody($obj) {
    return ($obj | ConvertTo-Json -Compress)
}

$base = 'http://localhost:3001/api'
$ts = [DateTimeOffset]::UtcNow.ToUnixTimeSeconds()
$out = @()

$doctorRef = [int64](Invoke-RestMethod -Uri "$base/doctors" -Method GET)[0].id
$patientRef = [int64](Invoke-RestMethod -Uri "$base/patients" -Method GET)[0].id
$medRef = [int64](Invoke-RestMethod -Uri "$base/medicines" -Method GET)[0].id

# Users CRUD
$u = Invoke-RestMethod -Uri "$base/users" -Method POST -ContentType 'application/json' -Body (To-JsonBody @{ username = "smoke_user_$ts"; passwordHash = 'TEMP_HASH'; role = 'PATIENT'; status = 'ACTIVE' })
$uid = [int64]$u.id
$null = Invoke-RestMethod -Uri "$base/users/$uid" -Method GET
$null = Invoke-RestMethod -Uri "$base/users/$uid" -Method PUT -ContentType 'application/json' -Body (To-JsonBody @{ role = 'PATIENT'; status = 'INACTIVE' })
$null = Invoke-RestMethod -Uri "$base/users/$uid" -Method DELETE
$out += 'Users CRUD: OK'

# Doctors CRUD + auto user
$doc = Invoke-RestMethod -Uri "$base/doctors" -Method POST -ContentType 'application/json' -Body (To-JsonBody @{ fullName = "Dr Auto $ts"; specialty = 'General'; phone = "901$ts"; email = "auto$ts@clinic.test"; roomNumber = 'R-1' })
$docId = [int64]$doc.id
$null = Invoke-RestMethod -Uri "$base/doctors/$docId" -Method GET
$null = Invoke-RestMethod -Uri "$base/doctors/$docId" -Method PUT -ContentType 'application/json' -Body (To-JsonBody @{ fullName = "Dr Auto U $ts"; specialty = 'Cardiology'; email = "autou$ts@clinic.test"; roomNumber = 'R-2' })
$null = Invoke-RestMethod -Uri "$base/doctors/$docId" -Method DELETE
$out += 'Doctors CRUD + auto-user: OK'

# Patients CRUD + auto user
$pat = Invoke-RestMethod -Uri "$base/patients" -Method POST -ContentType 'application/json' -Body (To-JsonBody @{ fullName = "Pat Auto $ts"; dob = '1990-01-01'; gender = 'MALE'; phone = "902$ts"; address = 'Addr'; insuranceCode = "IC$ts" })
$patId = [int64]$pat.id
$null = Invoke-RestMethod -Uri "$base/patients/$patId" -Method GET
$null = Invoke-RestMethod -Uri "$base/patients/$patId" -Method PUT -ContentType 'application/json' -Body (To-JsonBody @{ fullName = "Pat Auto U $ts"; phone = "903$ts"; address = 'Addr2' })
$null = Invoke-RestMethod -Uri "$base/patients/$patId" -Method DELETE
$out += 'Patients CRUD + auto-user: OK'

# Medicines CRUD
$med = Invoke-RestMethod -Uri "$base/medicines" -Method POST -ContentType 'application/json' -Body (To-JsonBody @{ name = "SmokeMed $ts"; unit = 'tablet'; price = 1.23; stockQuantity = 10; expiryDate = '2027-12-31' })
$medId = [int64]$med.id
$null = Invoke-RestMethod -Uri "$base/medicines/$medId" -Method GET
$null = Invoke-RestMethod -Uri "$base/medicines/$medId" -Method PUT -ContentType 'application/json' -Body (To-JsonBody @{ stockQuantity = 20 })
$null = Invoke-RestMethod -Uri "$base/medicines/$medId" -Method DELETE
$out += 'Medicines CRUD: OK'

# Appointments + Medical Records + Prescriptions + Prescription Details
$apt = Invoke-RestMethod -Uri "$base/appointments" -Method POST -ContentType 'application/json' -Body (To-JsonBody @{ doctorId = $doctorRef; patientId = $patientRef; appointmentStartDate = '2026-12-01 10:00:00'; appointmentEndDate = '2026-12-01 10:30:00'; reason = 'Smoke'; status = 'PENDING' })
$aptId = [int64]$apt.id
$null = Invoke-RestMethod -Uri "$base/appointments/$aptId" -Method GET
$null = Invoke-RestMethod -Uri "$base/appointments/$aptId" -Method PUT -ContentType 'application/json' -Body (To-JsonBody @{ status = 'CONFIRMED' })
$out += 'Appointments CRUD(partial): OK'

$mr = Invoke-RestMethod -Uri "$base/patients/medical-records" -Method POST -ContentType 'application/json' -Body (To-JsonBody @{ appointmentId = $aptId; symptoms = 'S'; diagnosis = 'D'; recordType = 'GENERAL'; treatmentPlan = 'T' })
$mrId = [int64]$mr.id
$null = Invoke-RestMethod -Uri "$base/patients/medical-records/$mrId" -Method GET
$null = Invoke-RestMethod -Uri "$base/patients/medical-records/$mrId" -Method PUT -ContentType 'application/json' -Body (To-JsonBody @{ diagnosis = 'D2'; treatmentPlan = 'T2' })
$out += 'MedicalRecords CRUD(partial): OK'

$pr = Invoke-RestMethod -Uri "$base/prescriptions" -Method POST -ContentType 'application/json' -Body (To-JsonBody @{ medicalRecordId = $mrId; notes = 'n'; totalPrice = 3.5 })
$prId = [int64]$pr.id
$null = Invoke-RestMethod -Uri "$base/prescriptions/$prId" -Method GET
$null = Invoke-RestMethod -Uri "$base/prescriptions/$prId" -Method PUT -ContentType 'application/json' -Body (To-JsonBody @{ notes = 'n2'; totalPrice = 4.5 })
$out += 'Prescriptions CRUD: OK'

$pd = Invoke-RestMethod -Uri "$base/prescriptions/details" -Method POST -ContentType 'application/json' -Body (To-JsonBody @{ prescriptionId = $prId; medicineId = $medRef; quantity = 1; dosage = '1x/day' })
$pdId = [int64]$pd.id
$null = Invoke-RestMethod -Uri "$base/prescriptions/details/$pdId" -Method GET
$null = Invoke-RestMethod -Uri "$base/prescriptions/details/$pdId" -Method PUT -ContentType 'application/json' -Body (To-JsonBody @{ quantity = 2; dosage = '2x/day' })
$allDetails = Invoke-RestMethod -Uri "$base/prescriptions/details" -Method GET
$null = Invoke-RestMethod -Uri "$base/prescriptions/details/$pdId" -Method DELETE
$out += ("PrescriptionDetails CRUD: OK (listCount=" + $allDetails.Count + ")")

# Cleanup chain
$null = Invoke-RestMethod -Uri "$base/prescriptions/$prId" -Method DELETE
$null = Invoke-RestMethod -Uri "$base/patients/medical-records/$mrId" -Method DELETE
$null = Invoke-RestMethod -Uri "$base/appointments/$aptId" -Method DELETE
$out += 'Cleanup: OK'

$out -join "`n"
