# ADR-0006: SPI xác nhận thanh toán VietQR

**Trạng thái:** Accepted (interface); adapter dự kiến ở 0.4

## Bối cảnh

- Mã VietQR chỉ là dữ liệu hiển thị. Việc chuyển tiền diễn ra giữa app của người trả và ngân hàng; thư viện và ứng dụng
  của merchant **không được báo** khi tiền về.
- Các dịch vụ như SePay, Casso theo dõi tài khoản của merchant và thông báo cho merchant khi có giao dịch (webhook).
  Mỗi dịch vụ có định dạng, cách xác thực và cơ chế gửi lại riêng.
- Roadmap Phase 1 yêu cầu **chỉ thiết kế interface**, adapter làm ở 0.4. Interface phải đủ ổn định để adapter sau này
  không phải đổi code nghiệp vụ của người dùng.
- Core không được phụ thuộc Spring (ADR-0002), nên không thể dùng `ApplicationEvent` làm hợp đồng chính.

## Quyết định

1. Trong `vietnam-vietqr-core`, package `io.github.hoangluongtran0309.vietnam.vietqr.confirmation`:
   - `PaymentConfirmation` (record): một giao dịch **tiền vào**, độc lập với nhà cung cấp — `provider`, `transactionId`,
     `bankBin` (có thể `null`), `accountNumber`, `amount` (VND, > 0), `content`, `occurredAt`, `attributes`.
   - `PaymentConfirmationListener` (`@FunctionalInterface`): `void onPaymentConfirmed(PaymentConfirmation)`.
2. Hợp đồng ghi trong Javadoc của listener:
   - **At-least-once**: cùng một giao dịch có thể đến nhiều lần → listener phải idempotent theo
     `idempotencyKey()` = `provider:transactionId`. `provider` không được chứa `:` để khóa không bị trùng giữa
     hai giao dịch khác nhau (ví dụ `("a:", "b")` và `("a", ":b")`).
   - **Chỉ tiền vào**: adapter lọc bỏ giao dịch tiền ra.
   - **Giao dịch không khớp đơn là bình thường**: ghi lại để xử lý tay, không ném exception.
   - **Ném exception = xử lý thất bại**: adapter báo lỗi cho nhà cung cấp để gửi lại (nếu nhà cung cấp hỗ trợ).
   - Có thể bị gọi đồng thời từ nhiều thread.
3. `content` không bao giờ `null` (chuỗi rỗng nếu không có) và `attributes` là bản sao bất biến, để code đối soát không
   phải kiểm tra `null`.
4. `attributes` là "lối thoát" cho dữ liệu riêng của từng nhà cung cấp, để không phải thêm field vào record mỗi khi có
   adapter mới.
5. Chưa thêm bean nào vào starter, vì chưa có adapter để nối dây.

Dự kiến cho 0.4 (sẽ chốt bằng ADR riêng):

- Mỗi nhà cung cấp một module adapter (ví dụ `vietnam-vietqr-confirmation-<provider>`): nhận webhook, xác thực,
  chuyển thành `PaymentConfirmation`, gọi listener.
- Starter: controller webhook tùy chọn, gọi **mọi** bean `PaymentConfirmationListener` một cách **đồng bộ** để trả lỗi
  cho nhà cung cấp khi listener thất bại; có thể publish thêm `ApplicationEvent`.
- `TODO(v0.4): verify` các trường webhook của SePay và Casso ánh xạ được vào `PaymentConfirmation`, cách xác thực
  webhook, và cơ chế gửi lại của từng bên. **Chưa đối chiếu với tài liệu của các nhà cung cấp.**

## Phương án đã cân nhắc

- **Chỉ dùng `ApplicationEvent` của Spring**: tiện cho người dùng Spring nhưng buộc core phụ thuộc Spring; vẫn có thể
  thêm ở starter như một lớp bọc.
- **Thư viện tự khớp giao dịch với đơn hàng** (`PaymentMatcher`): mô hình đơn hàng là của ứng dụng; cách khớp
  (theo mã đơn, số tiền, thời gian) quá khác nhau giữa các hệ thống. Docs đưa ví dụ thay vì API.
- **Interface kéo dữ liệu (polling)** cho nhà cung cấp chỉ có API tra cứu: để sau, nếu adapter nào cần.

## Hệ quả

- Người dùng có thể viết và unit test listener ngay từ 0.1.0 bằng cách tự tạo `PaymentConfirmation`.
- Thêm component vào record sau này là breaking change với constructor; trước 1.0 được phép nhưng phải ghi
  `CHANGELOG.md`. `attributes` giúp giảm nhu cầu này.
- Field set có thể cần điều chỉnh khi đối chiếu webhook thật ở 0.4.
