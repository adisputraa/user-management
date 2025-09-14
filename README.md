
# User Management System API

Ini adalah REST API untuk sistem manajemen user yang dibuat untuk implementasi toko online mitra merchant.


## 🛠️ Tech Stack

- Java 17
- Spring Boot 3.x.x
- Spring Security (OAuth2 / JWT)
- Spring Data JPA
- PostgreSQL
- Keycloak (Identity and Access Management)
- Docker & Docker Compose
- Maven
## 🚀 Prasyarat
- JDK 17+
- Docker & Docker Compose
- Git



## ⚙️ How to Run

Proses penyiapan dan menjalankan proyek ini terdiri dari 3 langkah utama.

1.  **Clone Repositori**
    ```bash
    git clone https://github.com/adisputraa/user-management.git
    cd usermanagement
    ```

2.  **Jalankan Service Pendukung**
Pastikan Docker sedang berjalan, lalu jalankan perintah berikut dari root folder project untuk menyalakan PostgreSQL dan Keycloak:

    docker-compose up -d
    

3.  **Konfigurasi Keycloak (Setup Awal)**
    - Buka Keycloak Admin Console di `http://localhost:8081` dan login (`admin`/`admin`).
    - Buat Realm baru: `bionic-merchant`.
    - Di dalam realm tersebut, buat 3 Roles: `ADMIN`, `CUSTOMER`, `MITRA`.
    - Konfigurasi client `admin-cli` dan `bionic-api` dengan mengaktifkan `Client authentication` dan `Service accounts roles`, lalu catat **Client Secret** dari keduanya.

4.  **Konfigurasi Aplikasi**
    - Di folder `src/main/resources/`, buat duplikat dari `application.yml.example` dan ganti namanya menjadi `application.yml`.
    - Buka file `application.yml` tersebut dan isi nilai `client-secret` dan `login-client-secret` dengan secret yang kamu dapatkan dari Keycloak pada langkah sebelumnya.

5.  **Jalankan Aplikasi Spring Boot**
Jalankan perintah berikut dari root folder project:

    ./mvnw spring-boot:run
Aplikasi akan berjalan di 
`http://localhost:8080`.

## Documentation API

Dokumentasi API lengkap, termasuk contoh request dan response untuk setiap endpoint, dapat diakses melalui link Postman publik di bawah ini:

**[Documentation](https://documenter.getpostman.com/view/33766988/2sB3HqGd6n)**



## Authors
Adi Setiadi Putra

[@adisputraa](https://github.com/adisputraa)

