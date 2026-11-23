# Checklist capture Zalo thật

Chế độ capture của demo ghi lại trao đổi HTTP với Zalo và webhook nhận vào sau khi đã redact. Capture chỉ dùng để
đối chiếu hợp đồng thực tế; không chứng minh một hành vi chưa quan sát là đặc tả ổn định. Không gọi Zalo bằng dữ
liệu của khách hàng thật.

## Chuẩn bị

- Một Zalo App đã kích hoạt, một OA doanh nghiệp đã liên kết và các quyền OA cần kiểm tra. Nếu kiểm nhiều OA, chuẩn
  bị hai OA có thể phân biệt rõ.
- ZBS Account có quyền gửi qua số điện thoại, một template test đã duyệt và tracking ID mới cho mỗi lần gửi.
- Số điện thoại test do người thực hiện kiểm soát và một test user đã tương tác với OA, đủ điều kiện nhận tin Tư vấn.
- Hai URL HTTPS công khai qua tunnel: callback OAuth trỏ tới `/zalo/oauth/callback`, webhook trỏ tới
  `/webhooks/zalo`. Không đưa credential vào URL hoặc lịch sử shell.
- PostgreSQL đã áp dụng migration `V1__create_zalo_oa_tokens.sql`; demo phải chạy với `ZALO_TOKEN_STORE=jdbc` để
  kiểm cả vòng đời refresh token thật.
- Tạo một thư mục capture nằm hoàn toàn ngoài repository và mọi thư mục cha chứa `.git`, ví dụ
  `/var/tmp/zalo-capture-review`. Đặt `ZALO_CAPTURE_DIR` tới thư mục này. Demo sẽ từ chối khởi động nếu thư mục nằm
  trong cây Git.
- Bật `ZALO_ENABLED=true`, `ZALO_WEBHOOK_ENABLED=true`, cung cấp App/OA credential, JDBC và URL callback HTTPS.
  Không bật DEBUG/TRACE cho HTTP hoặc Spring Web trong phiên capture.

Sau khi khởi động, xác nhận có thư mục phiên dạng `yyyy-MM-ddTHHmmss-session` và `session.json`. Không gửi file
capture qua chat, email hoặc hệ thống ticket trước khi hoàn tất bước duyệt thủ công ở cuối tài liệu.

## Kịch bản tối thiểu

Mỗi thao tác phải dùng dữ liệu test riêng để dễ nhận biết capture tương ứng. Cột cuối để trống cho người chạy điền.

| # | Bước thao tác | File capture mong đợi | Câu hỏi được trả lời | Ngày giờ / Kết quả |
|---:|---|---|---|---|
| 1 | Mở `/zalo/oauth/start` để bắt đầu cấp quyền. Sau khi Zalo chuyển về callback, hoàn tất code exchange bằng authorization code mới; riêng endpoint `start` chỉ redirect nên không sinh capture HTTP tới Zalo. | `http-POST-v4-oa-access-token` cho code exchange. | Tên form/header thực tế; kiểu JSON của `expires_in`; code exchange thành công trả field token nào. | |
| 2 | Nếu sẽ chạy cả bước 3, trước bước này chạy `CREATE TABLE zalo_capture_backup AS SELECT * FROM zalo_oa_tokens;`. Ép access token hết hạn bằng `UPDATE zalo_oa_tokens SET access_token_expires_at = now() - interval '1 minute' WHERE oa_id = '<oa id>';`, rồi gọi một thao tác cần access token. | `http-POST-v4-oa-access-token` có `grant_type=refresh_token`. | Kiểu `expires_in`; refresh trả access/refresh token theo field nào. | |
| 3 | Sau khi bước 2 refresh thành công, đưa refresh token đã dùng trở lại mà không lấy token ra khỏi database: chạy `UPDATE zalo_oa_tokens t SET refresh_token = b.refresh_token, access_token_expires_at = now() - interval '1 minute' FROM zalo_capture_backup b WHERE t.oa_id = b.oa_id;`. Gọi một thao tác cần access token để kích hoạt refresh, ghi nhận lỗi và không retry, rồi chạy `DROP TABLE zalo_capture_backup;`. Sau bước này OA phải cấp quyền lại qua `/zalo/oauth/start`. | `http-POST-v4-oa-access-token` có response lỗi. | Mã `error`, HTTP status và `message` khi refresh token đã dùng/hết hạn/bị thu hồi. | |
| 4 | Gửi một tin Tư vấn text hợp lệ tới test user. | `http-POST-v3-0-oa-message-cs`. | Field message ID và `sent_time` của OA; request recipient/text thực tế. | |
| 5 | Gửi tin Tư vấn tới user ID test không hợp lệ hoặc không đủ điều kiện. | `http-POST-v3-0-oa-message-cs` có response lỗi. | HTTP status, `error`, `message` và cấu trúc lỗi OA. | |
| 6 | Gửi ZBS bằng template đã duyệt, số test và template data gồm ít nhất một string, một number. | `http-POST-message-template`. | Định dạng phone ZBS chấp nhận; kiểu value trong `template_data`; field `msg_id`/`message_id` và `sent_time`. | |
| 7 | Gửi ZBS với template ID sai trong tài khoản test. | `http-POST-message-template` có response lỗi. | Cấu trúc/mã lỗi template và field ID trong response lỗi hoặc thành công. | |
| 8 | Test user gửi text vào OA. | `webhook-user-send-text`. | `Content-Type`; OA ID nằm ở sender hay recipient; tên field message ID, text và timestamp. | |
| 9 | Test user follow OA. | `webhook-follow`. | Cấu trúc sender/recipient, OA ID, timestamp và header thực tế. | |
| 10 | Test user unfollow OA. | `webhook-unfollow`. | Cấu trúc sender/recipient, OA ID, timestamp và header thực tế. | |
| 11 | Tạo điều kiện để nhận `user_received_message`. | `webhook-user-received-message`. | Tên field message ID, `sent_time`/timestamp và vị trí OA ID. | |
| 12 | Test user xem tin để nhận `user_seen_message`. | `webhook-user-seen-message`. | Tên field message ID, `sent_time`/timestamp và vị trí OA ID. | |
| 13 | Nếu có hai OA, gửi một webhook thật từ mỗi OA vào cùng endpoint. | Hai file webhook, `session.json` có hai alias đã pseudonym. | OA ID định tuyến theo event; chữ ký dùng secret theo OA hay theo App. | |
| 14 | `ZaloConsultationTextRequest` chặn ở 2.000 UTF-16 unit, bảo thủ hơn cách đếm theo code point. Gửi một tin Tư vấn có emoji ngoài BMP và dài đúng 2.000 UTF-16 unit để xác nhận Zalo chấp nhận. | `http-POST-v3-0-oa-message-cs`. | Zalo có chấp nhận request ở đúng giới hạn an toàn mà client đang áp dụng không. | |

Không chạy kịch bản 3, 5 hoặc 7 trên dữ liệu production. Mỗi thao tác không idempotent chỉ thực hiện một lần;
nếu timeout sau khi đã gửi thì ghi nhận kết quả không xác định, không tự gửi lại.

## Câu hỏi và kịch bản trả lời

| Câu hỏi cần xác minh | Kịch bản | Dấu hiệu cần đọc trong capture |
|---|---:|---|
| ZBS chấp nhận định dạng số điện thoại nào? | 6 | Request phone đã redact nhưng giữ độ dài/lớp ký tự; đối chiếu input test với status và response. |
| `template_data` có bắt buộc mọi value là string không? | 6 | Metadata `REDACTED_string_*`, `REDACTED_number_*` và kết quả API. |
| Có nên giữ giới hạn 2.000 UTF-16 unit của client không? | 14 | Kết luận dự kiến: giữ giới hạn UTF-16 vì giới hạn này luôn an toàn. Muốn nới theo code point phải có kiểm chứng riêng, ngoài phạm vi lần capture này. |
| OA ID nằm ở sender hay recipient theo từng event? | 8–13 | So sánh pseudonym ổn định của OA trong `session.json` với sender/recipient/`oa_id`. |
| OA Secret Key thuộc từng OA hay dùng chung theo App? | 13 | Kết quả xác thực của hai payload thật với cấu hình secret tương ứng; không ghi secret vào ghi chú. |
| `Content-Type` thực tế Zalo gửi là gì? | 8–13 | Header `Content-Type` nguyên văn, gồm cả charset nếu có. |
| Refresh token hết hạn, bị thu hồi hoặc đã dùng trả mã lỗi nào? | 3 | HTTP status, `error` và root `message`; không suy rộng từ một trường hợp sang mọi trường hợp. |
| `expires_in` là JSON string hay number? | 1, 2 | Kiểu node JSON trong response OAuth. |
| Field message ID và `sent_time` của OA là gì/kiểu gì? | 4, 5 | Response OA và webhook liên quan; kiểm `message_id`, `msg_id`, `sent_time`, `timestamp`. |
| Field message ID và `sent_time` của ZBS là gì/kiểu gì? | 6, 7 | Response ZBS; kiểm `msg_id`, `message_id`, `sent_time`. |

Sau khi tắt demo, đọc `session.json.defaultFields`. Mỗi field xuất hiện ở đây cần được xem lại để quyết định mở rộng
danh sách KEEP/PSEUDONYM/SECRET; không tự coi field chưa phân loại là an toàn.

## Duyệt trước khi đưa capture vào repository

1. Dừng demo để `session.json` được cập nhật danh sách field chưa phân loại.
2. Mở và đọc bằng mắt **từng file**, gồm `session.json`, request, response và webhook. Tìm token, code/PKCE,
   credential, số điện thoại, user/OA/App ID, message ID, tracking ID, text và template value thật.
3. Nếu thấy bất kỳ giá trị thật nào, sửa redactor và test trước, xóa phiên capture lỗi, rồi chạy capture lại. Không
   sửa tay file capture vì sẽ che mất lỗ hổng của công cụ.
4. Xác nhận mọi file là JSON parse được, header chữ ký chỉ còn prefix cùng `REDACTED_SIG_*`, và pseudonym của cùng
   một ID ổn định trong phiên nhưng không dùng để liên kết giữa các phiên.
5. Chỉ sau khi duyệt xong, chép cả tập cần thiết vào
   `vietnam-zalo-oa-core/src/test/resources/zalo/captures/<yyyy-mm-dd>/`. Ghi ngày capture, nguồn App/OA test và
   kịch bản trong metadata không nhạy cảm; tuyệt đối không chép credential hoặc ghi chú nội bộ chứa ID thật.
