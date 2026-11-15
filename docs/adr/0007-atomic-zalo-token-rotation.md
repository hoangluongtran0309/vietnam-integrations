# ADR-0007: Xoay refresh token Zalo atomically trên PostgreSQL

**Trạng thái:** Accepted

## Bối cảnh

OAuth OA v4 cấp access token có hiệu lực 25 giờ và refresh token có hiệu lực 3 tháng. Refresh token chỉ dùng được
một lần; mỗi lần refresh thành công vô hiệu hóa cả refresh token lẫn access token cũ. Lock trong JVM của
`ZaloTokenManager` không bảo vệ được hai application instance cùng refresh.

## Quyết định

- Mở rộng `ZaloTokenStore` bằng `updateAtomically(oaId, updater)`.
- `InMemoryZaloTokenStore` khóa theo OA ID; `JdbcZaloTokenStore` dùng transaction PostgreSQL và
  `SELECT ... FOR UPDATE`.
- Remote refresh được gọi trong critical section. Token mới được commit trước khi access token được trả cho caller.
- Không retry refresh hoặc gửi tin tự động. Nếu kết nối mất sau khi Zalo đã xử lý nhưng trước khi nhận response, hệ
  thống không thể biết refresh token mới; operator phải cấp quyền OAuth lại.
- Schema do người dùng áp dụng bằng migration SQL; starter không tự chạy DDL.

## Hệ quả

Transaction có thể giữ row lock trong thời gian gọi OAuth, nên request timeout mặc định là 10 giây. PostgreSQL là
database duy nhất được cam kết trong 0.2.0. Token được lưu plaintext ở cấp ứng dụng; production phải bật encryption
at rest, giới hạn quyền database hoặc cung cấp `ZaloTokenStore` riêng nếu cần mã hóa từng giá trị.

`JdbcZaloTokenStore` đặt `lock_timeout` cục bộ cho mỗi transaction, mặc định 15 giây. Giá trị này phải lớn hơn HTTP
request timeout vì instance đang giữ row lock có thể chờ Zalo trả lời tới hết request timeout. Khi hết thời gian chờ,
caller nhận `ZaloException` cho biết instance khác đang refresh thay vì chờ vô hạn.

Khi Zalo trả mã đã xác minh là refresh token không còn hợp lệ, OAuth client ném
`ZaloReauthorizationRequiredException`. Lỗi mạng hoặc timeout vẫn là `ZaloApiException` vì kết quả refresh có thể
không xác định và không được tự retry.

ADR này cụ thể hóa phần PostgreSQL đã dự kiến trong [ADR-0004](0004-zalo-token-store.md).
