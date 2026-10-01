# Changelog

Định dạng theo [Keep a Changelog](https://keepachangelog.com/vi/1.1.0/), phiên bản theo [SemVer](https://semver.org).

## [Unreleased]

### Added
- VietQR: encoder, decoder (kiểm CRC), chuẩn hóa nội dung tiếng Việt, render PNG, auto-configuration.
- Zalo OA: `ZaloTokenStore`, `ZaloTokenManager` (single-flight refresh), webhook verifier (chưa xác nhận công thức).
- E-invoice: domain model, `EInvoiceProvider` SPI, `EInvoiceService`.

### Changed
- Build với Spring Boot 4.1.1 (trước đó 4.0.1).
- Zalo OA starter: `vietnam.zalo-oa.enabled=false` giờ thực sự tắt auto-configuration.
- Zalo OA starter: `vietnam.zalo-oa.token-store=jdbc` báo lỗi rõ ràng khi khởi động (chưa implement)
  thay vì âm thầm dùng in-memory; bean `ZaloTokenStore` do người dùng tự định nghĩa vẫn được ưu tiên.

### Removed
- VietQR starter: bỏ `vietnam.vietqr.account-name` và `vietnam.vietqr.image-size` (chưa được dùng ở đâu).
