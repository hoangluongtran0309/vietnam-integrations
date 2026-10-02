# Render ảnh QR (PNG, màu, logo)

`QrImageRenderer` biến chuỗi payload VietQR thành ảnh PNG. Từ Phase 1 có thể đổi màu và chèn logo ở giữa
qua `QrImageOptions`.

## 1. Thêm dependency ZXing

Renderer dùng [ZXing](https://github.com/zxing/zxing). Dependency này **optional** trong thư viện (không bị kéo
theo tự động), nên bạn phải tự thêm:

```xml
<dependency>
  <groupId>com.google.zxing</groupId>
  <artifactId>javase</artifactId>
  <version>3.5.3</version>
</dependency>
```

Với Spring Boot starter: khi có ZXing trên classpath, bean `QrImageRenderer` được tạo tự động
(`@ConditionalOnClass`). Không có ZXing thì không có bean — `VietQrGenerator` vẫn chạy bình thường.

## 2. Dùng cơ bản

```java
QrImageRenderer renderer = new QrImageRenderer();          // hoặc inject bean trong Spring
byte[] png = renderer.renderPng(payload, 400);              // 400 × 400 px, đen trên trắng
```

`renderPng(payload, size)` cho ra **đúng ảnh như trước Phase 1** (mức sửa lỗi M, lề 2 module) — có test
đảm bảo kết quả không đổi từng byte.

## 3. Tùy chọn: `QrImageOptions`

```java
QrImageOptions options = QrImageOptions.builder()
        .size(400)                                   // px
        .margin(2)                                   // module
        .foreground(new Color(0x0B, 0x2A, 0x5B))     // xanh navy
        .background(Color.WHITE)
        .build();

byte[] png = renderer.renderPng(payload, options);
```

`QrImageOptions` là record bất biến — nên tạo **một lần** (hằng số hoặc bean) rồi dùng lại.

| Thuộc tính | Mặc định | Giới hạn | Ghi chú |
|---|---|---|---|
| `size` | `400` | `64`–`4096` px | Ảnh luôn vuông. Nếu mã cần nhiều điểm ảnh hơn `size`, ZXing có thể trả ảnh lớn hơn. |
| `margin` | `2` | `≥ 0` module | Vùng trắng quanh mã (quiet zone). Đừng để `0` nếu ảnh đặt trên nền tối hoặc nhiều chi tiết. |
| `errorCorrection` | `M`; `H` khi có logo | `L`, `M`, `Q`, `H` | Có logo thì bắt buộc `Q` hoặc `H`. |
| `foreground` | `Color.BLACK` | phải **tối hơn** `background` | Màu các ô đậm. |
| `background` | `Color.WHITE` | | Màu nền, lề và ô vuông sau logo. |
| `logo` | không có | | `BufferedImage`, hoặc `byte[]` (PNG/JPEG/GIF/BMP) qua builder. |
| `logoScale` | `0.2` | `(0, 0.25]` | Tỉ lệ cạnh vùng logo so với cạnh ảnh. |

Các mức sửa lỗi (`QrErrorCorrection`): `L` ≈ 7%, `M` ≈ 15%, `Q` ≈ 25%, `H` ≈ 30% dữ liệu có thể khôi phục.
Mức càng cao mã càng dày (nhiều module hơn).

### Lỗi khi tạo options

Tất cả là `VietQrException`, xảy ra ngay lúc `build()` (không phải lúc render):

| Tình huống | Thông báo |
|---|---|
| `size` ngoài khoảng | `size must be between 64 and 4096 pixels: 50` |
| `margin` âm | `margin must not be negative: -1` |
| `logoScale` ≤ 0 hoặc > 0.25 | `logoScale must be greater than 0 and at most 0.25: 0.3` |
| Màu đảo (chữ sáng nền tối) hoặc trùng màu | `foreground must be darker than background; many scanners cannot read inverted codes` |
| Logo với mức `L`/`M` | `a logo requires errorCorrection Q or H, but was M` |
| `logo(byte[])` không phải ảnh | `Unsupported logo image format` |

## 4. Màu sắc

- Thư viện chỉ chặn trường hợp rõ ràng sai: `foreground` không tối hơn `background` (mã "âm bản" — nhiều máy quét
  không đọc được).
- Thư viện **không** kiểm tra độ tương phản tối thiểu. Hãy chọn màu đậm trên nền sáng, chênh lệch lớn.
  Màu nhạt (vàng, xanh lá nhạt) trên nền trắng rất dễ khiến app ngân hàng không quét được.
- Nền trong suốt (`Color` có alpha) được giữ nguyên trong PNG, nhưng khi hiển thị trên nền tối mã sẽ thành âm bản
  — tránh dùng.

## 5. Logo

```java
byte[] logoBytes;
try (InputStream in = getClass().getResourceAsStream("/static/logo.png")) {
    logoBytes = in.readAllBytes();
}

QrImageOptions withLogo = QrImageOptions.builder()
        .size(480)
        .logo(logoBytes)        // tự chuyển errorCorrection sang H
        .logoScale(0.2)         // vùng logo rộng 20% cạnh ảnh
        .build();

byte[] png = renderer.renderPng(payload, withLogo);
```

Cách vẽ:

1. Sinh mã QR như bình thường (mặc định mức `H` khi có logo).
2. Xóa một hình vuông ở giữa, cạnh = `logoScale × cạnh ảnh`, tô bằng màu `background`.
3. Thu nhỏ logo (giữ tỉ lệ) để vừa trong hình vuông đó, chừa viền đệm 10% mỗi bên.

Các module bị logo che được khôi phục nhờ sửa lỗi. Vì thế giới hạn `logoScale` tối đa là `0.25` và mức sửa lỗi
tối thiểu là `Q`.

Khuyến nghị:

- Dùng logo đơn giản, nền trong suốt hoặc cùng màu `background`.
- Giữ mặc định `0.2`; chỉ tăng lên `0.25` sau khi đã quét thử.
- **Luôn quét thử bằng app ngân hàng thật** trước khi dùng logo cho khách hàng
  (xem [manual-verification.md](manual-verification.md)). Test tự động chỉ chứng minh ZXing đọc được.

## 6. Ví dụ Spring MVC

```java
@RestController
class QrController {

    private static final QrImageOptions BRANDED = QrImageOptions.builder()
            .size(480)
            .foreground(new Color(0x0B, 0x2A, 0x5B))
            .build();

    private final VietQrGenerator vietQr;
    private final QrImageRenderer renderer;

    QrController(VietQrGenerator vietQr, QrImageRenderer renderer) {
        this.vietQr = vietQr;
        this.renderer = renderer;
    }

    @GetMapping(value = "/orders/{code}/qr.png", produces = MediaType.IMAGE_PNG_VALUE)
    byte[] qr(@PathVariable String code, @RequestParam long amount) {
        return renderer.renderPng(vietQr.forAmount(amount, code), BRANDED);
    }
}
```

## 7. Lưu ý: payload dài → mã dày

Payload càng dài và mức sửa lỗi càng cao thì mã càng nhiều module, mỗi module càng nhỏ trên cùng một kích thước ảnh.
Số liệu đo bằng chính encoder của thư viện (version QR, trong ngoặc là số module mỗi cạnh):

| Payload | Độ dài | L | M | Q | H |
|---|---|---|---|---|---|
| QR tĩnh, không nội dung | 91 | 4 (33) | 5 (37) | 6 (41) | 7 (45) |
| QR động, nội dung 18 ký tự | 127 | 6 (41) | 8 (49) | 9 (53) | 11 (61) |
| QR động, nội dung 25 ký tự | 134 | 6 (41) | 8 (49) | 10 (57) | 11 (61) |
| QR động, nội dung 50 ký tự | 159 | 8 (49) | 9 (53) | 11 (61) | 13 (69) |
| Dài nhất (TK 19 ký tự, nội dung 95) | 216 | 9 (53) | 11 (61) | 13 (69) | 15 (77) |

Phát hiện khi viết test: với payload dài nhất ở mức `H` (version 15), **bộ đọc của ZXing không tìm thấy mã**
ở mọi kích thước thử (300–1000 px), **kể cả khi không có logo**; OpenCV vẫn đọc đúng các ảnh đó. Nghĩa là
ảnh hợp lệ, nhưng một số máy quét có thể gặp khó với mã quá dày. Đây thêm một lý do để:

- giữ nội dung chuyển khoản ngắn (≤ 25 ký tự, xem [README](README.md#nội-dung-chuyển-khoản-purpose));
- không dùng logo cho payload dài;
- tăng `size` khi mã dày để mỗi module có nhiều điểm ảnh hơn.
