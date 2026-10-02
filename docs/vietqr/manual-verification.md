# Kiểm tra thủ công bằng app ngân hàng

Test tự động chỉ chứng minh **ZXing** đọc được ảnh và **decoder của thư viện** hiểu payload. Điều người dùng cần biết
là **app ngân hàng thật** quét ra đúng người nhận, số tiền, nội dung. Trang này là quy trình và nơi ghi kết quả cho hai
mục của Phase 1 ([ROADMAP](../ROADMAP.md)):

- Quét QR sinh ra bằng **≥ 3 app ngân hàng** khác nhau.
- **Đo giới hạn độ dài `purpose`** thực tế trên các app → chốt [ADR-0005](../adr/0005-purpose-length-policy.md).

## 1. Chuẩn bị

Bạn cần: một máy chạy demo app, một điện thoại có ≥ 1 app ngân hàng (càng nhiều app càng tốt), và **một tài khoản
nhận tiền thật** (của chính bạn). Với số tài khoản giả, app sẽ báo không tìm thấy người nhận.

```bash
./mvnw -q install -DskipTests          # lần đầu, để demo dùng được các module trong repo
./mvnw -Psamples -pl samples/demo-app spring-boot:run \
  -Dspring-boot.run.arguments="--vietnam.vietqr.bank-bin=970422 --vietnam.vietqr.account-number=<STK của bạn>"
```

> Truyền tài khoản thật qua tham số như trên, **không** sửa `application.yml` để tránh lỡ commit số tài khoản.

Mở <http://localhost:8080/verify> trên máy tính. Trang hiển thị 12 mã QR, mỗi mã kèm cột **"Mong đợi"** (ngân hàng,
số tài khoản, số tiền, nội dung, độ dài) và chuỗi payload.

| Mã | Kiểm tra gì |
|---|---|
| `static` | QR tĩnh: app phải để người trả tự nhập số tiền |
| `amount` | QR động 10.000 đ, nội dung `DH001` |
| `diacritics` | Nội dung có dấu (`Thanh toán đơn hàng DH002`) đã được bỏ dấu thành `Thanh toan don hang DH002` |
| `special` | Ký tự đặc biệt `# - / : . ,` có được giữ nguyên |
| `purpose-25` … `purpose-95` | Đo độ dài nội dung, xem [mục 3](#3-đo-giới-hạn-độ-dài-purpose) |
| `colors` | QR màu navy trên nền kem |
| `logo` | QR có logo, scale mặc định 0.2 |
| `logo-max` | QR có logo, scale tối đa 0.25 |

## 2. Quét và đối chiếu

Với mỗi app:

1. Mở app → chức năng **quét QR / chuyển tiền bằng QR**.
2. Quét từng mã trên màn hình máy tính.
3. Ở màn hình xác nhận chuyển tiền, đối chiếu với cột "Mong đợi":
   - **Ngân hàng** và **số tài khoản** đúng;
   - **Tên người nhận** app tự tra ra đúng chủ tài khoản;
   - **Số tiền** (mã động) được điền sẵn đúng 10.000 đ; mã `static` để trống;
   - **Nội dung** được điền sẵn đúng, không bị cắt hay biến đổi.
4. **Không cần chuyển tiền thật** — dừng ở màn hình xác nhận. Nếu muốn chắc chắn hơn, chuyển 10.000 đ vào chính
   tài khoản của bạn với mã `amount` và kiểm tra nội dung trong sao kê.
5. Ghi kết quả vào bảng dưới. Chụp màn hình các trường hợp lỗi.

Ký hiệu: ✅ đúng hoàn toàn · ⚠️ quét được nhưng có sai khác (ghi chú) · ❌ không quét được / báo lỗi · — chưa thử.

### Kết quả quét

| | App 1 | App 2 | App 3 |
|---|---|---|---|
| **App, phiên bản** | | | |
| **Nền tảng** | | | |
| **Ngày kiểm tra** | | | |
| **Người kiểm tra** | | | |
| `static` | — | — | — |
| `amount` | — | — | — |
| `diacritics` | — | — | — |
| `special` | — | — | — |
| `colors` | — | — | — |
| `logo` | — | — | — |
| `logo-max` | — | — | — |

Ghi chú sai khác:

- _(chưa có)_

## 3. Đo giới hạn độ dài `purpose`

Các mã `purpose-25`, `purpose-26`, `purpose-50`, `purpose-70`, `purpose-95` có nội dung dài **đúng** 25, 26, 50, 70,
95 ký tự, dạng "thước đo":

```text
L50-xxxxx1xxxxxxxxx2xxxxxxxxx3xxxxxxxxx4xxxxxxxxx5
```

- `L50-` cho biết mã nào.
- Ký tự thứ 10, 20, 30… là **chữ số hàng chục của vị trí**; các vị trí khác là `x`.

Nếu app cắt nội dung, đọc vị trí cắt từ chữ số cuối cùng còn thấy cộng số `x` sau nó. Ví dụ app chỉ hiển thị
`L50-xxxxx1xxxxxxxxx2xxxxx` → chữ số cuối là `2` (vị trí 20), thêm 5 ký tự `x` → app giữ **25** ký tự.

Với mỗi app, quét lần lượt và ghi lại app **hiển thị đủ**, **cắt còn N ký tự**, hay **báo lỗi / không quét được**.
Nếu app cho sửa nội dung, ghi thêm độ dài tối đa ô nội dung cho phép nhập.

| Mã | App 1 | App 2 | App 3 |
|---|---|---|---|
| `purpose-25` | — | — | — |
| `purpose-26` | — | — | — |
| `purpose-50` | — | — | — |
| `purpose-70` | — | — | — |
| `purpose-95` | — | — | — |
| Ô nội dung cho nhập tối đa | — | — | — |

### Kết luận

_(Điền sau khi đo.)_ Dựa trên bảng trên, cập nhật [ADR-0005](../adr/0005-purpose-length-policy.md): giữ giới hạn cứng
95 hay hạ xuống, giới hạn khuyến nghị bao nhiêu. Nếu đổi `VietQrEncoder.MAX_PURPOSE_LENGTH`, ghi breaking change
vào `CHANGELOG.md`.

## 4. Sau khi kiểm tra

- Đủ ≥ 3 app ở mục 2 → tick mục "Quét QR sinh ra bằng ≥ 3 app ngân hàng thật" trong [ROADMAP](../ROADMAP.md).
- Đo xong mục 3 và chốt ADR-0005 → tick mục "Quyết định giới hạn độ dài `purpose`".
- Trong lúc kiểm tra, hãy tạo luôn QR **từ chính các app đó** và lưu thành test vector
  ([test-vectors.md](test-vectors.md)) — dùng form "Decode ảnh" ở đầu trang `/verify`.
