#!/usr/bin/env powershell
# Test script for email and password CRUD operations for doctors and patients

$baseUrl = "http://localhost:8888"

# Colors for output
$green = "`e[32m"
$red = "`e[31m"
$reset = "`e[0m"

function Test-Request {
    param (
        [string]$Method,
        [string]$Endpoint,
        [string]$Body,
        [int]$ExpectedStatus
    )
    
    try {
        if ($Body) {
            $response = Invoke-WebRequest -Uri "$baseUrl$Endpoint" -Method $Method -Body $Body -ContentType "application/json" -ErrorAction SilentlyContinue -SkipHttpErrorCheck
        } else {
            $response = Invoke-WebRequest -Uri "$baseUrl$Endpoint" -Method $Method -ErrorAction SilentlyContinue -SkipHttpErrorCheck
        }
        
        $status = $response.StatusCode
        if ($status -eq $ExpectedStatus) {
            Write-Host "${green}✓${reset} $Method $Endpoint - Status: $status"
            return $response.Content | ConvertFrom-Json
        } else {
            Write-Host "${red}✗${reset} $Method $Endpoint - Expected: $ExpectedStatus, Got: $status"
            Write-Host $response.Content
            return $null
        }
    } catch {
        Write-Host "${red}✗${reset} $Method $Endpoint - Error: $_"
        return $null
    }
}

Write-Host "`nTesting Email and Password CRUD Operations`n"
Write-Host "========================================`n"

# Test 1: Create a doctor with email
Write-Host "Test 1: Create a doctor with email and password capability"
$doctorPayload = @{
    fullName = "Dr. Test Doctor"
    specialty = "Cardiology"
    phone = "0987654321"
    email = "doctor@test.com"
    roomNumber = "101"
} | ConvertTo-Json

$doctorResponse = Test-Request -Method "POST" -Endpoint "/api/doctors" -Body $doctorPayload -ExpectedStatus 201
$doctorId = $doctorResponse.id
Write-Host "Created doctor with ID: $doctorId`n"

# Test 2: Get the doctor to verify email is stored
Write-Host "Test 2: Get doctor details"
$getDoctorResponse = Test-Request -Method "GET" -Endpoint "/api/doctors/$doctorId" -ExpectedStatus 200
if ($getDoctorResponse) {
    Write-Host "Doctor email: $($getDoctorResponse.email)`n"
}

# Test 3: Update doctor email and password
Write-Host "Test 3: Update doctor email and password"
$updateDoctorPayload = @{
    fullName = "Dr. Test Doctor"
    specialty = "Cardiology"
    email = "newemail@doctor.com"
    roomNumber = "101"
    password = "NewSecurePassword123!"
} | ConvertTo-Json

Test-Request -Method "PUT" -Endpoint "/api/doctors/$doctorId" -Body $updateDoctorPayload -ExpectedStatus 200

# Test 4: Verify email was updated
Write-Host "`nTest 4: Verify doctor email was updated"
$verifyDoctorResponse = Test-Request -Method "GET" -Endpoint "/api/doctors/$doctorId" -ExpectedStatus 200
if ($verifyDoctorResponse) {
    Write-Host "Updated doctor email: $($verifyDoctorResponse.email)`n"
}

# Test 5: Create a patient with email support
Write-Host "Test 5: Create a patient with email and password capability"
$patientPayload = @{
    fullName = "Test Patient"
    dob = "1990-01-01"
    gender = "M"
    phone = "0123456789"
    address = "123 Test Street"
    insuranceCode = "INS123456"
} | ConvertTo-Json

$patientResponse = Test-Request -Method "POST" -Endpoint "/api/patients" -Body $patientPayload -ExpectedStatus 201
$patientId = $patientResponse.id
Write-Host "Created patient with ID: $patientId`n"

# Test 6: Get the patient to verify initial data
Write-Host "Test 6: Get patient details"
$getPatientResponse = Test-Request -Method "GET" -Endpoint "/api/patients/$patientId" -ExpectedStatus 200
if ($getPatientResponse) {
    Write-Host "Patient name: $($getPatientResponse.fullName)`n"
}

# Test 7: Update patient email and password
Write-Host "Test 7: Update patient email and password"
$updatePatientPayload = @{
    fullName = "Test Patient"
    phone = "0123456789"
    address = "456 New Street"
    email = "patient@test.com"
    password = "PatientSecurePassword123!"
} | ConvertTo-Json

Test-Request -Method "PUT" -Endpoint "/api/patients/$patientId" -Body $updatePatientPayload -ExpectedStatus 200

# Test 8: Verify patient email was updated
Write-Host "`nTest 8: Verify patient was updated"
$verifyPatientResponse = Test-Request -Method "GET" -Endpoint "/api/patients/$patientId" -ExpectedStatus 200
if ($verifyPatientResponse) {
    Write-Host "Patient name: $($verifyPatientResponse.fullName)`n"
}

# Test 9: Delete doctor
Write-Host "Test 9: Delete doctor"
Test-Request -Method "DELETE" -Endpoint "/api/doctors/$doctorId" -ExpectedStatus 200

# Test 10: Verify doctor is deleted
Write-Host "`nTest 10: Verify doctor is deleted"
Test-Request -Method "GET" -Endpoint "/api/doctors/$doctorId" -ExpectedStatus 404

Write-Host "`nTests completed!`n"
