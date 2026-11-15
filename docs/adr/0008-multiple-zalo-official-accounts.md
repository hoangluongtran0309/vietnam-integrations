# ADR-0008: Hỗ trợ nhiều Zalo Official Account

**Trạng thái:** Accepted

## Bối cảnh

Một ứng dụng có thể quản lý nhiều Official Account (OA), ví dụ một OA chăm sóc khách hàng và một OA marketing.
Các OA có thể cùng thuộc một Zalo App hoặc thuộc các App khác nhau. API hiện tại tạo một `ZaloTokenManager`,
`ZaloOaClient` và `ZaloZbsClient` gắn với một `oaId`, nên ứng dụng phải tự tổ chức các bộ client nếu có nhiều OA.

Token store đã khóa và lưu token theo `oaId`; schema PostgreSQL hiện tại vì vậy không cần thay đổi. Refresh token
chỉ dùng được một lần, nên mọi account vẫn phải dùng lại cơ chế atomic refresh và lock timeout đã chốt trong
[ADR-0007](0007-atomic-zalo-token-rotation.md).

## Quyết định

1. Core cung cấp `ZaloOaRegistry`, độc lập framework, để tra account theo alias hoặc `oaId`. Mỗi `ZaloOa` có
   `ZaloTokenManager`, `ZaloOaClient` và `ZaloZbsClient` riêng; các account dùng chung một `ZaloTokenStore` vẫn được
   cô lập bằng `oaId`.
2. `ZaloAccountConfig` giữ alias, `appId`, OAuth `secretKey`, `oaId` và webhook `oaSecretKey`. Alias phải khớp
   `[a-z0-9-]{1,32}`; alias và `oaId` không được trùng trong một registry. Các secret luôn được che khi gọi
   `toString()`.
3. OAuth `secretKey` thuộc Zalo App. Registry chỉ tạo một `ZaloOAuthClient` cho mỗi `appId`; các account cùng
   `appId` phải dùng cùng `secretKey`, nếu không registry fail-fast.
4. Builder nhận HTTP client, endpoint OAuth/OA/ZBS, request timeout, lock timeout và `Clock`. Lock timeout phải lớn
   hơn request timeout, giống invariant của JDBC token store. Registry không retry refresh token hoặc gửi tin.
5. Danh sách account giữ nguyên thứ tự cấu hình và là bất biến. Registry tự chọn account duy nhất làm mặc định;
   khi có nhiều account, ứng dụng có thể chỉ định alias mặc định.
6. Starter sẽ hỗ trợ map `accounts`, đồng thời chuyển cấu hình một OA cũ thành account alias `default`. Khi có một
   account hoặc có account mặc định, starter tiếp tục cung cấp các bean đơn `ZaloTokenManager`, `ZaloOaClient` và
   `ZaloZbsClient` để giữ tương thích cho ứng dụng một OA.
7. Webhook nhiều OA sẽ xác định account từ payload trước, sau đó mới kiểm chữ ký bằng `oaSecretKey` của account đó.

## Phương án đã cân nhắc

- **Một bộ bean cho mỗi OA và phân biệt bằng qualifier:** bám sát Spring nhưng buộc code nghiệp vụ biết tên bean,
  khó tra account động theo `oaId`, và không dùng được trong Java thuần. Registry vẫn cho starter tạo các bean tiện
  ích cho account mặc định.
- **Một prefix property riêng cho mỗi OA:** đơn giản khi số OA cố định nhưng lặp cấu hình, không giữ được thứ tự một
  cách rõ ràng và khó bind số account thay đổi. Map `accounts` biểu diễn trực tiếp alias và cho phép kế thừa cấu hình
  App ở cấp gốc.

## Hệ quả

- Ứng dụng có một API thống nhất để chọn OA theo alias hoặc theo sự kiện chứa `oaId`.
- Account cùng App dùng ít OAuth client hơn nhưng vẫn có token manager và messaging client riêng.
- Cấu hình sai do trùng định danh, xung đột App secret hoặc timeout không hợp lệ bị phát hiện lúc build registry.
- Public API core tăng thêm `ZaloAccountConfig`, `ZaloOa` và `ZaloOaRegistry`; các constructor public hiện tại của
  token manager và HTTP client được giữ nguyên.
- Webhook và auto-configuration nhiều OA được triển khai ở work package kế tiếp; registry chỉ chuẩn bị hợp đồng core.

## Chưa xác minh

- `TODO(v0.2): verify OA Secret Key dùng để ký webhook thuộc từng OA hay dùng chung theo Zalo App — cần đối chiếu
  tài liệu chính thức Zalo OA và capture thật.` Vì chưa có xác nhận, mỗi account giữ `oaSecretKey` riêng và nhiều
  account được phép cấu hình cùng một giá trị; mô hình này hỗ trợ cả hai trường hợp mà không đổi API.
