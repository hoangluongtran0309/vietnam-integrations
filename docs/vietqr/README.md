# VietQR

Tạo và đọc mã **VietQR** — mã QR chuyển khoản nhanh NAPAS 247, theo chuẩn EMVCo Merchant-Presented Mode — cho Java
và Spring Boot. Payload được sinh **offline**: không cần đăng ký, không cần API key, thư viện không gọi mạng.

| Trang | Nội dung |
|---|---|
| **Trang này** | Cài đặt, bắt đầu nhanh, cấu hình, sinh/đọc payload, cấu trúc payload, nội dung chuyển khoản, lỗi, FAQ |
| [Danh sách ngân hàng (BIN)](banks.md) | 65 thành viên NAPAS trong `VietQrBank`, nguồn dữ liệu, cách cập nhật |
| [Render ảnh QR](rendering.md) | PNG, màu, logo, `QrImageOptions` |
| [Xác nhận thanh toán](payment-confirmation.md) | SPI `PaymentConfirmationListener` (interface; adapter ở 0.4) |
| [Test vector thật](test-vectors.md) | Thu thập payload từ app ngân hàng để kiểm thử decoder |
| [Kiểm tra thủ công](manual-verification.md) | Quét QR bằng app ngân hàng thật, đo giới hạn nội dung |

## Cài đặt

Yêu cầu Java 17+. Với Spring Boot: Spring Boot 4.x.

### Spring Boot

```xml
<dependencyManagement>
  <dependencies>
    <dependency>
      <groupId>io.github.hoangluongtran0309</groupId>
      <artifactId>vietnam-bom</artifactId>
      <version>0.1.0</version>
      <type>pom</type>
      <scope>import</scope>
    </dependency>
  </dependencies>
</dependencyManagement>

<dependencies>
  <dependency>
    <groupId>io.github.hoangluongtran0309</groupId>
    <artifactId>vietnam-vietqr-spring-boot-starter</artifactId>
  </dependency>
  <!-- Tùy chọn: chỉ cần nếu muốn render ảnh PNG -->
  <dependency>
    <groupId>com.google.zxing</groupId>
    <artifactId>javase</artifactId>
    <version>3.5.3</version>
  </dependency>
</dependencies>
```

### Java thuần (không Spring)

```xml
<dependency>
  <groupId>io.github.hoangluongtran0309</groupId>
  <artifactId>vietnam-vietqr-core</artifactId>
  <version>0.1.0</version>
</dependency>
```

`vietnam-vietqr-core` chỉ dùng JDK. ZXing chỉ cần cho `QrImageRenderer`.

## Bắt đầu nhanh (Spring Boot)

**1.** Thêm dependency như trên (kèm ZXing).

**2.** Khai báo tài khoản nhận tiền mặc định:

```yaml
vietnam:
  vietqr:
    bank-bin: "970436"            # BIN ngân hàng — tra trong banks.md
    account-number: "0123456789"  # số tài khoản của bạn, để trong ngoặc kép
```

**3.** Sinh QR:

```java
@RestController
class PaymentController {

    private final VietQrGenerator vietQr;
    private final QrImageRenderer renderer;

    PaymentController(VietQrGenerator vietQr, QrImageRenderer renderer) {
        this.vietQr = vietQr;
        this.renderer = renderer;
    }

    @GetMapping(value = "/pay.png", produces = MediaType.IMAGE_PNG_VALUE)
    byte[] pay(@RequestParam long amount, @RequestParam String orderCode) {
        String payload = vietQr.forAmount(amount, orderCode);   // ví dụ orderCode = "DH12345"
        return renderer.renderPng(payload, 400);
    }
}
```

**4.** Mở `http://localhost:8080/pay.png?amount=10000&orderCode=DH12345` và quét bằng app ngân hàng: app phải hiện đúng
người nhận, số tiền 10.000 đ và nội dung `DH12345`.

## Bắt đầu nhanh (Java thuần)

```java
String payload = new VietQrEncoder().encode(VietQrRequest.builder()
        .bank(VietQrBank.VIETCOMBANK)          // hoặc .bankBin("970436")
        .accountNumber("0123456789")
        .amount(150_000)                       // bỏ dòng này để có QR tĩnh
        .purpose("DH12345 Thanh toán")          // dấu tiếng Việt được tự bỏ
        .build());

byte[] png = new QrImageRenderer().renderPng(payload, 400);   // cần ZXing
```

## Cấu hình Spring Boot

Prefix `vietnam.vietqr`:

| Property | Mặc định | Ý nghĩa |
|---|---|---|
| `enabled` | `true` | `false` để tắt toàn bộ auto-configuration VietQR |
| `bank-bin` | — | BIN 6 chữ số của ngân hàng nhận mặc định |
| `account-number` | — | Số tài khoản nhận mặc định |
| `service-code` | `TO_ACCOUNT` | `TO_ACCOUNT` (chuyển tới số tài khoản) hoặc `TO_CARD` (tới số thẻ) |

`bank-bin` và `account-number` chỉ cần khi dùng `VietQrGenerator.forAmount(...)` / `staticQr()`. Thiếu thì app vẫn khởi
động, nhưng gọi hai method đó sẽ báo `VietQrException`.

Bean được tạo tự động — bean nào bạn tự khai báo cùng kiểu sẽ được ưu tiên (`@ConditionalOnMissingBean`):

| Bean | Điều kiện |
|---|---|
| `VietQrEncoder` | luôn có |
| `VietQrDecoder` | luôn có |
| `VietQrGenerator` | luôn có |
| `QrImageRenderer` | khi có ZXing (`com.google.zxing:javase`) trên classpath |

## Sinh payload

### `VietQrGenerator` (Spring)

| Method | Kết quả |
|---|---|
| `forAmount(long amount, String purpose)` | QR động cho tài khoản mặc định |
| `staticQr()` | QR tĩnh cho tài khoản mặc định — người trả tự nhập số tiền |
| `generate(VietQrRequest request)` | QR theo request tùy ý, bỏ qua cấu hình mặc định (ví dụ shop có nhiều tài khoản) |

### `VietQrRequest` + `VietQrEncoder` (mọi nơi)

| Field (builder) | Bắt buộc | Ràng buộc |
|---|---|---|
| `bankBin(String)` / `bank(VietQrBank)` | ✅ | Đúng 6 chữ số |
| `accountNumber(String)` | ✅ | 1–19 ký tự chữ hoặc số |
| `serviceCode(ServiceCode)` | | Mặc định `TO_ACCOUNT` |
| `amount(long)` | | > 0. Không đặt → **QR tĩnh** (tag `01` = `11`); có → **QR động** (`12`) |
| `purpose(String)` | | Xem [Nội dung chuyển khoản](#nội-dung-chuyển-khoản-purpose) |

Lỗi ràng buộc xảy ra ngay ở `build()`, trừ giới hạn độ dài `purpose` được kiểm tra lúc `encode()`.
`VietQrEncoder`, `VietQrDecoder`, `QrImageRenderer` không giữ trạng thái — dùng chung một instance cho cả ứng dụng.

## Đọc payload

```java
VietQrPayload p = new VietQrDecoder().decode(qrText);

p.bankBin();         // "970436"
p.bank();            // Optional[VIETCOMBANK]
p.accountNumber();   // "0123456789"
p.serviceCode();     // TO_ACCOUNT
p.amount();          // 150000L, hoặc null với QR tĩnh
p.purpose();         // "DH12345 Thanh toan", hoặc null
p.dynamic();         // true
```

Decoder kiểm tra: CRC đúng, có tag `38` với GUID NAPAS `A000000727`, có BIN + số tài khoản + service code.
Payload sai sẽ bị từ chối bằng `VietQrException` (xem [Lỗi thường gặp](#lỗi-thường-gặp)).

Decoder đã được kiểm thử với payload do chính thư viện sinh; **chưa** có test vector từ app ngân hàng thật
([test-vectors.md](test-vectors.md)).

## Cấu trúc payload

Payload là chuỗi các trường TLV: **ID** 2 chữ số + **độ dài** 2 chữ số + **giá trị**.

| Tag | Ý nghĩa | Giá trị |
|---|---|---|
| `00` | Payload format | `01` |
| `01` | Point of initiation | `11` tĩnh / `12` động (có số tiền) |
| `38` | Merchant account | `00` = `A000000727` (GUID NAPAS), `01` = [`00` BIN, `01` số tài khoản], `02` = service code |
| `53` | Tiền tệ | `704` (VND) |
| `54` | Số tiền | Chỉ khi QR động |
| `58` | Quốc gia | `VN` |
| `62` | Additional data | `08` = nội dung chuyển khoản |
| `63` | CRC | CRC-16/CCITT-FALSE trên toàn bộ chuỗi phía trước, tính cả `6304` |

Ví dụ phân tích một payload (lấy từ test của encoder):

```text
00020101021238540010A00000072701240006970436011001234567890208QRIBFTTA530370454061500005802VN62320828Thanh toan don hang #DH1234563046E65
```

```text
00 02 01                         Payload format = 01
01 02 12                         QR động
38 54                            Merchant account (54 ký tự):
   00 10 A000000727                GUID NAPAS
   01 24                           Beneficiary:
      00 06 970436                   BIN (Vietcombank)
      01 10 0123456789               Số tài khoản
   02 08 QRIBFTTA                  Service code: chuyển tới tài khoản
53 03 704                        VND
54 06 150000                     150.000 đ
58 02 VN                         Việt Nam
62 32                            Additional data:
   08 28 Thanh toan don hang #DH12345   Nội dung
63 04 6E65                       CRC
```

> Bảng trên dựa trên EMVCo MPM v1.1 và cách thư viện đang mã hóa. `TODO(v0.1): verify` từng tag với đặc tả chính thức
> của NAPAS — tài liệu này chưa được công bố công khai.

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

## Ngân hàng

`VietQrBank` có sẵn BIN của 65 thành viên NAPAS (ngân hàng, ngân hàng số, ví điện tử…). Ngân hàng chưa có trong enum
vẫn dùng được qua `bankBin("...")`. Chi tiết, nguồn dữ liệu và độ tin cậy: [banks.md](banks.md).

## Render ảnh

`QrImageRenderer.renderPng(payload, size)` cho ảnh đen trắng; `renderPng(payload, QrImageOptions)` cho phép đổi màu,
lề, mức sửa lỗi và chèn logo. Xem [rendering.md](rendering.md).

## Lỗi thường gặp

Mọi lỗi nghiệp vụ là `VietQrException` (unchecked). Thiếu tham số bắt buộc (`null`) là `NullPointerException` với
thông báo là tên tham số, ví dụ quên `accountNumber(...)` → `NullPointerException: accountNumber`.

**Khi sinh QR**

| Thông báo | Nguyên nhân | Cách xử lý |
|---|---|---|
| `bankBin must be exactly 6 digits: 9704` | BIN sai định dạng | Tra BIN trong [banks.md](banks.md) |
| `accountNumber must be 1-19 alphanumeric characters` | Số tài khoản có dấu cách, gạch ngang, quá dài | Bỏ ký tự phân cách |
| `amount must be positive` | Số tiền ≤ 0 | Muốn QR tĩnh thì đừng gọi `amount(...)` |
| `purpose is 96 characters after normalization; the maximum is 95` | Nội dung quá dài | Rút gọn, xem [Giới hạn độ dài](#giới-hạn-độ-dài) |
| `Default beneficiary not configured: set vietnam.vietqr.bank-bin and vietnam.vietqr.account-number` | Gọi `forAmount`/`staticQr` khi chưa cấu hình | Thêm property, hoặc dùng `generate(request)` |

**Khi đọc QR**

| Thông báo | Nguyên nhân |
|---|---|
| `Payload too short` | Chuỗi rỗng hoặc dưới 8 ký tự |
| `Payload must end with CRC tag 63` | Không kết thúc bằng `6304xxxx` — thường do copy thiếu |
| `CRC mismatch: expected 6E65 but was FFFF` | Payload bị sửa hoặc copy sai một ký tự |
| `Truncated TLV at position N` / `Invalid TLV length at position N` / `TLV value of tag NN overflows payload` | Payload sai cấu trúc TLV |
| `Missing required tag 38` | Không phải mã chuyển khoản VietQR (mã QR loại khác), hoặc thiếu trường bắt buộc |
| `Not a NAPAS VietQR payload (unexpected GUID)` | Tag `38` không mang GUID `A000000727` |
| `Unknown service code: X` | Service code ngoài `QRIBFTTA` / `QRIBFTTC` |
| `Unsupported amount format: 10000.50` | Số tiền có phần thập phân — chưa hỗ trợ |

**Khi render ảnh**: lỗi của `QrImageOptions` liệt kê trong [rendering.md](rendering.md#lỗi-khi-tạo-options).

## Câu hỏi thường gặp

**QR tĩnh hay QR động?** QR tĩnh (không gọi `amount(...)`) dùng mãi được, người trả tự nhập số tiền — hợp để in dán ở
quầy. QR động có sẵn số tiền và nội dung — hợp cho từng đơn hàng.

**Có cần đăng ký với NAPAS hay ngân hàng không?** Không. Payload VietQR là dữ liệu công khai (BIN + số tài khoản),
thư viện sinh hoàn toàn offline.

**Vì sao nội dung bị mất dấu?** Nhiều app ngân hàng hiển thị sai ký tự có dấu, nên thư viện chủ động bỏ dấu. Xem
[Nội dung chuyển khoản](#nội-dung-chuyển-khoản-purpose).

**Làm sao biết khách đã trả tiền?** QR không báo được điều đó. Cần một dịch vụ theo dõi tài khoản; thư viện chuẩn bị sẵn
interface ở [payment-confirmation.md](payment-confirmation.md).

**Không có bean `QrImageRenderer`?** Thêm dependency `com.google.zxing:javase`.

**`TO_CARD` (chuyển tới số thẻ) dùng được chưa?** Encoder hỗ trợ (`serviceCode(ServiceCode.TO_CARD)` với số thẻ ở
`accountNumber`), nhưng **chưa** được kiểm tra bằng app ngân hàng thật.

**QR của tôi quét không được?** Kiểm tra theo thứ tự: decode lại payload bằng `VietQrDecoder`; dùng ảnh đen trắng mặc
định, không logo; tăng kích thước ảnh; rút ngắn nội dung. Nếu vẫn lỗi, mở issue kèm payload (che số tài khoản như
hướng dẫn trong [test-vectors.md](test-vectors.md#che-số-tài-khoản-và-tính-lại-crc)).
