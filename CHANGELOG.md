# Changelog

Định dạng theo [Keep a Changelog](https://keepachangelog.com/vi/1.1.0/), phiên bản theo [SemVer](https://semver.org).

## [Unreleased]

## [0.2.0] - 2026-11-24

Zalo OA/ZBS trong bản này là **Preview** theo [ADR-0010](docs/adr/0010-preview-releases.md) và chưa được
nghiệm thu với OA thật. Sáu điểm còn phải xác minh là: định dạng số điện thoại ZBS; kiểu giá trị
trong `template_data`; cách đếm giới hạn 2.000 ký tự; vị trí OA ID và phạm vi OA Secret Key trong webhook;
`Content-Type` webhook; mã lỗi refresh token hết hạn, bị thu hồi hoặc đã dùng.

### Added
- Zalo starter cung cấp bean `zaloHttpClient` dùng chung cho registry và OAuth client; ứng dụng có thể thay thế
  theo tên, bean mặc định đặt `defaultCandidate = false` để không ảnh hưởng việc inject `HttpClient` theo type.
- Demo Zalo có chế độ capture HTTP/webhook theo phiên, redact dữ liệu nhạy cảm trước khi ghi và checklist nghiệm
  thu với OA thật.
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
- Typed webhook events cho text/follow/unfollow/delivered/seen có `oaId`, unknown-event fallback, servlet controller
  opt-in và `ZaloWebhookRouter` định tuyến nhiều OA bằng webhook secret tương ứng.
- Demo OAuth callback, gửi OA/ZBS và webhook monitor hỗ trợ chọn account trong cấu hình nhiều OA; tài liệu tích hợp
  tại `docs/zalo/`.

### Changed
- Breaking: Zalo starter không còn đợi `vietnam.zalo-oa.app-id` mới kích hoạt. Khi dependency có trên classpath và
  `vietnam.zalo-oa.enabled` không được đặt hoặc là `true`, starter giờ dừng khởi động nếu thiếu cấu hình account
  hợp lệ. Với cấu hình một OA, `app-id`, `secret-key`, `oa-id` không được để trống; bật webhook còn yêu cầu
  `oa-secret-key`. Ở 0.1.0, starter không kích hoạt khi thiếu `app-id` và không fail-fast các giá trị còn lại khi
  đã có `app-id`.
- Breaking: Zalo starter không còn đăng ký bean `ZaloWebhookVerifier`. Ở 0.1.0 bean này được tạo khi
  auto-configuration đã kích hoạt bằng `vietnam.zalo-oa.app-id`, có `vietnam.zalo-oa.oa-secret-key` và ứng dụng
  chưa tự khai báo verifier.
- Breaking: `ZaloTokenStore` thêm `updateAtomically`; `ZaloTokenManager` dùng store-level atomic refresh thay cho
  lock chỉ trong JVM.
- Breaking: `ZaloOaClient.sendText/sendZns` được thay bằng request/result records và tách ZBS thành
  `ZaloZbsClient`. Ở 0.1.0, `ZaloOaClient` chỉ là interface chưa có implementation trong dự án; thay đổi này chỉ
  ảnh hưởng ứng dụng tự implement hoặc mock interface đó. Tên ZNS cũ không còn dùng vì dịch vụ đã hợp nhất vào
  ZBS từ 01/01/2026.
- Zalo starter tự cấu hình OAuth/OA/ZBS clients, PostgreSQL store và webhook với connect/request timeout.
- `JdbcZaloTokenStore` giới hạn thời gian chờ row lock; Zalo starter thêm
  `vietnam.zalo-oa.jdbc.lock-timeout` mặc định 15 giây và yêu cầu lớn hơn request timeout.
- `ZaloWebhookVerifier` thêm overload `verify(byte[], String)` để kiểm chữ ký từ request bytes sau khi
  decode UTF-8 nghiêm ngặt; webhook controller chấp nhận mọi `Content-Type` và trả response lỗi không lộ payload
  hay chi tiết exception.
- `ZbsPhoneTemplateRequest` nhận số Việt Nam dạng quốc gia, nội địa và các ký tự phân tách thông dụng, sau đó luôn
  chuẩn hóa thành `84` + 9 chữ số; `templateData` chỉ nhận scalar an toàn và ghi `BigDecimal` không dùng số mũ.

### Security
- Zalo token, PKCE verifier, application/OA secret key, recipient phone number và ZBS template values không còn
  bị lộ qua `toString()`.

### Nâng cấp từ 0.1.0

#### Starter Zalo trên classpath mà chưa cấu hình

Ở 0.1.0, auto-configuration chỉ kích hoạt khi có `vietnam.zalo-oa.app-id`. Ở 0.2.0,
`vietnam.zalo-oa.enabled` mặc định là `true`, nên starter kích hoạt ngay khi dependency có trên classpath và dừng
khởi động nếu không có cấu hình account hợp lệ.

Chọn một trong ba cách xử lý: cấu hình đủ account theo README; tắt starter khi chưa dùng Zalo; hoặc bỏ dependency
Zalo starter khỏi ứng dụng. Cấu hình để tắt starter:

```yaml
vietnam:
  zalo-oa:
    enabled: false
```

#### Webhook

Ở 0.1.0, starter đăng ký bean `ZaloWebhookVerifier` khi auto-configuration đã kích hoạt bằng
`vietnam.zalo-oa.app-id`, có `vietnam.zalo-oa.oa-secret-key` và ứng dụng chưa tự khai báo verifier. Ở 0.2.0,
verifier được tạo bên trong `ZaloWebhookRouter` và không còn là bean để inject. Method
`verify(String rawBody, String timestamp, String signatureHeader)` vẫn còn, nhưng ứng dụng phải tự tạo verifier
nếu không chuyển sang router.

Có hai cách chuyển:

1. Chuyển sang `ZaloWebhookRouter`. Để dùng controller có sẵn, cấu hình đủ account và bật webhook:

```yaml
vietnam:
  zalo-oa:
    app-id: ${ZALO_APP_ID}
    secret-key: ${ZALO_APP_SECRET_KEY}
    oa-id: ${ZALO_OA_ID}
    oa-secret-key: ${ZALO_OA_SECRET_KEY}
    webhook:
      enabled: true
```

Controller có sẵn kiểm chữ ký rồi publish `ZaloWebhookEvent` dưới dạng Spring application event. Nếu ứng dụng giữ
HTTP endpoint riêng, để `vietnam.zalo-oa.webhook.enabled=false` nhằm không tạo controller mặc định, rồi tự khai báo
router:

```java
@Configuration(proxyBeanMethods = false)
class ZaloWebhookRoutingConfiguration {
    @Bean
    ZaloWebhookRouter zaloWebhookRouter(ZaloOaRegistry registry) {
        return new ZaloWebhookRouter(registry);
    }
}
```

Inject router vào controller của ứng dụng và truyền đúng raw bytes cùng signature header:

```java
ZaloWebhookEvent event = webhookRouter.route(rawBodyBytes, signatureHeader);
```

2. Nếu muốn giữ cách tự kiểm chữ ký, khai báo lại verifier trong cấu hình của ứng dụng:

```java
@Configuration(proxyBeanMethods = false)
class ZaloWebhookVerifierConfiguration {
    @Bean
    ZaloWebhookVerifier zaloWebhookVerifier(
            @Value("${vietnam.zalo-oa.app-id}") String appId,
            @Value("${vietnam.zalo-oa.oa-secret-key}") String oaSecretKey) {
        return new ZaloWebhookVerifier(appId, oaSecretKey);
    }
}
```

Nếu ứng dụng chỉ dùng verifier mà không cấu hình đủ account của 0.2.0, đặt `vietnam.zalo-oa.enabled=false` để tránh
fail-fast như mục trên. Bean này chỉ phục vụ HTTP endpoint do ứng dụng sở hữu; router/controller của starter không
dùng nó. Sau khi tự tạo bean, code inject `ZaloWebhookVerifier` và gọi method ba tham số của 0.1.0 tiếp tục chạy.
Với code mới, nên dùng `verify(byte[], String)` để kiểm chữ ký trên đúng request body bytes.

#### Token store phải hỗ trợ update atomic

Trước:

```java
final class DatabaseZaloTokenStore implements ZaloTokenStore {
    public Optional<ZaloToken> load(String oaId) { /* ... */ }
    public void save(String oaId, ZaloToken token) { /* ... */ }
}
```

Sau:

```java
ZaloTokenStore tokenStore = new JdbcZaloTokenStore(
        dataSource, Duration.ofSeconds(15));
```

`JdbcZaloTokenStore` đã implement `updateAtomically` bằng transaction và row lock PostgreSQL. Nếu giữ store tùy
chỉnh, phải thêm method này và serialize toàn bộ thao tác giữa mọi instance chia sẻ store; không được
ghép `load` và `save` rời rạc.

#### Tách API tin Tư vấn và ZBS

Trước:

```java
String oaMessageId = zaloOaClient.sendText(userId, "Xin chào");
String znsMessageId = zaloOaClient.sendZns(phone, templateId, templateData, trackingId);
```

Sau:

```java
ZaloMessageResult oaResult = zaloOaClient.sendConsultationText(
        new ZaloConsultationTextRequest(userId, "Xin chào"));
ZaloMessageResult zbsResult = zaloZbsClient.sendTemplateByPhone(
        new ZbsPhoneTemplateRequest(phone, templateId, templateData, trackingId));
```

`sendZns` được chuyển sang client ZBS riêng và dùng tên ZBS thay cho ZNS.
Ở 0.1.0, `ZaloOaClient` chỉ là interface và dự án chưa cung cấp implementation; chỉ ứng dụng tự implement hoặc
mock interface này cần cập nhật.

#### Property bắt buộc fail-fast

Trước, cấu hình thiếu có thể khởi động mà không tạo bean Zalo:

```yaml
vietnam:
  zalo-oa:
    enabled: true
```

Sau, cấu hình một OA phải có đủ credential; bật webhook còn yêu cầu OA Secret Key:

```yaml
vietnam:
  zalo-oa:
    enabled: true
    app-id: ${ZALO_APP_ID}
    secret-key: ${ZALO_APP_SECRET_KEY}
    oa-id: ${ZALO_OA_ID}
    oa-secret-key: ${ZALO_OA_SECRET_KEY}
    webhook:
      enabled: true
```

Starter dừng khởi động và nêu đúng tên property khi thiếu `app-id`, `secret-key`, `oa-id` hoặc
`oa-secret-key` cho webhook.

#### JDBC lock timeout

Trước không có property giới hạn thời gian chờ row lock. Sau, có thể giữ mặc định 15 giây hoặc
cấu hình giá trị lớn hơn `request-timeout`:

```yaml
vietnam:
  zalo-oa:
    request-timeout: 10s
    jdbc:
      lock-timeout: 15s
```

#### HTTP client dùng chung

Trước, starter không có bean có tên để thay HTTP client nội bộ. Sau, định nghĩa bean `zaloHttpClient`
để registry và OAuth client cùng dùng:

```java
@Bean(name = "zaloHttpClient", defaultCandidate = false)
HttpClient zaloHttpClient() {
    return HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();
}
```

`defaultCandidate = false` giữ bean chuyên dụng này khỏi trở thành ứng viên mặc định khi ứng dụng inject
`HttpClient` theo type; starter vẫn chọn nó theo tên `zaloHttpClient`.

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
