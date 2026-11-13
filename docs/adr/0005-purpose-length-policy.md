# ADR-0005: Giới hạn độ dài nội dung chuyển khoản (`purpose`)

**Trạng thái:** Accepted — chấp nhận ngày 2026-11-13.

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
4. **Docs khuyến nghị ≤ 25 ký tự** (theo EMVCo) và đặt mã đơn ở đầu nội dung để giảm rủi ro bị cắt.
5. Giữ giới hạn cứng 95 và khuyến nghị 25 như chính sách ổn định cho 0.1.0. Quyết định này ưu tiên không phá vỡ
   trường hợp sử dụng hiện có, đồng thời đưa ra mức tương thích bảo thủ theo EMVCo. Nếu sau này có bằng chứng từ
   app phổ biến rằng nội dung dài hơn 25 bị cắt hoặc từ chối, thay đổi giới hạn phải được xem là breaking change và
   ghi trong `CHANGELOG.md`.

## Hệ quả

- Lỗi xuất hiện ngay khi sinh QR (ở server), không phải khi khách quét — dễ phát hiện hơn.
- Người dùng cần tự rút gọn nội dung dài; docs hướng dẫn cách làm.
- Giá trị `MAX_PURPOSE_LENGTH` là API công khai: người dùng có thể kiểm tra trước khi gọi encoder.
- Giới hạn cứng có thể thay đổi trước 1.0 nếu có dữ liệu tương thích mới; mọi thay đổi phải được công bố rõ.
