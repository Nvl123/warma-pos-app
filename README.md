# WARMAPOS - Point of Sale Android App

<p align="center">
  <img src="https://img.shields.io/badge/Android-3DDC84?style=for-the-badge&logo=android&logoColor=white" alt="Android"/>
  <img src="https://img.shields.io/badge/Kotlin-7F52FF?style=for-the-badge&logo=kotlin&logoColor=white" alt="Kotlin"/>
  <img src="https://img.shields.io/badge/Jetpack%20Compose-4285F4?style=for-the-badge&logo=jetpackcompose&logoColor=white" alt="Jetpack Compose"/>
</p>

Aplikasi Point of Sale (POS) / Kasir modern untuk Android dengan fitur OCR untuk scan struk belanja dan cetak struk via Bluetooth printer.

## ✨ Features

- 📦 **Product Management** - Kelola produk dengan kategori, harga, dan stok
- 🛒 **Transaction Processing** - Proses transaksi penjualan dengan cepat
- 📸 **OCR Receipt Scan** - Scan struk belanja menggunakan kamera (Online & Offline mode)
- 🖨️ **Bluetooth Printing** - Cetak struk ke thermal printer via Bluetooth
- 🎨 **Customizable Receipt** - Desain struk sesuai kebutuhan toko
- 📊 **Sales History** - Lihat riwayat transaksi
- 🔄 **Backup & Restore** - Backup data ke file CSV
- 🌙 **Multiple Themes** - Pilihan tema warna aplikasi

## 🛠️ Tech Stack

- **Language**: Kotlin
- **UI Framework**: Jetpack Compose
- **Architecture**: MVVM
- **OCR**:
  - Online: [OCR.space API](https://ocr.space)
  - Offline: Google ML Kit Text Recognition
- **Database**: Room (SQLite)
- **Bluetooth**: Android Bluetooth API

## 📱 Screenshots

_Coming soon_

## 🚀 Getting Started

### Prerequisites

- Android Studio Hedgehog or newer
- Android SDK 24+
- Kotlin 1.9+

### Installation

1. Clone repository ini:

```bash
git clone https://github.com/Nvl123/warma-pos-app.git
```

2. Buka project di Android Studio

3. Sync Gradle dan jalankan aplikasi

### API Configuration

Untuk menggunakan fitur OCR online, Anda perlu API key dari OCR.space:

1. Daftar gratis di https://ocr.space/ocrapi
2. Dapatkan API key dari email konfirmasi
3. Masukkan API key di **Settings > OCR API Key** pada aplikasi

> 💡 **Tip**: Anda juga bisa menggunakan mode OCR Offline (ML Kit) yang tidak memerlukan API key.

## 📁 Project Structure

```
app/src/main/java/com/dicoding/warmapos/
├── bluetooth/          # Bluetooth printer handler
├── data/
│   ├── api/            # API services (OCR)
│   ├── database/       # Room database & DAO
│   ├── model/          # Data models
│   └── repository/     # Repositories
├── ui/
│   ├── components/     # Reusable UI components
│   ├── navigation/     # Navigation setup
│   ├── screens/        # App screens
│   └── theme/          # App theming
├── utils/              # Utility classes (OCR handlers)
└── MainActivity.kt
```

## 🤝 Contributing

Kontribusi sangat diterima! Silakan buat Pull Request atau buka Issue untuk saran dan bug report.

## 📄 License

This project is open source and available under the [MIT License](LICENSE).

## 👨‍💻 Author

- **Nvl123** - [GitHub](https://github.com/Nvl123)

---

⭐ Jika project ini membantu, jangan lupa beri star!
