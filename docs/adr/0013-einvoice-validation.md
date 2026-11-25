# ADR-0013 — Báo cáo kiểm tra hóa đơn điện tử

**Trạng thái** — Accepted, ngày 2026-11-25.

## Bối cảnh

Read model giữ các trường thiếu và số tiền gốc để ứng dụng có thể đọc lại hóa đơn đã phát hành. Các nguồn
đã đọc chưa xác định cách làm tròn chung hay ngữ nghĩa đầy đủ của tính chất dòng và khoản thu mở rộng.
Xem [ADR-0011](0011-einvoice-read-model.md) và [cơ sở pháp lý](../einvoice/legal-basis.md).

## Quyết định

- Trả `ValidationReport` bất biến để bên gọi xem toàn bộ issue theo thứ tự xác định. Một issue có mã, mức độ,
  đường dẫn XML và thông điệp tiếng Anh. Message mặc định chỉ mô tả điều kiện kiểm tra và không chứa giá trị
  hóa đơn. Exception của rule ứng dụng được truyền về bên gọi.
- So sánh số tiền bằng `BigDecimal`. Ứng dụng cấu hình dung sai tuyệt đối và mặc định chấp nhận mức sai lệch
  đến một đơn vị tiền tệ với issue `INFO`. Dung sai là chính sách của ứng dụng.
- Báo `INFO` khi bỏ qua số học do tính chất dòng chưa xác minh hoặc bỏ qua kiểm tổng thanh toán khi phần mở
  rộng có thể chứa khoản thu chưa được mô hình hóa.
- Giữ kiểm tra nghiệp vụ ở core và cho phép ứng dụng bổ sung rule theo thứ tự đăng ký.

## Hệ quả

Ứng dụng có thể hiển thị nhiều vấn đề từ một lần kiểm tra và chọn cách xử lý `WARNING` hoặc `INFO`. Mã và
đường dẫn cho phép tổng hợp kết quả kiểm mẫu mà vẫn giữ giá trị hóa đơn ngoài log. Việc xác minh chữ ký,
XSD và điều kiện theo từng loại hóa đơn chuyên ngành thuộc các bước sau.
