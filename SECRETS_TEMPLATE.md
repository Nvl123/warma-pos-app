# API Keys Configuration

Proyek ini membutuhkan beberapa API key untuk berfungsi dengan baik.

## OCR API Key (OCR.space)

Digunakan untuk fitur OCR online (scan struk belanja).

### Cara mendapatkan:

1. Daftar gratis di https://ocr.space/ocrapi
2. Dapatkan API key dari email konfirmasi
3. Setup API key di aplikasi melalui **Settings > OCR API Key**

Atau langsung edit di source code:

```
app/src/main/java/com/dicoding/warmapos/data/repository/SettingsRepository.kt
```

Ganti `YOUR_OCR_API_KEY` dengan API key Anda.

## Mode Offline (ML Kit)

Aplikasi juga mendukung mode OCR offline menggunakan ML Kit. Untuk menggunakannya:

1. Buka **Settings** di aplikasi
2. Pilih **OCR Mode: Offline**

Mode offline tidak memerlukan API key.
