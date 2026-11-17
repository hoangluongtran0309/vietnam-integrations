# Changelog

Định dạng theo [Keep a Changelog](https://keepachangelog.com/vi/1.1.0/), phiên bản theo [SemVer](https://semver.org).

## [Unreleased]

### Added
- VietQR: `PaymentReferenceFormat` sinh, kiểm tra và trích mã tham chiếu có ký tự kiểm tra Luhn mod N từ nội dung
  chuyển khoản do ngân hàng ghi nhận.
- VietQR: `PaymentReconciler` phân loại khoản tiền vào thành khớp, thiếu, thừa, sai tài khoản hoặc không thể đối
  soát qua `PayableLookup` do ứng dụng cung cấp; core không lưu trạng thái hay tự chống xử lý trùng.
- VietQR starter: property `vietnam.vietqr.reference.prefix` tự cấu hình `PaymentReferenceFormat` và, khi ứng dụng
  cung cấp `PayableLookup`, `PaymentReconciler`; `VietQrGenerator.forReference` kiểm mã trước khi sinh QR.
- Demo VietQR: `/checkout` và `POST /demo/payments/simulate` minh họa đối soát offline bằng dữ liệu giả lập; tài liệu
  hướng dẫn mã tham chiếu, xử lý thiếu/thừa tiền và lưu idempotency key an toàn.
- `ZaloOaRegistry`, `ZaloOa` và `ZaloAccountConfig` để quản lý nhiều Official Account theo alias hoặc `oaId`, dùng
  chung OAuth client cho các account thuộc cùng một Zalo App.
- Zalo starter hỗ trợ map `vietnam.zalo-oa.accounts`, `default-account`, registry và webhook router nhiều OA, đồng
  thời giữ cấu hình một OA cấp gốc cùng các bean client tiện ích tương thích.
- Zalo OA OAuth v4: `HttpZaloOAuthClient`, PKCE S256 helper và error model không giữ credential/raw response.
- Zalo OA messaging v3 qua `ZaloOaClient.sendConsultationText`; ZBS Template Message qua SĐT bằng
  `ZaloZbsClient.sendTemplateByPhone`.
- PostgreSQL `JdbcZaloTokenStore`, versioned schema và atomic refresh bằng `SELECT ... FOR UPDATE`.
- `ZaloReauthorizationRequiredException` để phân biệt refresh token không còn hợp lệ với lỗi OAuth tạm thời.
- Typed webhook events cho text/follow/unfollow/delivered/seen, unknown-event fallback và servlet controller opt-in.
- Demo OAuth callback, gửi OA/ZBS, webhook monitor; tài liệu tích hợp tại `docs/zalo/`.

### Changed
- Breaking: mọi `ZaloWebhookEvent` thêm `oaId`; `ZaloWebhookRouter` định tuyến payload tới đúng Official Account
  trước khi kiểm chữ ký bằng webhook secret tương ứng.
- Breaking: `ZaloTokenStore` thêm `updateAtomically`; `ZaloTokenManager` dùng store-level atomic refresh thay cho
  lock chỉ trong JVM.
- Breaking: `ZaloOaClient.sendText/sendZns` được thay bằng request/result records và tách ZBS thành
  `ZaloZbsClient`. Tên ZNS cũ không còn dùng vì dịch vụ đã hợp nhất vào ZBS từ 01/01/2026.
- Zalo starter tự cấu hình OAuth/OA/ZBS clients, PostgreSQL store và webhook với connect/request timeout.
- `JdbcZaloTokenStore` giới hạn thời gian chờ row lock; Zalo starter thêm
  `vietnam.zalo-oa.jdbc.lock-timeout` mặc định 15 giây và yêu cầu lớn hơn request timeout.
- Zalo OA starter giờ fail-fast khi bật Zalo mà thiếu hoặc để trống `vietnam.zalo-oa.app-id`,
  `vietnam.zalo-oa.secret-key`, `vietnam.zalo-oa.oa-id`, hoặc khi đặt
  `vietnam.zalo-oa.webhook.enabled=true` mà thiếu `vietnam.zalo-oa.oa-secret-key`; trước đây context vẫn khởi động
  nhưng âm thầm thiếu bean.
- `ZaloWebhookVerifier` thêm overload `verify(byte[], String)` để kiểm chữ ký từ request bytes sau khi
  decode UTF-8 nghiêm ngặt; webhook controller chấp nhận mọi `Content-Type` và trả response lỗi không lộ payload
  hay chi tiết exception.
- `ZbsPhoneTemplateRequest` nhận số Việt Nam dạng quốc gia, nội địa và các ký tự phân tách thông dụng, sau đó luôn
  chuẩn hóa thành `84` + 9 chữ số; `templateData` chỉ nhận scalar an toàn và ghi `BigDecimal` không dùng số mũ.

### Security
- Zalo token, PKCE verifier, application/OA secret key, recipient phone number và ZBS template values không còn
  bị lộ qua `toString()`.

## [0.1.0] - 2026-11-13

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
- VietQR: ràng buộc `accountNumber` đổi từ 1–19 thành 5–19 ký tự chữ-số và `amount` giới hạn tối đa 13 chữ số,
  khớp tài liệu NAPAS Core v1.2. Đây là thay đổi validation trước bản phát hành 0.1.0.
- VietQR: `purpose` dài hơn 95 ký tự (sau chuẩn hóa) giờ báo `VietQrException` rõ ràng
  (`purpose is N characters after normalization; the maximum is 95`) thay cho lỗi TLV
  `Value of tag 62 exceeds 99 characters`. Nội dung không bao giờ bị tự cắt. Xem ADR-0005.
- Build với Spring Boot 4.1.1 (trước đó 4.0.1).
- Zalo OA starter: `vietnam.zalo-oa.enabled=false` giờ thực sự tắt auto-configuration.
- Zalo OA starter: `vietnam.zalo-oa.token-store=jdbc` báo lỗi rõ ràng khi khởi động (chưa implement)
  thay vì âm thầm dùng in-memory; bean `ZaloTokenStore` do người dùng tự định nghĩa vẫn được ưu tiên.

### Removed
- VietQR starter: bỏ `vietnam.vietqr.account-name` và `vietnam.vietqr.image-size` (chưa được dùng ở đâu).
