# 📱 Perkapp API Documentation (v1)

Dokumentasi API untuk aplikasi mobile Perkapp (Manajemen Perlengkapan UKM Cakra Manggala).

## 🔐 Autentikasi

Gunakan JWT (JSON Web Token) untuk autentikasi. Token harus dikirimkan melalui header `Authorization` dengan format Bearer.

**Base URL:** `https://domain.com/api/v1`

**Header:**
```http
Authorization: Bearer <your_token>
Accept: application/json
```

---

## 🔑 Auth Endpoints

### 1. Register
* **URL:** `/auth/register`
* **Method:** `POST`
* **Body:**
  ```json
  {
    "name": "John Doe",
    "email": "john@example.com",
    "password": "password123",
    "role": "member" 
  }
  ```
* **Response (201):**
  ```json
  {
    "success": true,
    "message": "User successfully registered",
    "data": {
      "user": { ... },
      "token": "xxx.yyy.zzz"
    }
  }
  ```

### 2. Login
* **URL:** `/auth/login`
* **Method:** `POST`
* **Body:**
  ```json
  {
    "email": "john@example.com",
    "password": "password123"
  }
  ```
* **Response (200):**
  ```json
  {
    "success": true,
    "message": "Login successful",
    "data": {
      "token": "xxx.yyy.zzz",
      "user": { ... }
    }
  }
  ```

### 3. Me (Profile)
* **URL:** `/auth/me`
* **Method:** `GET`
* **Auth:** Required
* **Response (200):**
  ```json
  {
    "success": true,
    "message": "User profile retrieved",
    "data": { ... }
  }
  ```

---

## 📅 Kegiatan Endpoints

### 1. List Kegiatan
* **URL:** `/kegiatan`
* **Method:** `GET`
* **Auth:** Required
* **Response:**
  ```json
  {
    "success": true,
    "message": "List of activities",
    "data": [ { ... }, { ... } ]
  }
  ```

### 2. Create Kegiatan
* **URL:** `/kegiatan`
* **Method:** `POST`
* **Auth:** Required
* **Body:**
  ```json
  {
    "name": "Latihan Dasar Kepemimpinan",
    "description": "Pelatihan untuk pengurus baru",
    "date": "2024-05-20",
    "status": "draft"
  }
  ```

### 3. Detail Kegiatan
* **URL:** `/kegiatan/{id}`
* **Method:** `GET`
* **Auth:** Required

### 4. Update Kegiatan
* **URL:** `/kegiatan/{id}`
* **Method:** `PUT`
* **Auth:** Required
* **Logic:** Jika status diubah menjadi `completed`, maka `available_qty` pada alat-alat yang terkait akan otomatis bertambah (dikembalikan).

### 5. Delete Kegiatan
* **URL:** `/kegiatan/{id}`
* **Method:** `DELETE`
* **Auth:** Required
* **Logic:** Jika kegiatan dihapus dan status belum `completed`, stok alat akan dikembalikan secara otomatis.

---

## 🛠️ Alat Endpoints

### 1. List Alat
* **URL:** `/alat`
* **Method:** `GET`
* **Auth:** Required

### 2. Detail Alat
* **URL:** `/alat/{id}`
* **Method:** `GET`
* **Auth:** Required

### 3. Create Alat
* **URL:** `/alat`
* **Method:** `POST`
* **Body:**
  ```json
  {
    "name": "Tenda Dome",
    "category": "Outdoor",
    "total_qty": 10,
    "condition": "good"
  }
  ```

### 4. Update Alat
* **URL:** `/alat/{id}`
* **Method:** `PUT`
* **Auth:** Required

### 5. Delete Alat
* **URL:** `/alat/{id}`
* **Method:** `DELETE`
* **Auth:** Required

---

## 🔗 Kegiatan Alat (Relasi)

### 1. Tambah Alat ke Kegiatan
* **URL:** `/kegiatan-alat`
* **Method:** `POST`
* **Auth:** Required
* **Body:**
  ```json
  {
    "kegiatan_id": "uuid-kegiatan",
    "alat_id": "uuid-alat",
    "qty": 2
  }
  ```
* **Logic:** Mengurangi `available_qty` pada tabel `alat`. Tidak bisa melebihi stok yang tersedia.

### 2. Get Alat per Kegiatan
* **URL:** `/kegiatan/{id}/alat`
* **Method:** `GET`

---

## 📸 Image Upload

### 1. Upload Gambar
* **URL:** `/upload-image`
* **Method:** `POST`
* **Auth:** Required
* **Body (Multipart Form):**
  * `image`: File (jpeg, png, max 2MB)
  * `entity_type`: "alat" | "kegiatan"
  * `entity_id`: UUID alat atau kegiatan
* **Response:**
  ```json
  {
    "success": true,
    "message": "Image uploaded successfully",
    "data": {
      "image_url": "https://domain.com/storage/inventory/xxx.jpg"
    }
  }
  ```

---

## ⚠️ Error Responses

Semua error akan mengembalikan format:
```json
{
  "success": false,
  "message": "Pesan error",
  "data": { ... validation details ... }
}
```
* `401`: Unauthorized / Token Expired
* `422`: Validation Error
* `404`: Not Found
* `400`: Bad Request (contoh: Stok habis)
