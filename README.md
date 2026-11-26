# Vietnam Integrations

**Vietnam Integrations for Java & Spring Boot** — thư viện tích hợp với các hệ thống mà dev Việt Nam cần hằng ngày: **VietQR**, **Zalo OA / ZBS**, **hóa đơn điện tử**. Dùng được với Spring Boot (auto-configuration) hoặc Java thuần.

*Integrations with Vietnamese platforms for Java and Spring Boot: VietQR payment QR codes, Zalo Official Account / ZBS messaging, and e-invoices.*

[![CI](https://github.com/hoangluongtran0309/vietnam-integrations/actions/workflows/ci.yml/badge.svg)](https://github.com/hoangluongtran0309/vietnam-integrations/actions/workflows/ci.yml)
![Status](https://img.shields.io/badge/status-0.2.0-brightgreen)
![License](https://img.shields.io/badge/license-Apache%202.0-blue)

## Trạng thái

| Module | Trạng thái | Phiên bản mục tiêu |
|---|---|---|
| `vietnam-vietqr-spring-boot-starter` | 🟢 0.2.0 (QR, decode, đối soát) | 0.2.0 |
| `vietnam-zalo-oa-spring-boot-starter` | 🟡 Preview 0.2.0, chưa nghiệm thu với OA thật ([mức độ kiểm chứng](docs/zalo/README.md#6-mức-độ-kiểm-chứng)) | 0.2.0 |
| `vietnam-einvoice-spring-boot-starter` | 🟡 Preview 0.3.0: đọc, kiểm tra và xác minh chữ ký XML hóa đơn đã phát hành; phát hành hóa đơn qua nhà cung cấp chưa có ([mức độ kiểm chứng](docs/einvoice/README.md#mức-độ-kiểm-chứng)) | 0.3.0 |

Xem kế hoạch chi tiết tại [docs/ROADMAP.md](docs/ROADMAP.md).

Tài liệu Zalo OA/ZBS: [docs/zalo/README.md](docs/zalo/README.md).

## Yêu cầu

- Java 17+
- Spring Boot 4.x

## Cài đặt

```xml
<dependencyManagement>
  <dependencies>
    <dependency>
      <groupId>io.github.hoangluongtran0309</groupId>
      <artifactId>vietnam-bom</artifactId>
      <version>0.2.0</version>
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

## Zalo OA / ZBS — dùng nhanh

> [!WARNING]
> Zalo OA/ZBS trong 0.2.0 là **Preview** và chưa được nghiệm thu với OA thật. Sáu điểm còn
> phải xác minh là: định dạng số điện thoại ZBS; kiểu giá trị trong `template_data`; cách Zalo đếm
> giới hạn 2.000 ký tự; vị trí OA ID và phạm vi OA Secret Key trong webhook; `Content-Type` webhook;
> mã lỗi refresh token hết hạn, bị thu hồi hoặc đã dùng. Xem
> [mức độ kiểm chứng](docs/zalo/README.md#6-mức-độ-kiểm-chứng) trước khi dùng.

Một OA dùng properties cấp gốc:

```yaml
vietnam:
  zalo-oa:
    app-id: ${ZALO_APP_ID}
    secret-key: ${ZALO_APP_SECRET_KEY}
    oa-id: ${ZALO_OA_ID}
    oa-secret-key: ${ZALO_OA_SECRET_KEY}
```

Nhiều OA dùng map `accounts`; `default-account` chọn account cho các bean client tiện ích:

```yaml
vietnam:
  zalo-oa:
    app-id: ${ZALO_APP_ID}
    secret-key: ${ZALO_APP_SECRET_KEY}
    default-account: cskh
    accounts:
      cskh:
        oa-id: ${ZALO_CSKH_OA_ID}
        oa-secret-key: ${ZALO_CSKH_OA_SECRET_KEY}
      marketing:
        oa-id: ${ZALO_MARKETING_OA_ID}
        oa-secret-key: ${ZALO_MARKETING_OA_SECRET_KEY}
```

Tài liệu cấu hình, OAuth, gửi tin và webhook đầy đủ: [docs/zalo/README.md](docs/zalo/README.md).

## VietQR — dùng nhanh

Tài liệu đầy đủ: **[docs/vietqr](docs/vietqr/README.md)** — cấu hình, cấu trúc payload, giới hạn nội dung,
[65 ngân hàng](docs/vietqr/banks.md), [render ảnh có màu/logo](docs/vietqr/rendering.md),
[mã tham chiếu và đối soát](docs/vietqr/reconciliation.md),
[xác nhận thanh toán](docs/vietqr/payment-confirmation.md), bảng lỗi thường gặp.

```yaml
vietnam:
  vietqr:
    bank-bin: "970436"        # Vietcombank
    account-number: "0123456789"
    reference:
      prefix: "DH"
```

```java
@Service
class PaymentService {
    private final VietQrGenerator vietQr;
    private final PaymentReferenceFormat references;
    private final QrImageRenderer renderer;

    PaymentService(VietQrGenerator vietQr, PaymentReferenceFormat references, QrImageRenderer renderer) {
        this.vietQr = vietQr;
        this.references = references;
        this.renderer = renderer;
    }

    byte[] qrForOrder(Order order) {
        String reference = references.encode(order.code());
        String payload = vietQr.forReference(order.total(), reference);
        return renderer.renderPng(payload, 400);
    }
}
```

Để đối soát, đặt prefix cho mã tham chiếu, cung cấp một `PayableLookup`, rồi xử lý kết quả từ
`PaymentReconciler`:

```yaml
vietnam:
  vietqr:
    bank-bin: "970436"
    account-number: "0123456789"
    reference:
      prefix: "DH"
```

```java
import java.util.Optional;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;

import io.github.hoangluongtran0309.vietnam.vietqr.confirmation.PaymentConfirmation;
import io.github.hoangluongtran0309.vietnam.vietqr.reconciliation.Payable;
import io.github.hoangluongtran0309.vietnam.vietqr.reconciliation.PayableLookup;
import io.github.hoangluongtran0309.vietnam.vietqr.reconciliation.PayableStatus;
import io.github.hoangluongtran0309.vietnam.vietqr.reconciliation.PaymentReconciler;
import io.github.hoangluongtran0309.vietnam.vietqr.reconciliation.ReconciliationResult;

@Configuration
class ReconciliationConfig {
    @Bean
    PayableLookup payableLookup() {
        Payable payable = new Payable("DH123455", 150_000, PayableStatus.OPEN,
                "0123456789", "970436");
        return reference -> payable.reference().equals(reference)
                ? Optional.of(payable)
                : Optional.empty();
    }
}

@Component
class IncomingPaymentHandler {
    private final PaymentReconciler reconciler;

    IncomingPaymentHandler(PaymentReconciler reconciler) {
        this.reconciler = reconciler;
    }

    ReconciliationResult handle(PaymentConfirmation confirmation) {
        return reconciler.reconcile(confirmation);
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

## Đọc hóa đơn điện tử

`InvoiceInspector` đọc XML gốc, kiểm tra cấu trúc và số liệu, xác minh chữ ký rồi trả một báo cáo tổng hợp.
[Hướng dẫn Java thuần, cấu hình Spring Boot và demo HTTP](docs/einvoice/README.md).

## Chạy thử

```bash
./mvnw verify                          # build + test
./mvnw -q install -DskipTests          # lần đầu: để demo app dùng được các module trong repo
./mvnw -Psamples -pl samples/demo-app spring-boot:run
# mở http://localhost:8080/qr.png?amount=10000&orderId=DH1 và quét bằng app ngân hàng
# mở http://localhost:8080/checkout?amount=150000 để tạo khoản cần thu và mã tham chiếu giả lập
# mở http://localhost:8080/verify để có bộ QR kiểm tra thủ công (docs/vietqr/manual-verification.md)
```

## Đóng góp

Xem [CONTRIBUTING.md](CONTRIBUTING.md). Đặc biệt cần: [test vector thật từ các app ngân hàng](docs/vietqr/test-vectors.md), tài khoản sandbox của nhà cung cấp hóa đơn điện tử.

## Lưu ý pháp lý

Dự án độc lập, không liên kết với NAPAS, Zalo/VNG hay bất kỳ nhà cung cấp hóa đơn điện tử nào. Các tên thương hiệu thuộc về chủ sở hữu tương ứng.

## License

[Apache License 2.0](LICENSE)
