# VietQR

Tạo và đọc mã VietQR (chuẩn NAPAS dựa trên EMVCo Merchant-Presented Mode) cho chuyển khoản nhanh NAPAS 247.

> Trang này đang được hoàn thiện trong Phase 1. Các trang con:
>
> - [Danh sách ngân hàng (BIN)](banks.md)
> - [Render ảnh QR (PNG, màu, logo)](rendering.md)

## Nội dung chuyển khoản (`purpose`)

`purpose` là nội dung chuyển khoản điền sẵn cho người trả tiền. Nó nằm ở tag `62` → sub-tag `08` của payload.
Không bắt buộc: bỏ trống (`null`) thì payload không có tag `62`.

### Thư viện làm gì với `purpose`

`VietQrEncoder` chuẩn hóa nội dung bằng `PurposeNormalizer` trước khi mã hóa, vì nhiều app ngân hàng hiển thị sai
ký tự ngoài ASCII:

1. `đ`/`Đ` → `d`/`D`.
2. Tách dấu (Unicode NFD) rồi bỏ toàn bộ dấu.
3. Bỏ mọi ký tự ngoài ASCII in được (`0x20`–`0x7E`): emoji, `№`, gạch ngang dài `–`, …
4. Bỏ khoảng trắng đầu/cuối, gộp nhiều khoảng trắng thành một.

Ví dụ (kết quả thật của `PurposeNormalizer.normalize`):

| Đầu vào | Sau chuẩn hóa | Độ dài |
|---|---|---|
| `Thanh toán đơn hàng #DH12345` | `Thanh toan don hang #DH12345` | 28 |
| `ĐẶT CỌC  phòng   302` | `DAT COC phong 302` | 17 |
| `  Mua 2 ly cà phê ☕ ` | `Mua 2 ly ca phe` | 15 |
| `Nguyễn Văn A chuyển tiền` | `Nguyen Van A chuyen tien` | 24 |
| `Đơn №15 – giao nhanh` | `Don 15 giao nhanh` | 17 |
| `Hóa đơn: HD-2026/001` | `Hoa don: HD-2026/001` | 20 |

Nếu sau chuẩn hóa nội dung rỗng (ví dụ chỉ toàn emoji), payload sẽ không có tag `62`.

### Giới hạn độ dài

| Giới hạn | Giá trị | Ý nghĩa |
|---|---|---|
| Cứng (thư viện kiểm tra) | **95** ký tự — `VietQrEncoder.MAX_PURPOSE_LENGTH` | Dài hơn → `VietQrException`. Đây là giới hạn cấu trúc: tag `62` tối đa 99 ký tự, trừ 4 ký tự header của sub-tag `08`. |
| Khuyến nghị | **≤ 25** ký tự | Giới hạn của sub-tag `08` trong đặc tả EMVCo MPM v1.1 (bảng 3.7). App ngân hàng có thể cắt hoặc từ chối nội dung dài hơn — đang được đo, xem [ADR-0005](../adr/0005-purpose-length-policy.md). |

Độ dài được **đếm sau khi chuẩn hóa**, nên chữ có dấu không làm nội dung "dài hơn".

Khi vượt giới hạn cứng, encoder **báo lỗi chứ không tự cắt** (để không làm mất mã đơn hàng dùng cho đối soát):

```text
io.github.hoangluongtran0309.vietnam.vietqr.VietQrException:
    purpose is 96 characters after normalization; the maximum is 95
```

Muốn kiểm tra trước khi sinh QR:

```java
String normalized = PurposeNormalizer.normalize(purpose);
if (normalized != null && normalized.length() > VietQrEncoder.MAX_PURPOSE_LENGTH) {
    // rút gọn nội dung, hoặc báo lỗi cho người dùng
}
```

### Khuyến nghị khi đặt nội dung

- **Đặt mã đơn hàng ở đầu**: `DH12345 Thanh toan` tốt hơn `Thanh toan don hang so DH12345` — nếu có app cắt nội dung
  thì mã đơn vẫn còn.
- **Giữ ≤ 25 ký tự** cho tới khi có số liệu đo trên app thật.
- Mã đơn nên chỉ gồm chữ và số. Ký tự đặc biệt (`#`, `/`, `:`) vẫn được mã hóa, nhưng chưa xác nhận mọi app
  ngân hàng giữ nguyên chúng.
- Khi đối soát, so khớp theo kiểu "nội dung **chứa** mã đơn" thay vì so bằng tuyệt đối: nội dung người nhận thấy
  có thể khác nội dung trong QR nếu người trả tiền sửa lại trước khi chuyển.
