# ADR-0004: Lưu token Zalo OA qua port `ZaloTokenStore`

**Trạng thái:** Accepted

## Bối cảnh
Refresh token của Zalo chỉ dùng được một lần. Nếu hai instance cùng refresh, một instance sẽ làm token của
instance kia mất hiệu lực → OA mất kết nối, phải cấp quyền lại thủ công. Đây là lỗi phổ biến nhất khi tự tích hợp.

## Quyết định
- Port `ZaloTokenStore`; mặc định `InMemoryZaloTokenStore` (chỉ an toàn khi chạy 1 instance).
- `ZaloTokenManager` refresh kiểu single-flight trong JVM (lock + double-check).
- `JdbcZaloTokenStore` dùng `SELECT ... FOR UPDATE` để chỉ một instance refresh; chi tiết tại
  [ADR-0007](0007-atomic-zalo-token-rotation.md).
- Lưu token mới **trước** khi dùng.

## Hệ quả
Người dùng chạy nhiều instance phải chọn `token-store: jdbc` hoặc tự cung cấp bean. Docs phải cảnh báo rõ.
