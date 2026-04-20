# Electronic Clinic


## Cách chạy nhanh:


## Compile:
javac -encoding UTF-8 -d target/classes src/main/java/com/eclinic/*.java

## Run
java -cp target/classes com.eclinic.App

## One-command run on Windows PowerShell
```powershell
.\run-backend.ps1
```

## Mục tiêu

Demo Factory Method Design Pattern cho use case **Create User** trong hệ thống Electronic Clinic.

Lý do chọn use case này:

- Một luồng tạo tài khoản có nhiều biến thể: `Doctor` và `Patient`.
- Mỗi biến thể phải lưu dữ liệu vào các bảng khác nhau.
- Dễ mở rộng thêm `Nurse`, `Pharmacist`, `Receptionist` sau này.

## Triển khai từng bước bằng Java

### Bước 1: Tạo Product chung

File: [src/main/java/com/eclinic/domain/users/IUser.java](src/main/java/com/eclinic/domain/users/IUser.java)

`IUser` định nghĩa các hành vi chung của mọi user:

- `getId()`
- `getFullName()`
- `getEmail()`
- `getPhoneNumber()`
- `getRole()`
- `saveProfile()`

### Bước 2: Tạo Concrete Products

File: [src/main/java/com/eclinic/domain/users/DoctorUser.java](src/main/java/com/eclinic/domain/users/DoctorUser.java)

File: [src/main/java/com/eclinic/domain/users/PatientUser.java](src/main/java/com/eclinic/domain/users/PatientUser.java)

- `DoctorUser` lưu vào bảng `Users` và `Doctor`.
- `PatientUser` lưu vào bảng `Users` và `Patient`.

### Bước 3: Tạo Creator trừu tượng

File: [src/main/java/com/eclinic/factories/UserFactory.java](src/main/java/com/eclinic/factories/UserFactory.java)

`UserFactory` định nghĩa factory method `createUser()` và hàm chung `registerAccount()`.

### Bước 4: Tạo Concrete Creators

File: [src/main/java/com/eclinic/factories/DoctorFactory.java](src/main/java/com/eclinic/factories/DoctorFactory.java)

File: [src/main/java/com/eclinic/factories/PatientFactory.java](src/main/java/com/eclinic/factories/PatientFactory.java)

- `DoctorFactory` tạo `DoctorUser`.
- `PatientFactory` tạo `PatientUser`.

### Bước 5: Tạo service điều phối

File: [src/main/java/com/eclinic/application/UserService.java](src/main/java/com/eclinic/application/UserService.java)

Service này nhận `userType`, chọn đúng factory, rồi gọi `registerAccount()`.

### Bước 6: Controller chỉ làm nhiệm vụ điều hướng

File: [src/main/java/com/eclinic/application/Client.java](src/main/java/com/eclinic/application/Client.java)

Controller không biết chi tiết khởi tạo object cụ thể. Nó chỉ gọi service.

### Bước 7: Mô phỏng database

File: [src/main/java/com/eclinic/persistence/Database.java](src/main/java/com/eclinic/persistence/Database.java)

Các record đi kèm:

- [UserRecord](src/main/java/com/eclinic/persistence/UserRecord.java)
- [DoctorRecord](src/main/java/com/eclinic/persistence/DoctorRecord.java)
- [PatientRecord](src/main/java/com/eclinic/persistence/PatientRecord.java)

Phần này mô phỏng việc lưu vào bảng `Users`, `Doctor`, `Patient`.

### Bước 8: Chạy demo

File: [src/main/java/com/eclinic/App.java](src/main/java/com/eclinic/App.java)

`App` tạo `Doctor` và `Patient`, sau đó in snapshot dữ liệu để kiểm tra.

## Luồng Factory Method trong bài toán này

1. Admin gửi yêu cầu tạo tài khoản.
2. Service xác định loại user.
3. Factory tương ứng tạo object cụ thể.
4. Object tự lưu dữ liệu của nó.
5. Database mô phỏng cho thấy dữ liệu đã vào đúng bảng.

## Cấu trúc thư mục

```text
src/main/java/com/eclinic/
├── App.java
├── application/
│   ├── Client.java
│   └── UserService.java
├── domain/
│   └── users/
│       ├── IUser.java
│       ├── DoctorUser.java
│       └── PatientUser.java
├── factories/
│   ├── UserFactory.java
│   ├── DoctorFactory.java
│   └── PatientFactory.java
└── persistence/
    ├── Database.java
    ├── UserRecord.java
    ├── DoctorRecord.java
    └── PatientRecord.java
```

## Cách chạy backend độc lập

Backend Java là một HTTP server chạy riêng, không phụ thuộc vào frontend.

### PowerShell

```powershell
cd "f:\2026CodingStuff\Electronic-Clinic"
.\run-backend.ps1
```

Khi chạy xong, backend sẽ lắng nghe tại `http://localhost:8080`.

Nếu bạn đang chạy trong PowerShell của máy này, `javac` hiện tại là bản cũ nhưng vẫn đủ để build code hiện tại. Nếu một máy khác có JDK mới hơn, backend vẫn chạy bình thường sau khi compile.

### Kiểm tra backend

```powershell
Invoke-WebRequest http://localhost:8080/api/health
```

Nếu backend chạy đúng, response sẽ trả về JSON `{"status":"ok"}`.

Ghi chú: không chạy `javac App.java` bên trong `src/main/java/com/eclinic`, vì cách đó sẽ sinh `.class` trực tiếp trong thư mục source.

## Frontend demo

Ngoài bản Java console demo, bạn có thể mở frontend tĩnh tại [frontend/index.html](frontend/index.html) để xem Factory Method theo dạng giao diện người dùng.

Giao diện frontend hiện đã được chuyển sang tiếng Việt.

Frontend này chỉ là client gọi vào backend đang chạy sẵn. Bạn có thể mở nó sau khi backend đã bật.

Frontend này cho phép:

- Chọn `Doctor` hoặc `Patient`
- Nhập thông tin user
- Tạo bản ghi mới bằng flow Factory Method
- Xem dữ liệu được đưa vào bảng `Users` và bảng riêng theo loại user

## Kết luận

Use case `Create User` là ví dụ phù hợp nhất để demo Factory Method trong Electronic Clinic vì nó có nhiều biến thể, có logic lưu trữ riêng, và dễ mở rộng mà không phải sửa code cũ.
