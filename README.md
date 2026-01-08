# WARMAPOS - Point of Sale Android App

<p align="center">
  <img src="https://img.shields.io/badge/Android-3DDC84?style=for-the-badge&logo=android&logoColor=white" alt="Android"/>
  <img src="https://img.shields.io/badge/Kotlin-7F52FF?style=for-the-badge&logo=kotlin&logoColor=white" alt="Kotlin"/>
  <img src="https://img.shields.io/badge/Jetpack%20Compose-4285F4?style=for-the-badge&logo=jetpackcompose&logoColor=white" alt="Jetpack Compose"/>
</p>

Aplikasi Point of Sale (POS) / Kasir modern untuk Android dengan fitur **OCR untuk scan struk belanja** dan **Fuzzy Search** untuk pencocokan produk yang cerdas.

---

## ✨ Fitur Lengkap

### 📦 Manajemen Produk

- **CRUD Produk** - Tambah, edit, hapus produk dengan mudah
- **Kategori Produk** - Organisasi produk berdasarkan kategori
- **SKU & Barcode** - Dukungan kode SKU untuk identifikasi produk
- **Import/Export CSV** - Import data produk dari file CSV atau export untuk backup
- **Auto-detect CSV Format** - Mendukung berbagai format CSV (5 kolom atau 12 kolom)

### 🔍 Fuzzy Search (Pencarian Cerdas)

Fitur pencarian produk menggunakan algoritma **Token Set Ratio** dan **Levenshtein Distance** yang dapat:

- Mencocokkan nama produk meskipun ada typo/kesalahan ketik
- Mencari dengan partial match (sebagian kata)
- Multi-word matching untuk nama produk panjang
- Threshold matching 40% untuk hasil yang relevan
- Menampilkan skor kecocokan untuk setiap hasil

**Cara Kerja:**

```
Query: "indomi gorng" → Match: "Indomie Goreng" (92% match)
Query: "aqua" → Match: "Aqua 600ml", "Aqua 1.5L" (sorted by relevance)
```

### 📸 OCR Receipt Scan

Scan struk belanja menggunakan kamera untuk otomatis mengenali produk:

| Mode        | Deskripsi                                      | Kebutuhan                  |
| ----------- | ---------------------------------------------- | -------------------------- |
| **Online**  | Menggunakan [OCR.space API](https://ocr.space) | Koneksi internet + API Key |
| **Offline** | Menggunakan Google ML Kit                      | Tidak perlu internet       |

**Fitur OCR:**

- Crop gambar sebelum proses OCR
- Auto-parsing nama produk dan harga dari hasil OCR
- Fuzzy matching otomatis ke database produk
- Menampilkan skor kecocokan untuk verifikasi

### 🛒 Keranjang & Transaksi

- **Add to Cart** - Tambah produk ke keranjang dengan quantity
- **Edit Quantity** - Ubah jumlah item di keranjang
- **Remove Item** - Hapus item dari keranjang
- **Auto Calculate** - Perhitungan subtotal dan total otomatis
- **Clear Cart** - Kosongkan keranjang sekaligus

### 🧾 Struk & Riwayat

- **Simpan Transaksi** - Setiap transaksi disimpan sebagai riwayat
- **Lihat Riwayat** - Browse semua transaksi sebelumnya
- **Load ke Cart** - Load transaksi lama ke keranjang untuk edit/reprint
- **Hapus Riwayat** - Hapus transaksi yang tidak diperlukan

### 🖨️ Cetak Struk Bluetooth

Cetak struk ke thermal printer via Bluetooth:

- **Auto-discover** - Deteksi printer Bluetooth yang paired
- **Connect & Save** - Simpan printer favorit untuk koneksi cepat
- **Test Print** - Test koneksi printer sebelum cetak
- **ESC/POS Commands** - Kompatibel dengan thermal printer standar

### 🎨 Kustomisasi Struk

Desain struk sesuai kebutuhan toko:

- **Nama Toko** - Ubah nama toko di header struk
- **Alamat** - Tambahkan alamat toko
- **Footer** - Pesan terima kasih custom
- **Format Harga** - Format Rupiah dengan pemisah ribuan

### 📝 Sinonim Produk

Mapping nama alternatif untuk produk:

```
"aqua" → "Aqua 600ml"
"mie" → "Indomie Goreng"
```

### 💾 Backup & Restore

- **Create Backup** - Backup semua data ke file ZIP
- **Restore Backup** - Pulihkan data dari file backup
- **Export CSV** - Export produk ke format CSV

### 📑 Kelompok Struk (Group Receipts)

Fitur baru untuk mengelompokkan beberapa struk menjadi satu laporan:

- **Pilih & Kelompokkan** - Pilih beberapa struk dan gabungkan menjadi satu kelompok
- **Edit Kelompok** - Tambah atau hapus struk dari kelompok yang sudah ada
- **Print Kelompok** - Cetak ringkasan kelompok struk (total gabungan)
- **Grand Total** - Lihat total dari semua struk dalam kelompok

**Cara Penggunaan:**

1. Di tab **Riwayat**, tap tombol checklist (FAB) untuk masuk mode pilih
2. Pilih struk yang ingin dikelompokkan
3. Tekan **"Buat Kelompok"**
4. Beri nama kelompok dan simpan

### ♻️ Pakai Ulang Struk (Reuse Receipt)

Load struk lama kembali ke keranjang untuk:

- **Edit transaksi** - Ubah item dan buat transaksi baru
- **Repeat order** - Buat pesanan yang sama dengan mudah
- **Harga snapshot** - Menggunakan harga saat struk dibuat (bukan harga saat ini)

### 🌙 Tema Aplikasi

Pilihan tema warna:

- Emerald (default)
- Blue
- Purple
- Orange
- Dan lainnya

---

## 🛠️ Tech Stack

| Komponen         | Teknologi                            |
| ---------------- | ------------------------------------ |
| **Language**     | Kotlin                               |
| **UI Framework** | Jetpack Compose                      |
| **Architecture** | MVVM + Repository Pattern            |
| **Async**        | Kotlin Coroutines + StateFlow        |
| **OCR Online**   | [OCR.space API](https://ocr.space)   |
| **OCR Offline**  | Google ML Kit Text Recognition       |
| **Printing**     | Android Bluetooth API + ESC/POS      |
| **Data Storage** | SharedPreferences + Internal Storage |

---

## 📁 Struktur Project

```
app/src/main/java/com/dicoding/warmapos/
├── bluetooth/                  # Bluetooth printing
│   ├── BluetoothPrinterManager.kt  # Manage Bluetooth connections
│   ├── EscPosBuilder.kt            # ESC/POS command builder
│   └── ReceiptPrinter.kt           # Receipt formatting & printing
│
├── data/
│   ├── api/
│   │   ├── OcrApiService.kt        # OCR.space API interface
│   │   └── RetrofitClient.kt       # HTTP client setup
│   │
│   ├── model/
│   │   ├── Product.kt              # Product data model
│   │   ├── CartItem.kt             # Cart item model
│   │   ├── Receipt.kt              # Receipt & ReceiptItem
│   │   ├── ReceiptDesign.kt        # Receipt customization
│   │   ├── GroupedReceipt.kt       # Grouped receipts model
│   │   └── OcrModels.kt            # OCR result models
│   │
│   └── repository/
│       ├── ProductRepository.kt    # Product data management
│       ├── ReceiptRepository.kt    # Receipt persistence
│       ├── GroupedReceiptRepository.kt  # Grouped receipts persistence
│       ├── SettingsRepository.kt   # App settings
│       └── BackupRepository.kt     # Backup/restore logic
│
├── ui/
│   ├── components/
│   │   └── ProductComponents.kt    # Reusable UI components
│   │
│   ├── navigation/
│   │   └── Screen.kt               # Navigation routes
│   │
│   ├── screens/
│   │   ├── CameraScreen.kt         # Camera for OCR
│   │   ├── CropScreen.kt           # Image cropping
│   │   ├── OcrScreen.kt            # OCR results & matching
│   │   ├── CartScreen.kt           # Shopping cart
│   │   ├── SearchScreen.kt         # Product search
│   │   ├── HistoryScreen.kt        # Transaction history
│   │   └── SettingsScreen.kt       # App settings
│   │
│   ├── theme/
│   │   └── Theme.kt                # App theming
│   │
│   └── MainViewModel.kt            # Main ViewModel
│
├── utils/
│   ├── OcrHandler.kt               # Online OCR processing
│   ├── MlKitOcrHandler.kt          # Offline OCR (ML Kit)
│   └── ProductMatcher.kt           # Fuzzy search algorithm
│
└── MainActivity.kt
```

---

## 🚀 Getting Started

### Prerequisites

- Android Studio Hedgehog atau lebih baru
- Android SDK 24+ (Android 7.0 Nougat)
- Kotlin 1.9+

### Instalasi

1. **Clone repository:**

```bash
git clone https://github.com/Nvl123/warma-pos-app.git
```

2. **Buka di Android Studio**

3. **Sync Gradle** dan tunggu dependencies terdownload

4. **Run** aplikasi ke device/emulator

### Konfigurasi OCR API (Opsional)

Untuk menggunakan OCR online:

1. Daftar gratis di https://ocr.space/ocrapi
2. Dapatkan API key dari email konfirmasi
3. Buka **Settings > OCR API Key** di aplikasi
4. Masukkan API key

> 💡 **Tip**: Mode OCR Offline (ML Kit) tidak memerlukan API key dan bekerja tanpa internet.

---

## 📱 Cara Penggunaan

### Scan Struk dengan OCR

1. Buka menu **Scan** atau klik tombol kamera
2. Ambil foto struk belanja
3. Crop area yang berisi daftar item
4. Tunggu proses OCR
5. Review hasil matching - produk akan dicocokkan otomatis dengan database
6. Tambahkan item yang terverifikasi ke keranjang

### Pencarian Produk

1. Buka menu **Search**
2. Ketik nama produk (tidak perlu tepat)
3. Sistem akan menampilkan produk dengan skor kecocokan
4. Pilih produk dan tambahkan ke keranjang

### Cetak Struk

1. Buka **Settings > Printer**
2. Pilih printer Bluetooth yang sudah di-pair
3. Klik **Connect**
4. Setelah transaksi selesai, klik **Print**

---

## 🔧 Algoritma Fuzzy Search

Aplikasi menggunakan kombinasi dua algoritma:

### 1. Token Set Ratio

Memecah string menjadi token dan membandingkan:

- Intersection (kata yang sama)
- Difference (kata yang berbeda)
- Membandingkan berbagai kombinasi

### 2. Levenshtein Distance

Menghitung jarak edit minimum antara dua string:

- Insert, delete, atau replace karakter
- Dinormalisasi menjadi ratio 0-1

**Threshold**: Hasil dengan skor ≥ 40% akan ditampilkan, diurutkan berdasarkan relevansi.

---

## 🤝 Contributing

Kontribusi sangat diterima! Silakan:

1. Fork repository ini
2. Buat branch fitur (`git checkout -b feature/AmazingFeature`)
3. Commit perubahan (`git commit -m 'Add some AmazingFeature'`)
4. Push ke branch (`git push origin feature/AmazingFeature`)
5. Buat Pull Request

---

## 📄 License

This project is open source and available under the [MIT License](LICENSE).

---

## 👨‍💻 Author

**Nvl123** - [GitHub](https://github.com/Nvl123)

---

⭐ **Jika project ini membantu, jangan lupa beri star!**
