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
    jdbc:
      lock-timeout: 15s
    webhook:
      enabled: true
      path: /webhooks/zalo
```

Không đưa credential vào YAML trong Git. Dùng secret manager hoặc environment variables.

### Nhiều OA

Dùng cấu hình nhiều OA khi một ứng dụng cần chọn động giữa các Official Account, chẳng hạn OA chăm sóc khách hàng
và OA marketing. Đặt alias ổn định cho từng OA; alias chỉ gồm chữ thường, chữ số và dấu gạch ngang, dài tối đa 32
ký tự. Không cấu hình đồng thời `oa-id` cấp gốc và `accounts`.

```yaml
vietnam:
  zalo-oa:
    app-id: ${ZALO_APP_ID} # mặc định cho account không override
    secret-key: ${ZALO_APP_SECRET_KEY}
    default-account: cskh
    accounts:
      cskh:
        oa-id: ${ZALO_CSKH_OA_ID}
        oa-secret-key: ${ZALO_CSKH_OA_SECRET_KEY}
      marketing:
        oa-id: ${ZALO_MARKETING_OA_ID}
        oa-secret-key: ${ZALO_MARKETING_OA_SECRET_KEY}
        app-id: ${ZALO_MARKETING_APP_ID} # chỉ cần khi OA thuộc App khác
        secret-key: ${ZALO_MARKETING_APP_SECRET_KEY}
```

`app-id` và `secret-key` trong từng account là tùy chọn; khi bỏ qua, account kế thừa cặp giá trị cấp gốc. Các
account cùng App phải dùng cùng `secret-key`. `default-account` tạo các bean tiện ích `ZaloTokenManager`,
`ZaloOaClient` và `ZaloZbsClient` cho alias đó. Nếu không đặt mặc định khi có nhiều account, inject registry và chọn
OA rõ ràng:

```java
ZaloOa customerCare = zaloOaRegistry.oa("cskh");
customerCare.messaging().sendConsultationText(request);

@EventListener
void onUserText(UserTextMessageEvent event) {
    ZaloOa source = zaloOaRegistry.byOaId(event.oaId());
    // Xử lý theo OA đã nhận webhook.
}
```

Đăng ký cùng URL webhook của ứng dụng cho từng OA và cấu hình đúng `oa-secret-key` của từng alias. Router đọc OA
từ payload trước, kiểm `app_id`, rồi mới xác thực chữ ký bằng secret tương ứng; OA lạ hoặc App không khớp bị từ chối.
Token vẫn được lưu và khóa độc lập theo `oaId`, kể cả khi nhiều OA dùng chung một App và token store.

### Thuộc tính bắt buộc theo tính năng

| Thuộc tính | OAuth/gửi tin mặc định | Có `ZaloTokenManager` tùy chỉnh | Webhook | Mặc định |
|---|---:|---:|---:|---|
| `vietnam.zalo-oa.enabled` | Không | Không | Không | `true` |
| `vietnam.zalo-oa.app-id` | Có nếu account không override | Có nếu account không override | Có nếu account không override | — |
| `vietnam.zalo-oa.secret-key` | Có nếu account không override | Có nếu account không override | Có nếu account không override | — |
| `vietnam.zalo-oa.oa-id` | Có khi không dùng `accounts` | Có khi không dùng `accounts` | Có khi không dùng `accounts` | — |
| `vietnam.zalo-oa.oa-secret-key` | Không | Không | Có | — |
| `vietnam.zalo-oa.accounts.<alias>.oa-id` | Thay cho `oa-id` cấp gốc | Thay cho `oa-id` cấp gốc | Có | — |
| `vietnam.zalo-oa.accounts.<alias>.oa-secret-key` | Không | Không | Có với mọi alias | — |
| `vietnam.zalo-oa.accounts.<alias>.app-id` | Không nếu có `app-id` cấp gốc | Không nếu có `app-id` cấp gốc | Không nếu có `app-id` cấp gốc | Kế thừa cấp gốc |
| `vietnam.zalo-oa.accounts.<alias>.secret-key` | Không nếu có `secret-key` cấp gốc | Không nếu có `secret-key` cấp gốc | Không nếu có `secret-key` cấp gốc | Kế thừa cấp gốc |
| `vietnam.zalo-oa.default-account` | Không | Không | Không | Tự chọn nếu chỉ có một account |
| `vietnam.zalo-oa.token-store` | Không | Không | Không | `memory` |
| `vietnam.zalo-oa.connect-timeout` | Không | Không | Không | `5s` |
| `vietnam.zalo-oa.request-timeout` | Không | Không | Không | `10s` |
| `vietnam.zalo-oa.jdbc.lock-timeout` | Không | Không | Không | `15s` |
| `vietnam.zalo-oa.webhook.enabled` | Không | Không | Phải đặt `true` | `false` |
| `vietnam.zalo-oa.webhook.path` | Không | Không | Không | `/webhooks/zalo` |

Starter dừng khởi động và nêu đúng tên thuộc tính khi cấu hình bắt buộc bị thiếu hoặc rỗng. Cấu hình một OA luôn cần
`oa-id` cấp gốc; cấu hình map luôn cần `oa-id` cho từng account. Có thể bỏ credential khỏi properties khi ứng dụng
tự cung cấp toàn bộ `ZaloOaRegistry`.

### PostgreSQL

`JdbcZaloTokenStore` hỗ trợ PostgreSQL 14+. Áp dụng migration
`io/github/hoangluongtran0309/vietnam/zalo/postgresql/V1__create_zalo_oa_tokens.sql` từ JAR bằng Flyway,
Liquibase hoặc công cụ migration hiện có. Starter **không** tự tạo bảng.

Ứng dụng phải cung cấp một `DataSource` bean. Token trong bảng là credential nhạy cảm: bật encryption at rest,
chỉ cấp quyền `SELECT/INSERT/UPDATE` cần thiết cho application role, không log/query token trong công cụ quan sát.

`token-store: jdbc` dùng row lock để chỉ một instance sử dụng refresh token một lần. Nếu refresh gặp lỗi mạng với
kết quả không xác định, thư viện không retry; hãy kiểm tra kết nối và cấp quyền OAuth lại. `jdbc.lock-timeout` phải
lớn hơn `request-timeout`, vì instance đang giữ row lock có thể chờ Zalo tới hết request timeout. Hết thời gian chờ
lock sẽ báo rõ instance khác đang refresh token thay vì chờ vô hạn.

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
`GET /zalo/oauth/start?account=cskh` và `GET /zalo/oauth/callback` để minh họa toàn bộ luồng. Endpoint `start` trả
HTTP 302 tới trang cấp quyền và lưu PKCE verifier/state trong session. Khi bỏ tham số `account`, demo dùng
`default-account` (hoặc account duy nhất); callback chỉ chấp nhận `oa_id` đúng với alias đã lưu cùng PKCE state
trong session.

### Khi gặp `ZaloReauthorizationRequiredException`

Exception này cho biết refresh token không còn dùng được. Operator cần chạy lại toàn bộ luồng OAuth cho đúng OA,
kiểm tra app vẫn được liên kết với OA và có đủ quyền cần thiết, sau đó xác nhận token mới đã được lưu trước khi cho
phép gửi tin trở lại. Không retry refresh token cũ.

## 4. Gửi tin

Tin Tư vấn text, giới hạn 2.000 ký tự:

```java
ZaloMessageResult result = zaloOaClient.sendConsultationText(
        new ZaloConsultationTextRequest(userId, "Xin chào"));
```

ZBS nhận số điện thoại Việt Nam ở dạng `0` + 9 chữ số, `84` + 9 chữ số hoặc `+84` + 9 chữ số. Có thể dùng
khoảng trắng, dấu chấm, gạch ngang và ngoặc tròn làm ký tự phân tách; thư viện luôn chuẩn hóa giá trị được lưu và
gửi thành `84` + 9 chữ số. Validation chỉ kiểm cấu trúc, không kiểm đầu số nhà mạng.

```java
ZaloMessageResult result = zaloZbsClient.sendTemplateByPhone(
        new ZbsPhoneTemplateRequest(
                "0912 345 678",
                "template-id",
                Map.of("order_code", "DH-123", "amount", 150000),
                "order-DH-123"));
```

Trong demo nhiều OA, truyền `account` cho từng request, ví dụ
`POST /zalo/consultation?account=cskh&userId=...&text=...` và `POST /zalo/zbs?account=marketing`. Có thể bỏ tham số
này khi đã cấu hình `default-account`.

Key trong `templateData` không được để trống. Value hỗ trợ `String`, `Boolean`, các kiểu số nguyên
`Integer`/`Long`/`Short`/`Byte`/`BigInteger` và `BigDecimal`; không nhận `Double`, `Float` hoặc object tùy ý.
`BigDecimal` được gửi dưới dạng số thập phân thường, không dùng ký pháp số mũ.

`trackingId` dài tối đa 48 ký tự và nên duy nhất theo business operation. Client không tự retry; caller phải quyết
định retry theo trạng thái nghiệp vụ để tránh gửi trùng.

## 5. Webhook

Khi bật webhook, starter kiểm `X-ZEvent-Signature` trên **raw JSON body** được decode UTF-8 nghiêm ngặt, parse
event và publish record core trực tiếp qua Spring event bus. Endpoint không giới hạn `Content-Type`; request có body
không phải JSON UTF-8 hợp lệ vẫn bị từ chối.

```java
@EventListener
void onUserText(UserTextMessageEvent event) {
    // Handler phải idempotent vì Zalo có thể retry webhook.
}
```

Các event typed trong 0.2.0: user text, follow, unfollow, delivered và seen. Event mới/chưa hỗ trợ trở thành
`UnknownZaloWebhookEvent` và vẫn trả HTTP 200. Thiếu/sai signature trả 401; body lỗi trả 400; exception từ
listener được controller bắt và trả 500 để Zalo retry. Response lỗi có body rỗng, không chứa payload hay
chi tiết exception.

Starter chưa chống replay: cùng một payload hợp lệ gửi lại vẫn được chấp nhận. Listener phải idempotent
theo `msg_id`, hoặc theo tổ hợp `event_name` + `timestamp` + sender đối với event không có `msg_id`.

## 6. Mức độ kiểm chứng

Test end-to-end dùng WireMock làm **Zalo giả** và PostgreSQL 16 thật trong Testcontainers. Test này bắt lỗi nối dây
giữa demo controller, session OAuth, registry nhiều OA, HTTP client, JDBC token store, webhook controller và event
monitor. Nó **không chứng minh Zalo thật sẽ chấp nhận request**, không thay thế quyền App/OA, template đã duyệt,
chính sách gửi tin, hạn mức, giao nhận thực tế hoặc capture từ OA thật.

| Kiểm bằng test đơn vị | Kiểm bằng Zalo giả end-to-end | Chỉ kiểm được với OA thật |
|---|---|---|
| OAuth v4: form/header, PKCE, parse token, không retry; token manager và PostgreSQL row lock được kiểm riêng. | HTTP 302 có `code_challenge`/`state`; callback giữ session và lưu token đúng `oa_id`; đồng hồ được đẩy qua hạn để refresh đúng một lần và lưu refresh token mới. | Hoàn tất cấp quyền trên App/OA thật và xác nhận callback/token thật. `TODO(v0.2)`: xác minh đầy đủ mã lỗi refresh token hết hạn hoặc bị thu hồi từ OAuth OA v4. |
| Tin Tư vấn: validate request, escape Unicode/control character, serialize body và parse response. | Gọi HTTP thật của demo; WireMock nhận đúng một lệnh với access token sau refresh. | Gửi tới test user đủ điều kiện và xác nhận chính sách/quyền/giao nhận. `TODO(v0.2)`: xác minh giới hạn 2.000 ký tự được Zalo đếm theo code point hay UTF-16 unit. |
| ZBS: chuẩn hóa cấu trúc số điện thoại, giới hạn tracking ID, kiểu scalar và dạng số thập phân. | Gọi HTTP thật của demo; WireMock nhận đúng một lệnh ZBS với số đã chuẩn hóa và access token đúng. | Gửi template đã duyệt tới số test và kiểm tra giao nhận. `TODO(v0.2)`: xác minh chính xác định dạng số điện thoại ZBS và việc mọi template parameter có bắt buộc là chuỗi hay không. |
| Webhook: chữ ký trên raw body, UTF-8, typed event, router nhiều OA, status 200/400/401/500. | Payload fixture của OA thứ hai với secret thứ hai trả 200 và monitor hiện đúng `oaId`; cùng payload ký bằng secret OA thứ nhất trả 401. | Nhận capture thật từ từng OA. `TODO(v0.2)`: xác minh OA ID nằm ở sender/recipient theo từng event, OA Secret Key thuộc OA hay App, và `Content-Type` Zalo thực gửi. |
| Lỗi API, timeout và quy tắc không tự retry được kiểm ở từng client; dữ liệu nhạy cảm được kiểm tra khi `toString()`. | Toàn bộ luồng demo chạy qua random port, cùng migration V1 và `token-store=jdbc`; DB được kiểm sau từng chặng. | Xác nhận mã lỗi, payload và header thực tế có khớp tài liệu tại thời điểm release; redact capture trước khi đưa vào fixture. |

## 7. Chạy demo và nghiệm thu thật

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

Để nghiệm thu với OA thật mà không ghi dữ liệu thô xuống đĩa, đặt `ZALO_CAPTURE_DIR` tới một thư mục **nằm ngoài
mọi cây Git**. Demo sẽ ghi request/response Zalo và webhook đã redact vào một thư mục phiên; khi không đặt biến này,
bean graph và hành vi demo giữ nguyên. Thực hiện đủ các bước chuẩn bị, kịch bản và duyệt thủ công trong
[checklist capture Zalo thật](capture-checklist.md) trước khi đưa bất kỳ capture nào vào test fixture.

Checklist release:

- `/zalo/oauth/start?account=<alias>` → cấp quyền → callback lưu token đúng `oaId` của alias đó.
- `POST /zalo/consultation?account=<alias>` gửi được tới test user đủ điều kiện.
- `POST /zalo/zbs?account=<alias>` gửi template đã duyệt tới số test với tracking ID mới.
- Gửi tin vào từng OA; `/zalo/webhook/last` trả đúng typed event và `oaId` nguồn.
- Payload bị sửa hoặc signature sai phải bị từ chối.
- Redact toàn bộ token, số điện thoại và user ID trước khi thêm response thật làm test vector.

## 8. Giới hạn 0.2.0

Chưa hỗ trợ ZBS qua UID, quản lý template, media, broadcast, quota/status, Journey, MySQL, webhook deduplication
hoặc tự mã hóa token cấp ứng dụng.
