# Test vector VietQR thật

Test hiện có của encoder/decoder chủ yếu là **round-trip với chính thư viện**: encoder sinh, decoder đọc lại. Cách này
không bắt được lỗi hiểu sai đặc tả — nếu cả hai cùng sai giống nhau, test vẫn xanh. **Test vector thật** là payload do
hệ thống khác (app ngân hàng) sinh ra; decoder của thư viện phải đọc đúng chúng.

Mục tiêu Phase 1: **≥ 5 payload từ các app ngân hàng khác nhau** ([ROADMAP](../ROADMAP.md)).

## Tổng quan

| Thành phần | Vị trí |
|---|---|
| Thư mục vector | `vietnam-vietqr-core/src/test/resources/vectors/` |
| File mẫu | `vectors/example.properties.template` |
| Test đọc vector | `vietnam-vietqr-core/src/test/java/.../RealPayloadVectorsTest.java` |
| Công cụ lấy payload từ ảnh | demo app: `POST /decode-image`, `POST /decode`, form trên trang `/verify` |

`RealPayloadVectorsTest` tạo **một test cho mỗi file** `*.properties` (tên test = tên file). Thư mục chưa có vector
thì test vẫn xanh với 0 trường hợp.

## Quy trình thu thập một vector

### 1. Tạo QR trong app ngân hàng

Mở chức năng nhận tiền bằng QR của app (tên tùy app: "Nhận tiền", "Mã QR của tôi", "Tạo mã QR"…). Nên lấy cả hai loại
nếu app hỗ trợ:

- **QR tĩnh**: chỉ có tài khoản, người trả tự nhập số tiền.
- **QR động**: có số tiền và nội dung.

Chụp màn hình mã QR.

### 2. Lấy chuỗi payload

Chạy demo app (lần đầu cần `install` để demo dùng được các module trong repo):

```bash
./mvnw -q install -DskipTests
./mvnw -Psamples -pl samples/demo-app spring-boot:run
```

Sau đó dùng **một** trong các cách:

- Mở <http://localhost:8080/verify>, dùng form **"Decode ảnh"** ở đầu trang để tải ảnh chụp lên.
- Hoặc dùng `curl`:

  ```bash
  curl -F file=@screenshot.png localhost:8080/decode-image
  ```

- Hoặc quét bằng một app đọc QR hiển thị **nguyên văn** nội dung, rồi kiểm tra:

  ```bash
  curl -H 'Content-Type: text/plain' --data '000201...' localhost:8080/decode
  ```

Kết quả là JSON:

```json
{
  "payload": "00020101021238540010A000000727...6304E62B",
  "decoded": {
    "bankBin": "970436",
    "bankName": "Vietcombank",
    "accountNumber": "0123456789",
    "serviceCode": "TO_ACCOUNT",
    "amount": 10000,
    "purpose": "DH006",
    "dynamic": true
  },
  "error": null
}
```

- `payload` luôn là **chuỗi thô** đọc từ QR, kể cả khi thư viện không decode được.
- `error` khác `null` (ví dụ `"Missing required tag 38"`) nghĩa là **decoder của thư viện chưa đọc được payload này**.
  Đây là vector giá trị nhất: vẫn tạo file vector và mở issue kèm payload — test sẽ đỏ cho tới khi decoder được sửa.
- Ảnh không có QR → HTTP 400 `"No QR code found in image"`. Thử cắt ảnh sát mã QR hơn.

### 3. Che số tài khoản (nếu cần)

Payload chứa số tài khoản thật và sẽ nằm trong repo công khai. Nếu không muốn công khai số tài khoản, thay số và
**tính lại CRC** (xem [mục dưới](#che-số-tài-khoản-và-tính-lại-crc)), rồi đặt `masked=true`.

### 4. Tạo file vector

Sao chép `example.properties.template` thành `<ngan-hang>-<truong-hop>.properties`, ví dụ `mbbank-static.properties`,
`vcb-dynamic-purpose.properties`. Điền theo [định dạng](#định-dạng-file).

### 5. Chạy test

```bash
./mvnw -pl vietnam-vietqr-core test -Dtest=RealPayloadVectorsTest
```

Mỗi file là một test riêng mang tên file, nên báo cáo chỉ rõ vector nào hỏng. Ví dụ phần thông báo khi số tiền
không khớp:

```text
expected: 1L
 but was: 999999999L
```

> Mẹo: Maven không xóa file cũ trong `target/test-classes/vectors/`. Nếu đổi tên hoặc xóa vector mà test vẫn thấy file
> cũ, chạy `./mvnw clean` trước.

### 6. Gửi PR

Commit file vector, cập nhật bảng [Vector đã có](#vector-đã-có) ở cuối trang.

## Định dạng file

File `.properties`, mã hóa **UTF-8**, mỗi file một payload.

| Khóa | Bắt buộc | Ý nghĩa |
|---|---|---|
| `source` | ✅ | `app` (app ngân hàng), `spec` (ví dụ trong đặc tả chính thức), `other` |
| `collectedOn` | ✅ | Ngày thu thập, `yyyy-MM-dd` |
| `masked` | ✅ | `true` nếu đã thay số tài khoản và tính lại CRC |
| `payload` | ✅ | Chuỗi payload thô, nguyên văn, một dòng |
| `expected.bankBin` | ✅ | BIN mong đợi |
| `expected.accountNumber` | ✅ | Số tài khoản mong đợi (sau khi che, nếu có) |
| `expected.dynamic` | ✅ | `true` nếu tag `01` = `12` (QR động) |
| `expected.amount` | | Số tiền. **Bỏ hẳn dòng** nếu payload không có số tiền — test kiểm tra decoder trả `null` |
| `expected.purpose` | | Nội dung. **Bỏ hẳn dòng** nếu payload không có nội dung |
| `expected.serviceCode` | | `TO_ACCOUNT` hoặc `TO_CARD`; bỏ trống thì không kiểm tra |
| `expected.reencodes` | | `true` nếu encoder của thư viện sinh ra **đúng từng byte** payload này (thứ tự tag giống hệt) |
| `app`, `appVersion`, `platform`, `description` | | Thông tin để người sau tái hiện được |

Lưu ý: `expected.purpose=` (có dòng nhưng rỗng) nghĩa là mong đợi chuỗi rỗng, **không** phải "không có nội dung".

Ví dụ (payload minh họa do thư viện sinh, **không** phải vector thật):

```properties
source=app
app=MB Bank
appVersion=6.1.2
platform=Android 15
collectedOn=2026-10-05
description=QR động từ màn hình "Nhận tiền", nhập 10.000 đ và nội dung DH1
masked=true
payload=00020101021238540010A00000072701240006970436011009999999990208QRIBFTTA53037045405100005802VN62180814Thanh toan DH163044DAC
expected.bankBin=970436
expected.accountNumber=0999999999
expected.dynamic=true
expected.amount=10000
expected.purpose=Thanh toan DH1
expected.serviceCode=TO_ACCOUNT
```

## Che số tài khoản và tính lại CRC

Payload là chuỗi TLV: mỗi trường có **độ dài** đứng trước giá trị, và 4 ký tự cuối là CRC của toàn bộ phần trước.

1. **Thay chữ số, giữ nguyên số ký tự** của số tài khoản (ví dụ `0123456789` → `0999999999`). Giữ nguyên độ dài thì
   mọi trường độ dài trong payload vẫn đúng; chỉ CRC thay đổi.
2. **Bỏ 4 ký tự CRC cuối** (giữ lại `6304`).
3. Tính CRC mới bằng chính thư viện qua `jshell`:

   ```bash
   ./mvnw -q -pl vietnam-vietqr-core package -DskipTests
   jshell --class-path vietnam-vietqr-core/target/classes
   ```

   ```java
   import io.github.hoangluongtran0309.vietnam.vietqr.*;
   String masked = "00020101021238540010A00000072701240006970436011009999999990208QRIBFTTA53037045405100005802VN62180814Thanh toan DH16304";
   String fixed = masked + Crc16Ccitt.computeHex(masked);
   System.out.println(fixed);
   new VietQrDecoder().decode(fixed);   // kiểm tra lại: phải decode được
   ```

4. Dùng `fixed` làm `payload`, đặt `masked=true` và ghi trong `description` là đã thay số tài khoản.

Che số tài khoản chỉ thay **giá trị** của một trường; cấu trúc, thứ tự tag và các trường khác vẫn là của app ngân hàng,
nên vector vẫn có giá trị kiểm tra. Đừng che theo cách đổi độ dài, xóa tag hay sắp lại thứ tự.

## Nguồn được chấp nhận

- ✅ Payload đọc từ QR do app ngân hàng / ví điện tử sinh ra.
- ✅ Ví dụ trong **đặc tả chính thức** (NAPAS, NHNN) — ghi rõ tài liệu, phiên bản, trang trong `description`.
- ❌ Payload do encoder của thư viện này sinh ra.
- ❌ Ví dụ trên blog, diễn đàn, bản sao tài liệu không rõ nguồn gốc (Scribd, Studocu…). Đặc tả VietQR của NAPAS hiện
  không được công bố công khai, nên chưa có vector loại `spec`.

## Vector đã có

| File | App / nguồn | Loại | Ngày | Ghi chú |
|---|---|---|---|---|
| _(chưa có)_ | | | | Cần ≥ 5 app khác nhau trước khi release 0.1.0 |
