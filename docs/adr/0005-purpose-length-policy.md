# ADR-0005: Giới hạn độ dài nội dung chuyển khoản (`purpose`)

**Trạng thái:** Proposed — giới hạn khuyến nghị sẽ chốt sau khi đo trên app ngân hàng thật
([manual-verification.md](../vietqr/manual-verification.md)).

## Bối cảnh

- Nội dung chuyển khoản nằm ở tag `62` (Additional Data Field Template), sub-tag `08` (Purpose of Transaction).
- Theo cấu trúc TLV, giá trị tag `62` tối đa 99 ký tự; sub-tag `08` tốn 4 ký tự cho ID + độ dài, nên nội dung
  dài nhất có thể mã hóa là **95** ký tự.
- Đặc tả **EMVCo MPM v1.1, bảng 3.7** giới hạn sub-tag `08` ở **25** ký tự (`ans`, `var. up to "25"`).
  Đặc tả NAPAS cho VietQR dựa trên EMVCo nhưng chưa được đối chiếu ở điểm này.
- Thực tế nhiều hệ thống dùng nội dung dài hơn 25 (ví dụ `Thanh toan don hang #DH12345` = 28 ký tự). Chưa biết
  các app ngân hàng xử lý nội dung > 25 thế nào: hiển thị đủ, tự cắt, hay từ chối QR.
- Trước đây, nội dung > 95 ký tự gây lỗi khó hiểu từ tầng TLV: `Value of tag 62 exceeds 99 characters`.
- Nội dung thường chứa **mã đơn hàng dùng để đối soát**. Nếu bị cắt mất, tiền vào nhưng không khớp được đơn.

## Quyết định

1. **Từ chối, không cắt.** Nội dung sau khi chuẩn hóa dài hơn giới hạn → `VietQrException` nêu rõ độ dài thực tế
   và giới hạn: `purpose is 96 characters after normalization; the maximum is 95`. Thư viện không bao giờ tự cắt
   nội dung, vì có thể làm mất mã đơn.
2. **Đếm sau khi chuẩn hóa** (`PurposeNormalizer`: bỏ dấu, bỏ ký tự ngoài ASCII, gộp khoảng trắng). Người dùng
   truyền chuỗi có dấu không bị phạt vì ký tự tổ hợp.
3. **Giới hạn cứng hiện tại là 95** (`VietQrEncoder.MAX_PURPOSE_LENGTH`) — giới hạn cấu trúc, đảm bảo không bao
   giờ sinh payload sai định dạng. Chưa hạ xuống 25 vì sẽ làm hỏng code hiện có mà chưa có bằng chứng là app
   ngân hàng không đọc được nội dung dài hơn.
4. **Docs khuyến nghị ≤ 25 ký tự** (theo EMVCo) và đặt mã đơn ở đầu nội dung cho tới khi có số liệu đo.
5. Sau khi đo (TODO(v0.1) trong `VietQrEncoder`): nếu có app phổ biến cắt/từ chối nội dung > 25, cân nhắc hạ
   `MAX_PURPOSE_LENGTH` (breaking change, ghi `CHANGELOG.md`) hoặc thêm chế độ kiểm tra chặt. Cập nhật ADR này
   sang **Accepted** kèm bảng số liệu.

## Hệ quả

- Lỗi xuất hiện ngay khi sinh QR (ở server), không phải khi khách quét — dễ phát hiện hơn.
- Người dùng cần tự rút gọn nội dung dài; docs hướng dẫn cách làm.
- Giá trị `MAX_PURPOSE_LENGTH` là API công khai: người dùng có thể kiểm tra trước khi gọi encoder.
- Có thể phải thay đổi giới hạn trước 1.0, tùy kết quả đo.
