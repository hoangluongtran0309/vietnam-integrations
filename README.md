# Vietnam Integrations

**Vietnam Integrations for Java & Spring Boot** — thư viện tích hợp với các hệ thống mà dev Việt Nam cần hằng ngày: **VietQR**, **Zalo OA / ZNS**, **hóa đơn điện tử**. Dùng được với Spring Boot (auto-configuration) hoặc Java thuần.

*Integrations with Vietnamese platforms for Java and Spring Boot: VietQR payment QR codes, Zalo Official Account / ZNS messaging, and e-invoices.*

[![CI](https://github.com/hoangluongtran0309/vietnam-integrations/actions/workflows/ci.yml/badge.svg)](https://github.com/hoangluongtran0309/vietnam-integrations/actions/workflows/ci.yml)
![Status](https://img.shields.io/badge/status-pre--alpha-orange)
![License](https://img.shields.io/badge/license-Apache%202.0-blue)

## Trạng thái

| Module | Trạng thái | Phiên bản mục tiêu |
|---|---|---|
| `vietnam-vietqr-spring-boot-starter` | 🟡 Đang phát triển — encoder/decoder đã chạy | 0.1.0 |
| `vietnam-zalo-oa-spring-boot-starter` | 🔴 Khung (interfaces, token manager) | 0.2.0 |
| `vietnam-einvoice-spring-boot-starter` | 🔴 Khung (domain model, SPI) | 0.3.0 |

Xem kế hoạch chi tiết tại [docs/ROADMAP.md](docs/ROADMAP.md).

## Yêu cầu

- Java 17+
- Spring Boot 4.x

## Cài đặt (sau khi phát hành 0.1.0)

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
  <!-- Tùy chọn: để render ảnh PNG -->
  <dependency>
    <groupId>com.google.zxing</groupId>
    <artifactId>javase</artifactId>
    <version>3.5.3</version>
  </dependency>
</dependencies>
```

## VietQR — dùng nhanh

```yaml
vietnam:
  vietqr:
    bank-bin: "970436"        # Vietcombank
    account-number: "0123456789"
```

```java
@Service
class PaymentService {
    private final VietQrGenerator vietQr;
    private final QrImageRenderer renderer;

    byte[] qrForOrder(Order order) {
        String payload = vietQr.forAmount(order.total(), "Thanh toan " + order.code());
        return renderer.renderPng(payload, 400);
    }
}
```

Không dùng Spring? Dùng thẳng `vietnam-vietqr-core`:

```java
String payload = new VietQrEncoder().encode(VietQrRequest.builder()
        .bank(VietQrBank.VIETCOMBANK)
        .accountNumber("0123456789")
        .amount(150_000)
        .purpose("Thanh toán đơn #DH12345")   // dấu tiếng Việt được tự bỏ
        .build());
```

## Chạy thử

```bash
./mvnw verify                          # build + test
./mvnw -Psamples -pl samples/demo-app spring-boot:run
# mở http://localhost:8080/qr.png?amount=10000&orderId=DH1 và quét bằng app ngân hàng
```

## Đóng góp

Xem [CONTRIBUTING.md](CONTRIBUTING.md). Đặc biệt cần: test vector thật từ các app ngân hàng, tài khoản sandbox của nhà cung cấp hóa đơn điện tử.

## Lưu ý pháp lý

Dự án độc lập, không liên kết với NAPAS, Zalo/VNG hay bất kỳ nhà cung cấp hóa đơn điện tử nào. Các tên thương hiệu thuộc về chủ sở hữu tương ứng.

## License

[Apache License 2.0](LICENSE)
