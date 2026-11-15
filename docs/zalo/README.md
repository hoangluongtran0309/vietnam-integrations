# Zalo OA / ZBS

Module Zalo cung cấp OAuth OA v4 + PKCE, quản lý vòng đời token, gửi tin Tư vấn OA v3, gửi ZBS Template Message
qua số điện thoại và nhận webhook có xác thực chữ ký. Tài liệu được đối chiếu với tài liệu Zalo công khai ngày
2026-11-13. ZNS đã được hợp nhất vào **ZBS Template Message** từ 01/01/2026.

> Dự án không liên kết với Zalo/VNG. Chính sách quyền, hạn mức và giá có thể thay đổi; luôn kiểm tra lại trang Zalo
> for Developers trước khi bật production.

## 1. Chuẩn bị App, OA và ZBS

1. Tạo Zalo App tại Zalo for Developers, kích hoạt app và ghi lại App ID/App Secret Key.
2. Tạo hoặc chọn OA doanh nghiệp đã xác thực. Trong app, liên kết OA và cấu hình callback HTTPS đã xác minh domain.
3. Ở luồng cấp quyền OA, chọn tối thiểu quyền gửi tin/thông báo qua OA và các quyền webhook cần dùng.
4. Tạo ZBS Account, liên kết App/OA, tạo template và chờ duyệt. Cấp quyền **Gửi tin qua số điện thoại**.
5. Trong phần Webhook, khai báo URL `https://your-domain.example/webhooks/zalo`, OA Secret Key và bật các event
   cần dùng: `user_send_text`, `follow`, `unfollow`, `user_received_message`, `user_seen_message`.

Nguồn chính thức:

- [OAuth OA v4](https://stc-developers.zdn.vn/docs/v2/official-account/bat-dau/xac-thuc-va-uy-quyen-cho-ung-dung-new)
- [Tin Tư vấn](https://stc-developers.zdn.vn/docs/v2/official-account/tin-nhan/tin-tu-van/gui-tin-tu-van-trich-dan)
- [ZBS qua SĐT](https://stc-developers.zdn.vn/docs/v2/zbs-template-message/gui-tin-template-qua-sdt/api-gui-tin-qua-sdt/api-gui-tin)
- [Webhook người dùng gửi tin](https://stc-developers.zdn.vn/docs/v2/official-account/webhook/tin-nhan/su-kien-nguoi-dung-gui-tin-nhan)

## 2. Cài đặt và cấu hình

```xml
<dependency>
  <groupId>io.github.hoangluongtran0309</groupId>
  <artifactId>vietnam-zalo-oa-spring-boot-starter</artifactId>
  <version>0.2.0</version>
</dependency>
```

```yaml
vietnam:
  zalo-oa:
    app-id: ${ZALO_APP_ID}
    secret-key: ${ZALO_APP_SECRET_KEY}
    oa-id: ${ZALO_OA_ID}
    oa-secret-key: ${ZALO_OA_SECRET_KEY}
    token-store: jdbc # memory chỉ dành cho một instance/dev
    connect-timeout: 5s
    request-timeout: 10s
    webhook:
      enabled: true
      path: /webhooks/zalo
```

Không đưa credential vào YAML trong Git. Dùng secret manager hoặc environment variables.

### PostgreSQL

`JdbcZaloTokenStore` hỗ trợ PostgreSQL 14+. Áp dụng migration
`io/github/hoangluongtran0309/vietnam/zalo/postgresql/V1__create_zalo_oa_tokens.sql` từ JAR bằng Flyway,
Liquibase hoặc công cụ migration hiện có. Starter **không** tự tạo bảng.

Ứng dụng phải cung cấp một `DataSource` bean. Token trong bảng là credential nhạy cảm: bật encryption at rest,
chỉ cấp quyền `SELECT/INSERT/UPDATE` cần thiết cho application role, không log/query token trong công cụ quan sát.

`token-store: jdbc` dùng row lock để chỉ một instance sử dụng refresh token một lần. Nếu refresh gặp lỗi mạng với
kết quả không xác định, thư viện không retry; hãy kiểm tra kết nối và cấp quyền OAuth lại.

## 3. OAuth + PKCE

Mỗi lần cấp quyền phải tạo PKCE mới và lưu verifier/state ở server-side session:

```java
ZaloPkce pkce = ZaloPkce.generate();
// Lưu pkce.codeVerifier() và pkce.state() trong session.
// Gửi pkce.codeChallenge() + state trong authorization URL.
```

Callback phải so sánh `state`, lấy `code` và `oa_id`, sau đó đổi code và lưu token:

```java
ZaloToken token = oauthClient.exchangeAuthorizationCode(code, savedCodeVerifier);
tokenStore.save(oaId, token);
```

Authorization code chỉ dùng một lần. Không trả token ra browser hoặc log. Demo app có sẵn
`GET /zalo/oauth/start` và `GET /zalo/oauth/callback` để minh họa toàn bộ luồng.

## 4. Gửi tin

Tin Tư vấn text, giới hạn 2.000 ký tự:

```java
ZaloMessageResult result = zaloOaClient.sendConsultationText(
        new ZaloConsultationTextRequest(userId, "Xin chào"));
```

ZBS qua số điện thoại chuẩn hóa `84...` hoặc `+84...`:

```java
ZaloMessageResult result = zaloZbsClient.sendTemplateByPhone(
        new ZbsPhoneTemplateRequest(
                "84912345678",
                "template-id",
                Map.of("order_code", "DH-123", "amount", 150000),
                "order-DH-123"));
```

`trackingId` dài tối đa 48 ký tự và nên duy nhất theo business operation. Client không tự retry; caller phải quyết
định retry theo trạng thái nghiệp vụ để tránh gửi trùng.

## 5. Webhook

Khi bật webhook, starter kiểm `X-ZEvent-Signature` trên **raw JSON body**, parse event và publish record core trực
tiếp qua Spring event bus:

```java
@EventListener
void onUserText(UserTextMessageEvent event) {
    // Handler phải idempotent vì Zalo có thể retry webhook.
}
```

Các event typed trong 0.2.0: user text, follow, unfollow, delivered và seen. Event mới/chưa hỗ trợ trở thành
`UnknownZaloWebhookEvent` và vẫn trả HTTP 200. Signature sai trả 401; body lỗi trả 400; exception từ listener lan ra
thành 5xx để Zalo retry.

## 6. Chạy demo và nghiệm thu thật

```bash
export ZALO_ENABLED=true
export ZALO_APP_ID=...
export ZALO_APP_SECRET_KEY=...
export ZALO_OA_ID=...
export ZALO_OA_SECRET_KEY=...
export ZALO_WEBHOOK_ENABLED=true

./mvnw -q install -DskipTests
./mvnw -Psamples -pl samples/demo-app spring-boot:run
```

Với PostgreSQL, chạy migration trước rồi thêm `ZALO_TOKEN_STORE=jdbc`, `ZALO_JDBC_URL`, `ZALO_JDBC_USER` và
`ZALO_JDBC_PASSWORD`.

Checklist release:

- `/zalo/oauth/start` → cấp quyền → callback lưu token thành công.
- `POST /zalo/consultation` gửi được tới test user đủ điều kiện.
- `POST /zalo/zbs` gửi template đã duyệt tới số test với tracking ID mới.
- Gửi tin vào OA; `/zalo/webhook/last` trả đúng typed event.
- Payload bị sửa hoặc signature sai phải bị từ chối.
- Redact toàn bộ token, số điện thoại và user ID trước khi thêm response thật làm test vector.

## 7. Giới hạn 0.2.0

Chưa hỗ trợ ZBS qua UID, quản lý template, media, broadcast, quota/status, Journey, MySQL, webhook deduplication
hoặc tự mã hóa token cấp ứng dụng.
