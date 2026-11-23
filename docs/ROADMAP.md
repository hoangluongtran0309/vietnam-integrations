# Kế hoạch dự án

Giả định: làm part-time song song với các dự án khác (~8–12 giờ/tuần). Mốc thời gian tính từ tuần bắt đầu.

## Nguyên tắc ưu tiên

1. **Ship sớm module dễ nhất** (VietQR: offline, không cần credential) để có người dùng và phản hồi thật.
2. **Chỉ gọi là ổn định khi đã chạy với hệ thống thật.** Tích hợp chưa nghiệm thu chỉ được phát hành với nhãn
   Preview theo [ADR-0010](adr/0010-preview-releases.md).
3. **API công khai là hợp đồng.** Trước 1.0 được phép đổi, nhưng mọi thay đổi phá vỡ phải ghi vào CHANGELOG.

---

## Phase 0 — Nền móng (tuần 1)

- [x] Tạo repo GitHub `vietnam-integrations`, push scaffold này
- [x] Chạy `./mvnw verify` lần đầu, sửa mọi lỗi compile/test (scaffold chưa được build bằng Maven thật)
- [x] Bump `spring-boot.version` (4.1.1) và `central-publishing.version` (0.11.0) lên bản mới nhất
- [x] Đăng ký tài khoản [Central Portal](https://central.sonatype.com), verify namespace `io.github.hoangluongtran0309`
- [x] Tạo GPG key, publish public key lên keyserver, thêm secrets vào GitHub (xem [RELEASING.md](RELEASING.md))
- [x] Bật branch protection cho `main` — ruleset `protect-main` (xem [CONTRIBUTING.md](../CONTRIBUTING.md#quy-trình-pr))
- [x] Bật CodeRabbit
- [x] Chốt namespace Maven trước 0.1.0 (xem [RELEASING.md](RELEASING.md)) — `io.github.hoangluongtran0309`

**Done khi:** CI xanh trên Java 17 và 21; publish thử một bản `0.0.1-alpha` lên Central thành công. ✅ Đã publish `0.0.1-alpha` ngày 2026-10-01.

## Phase 1 — VietQR 0.1.0 (tuần 2–4)

- [x] Đối chiếu và bổ sung danh sách BIN — 65 mục từ `api.vietqr.io` (2026-10-02); 57 mục đã khớp nguồn chính
  thức NAPAS/NHNN, 8 mục còn lại dời sang `TODO(v0.1.x)`, xem [vietqr/banks.md](vietqr/banks.md).
- [x] Thu thập ≥ 5 payload thật (tạo từ app ngân hàng khác nhau), thêm làm test vector cho decoder
  - Đã có vector từ TPBank, VIB, VietinBank, MBBank và BIDV; xem
    [vietqr/test-vectors.md](vietqr/test-vectors.md).
- [x] Quét QR sinh ra bằng ≥ 3 app ngân hàng thật, xác nhận đúng người nhận/số tiền/nội dung
  - Đã được người kiểm tra xác nhận thủ công ngày 2026-11-13; không lưu chi tiết app theo yêu cầu.
    Công cụ kiểm tra: trang `/verify` trong demo app và
    [vietqr/manual-verification.md](vietqr/manual-verification.md).
- [x] Quyết định giới hạn độ dài `purpose`: giới hạn cứng 95 ký tự, báo lỗi thay vì cắt; khuyến nghị ≤ 25 theo
  EMVCo để tương thích bảo thủ ([ADR-0005](adr/0005-purpose-length-policy.md), trạng thái Accepted).
- [x] Render ảnh có logo/khung tùy chọn (optional, có thể để 0.1.x) — logo + màu qua `QrImageOptions`,
  xem [vietqr/rendering.md](vietqr/rendering.md). Khung/chữ quanh mã: chưa làm (font tiếng Việt trên server headless).
- [x] `PaymentConfirmationListener` SPI cho xác nhận thanh toán (SePay/Casso) — **chỉ thiết kế interface**, adapter để 0.4
  — [ADR-0006](adr/0006-payment-confirmation-spi.md), [vietqr/payment-confirmation.md](vietqr/payment-confirmation.md).
- [x] Viết docs: README, trang VietQR, ví dụ — [vietqr/README.md](vietqr/README.md) và các trang con
- [x] Release 0.1.0 — [GitHub Release](https://github.com/hoangluongtran0309/vietnam-integrations/releases/tag/v0.1.0),
  Maven Central ngày 2026-11-13
  - Trước khi tag `v0.1.0`:
    - [x] ≥ 5 test vector thật trong `vietnam-vietqr-core/src/test/resources/vectors/`, `./mvnw verify` xanh
    - [x] Đã xác nhận thủ công QR bằng ≥ 3 app ngày 2026-11-13; không lưu chi tiết app theo yêu cầu của người kiểm tra
    - [x] ADR-0005 chuyển sang Accepted (giới hạn `purpose`)
    - [x] Rà các `TODO(v0.1)` còn lại: 8 BIN chưa có nguồn chính thức và đối chiếu tag NAPAS đã được ghi rõ dời
          sang `0.1.x`; xem `VietQrBank` và `docs/vietqr/README.md`
    - [x] `CHANGELOG.md`: thêm `[0.1.0] - 2026-11-13` qua PR, rồi tag theo [RELEASING.md](RELEASING.md)
    - [x] Sau khi lên Central: project Spring Boot trống tải dependency từ repository Maven rỗng, build và tạo/decode
          payload thành công trong khoảng 61 giây (Java 21, 2026-11-13)

**Done khi:** người lạ thêm dependency từ Central và tạo QR quét được trong < 5 phút theo README. ✅ Hoàn tất ngày
2026-11-13.

## Phase 2 — Zalo OA (Preview) + VietQR đối soát 0.2.0

- [x] Viết quy trình tạo Zalo App/OA, cấp quyền OAuth, cấu hình ZBS và webhook — 2026-11-15,
  xem [tài liệu Zalo](zalo/README.md).
- [x] Đối chiếu endpoint, vòng đời token và chữ ký webhook với tài liệu công khai — 2026-11-13,
  xem [mức độ kiểm chứng](zalo/README.md#6-mức-độ-kiểm-chứng).
- [x] Implement `HttpZaloOAuthClient` và PKCE bằng `java.net.http.HttpClient` — 2026-11-15,
  xem [OAuth + PKCE](zalo/README.md#3-oauth--pkce).
- [x] Implement `JdbcZaloTokenStore`, schema PostgreSQL và khóa refresh đa instance — 2026-11-15,
  xem [ADR-0007](adr/0007-atomic-zalo-token-rotation.md).
- [x] Implement tin Tư vấn qua `ZaloOaClient` và ZBS qua SĐT bằng `ZaloZbsClient` — 2026-11-15,
  xem [hướng dẫn gửi tin](zalo/README.md#4-gửi-tin).
- [x] Implement webhook opt-in và typed events cho text/follow/unfollow/delivered/seen — 2026-11-15,
  xem [hướng dẫn webhook](zalo/README.md#5-webhook).
- [x] Hỗ trợ nhiều OA qua registry, starter và webhook router — 2026-11-16,
  xem [ADR-0008](adr/0008-multiple-zalo-official-accounts.md).
- [x] Thêm mã tham chiếu VietQR và đối soát không giữ trạng thái — 2026-11-17,
  xem [ADR-0009](adr/0009-payment-reference-and-reconciliation.md) và
  [hướng dẫn đối soát](vietqr/reconciliation.md).
- [x] Test end-to-end với Zalo giả bằng WireMock và PostgreSQL 16 thật — 2026-11-17,
  xem [mức độ kiểm chứng](zalo/README.md#6-mức-độ-kiểm-chứng).
- [x] Đối chiếu các hợp đồng Zalo với SDK khác — 2026-11-17,
  xem [bảng đối chiếu SDK](zalo/sdk-crosscheck.md).
- [x] Thêm công cụ capture HTTP/webhook có redact và test bằng Zalo giả — 2026-11-23,
  xem [checklist capture](zalo/capture-checklist.md).
- [ ] Release 0.2.0 (Preview cho Zalo, theo [ADR-0010](adr/0010-preview-releases.md)).

**Done khi:** 0.2.0 có trên Maven Central; một project trống dùng được VietQR đối soát và khởi tạo
được `ZaloOaRegistry` theo README.

## Phase 2.x — Nghiệm thu Zalo với OA thật (0.2.x)

**Điều kiện bắt đầu:** có Zalo App/OA, ZBS Account và template test đã duyệt.

- [ ] Chạy đủ [checklist capture Zalo thật](zalo/capture-checklist.md).
- [ ] Thay fixture chính bằng capture từ OA/ZBS thật đã redact và ghi ngày capture.
- [ ] Đóng sáu `TODO(v0.2)` sau khi có bằng chứng tương ứng:
  - định dạng số điện thoại ZBS trong `ZbsPhoneTemplateRequest`;
  - `template_data` ZBS có bắt buộc mọi giá trị là chuỗi hay không trong `ZbsPhoneTemplateRequest`;
  - giới hạn 2.000 ký tự được đếm theo code point hay UTF-16 unit trong `ZaloConsultationTextRequest`;
  - vị trí OA ID trong webhook và phạm vi OA Secret Key trong `ZaloWebhookRouter`;
  - `Content-Type` webhook Zalo thực gửi trong `ZaloWebhookController`;
  - mã lỗi refresh token hết hạn, bị thu hồi hoặc đã dùng trong `HttpZaloOAuthClient`.
- [ ] Gỡ nhãn Preview khỏi README, CHANGELOG và tài liệu Zalo theo [ADR-0010](adr/0010-preview-releases.md).

**Done khi:** sample gửi được ZBS thật và nhận webhook thật có verify chữ ký.

## Phase 3 — Đọc và kiểm tra XML hóa đơn điện tử 0.3.0

- [ ] Xác minh cơ sở pháp lý và định dạng XML đang có hiệu lực; ghi kết quả tại
  `docs/einvoice/legal-basis.md`.
- [ ] Thu XML hóa đơn thật đã redact làm test vector.
- [ ] Thiết kế domain model đọc hóa đơn.
- [ ] Implement parser XML an toàn, hỗ trợ nhiều phiên bản định dạng.
- [ ] Kiểm tra cấu trúc và số liệu với mã lỗi ổn định.
- [ ] Kiểm chữ ký số XMLDSig, không kiểm chuỗi tin cậy tới Root CA.
- [ ] Thêm starter, demo và release 0.3.0.

**Done khi:** ứng dụng đọc, kiểm tra cấu trúc/số liệu và xác minh chữ ký XMLDSig của test vector
đã redact với kết quả ổn định.

## Phase 4 — 0.4.0 — Phát hành hóa đơn bất đồng bộ qua một nhà cung cấp (MISA meInvoice) + ký số

- [ ] Xác minh API, sandbox và quy trình ký số của MISA meInvoice.
- [ ] Implement adapter phát hành bất đồng bộ, tra cứu trạng thái và tải artifact.
- [ ] Bảo đảm idempotency cho thao tác phát hành; không tự retry thao tác không idempotent.
- [ ] Tích hợp ký số, starter, demo và release 0.4.0.

## Hướng tới 1.0

- Adapter xác nhận thanh toán VietQR cho SePay/Casso.
- Adapter nhà cung cấp hóa đơn thứ hai.
- Hủy, thay thế và điều chỉnh hóa đơn.
- Docs site.
- Chính sách tương thích.
- Đóng băng API và release 1.0.0.

---

## Rủi ro

| Rủi ro | Mức | Giảm thiểu |
|---|---|---|
| Không có OA/ZBS để nghiệm thu Zalo | Cao | Gắn nhãn Preview theo ADR-0010; test end-to-end với Zalo giả; chuẩn bị sẵn công cụ capture |
| Không lấy được sandbox hóa đơn điện tử | Cao | Thiết kế SPI trước; mời cộng đồng đóng góp adapter; bắt đầu xin sớm từ Phase 1 |
| API Zalo thay đổi | Trung bình | Bọc sau port interface; test contract bằng WireMock; theo dõi changelog Zalo |
| Spring Boot major mới thay đổi auto-config | Thấp | Core không phụ thuộc Spring; CI matrix |
