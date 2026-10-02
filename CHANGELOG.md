# Changelog

Định dạng theo [Keep a Changelog](https://keepachangelog.com/vi/1.1.0/), phiên bản theo [SemVer](https://semver.org).

## [Unreleased]

### Added
- VietQR: encoder, decoder (kiểm CRC), chuẩn hóa nội dung tiếng Việt, render PNG, auto-configuration.
- VietQR: `VietQrBank` mở rộng từ 10 lên 65 thành viên NAPAS (nguồn `api.vietqr.io`, chụp 2026-10-02),
  thêm `displayName()` và `fromShortCode(String)`; 10 hằng cũ giữ nguyên. Xem `docs/vietqr/banks.md`.
- VietQR: `VietQrPayload.bank()` tra ngân hàng theo BIN của payload đã decode.
- VietQR: hằng `VietQrEncoder.MAX_PURPOSE_LENGTH` (95) — độ dài tối đa của `purpose` sau chuẩn hóa.
- VietQR: `QrImageOptions` + `QrErrorCorrection` và `QrImageRenderer.renderPng(String, QrImageOptions)`:
  chọn kích thước, lề, mức sửa lỗi, màu, logo ở giữa. `renderPng(String, int)` giữ nguyên kết quả cũ.
  Xem `docs/vietqr/rendering.md`.
- VietQR: SPI xác nhận thanh toán `PaymentConfirmation` + `PaymentConfirmationListener`
  (package `...vietqr.confirmation`; chỉ interface, adapter dự kiến 0.4). Xem ADR-0006.
- VietQR: `RealPayloadVectorsTest` đọc test vector thật trong `vietnam-vietqr-core/src/test/resources/vectors/`.
- Docs: trang VietQR đầy đủ `docs/vietqr/` (cài đặt, cấu hình, cấu trúc payload, nội dung chuyển khoản,
  ngân hàng, render ảnh, xác nhận thanh toán, test vector, kiểm tra thủ công, bảng lỗi, FAQ).
- Demo app: trang `/verify` (bộ QR để quét thử bằng app ngân hàng), `POST /decode` và `POST /decode-image`
  (lấy payload từ ảnh chụp QR để làm test vector).
- Zalo OA: `ZaloTokenStore`, `ZaloTokenManager` (single-flight refresh), webhook verifier (chưa xác nhận công thức).
- E-invoice: domain model, `EInvoiceProvider` SPI, `EInvoiceService`.

### Changed
- VietQR: `purpose` dài hơn 95 ký tự (sau chuẩn hóa) giờ báo `VietQrException` rõ ràng
  (`purpose is N characters after normalization; the maximum is 95`) thay cho lỗi TLV
  `Value of tag 62 exceeds 99 characters`. Nội dung không bao giờ bị tự cắt. Xem ADR-0005.
- Build với Spring Boot 4.1.1 (trước đó 4.0.1).
- Zalo OA starter: `vietnam.zalo-oa.enabled=false` giờ thực sự tắt auto-configuration.
- Zalo OA starter: `vietnam.zalo-oa.token-store=jdbc` báo lỗi rõ ràng khi khởi động (chưa implement)
  thay vì âm thầm dùng in-memory; bean `ZaloTokenStore` do người dùng tự định nghĩa vẫn được ưu tiên.

### Removed
- VietQR starter: bỏ `vietnam.vietqr.account-name` và `vietnam.vietqr.image-size` (chưa được dùng ở đâu).
