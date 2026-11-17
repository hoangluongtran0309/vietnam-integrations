# Đối chiếu Zalo OA với các SDK khác

Tài liệu này ghi nhận bằng chứng gián tiếp khi chưa có Official Account thật để capture. Kết quả chỉ cho biết cách
các SDK khác đang triển khai; nó không thay thế tài liệu chính thức hoặc capture đã redact từ Zalo. Các kết luận
`Chưa rõ` phải được giữ lại dưới dạng TODO cho đến khi có một trong hai nguồn đó.

Ngày đối chiếu: **2026-11-17**.

## Nguồn mã đã đọc

| Ký hiệu | SDK | Phiên bản/commit đã ghim | Commit cuối | Phạm vi hữu ích |
|---|---|---|---|---|
| SDK A | [Zalo PHP SDK chính thức](https://github.com/zaloplatform/zalo-php-sdk), package `zaloplatform/zalo-php-sdk` | tag [`4.0.4`](https://github.com/zaloplatform/zalo-php-sdk/tree/4.0.4), commit [`6bacb1b`](https://github.com/zaloplatform/zalo-php-sdk/commit/6bacb1bea670ce1fa9f1ae616bbbfd92742e20aa) | 2023-12-20 | OAuth OA v4, tin Tư vấn OA v3; không có ZBS hoặc webhook |
| SDK B | [Field VN Zalo SDK cho Laravel](https://github.com/field-vn/zalo), package `field-vn/zalo` | tag [`v0.2.8`](https://github.com/field-vn/zalo/tree/v0.2.8), commit [`18119e5`](https://github.com/field-vn/zalo/commit/18119e56514a84b9482a2a5bd36895254217ba03) | 2026-09-18 | OAuth OA, OA v3, ZBS, webhook; commit nằm trong 12 tháng trước ngày đối chiếu |

SDK B được ưu tiên cho các điểm mới hơn vì còn được bảo trì gần ngày đối chiếu. Một số sửa lỗi trong commit đã ghim
nêu trực tiếp việc đổi cách lấy OA/user ID cho delivery receipt và sửa endpoint sau khi gặp API không hợp lệ. Đây là
bằng chứng thực dụng tốt hơn suy đoán, nhưng vẫn không mạnh bằng capture của chính dự án này.

## Bảng đối chiếu

`Khớp` nghĩa là code hiện tại cùng endpoint/cấu trúc với ít nhất một SDK có triển khai tính năng và không bị SDK còn
lại phủ định. `Lệch` nghĩa là có khác biệt quan sát được cần một work package implementer xem xét. `Chưa rõ` nghĩa là
hai SDK không đủ bằng chứng để chốt đặc tả.

| Điểm cần kiểm | Code của mình | SDK A | SDK B | Kết luận |
|---|---|---|---|---|
| OAuth đổi authorization code: endpoint, method và credential | `HttpZaloOAuthClient.java:22,47-54,73-81`: `POST https://oauth.zaloapp.com/v4/oa/access_token`, form-urlencoded gồm `code`, `app_id`, `grant_type=authorization_code`, `code_verifier`; header `secret_key` | [`OAuth2Client.php:152-180,279-296`](https://github.com/zaloplatform/zalo-php-sdk/blob/6bacb1bea670ce1fa9f1ae616bbbfd92742e20aa/src/Authentication/OAuth2Client.php#L152-L180): cùng endpoint tương đối `/v4/oa/access_token`, POST form và header `secret_key` | [`OAuthClient.php:36-49,67-86`](https://github.com/field-vn/zalo/blob/18119e56514a84b9482a2a5bd36895254217ba03/src/Core/Auth/OAuthClient.php#L36-L49): cùng endpoint, method, form và header | **Khớp** |
| OAuth refresh: endpoint, method và body | `HttpZaloOAuthClient.java:57-64,73-81`: cùng endpoint, POST form gồm `refresh_token`, `app_id`, `grant_type=refresh_token`; không retry | [`OAuth2Client.php:208-234`](https://github.com/zaloplatform/zalo-php-sdk/blob/6bacb1bea670ce1fa9f1ae616bbbfd92742e20aa/src/Authentication/OAuth2Client.php#L208-L234): cùng endpoint và form | [`OAuthClient.php:52-74`](https://github.com/field-vn/zalo/blob/18119e56514a84b9482a2a5bd36895254217ba03/src/Core/Auth/OAuthClient.php#L52-L74): cùng endpoint và form | **Khớp** |
| Tên field và kiểu wire của `expires_in` | `HttpZaloOAuthClient.java:83-92` đọc `expires_in`, chấp nhận JSON string hoặc number qua Gson rồi parse `long`; test có cả hai dạng | [`OAuth2Client.php:245-264`](https://github.com/zaloplatform/zalo-php-sdk/blob/6bacb1bea670ce1fa9f1ae616bbbfd92742e20aa/src/Authentication/OAuth2Client.php#L245-L264) đọc đúng tên field nhưng không kiểm kiểu trước khi chuyển vào tham số được mô tả là `int` | [`TokenPair.php:25-43`](https://github.com/field-vn/zalo/blob/18119e56514a84b9482a2a5bd36895254217ba03/src/Core/Auth/TokenPair.php#L25-L43) ghi rõ Zalo trả **chuỗi giây** và ép sang `int`; fixture OAuth cũng dùng `"3600"` | **Khớp**, kiểu quan sát được là string; việc chấp nhận number là tương thích rộng hơn |
| Tin Tư vấn dạng text: endpoint, method và body | `HttpZaloOaClient.java:17,37-51`: POST JSON tới `/v3.0/oa/message/cs`, body `recipient.user_id`, `message.text`, tùy chọn `message.quote_message_id`; header `access_token` | [`ZaloEndPoint.php:88-92`](https://github.com/zaloplatform/zalo-php-sdk/blob/6bacb1bea670ce1fa9f1ae616bbbfd92742e20aa/src/ZaloEndPoint.php#L88-L92) và [`MessageBuilder.php:529-540`](https://github.com/zaloplatform/zalo-php-sdk/blob/6bacb1bea670ce1fa9f1ae616bbbfd92742e20aa/src/Builder/MessageBuilder.php#L529-L540): cùng endpoint/body | [`MessageResource.php:32-42`](https://github.com/field-vn/zalo/blob/18119e56514a84b9482a2a5bd36895254217ba03/src/Core/Channels/OA/Resources/MessageResource.php#L32-L42) và [`TextMessage.php:76-105`](https://github.com/field-vn/zalo/blob/18119e56514a84b9482a2a5bd36895254217ba03/src/Core/Channels/OA/Messages/TextMessage.php#L76-L105): cùng endpoint/body | **Khớp** |
| Giới hạn 2.000 ký tự: code point hay UTF-16 unit | `ZaloConsultationTextRequest.java:11-13` dùng `String.length()`, tức UTF-16 code unit | Không validate độ dài trong `MessageBuilder` | [`TextMessage.php:18,37-50`](https://github.com/field-vn/zalo/blob/18119e56514a84b9482a2a5bd36895254217ba03/src/Core/Channels/OA/Messages/TextMessage.php#L18-L50) dùng `mb_strlen`, tức đếm ký tự Unicode thay vì UTF-16 code unit | **Lệch** với SDK B; bằng chứng nghiêng về code point nhưng vẫn cần capture biên có ký tự ngoài BMP |
| ZBS gửi qua SĐT: endpoint, method và body | `HttpZaloZbsClient.java:16,35-46`: POST JSON tới `https://business.openapi.zalo.me/message/template`; body luôn có `phone`, `template_id`, `template_data`, `tracking_id`; không có `mode` | Không triển khai ZBS | [`ZbsResource.php:118-147`](https://github.com/field-vn/zalo/blob/18119e56514a84b9482a2a5bd36895254217ba03/src/Core/Channels/OA/Resources/ZbsResource.php#L118-L147): cùng endpoint/method; body có bốn field đầu nhưng `tracking_id` tùy chọn và luôn có thêm `mode` (`development` hoặc `production`) | **Lệch** với SDK B về `mode` và tính bắt buộc của `tracking_id`; chưa đủ bằng chứng để chốt `mode` có bắt buộc trên wire hay không |
| Định dạng SĐT ZBS | `ZbsPhoneTemplateRequest.java:13-15,38-55` nhận `0xxxxxxxxx`, `84xxxxxxxxx`, `+84xxxxxxxxx` và chuẩn hóa thành `84` + 9 chữ số, không có dấu `+` | Không triển khai ZBS | [`PhoneNumber.php:9-59`](https://github.com/field-vn/zalo/blob/18119e56514a84b9482a2a5bd36895254217ba03/src/Support/PhoneNumber.php#L9-L59) nhận các dạng tương tự và gửi dạng `84987654321` | **Khớp** cho số Việt Nam 10 chữ số hiện hành |
| Kiểu giá trị `template_data` của ZBS | `ZbsPhoneTemplateRequest.java:58-68` cho phép string, boolean và number; JSON giữ nguyên kiểu | Không triển khai ZBS | [`ZbsResource.php:114-138,581-593`](https://github.com/field-vn/zalo/blob/18119e56514a84b9482a2a5bd36895254217ba03/src/Core/Channels/OA/Resources/ZbsResource.php#L581-L593) nhận string/int nhưng `stringify()` mọi giá trị trước khi gửi; test ghim `123456` thành `"123456"` | **Lệch** với SDK B; cần capture/template thật trước khi thu hẹp public API |
| Header và công thức chữ ký webhook | `ZaloWebhookVerifier.java:20,38-54,74-77`: đọc `X-ZEvent-Signature`, chấp nhận `mac=` hoặc hash trần, so sánh constant-time với `sha256(appId + rawBody + timestamp + oaSecretKey)` | Không triển khai webhook | [`SignatureVerifier.php:7-46`](https://github.com/field-vn/zalo/blob/18119e56514a84b9482a2a5bd36895254217ba03/src/Core/Webhook/SignatureVerifier.php#L7-L46): cùng header, prefix và công thức trên raw body; SDK B còn kiểm timestamp trong cửa sổ 300 giây | **Khớp** về header/công thức |
| `Content-Type` webhook thực tế | Controller hiện không đặt `consumes`, nhận raw bytes với mọi content type (`ZaloWebhookController.java:24-28`) | Không triển khai webhook | [`WebhookController.php:30-37`](https://github.com/field-vn/zalo/blob/18119e56514a84b9482a2a5bd36895254217ba03/src/Laravel/Http/Controllers/WebhookController.php#L30-L37) cũng không ràng buộc content type, chỉ đọc raw body rồi decode JSON | **Chưa rõ** content type Zalo thực gửi; cách nhận mềm dẻo hiện tại không lệch SDK B |
| OA ID trong payload webhook theo loại event | Router hiện ưu tiên top-level `oa_id`, nếu thiếu thì thử cả `sender.id` và `recipient.id`; parser gọi trực tiếp lại ưu tiên recipient (`ZaloWebhookRouter.java:53-67,76-86`; `ZaloWebhookParser.java:64-72`) | Không triển khai webhook | [`WebhookEvent.php:7-80`](https://github.com/field-vn/zalo/blob/18119e56514a84b9482a2a5bd36895254217ba03/src/Core/Webhook/WebhookEvent.php#L7-L80): `user_send_*` có OA ở recipient; `user_received_message`/`user_seen_message` có OA ở top-level `oa_id` hoặc sender; follow/unfollow có `oa_id` và `follower.id`; `oa_*` có OA ở sender | **Lệch** ở đường `ZaloWebhookParser.parse(rawBody)` không qua router đối với delivery receipt; router nhiều OA vẫn định tuyến được nếu đúng một ID khớp |
| OA Secret Key thuộc OA hay Zalo App | `ZaloAccountConfig.java:6-11` và `ZaloWebhookRouter.java:38-47` giữ/chọn secret theo từng account/OA, sau khi đã suy ra OA ID | Không triển khai webhook | [`WebhookController.php:80-106`](https://github.com/field-vn/zalo/blob/18119e56514a84b9482a2a5bd36895254217ba03/src/Laravel/Http/Controllers/WebhookController.php#L80-L106) chọn cấu hình app bằng payload `app_id`, rồi dùng một `webhook_secret` của app; comment của verifier gọi giá trị đó là OA Secret Key | **Chưa rõ**: mô hình hiện tại lệch SDK B nhưng SDK B không phải đặc tả; chưa nên đổi public API chỉ từ một nguồn |
| Mã lỗi refresh token hết hạn, bị thu hồi hoặc đã dùng | `HttpZaloOAuthClient.java:23-24,63-69` tạm coi `-14014` là cần cấp quyền lại | [`ZaloResponse.php:153-173`](https://github.com/zaloplatform/zalo-php-sdk/blob/6bacb1bea670ce1fa9f1ae616bbbfd92742e20aa/src/ZaloResponse.php#L153-L173) chỉ chuyển mọi `error < 0` thành exception chung; không lập bảng mã refresh | [`OAuthClient.php:67-86`](https://github.com/field-vn/zalo/blob/18119e56514a84b9482a2a5bd36895254217ba03/src/Core/Auth/OAuthClient.php#L67-L86) chỉ báo refresh thất bại khi thiếu `access_token`; không phân biệt mã hết hạn/đã dùng. Catalog `-216/-220` của SDK B là lỗi **access token**, không chứng minh mã refresh | **Chưa rõ**; không SDK nào xác nhận `-14014` hoặc mã riêng cho refresh token đã dùng |

## Lệch cần work package implementer xem xét

Không sửa production trong work package đối chiếu này. Các vị trí dưới đây là đầu vào cho task implementer tiếp theo:

1. `vietnam-zalo-oa-core/src/main/java/io/github/hoangluongtran0309/vietnam/zalo/ZaloConsultationTextRequest.java:11-13`
   đang dùng UTF-16 unit. Cân nhắc đổi sang `text.codePointCount(0, text.length())` và thêm vector biên gồm emoji ngoài
   BMP. Chỉ xóa TODO sau khi có capture biên hoặc tài liệu chính thức.
2. `vietnam-zalo-oa-core/src/main/java/io/github/hoangluongtran0309/vietnam/zalo/ZbsPhoneTemplateRequest.java:58-68`
   và `HttpZaloZbsClient.java:38-46` đang phát boolean/number dưới dạng JSON native, trong khi SDK B ép toàn bộ
   `template_data` thành string. Việc đổi kiểu có thể là breaking change; cần template/capture thật trước khi sửa.
3. `vietnam-zalo-oa-core/src/main/java/io/github/hoangluongtran0309/vietnam/zalo/HttpZaloZbsClient.java:38-46`
   không gửi `mode`, còn `ZbsPhoneTemplateRequest.java:26-28` bắt buộc `trackingId`; SDK B làm ngược lại ở hai điểm
   này. Cần xác minh tài liệu ZBS hiện hành trước khi đổi request record.
4. `vietnam-zalo-oa-core/src/main/java/io/github/hoangluongtran0309/vietnam/zalo/ZaloWebhookParser.java:64-72`
   ưu tiên recipient cho mọi event khi không có `oa_id`. Với `user_received_message` và `user_seen_message`, SDK B
   xác định sender mới là OA. Nên suy ra theo `event_name`, đồng thời sửa fixture
   `vietnam-zalo-oa-core/src/test/resources/zalo/webhook/seen.json:1` đang đặt user ở sender và OA ở recipient.

Không đưa mô hình secret theo App vào danh sách sửa ngay: chỉ SDK B làm vậy, SDK A không có webhook, nên bằng chứng
chưa đủ để thay đổi `ZaloAccountConfig` hoặc ADR-0008.

## TODO vẫn chưa xác minh

- `TODO(v0.2): verify` mã lỗi refresh token hết hạn, bị thu hồi hoặc đã dùng. Cần tài liệu OAuth OA v4 hiện hành
  hoặc response đã redact; `-14014` chưa được hai SDK xác nhận.
- `TODO(v0.2): verify` content type webhook thực tế. SDK B chỉ chứng minh nên nhận mềm dẻo, không chứng minh header
  Zalo gửi.
- `TODO(v0.2): verify` OA Secret Key thuộc từng OA hay dùng chung theo Zalo App. SDK B nghiêng về theo App nhưng chưa
  đủ để chốt.
- Cần capture biên 2.000 ký tự có code point ngoài BMP để chốt phép đếm.
- Cần gửi thử ZBS bằng template thật để chốt `mode`, tính tùy chọn của `tracking_id`, và kiểu wire của từng
  `template_data`.
- Cần capture tối thiểu `user_send_text`, follow, unfollow, `user_received_message` và `user_seen_message` để chốt
  vị trí OA ID theo từng event. Bảng trên là bằng chứng gián tiếp từ SDK B, chưa phải capture của dự án.

## Test vector nên thêm sau khi đặc tả được chốt

- OAuth response có `expires_in: "90000"`; giữ thêm vector number để bảo đảm parser vẫn tương thích rộng.
- Text đúng 2.000 code point nhưng dài hơn 2.000 UTF-16 unit, và text 2.001 code point.
- ZBS phone đầu vào `0987654321`, `84987654321`, `+84987654321` đều phát cùng wire value `84987654321`.
- ZBS `template_data` chứa string, integer và boolean, với assertion trên JSON wire đã được capture xác nhận.
- Webhook từng loại event ở trên, cả trường hợp có và không có top-level `oa_id`; OA/user phải nằm đúng phía theo
  event.
- Refresh OAuth trả từng mã lỗi đã capture; chỉ mã chắc chắn không thể phục hồi mới ánh xạ sang
  `ZaloReauthorizationRequiredException`.
