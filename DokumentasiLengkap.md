# 📱 SIEPERKAP — Dokumentasi Lengkap

> Aplikasi mobile manajemen perlengkapan UKM PA Cakra Manggala

---

## 1. Standarisasi Global (WAJIB DIPEGANG TIM)

### Base URL API
```
https://api.perkapp.com/v1
```

### Format Response API
Semua endpoint harus menggunakan format:
```json
{
  "success": true,
  "message": "Success",
  "data": {}
}
```

### Header
```http
Authorization: Bearer <token>
Content-Type: application/json
```

### Penamaan Data
Gunakan `snake_case` dalam bahasa Inggris. Contoh: `created_at`, `updated_at`, `user_id`, `kegiatan_id`, `sync_status`.

### Status Standar

| Jenis | Nilai |
|-------|-------|
| Sync Status | `pending` \| `synced` \| `failed` |
| Status Kegiatan | `draft` \| `ongoing` \| `completed` |
| Kondisi Alat | `good` \| `damaged` |

---

## 2. Entity / Model Utama

### 1. User
```json
{
  "id": "string",
  "name": "string",
  "email": "string",
  "role": "admin | member",
  "created_at": "timestamp"
}
```

### 2. Kegiatan
```json
{
  "id": "string",
  "name": "string",
  "description": "string",
  "date": "date",
  "status": "draft | ongoing | completed",
  "created_by": "user_id",
  "created_at": "timestamp",
  "updated_at": "timestamp",
  "sync_status": "pending | synced"
}
```

### 3. Alat
```json
{
  "id": "string",
  "name": "string",
  "category": "string",
  "total_qty": 10,
  "available_qty": 6,
  "condition": "good | damaged",
  "created_at": "timestamp",
  "updated_at": "timestamp",
  "sync_status": "pending | synced"
}
```

### 4. Kegiatan_Alat
```json
{
  "id": "string",
  "kegiatan_id": "string",
  "alat_id": "string",
  "qty": 3,
  "created_at": "timestamp"
}
```

### 5. Image
```json
{
  "id": "string",
  "entity_type": "alat | kegiatan",
  "entity_id": "string",
  "image_url": "string",
  "created_at": "timestamp"
}
```

### 6. Sync Queue (Local Only)
```json
{
  "id": "string",
  "endpoint": "/kegiatan",
  "method": "POST",
  "body": {},
  "status": "pending",
  "created_at": "timestamp"
}
```

---

## 3. API Documentation (v1)

**Base URL:** `https://domain.com/api/v1`

**Header:**
```http
Authorization: Bearer <your_token>
Accept: application/json
```

---

### 🔑 Auth Endpoints

#### POST `/auth/register`
**Body:**
```json
{
  "name": "John Doe",
  "email": "john@example.com",
  "password": "password123",
  "role": "member"
}
```
**Response (201):**
```json
{
  "success": true,
  "message": "User successfully registered",
  "data": {
    "user": { "..." : "..." },
    "token": "xxx.yyy.zzz"
  }
}
```

#### POST `/auth/login`
**Body:**
```json
{
  "email": "john@example.com",
  "password": "password123"
}
```
**Response (200):**
```json
{
  "success": true,
  "message": "Login successful",
  "data": {
    "token": "xxx.yyy.zzz",
    "user": { "..." : "..." }
  }
}
```

#### GET `/auth/me` *(Auth required)*
**Response (200):**
```json
{
  "success": true,
  "message": "User profile retrieved",
  "data": { "..." : "..." }
}
```

---

### 📅 Kegiatan Endpoints

#### GET `/kegiatan` *(Auth required)*
List semua kegiatan.
```json
{
  "success": true,
  "message": "List of activities",
  "data": [ { "..." : "..." } ]
}
```

#### POST `/kegiatan` *(Auth required)*
**Body:**
```json
{
  "name": "Latihan Dasar Kepemimpinan",
  "description": "Pelatihan untuk pengurus baru",
  "date": "2024-05-20",
  "status": "draft"
}
```

#### GET `/kegiatan/{id}` *(Auth required)*
Detail satu kegiatan.

#### PUT `/kegiatan/{id}` *(Auth required)*
Update kegiatan. **Logic:** Jika status diubah menjadi `completed`, maka `available_qty` pada alat-alat yang terkait akan otomatis bertambah (dikembalikan).

#### DELETE `/kegiatan/{id}` *(Auth required)*
**Logic:** Jika kegiatan dihapus dan status belum `completed`, stok alat akan dikembalikan secara otomatis.

---

### 🛠️ Alat Endpoints

#### GET `/alat` *(Auth required)*
List semua alat.

#### GET `/alat/{id}` *(Auth required)*
Detail satu alat.

#### POST `/alat` *(Auth required)*
**Body:**
```json
{
  "name": "Tenda Dome",
  "category": "Outdoor",
  "total_qty": 10,
  "condition": "good"
}
```

#### PUT `/alat/{id}` *(Auth required)*
Update alat.

#### DELETE `/alat/{id}` *(Auth required)*
Hapus alat.

---

### 🔗 Kegiatan Alat (Relasi)

#### POST `/kegiatan-alat` *(Auth required)*
Tambah alat ke kegiatan.
**Body:**
```json
{
  "kegiatan_id": "uuid-kegiatan",
  "alat_id": "uuid-alat",
  "qty": 2
}
```
**Logic:** Mengurangi `available_qty` pada tabel `alat`. Tidak bisa melebihi stok yang tersedia.

#### GET `/kegiatan/{id}/alat`
Get daftar alat yang dipakai pada sebuah kegiatan.

---

### 📸 Image Upload

#### POST `/upload-image` *(Auth required)*
**Body (Multipart Form):**
- `image`: File (jpeg, png, max 2MB)
- `entity_type`: `"alat"` | `"kegiatan"`
- `entity_id`: UUID alat atau kegiatan

**Response:**
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

### ⚠️ Error Responses

Semua error mengembalikan format:
```json
{
  "success": false,
  "message": "Pesan error",
  "data": { "..." : "validation details" }
}
```

| Kode | Keterangan |
|------|------------|
| `401` | Unauthorized / Token Expired |
| `422` | Validation Error |
| `404` | Not Found |
| `400` | Bad Request (contoh: Stok habis) |

---

## 4. Daftar Halaman & Logic

### 1. Register Page
- **API:** `POST /auth/register`
- **Logic:** Validasi input → Kirim ke API → Redirect ke login / auto login

### 2. Login Page
- **API:** `POST /auth/login`
- **Logic:** Simpan token → Simpan user ke local DB → Redirect ke Home

### 3. Home (Kegiatan Aktif)
- **Data:** Local DB: kegiatan
- **Filter:** `status != "completed"`
- **Sync:** `GET /kegiatan`

### 4. Tambah Kegiatan
- Simpan ke local DB dengan `sync_status: "pending"`
- Masuk queue: `{ "endpoint": "/kegiatan", "method": "POST" }`

### 5. Detail Kegiatan
- **API:** `GET /kegiatan/{id}`, `GET /kegiatan/{id}/alat`
- **Fitur:** Lihat alat yang dipakai, update status kegiatan

### 6. Tambah Alat ke Kegiatan
- **API:** `POST /kegiatan-alat`
- **Logic:** Pilih alat → Input qty → Simpan local + queue

### 7. Inventaris (List Alat)
- **Data:** Local DB: alat
- **Sync:** `GET /alat`

### 8. Detail Alat
- Lihat `total_qty`, `available_qty`, riwayat pemakaian

### 9. Tambah Alat
- Simpan local → Masuk queue: `{ "endpoint": "/alat", "method": "POST" }`

### 10. Edit Alat
- **API:** `PUT /alat/{id}`

### 11. History Page
- **Data:** `status = "completed"`

### 12. Upload Gambar
- **Offline:** simpan `local_path`
- **Online:** `POST /upload-image`

---

## 5. Arsitektur Aplikasi

```
UI
↓
Local Database
↓
Sync Queue
↓
Sync Engine
↓
API Server
```

---

## 6. Pembagian Tugas Tim

### 👤 Adam — AUTH + APP SHELL

**Halaman:** Splash Screen, Register, Login, Main Navigation, Home Layout

**Tanggung Jawab:**
- Auth: login, register, token, session
- Navigation: bottom nav, routes, auth guard
- Shared UI: scaffold, app bar, reusable component
- Shared system: DataStore, token interceptor

---

### 👤 Reja — KEGIATAN FLOW

**Halaman:** Home (list kegiatan), Tambah Kegiatan, Detail Kegiatan, History

**Tanggung Jawab:**
- Logic kegiatan: CRUD kegiatan, sync kegiatan, filter status
- Relation: kegiatan ↔ alat
- RecyclerView/List Compose: kegiatan card, detail item
- SyncWorker, SyncManager

---

### 👤 Najib — INVENTARIS FLOW

**Halaman:** Inventaris, Detail Alat, Tambah Alat, Edit Alat, Upload Gambar

**Tanggung Jawab:**
- Logic alat: stok, kondisi, `available_qty`
- Upload media: image picker, multipart upload
- Inventory logic: validasi stok, update qty

---

## 7. Struktur Proyek

```
app/
│
├── core/                          ← Bersama
│   ├── database/                  ← Bersama
│   │   ├── AppDatabase.kt
│   │   ├── dao/
│   │   └── entity/
│   ├── network/                   ← Adam
│   │   ├── RetrofitClient.kt
│   │   ├── ApiInterceptor.kt
│   │   ├── ApiResponse.kt
│   │   └── NetworkMonitor.kt
│   ├── sync/                      ← Bersama + Reja dominan
│   │   ├── SyncWorker.kt          ← Reja
│   │   ├── SyncManager.kt         ← Reja
│   │   ├── SyncQueueEntity.kt     ← Bersama
│   │   └── SyncQueueDao.kt        ← Bersama
│   ├── datastore/                 ← Adam
│   │   └── UserPreferences.kt
│   ├── navigation/                ← Adam
│   │   ├── NavGraph.kt
│   │   ├── Routes.kt
│   │   └── BottomBar.kt
│   ├── ui/                        ← Bersama
│   │   ├── components/
│   │   ├── theme/
│   │   └── state/
│   └── utils/                     ← Bersama
│       ├── Constants.kt
│       ├── Resource.kt
│       ├── Validators.kt
│       └── Extensions.kt
│
├── features/
│   ├── auth/                      ← Adam
│   │   ├── api/
│   │   ├── data/
│   │   ├── domain/
│   │   ├── ui/
│   │   │   ├── SplashScreen.kt
│   │   │   ├── LoginScreen.kt
│   │   │   └── RegisterScreen.kt
│   │   └── mapper/
│   │
│   ├── kegiatan/                  ← Reja
│   │   ├── api/
│   │   ├── data/
│   │   ├── domain/
│   │   ├── ui/
│   │   │   ├── HomeScreen.kt
│   │   │   ├── TambahKegiatanScreen.kt
│   │   │   ├── DetailKegiatanScreen.kt
│   │   │   └── HistoryScreen.kt
│   │   └── mapper/
│   │
│   ├── alat/                      ← Najib
│   │   ├── api/
│   │   ├── data/
│   │   ├── domain/
│   │   ├── ui/
│   │   │   ├── InventarisScreen.kt
│   │   │   ├── DetailAlatScreen.kt
│   │   │   ├── TambahAlatScreen.kt
│   │   │   └── EditAlatScreen.kt
│   │   └── mapper/
│   │
│   ├── media/                     ← Najib
│   │   ├── api/
│   │   ├── data/
│   │   ├── ui/
│   │   └── utils/
│   │
│   └── shared/                    ← Bersama
│       ├── ImagePicker.kt         ← Najib
│       ├── PermissionHandler.kt   ← Najib
│       ├── LoadingDialog.kt       ← Adam
│       ├── EmptyState.kt          ← Reja
│       └── CustomButton.kt        ← Bersama
│
├── MainActivity.kt                ← Adam
└── PerkappApp.kt                  ← Bersama
```

---

## 8. Prinsip Pengembangan

- Semua write → **local DB dulu**
- API hanya lewat **sync engine**
- **UI tidak langsung ke API**
- Semua pakai **model data yang sama**
- Semua endpoint harus **konsisten**

---

## 9. Kesimpulan

Dengan struktur ini, sistem sudah mencakup:
- Auth
- Inventory
- Kegiatan
- Relasi alat
- Media / upload gambar

Fitur offline-first berjalan, API jelas dan konsisten, pembagian tim adil, serta siap implementasi dan presentasi UAS.