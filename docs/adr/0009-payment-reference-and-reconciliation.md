# ADR-0009: Mã tham chiếu và đối soát thanh toán VietQR

**Trạng thái:** Accepted — chấp nhận ngày 2026-11-16.

## Bối cảnh

Nội dung chuyển khoản mà ngân hàng ghi nhận có thể khác `purpose` trong mã QR: người trả có thể sửa nội dung, ngân
hàng có thể bỏ dấu, đổi chữ hoa/thường hoặc thêm tiền tố/hậu tố. So sánh toàn bộ nội dung vì thế không đủ tin cậy để
xác định khoản phải thu.

ADR-0006 đã loại phương án để thư viện tự khớp giao dịch với đơn hàng vì mô hình đơn hàng và cách lưu trạng thái thuộc
ứng dụng. Tuy nhiên, core vẫn có thể cung cấp hai cơ chế thuần, không giữ trạng thái: mã tham chiếu có ký tự kiểm tra và
chính sách phân loại kết quả dựa trên dữ liệu do ứng dụng cung cấp.

## Quyết định

### Phân chia trách nhiệm

- Thư viện sinh, kiểm tra và trích mã tham chiếu từ `PaymentConfirmation.content()`. Ở bước tiếp theo, một hàm đối
  soát thuần sẽ phân loại giao dịch dựa trên khoản phải thu mà ứng dụng trả về.
- Ứng dụng sở hữu mô hình đơn hàng/khoản phải thu, triển khai lookup, cập nhật trạng thái và chống xử lý trùng bằng
  `PaymentConfirmation.idempotencyKey()`.
- Core không ghi dữ liệu, không giữ trạng thái và không có database. Vì lookup đi từ ứng dụng vào thư viện, thiết kế
  này không đưa mô hình đơn hàng trở lại core và không mâu thuẫn với ADR-0006.

### Định dạng mã tham chiếu

`PaymentReferenceFormat.of(prefix)` tạo mã theo cấu trúc `prefix + body + checkCharacter`:

- `prefix`: 2–6 chữ cái ASCII hoa `A–Z`, do ứng dụng chọn để nhận diện không gian mã của mình.
- `body`: 1–20 ký tự `A–Z0–9`; `encode` tự đổi chữ thường ASCII thành chữ hoa.
- `checkCharacter`: một ký tự Luhn mod N với `N = 36`.
- Toàn bộ mã dài tối đa 25 ký tự, khớp mức tương thích bảo thủ của `purpose` trong ADR-0005. Với prefix dài, giới hạn
  25 ký tự có thể làm độ dài body thực tế nhỏ hơn 20.

Bảng ký tự và giá trị dùng cho Luhn mod N:

```text
Ký tự:  0 1 2 3 4 5 6 7 8 9 A B C D E F G H I J K L M N O P Q R S T U V W X Y Z
Giá trị: 0 1 2 3 4 5 6 7 8 9 10 11 12 13 14 15 16 17 18 19 20 21 22 23 24 25
         26 27 28 29 30 31 32 33 34 35
```

Tính từ phải sang trái trên `prefix + body`, nhân luân phiên với 2 và 1. Với mỗi tích `p`, cộng
`floor(p / 36) + (p mod 36)`. Giá trị kiểm tra là `(36 - (tổng mod 36)) mod 36`, rồi ánh xạ ngược qua bảng trên.
Ví dụ `DH12345` có tổng 67 nên ký tự kiểm tra là `5`; mã hoàn chỉnh là `DH123455`.

`extract` bỏ dấu bằng `PurposeNormalizer`, đổi sang chữ hoa, xem mọi ký tự ngoài `A–Z0–9` là dấu tách và kiểm tra mọi
vị trí bắt đầu bằng prefix. Mã vẫn được tìm khi dính với chữ/số ở trước hoặc sau. Tại một vị trí, mọi độ dài body hợp
lệ đều được thử; nếu nhiều độ dài cùng có checksum đúng thì trả về tất cả để tầng đối soát phân loại là nhiều mã. Mã
trùng được loại bỏ và thứ tự xuất hiện đầu tiên được giữ nguyên.

### Giới hạn đã biết

Luhn mod N phát hiện mọi lỗi thay một ký tự và hầu hết lỗi đảo hai ký tự kề nhau, nhưng không phát hiện được phép đảo
`0` với ký tự cuối bảng (`Z`). Đây là thuộc tính đã biết của thuật toán và được khóa bằng test. Mã tham chiếu không phải
chữ ký hay cơ chế chống giả mạo; ứng dụng vẫn phải kiểm tra tài khoản nhận và số tiền.

## Phương án đã cân nhắc

- **Không có ký tự kiểm tra:** ngắn hơn một ký tự nhưng dễ nhận nhầm chuỗi giống mã đơn trong phần ngân hàng thêm vào.
- **Chỉ dùng số:** thuận tiện nhập tay nhưng làm giảm không gian mã và không phù hợp mã đơn chữ-số phổ biến.
- **CRC hoặc hash dài hơn:** phát hiện lỗi tốt hơn nhưng chiếm thêm ngân sách 25 ký tự; Luhn mod N đủ cho lỗi nhập liệu
  và dễ triển khai độc lập framework.
- **Thư viện sở hữu repository/trạng thái đơn:** bị loại vì ghép core với mô hình và hạ tầng của ứng dụng, đúng lý do
  đã nêu trong ADR-0006.

## Hệ quả

- Ứng dụng có mã ngắn, dễ nhận diện và có thể trích từ nội dung bị ngân hàng định dạng lại.
- Có thể xuất hiện nhiều ứng viên checksum hợp lệ trong một chuỗi dính liền; thư viện giữ lại tất cả thay vì tự đoán.
- Thêm public API `PaymentReferenceFormat` vào core; thay đổi hợp đồng này trước 1.0 phải được ghi trong changelog.
- Việc lưu idempotency key và cập nhật khoản phải thu trong cùng transaction vẫn là trách nhiệm của ứng dụng.
