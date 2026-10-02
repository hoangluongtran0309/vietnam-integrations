# Kế hoạch dự án

Giả định: làm part-time song song với các dự án khác (~8–12 giờ/tuần). Mốc thời gian tính từ tuần bắt đầu.

## Nguyên tắc ưu tiên

1. **Ship sớm module dễ nhất** (VietQR: offline, không cần credential) để có người dùng và phản hồi thật.
2. **Chỉ làm tích hợp mà mình test được với hệ thống thật.** Không merge adapter khi chưa chạy với sandbox.
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

- [x] Đối chiếu và bổ sung đủ danh sách BIN ngân hàng từ nguồn chính thức NAPAS/VietQR — 65 mục từ `api.vietqr.io`
  (2026-10-02), xem [vietqr/banks.md](vietqr/banks.md). Còn `TODO`: đối chiếu khi có danh sách chính thức của NAPAS.
- [ ] Thu thập ≥ 5 payload thật (tạo từ app ngân hàng khác nhau), thêm làm test vector cho decoder
- [ ] Quét QR sinh ra bằng ≥ 3 app ngân hàng thật, xác nhận đúng người nhận/số tiền/nội dung
- [ ] Quyết định giới hạn độ dài `purpose` (đo thực tế trên các app)
  - Đã làm: giới hạn cứng 95 ký tự, báo lỗi thay vì cắt; EMVCo khuyến nghị ≤ 25
    ([ADR-0005](adr/0005-purpose-length-policy.md), trạng thái Proposed). Còn lại: đo trên app thật rồi chốt ADR.
- [x] Render ảnh có logo/khung tùy chọn (optional, có thể để 0.1.x) — logo + màu qua `QrImageOptions`,
  xem [vietqr/rendering.md](vietqr/rendering.md). Khung/chữ quanh mã: chưa làm (font tiếng Việt trên server headless).
- [ ] `PaymentConfirmationListener` SPI cho xác nhận thanh toán (SePay/Casso) — **chỉ thiết kế interface**, adapter để 0.4
- [ ] Viết docs: README, trang VietQR, ví dụ
- [ ] Release 0.1.0

**Done khi:** người lạ thêm dependency từ Central và tạo QR quét được trong < 5 phút theo README.

## Phase 2 — Zalo OA 0.2.0 (tuần 5–9)

- [ ] Tạo Zalo app + OA test; ghi lại toàn bộ quy trình cấp quyền vào docs (đây là phần người dùng hay kẹt nhất)
- [ ] Đối chiếu endpoint, thời hạn token, công thức chữ ký webhook với tài liệu Zalo hiện hành
- [ ] Implement `ZaloOAuthClient` (PKCE) bằng `java.net.http.HttpClient`
- [ ] `JdbcZaloTokenStore` + schema SQL; khóa phân tán cho refresh khi chạy nhiều instance (ADR-0004)
- [ ] Implement `ZaloOaClient`: tin tư vấn, ZNS
- [ ] Webhook: controller tùy chọn + publish `ApplicationEvent` có kiểu theo từng loại sự kiện
- [ ] Test bằng WireMock với response thu thập từ API thật
- [ ] Release 0.2.0

**Done khi:** sample app gửi được ZNS thật và nhận webhook có verify chữ ký.

## Phase 3 — Hóa đơn điện tử 0.3.0 (tuần 10–16)

- [ ] Liên hệ 2–3 nhà cung cấp (Viettel S-Invoice, MISA meInvoice, VNPT…) xin sandbox; chọn **một** cái có sandbox dễ nhất
- [ ] Rà soát domain model với NĐ 123/2020 + TT 78/2021 (ký hiệu, mẫu số, thuế suất, làm tròn)
- [ ] Module `vietnam-einvoice-provider-<x>` đầu tiên: issue, status, download PDF
- [ ] Idempotency theo `externalId` (không phát hành trùng khi retry)
- [ ] Release 0.3.0

**Done khi:** sample app phát hành được hóa đơn trên sandbox và tải được PDF.

## Phase 4 — Hướng tới 1.0 (sau tuần 16)

- Adapter nhà cung cấp hóa đơn thứ hai (do cộng đồng đóng góp càng tốt)
- Adapter xác nhận thanh toán VietQR (SePay/Casso webhook)
- Hủy / thay thế / điều chỉnh hóa đơn
- Docs site (MkDocs Material trên GitHub Pages), song ngữ
- Chính sách tương thích: hỗ trợ 2 dòng Spring Boot minor mới nhất
- Đóng băng API, release 1.0.0

---

## Rủi ro

| Rủi ro | Mức | Giảm thiểu |
|---|---|---|
| Không lấy được sandbox hóa đơn điện tử | Cao | Thiết kế SPI trước; mời cộng đồng đóng góp adapter; bắt đầu xin sớm từ Phase 1 |
| API Zalo thay đổi | Trung bình | Bọc sau port interface; test contract bằng WireMock; theo dõi changelog Zalo |
| Spring Boot major mới thay đổi auto-config | Thấp | Core không phụ thuộc Spring; CI matrix |
