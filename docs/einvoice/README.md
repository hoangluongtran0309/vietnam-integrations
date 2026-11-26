# Đọc và kiểm tra hóa đơn điện tử

Bản **Preview 0.3.0** đọc XML hóa đơn đã phát hành, kiểm tra trường bắt buộc và số liệu, xác minh chữ ký
XMLDSig và hỗ trợ kiểm tra chuỗi chứng thư PKIX ngoại tuyến. Phát hành hóa đơn qua nhà cung cấp chưa có.

## Java thuần

Thêm dependency `io.github.hoangluongtran0309:vietnam-einvoice-core:0.3.0`. Truyền byte XML gốc để trình đọc
nhận đúng encoding và verifier kiểm tra chữ ký trên cùng đầu vào.

```java
import io.github.hoangluongtran0309.vietnam.einvoice.inspection.InvoiceInspection;
import io.github.hoangluongtran0309.vietnam.einvoice.inspection.InvoiceInspector;

final class InvoiceInspectionExample {
    private static final InvoiceInspector INSPECTOR = InvoiceInspector.defaults();

    static InvoiceInspection inspect(byte[] originalXml) {
        return INSPECTOR.inspect(originalXml);
    }

    static boolean isValid(byte[] originalXml) {
        return inspect(originalXml).report().isValid();
    }
}
```

Kết quả gồm `document()`, `signatures().checks()` và `report().issues()`. `isValid()` trả `true` khi
báo cáo không có issue `ERROR`. Xử lý `InvoiceXmlException` bằng `code()` và `path()` khi không đọc được
đầu vào. Dùng `InvoiceInspector.builder().verifySignatures(false).build()` nếu chỉ cần parse và kiểm tra dữ
liệu. Kết quả kiểm chữ ký khi đó là danh sách rỗng, còn read model vẫn có metadata chữ ký đọc từ XML.

## Spring Boot

Thêm dependency `io.github.hoangluongtran0309:vietnam-einvoice-spring-boot-starter:0.3.0`, hoặc bỏ version
của dependency nếu đã import `vietnam-bom:0.3.0`. Inject `InvoiceInspector` để đọc và kiểm tra XML. Starter
tạo các bean đọc XML ngay cả khi chưa đặt `vietnam.einvoice.provider`.

```yaml
vietnam:
  einvoice:
    xml:
      max-input-size: 10MB
      max-elements: 100000
    validation:
      amount-tolerance: 1
    signature:
      enabled: true
      # Mặc định hai danh sách chứng thư đều rỗng.
      # trust-anchors:
      #   - classpath:certs/root.pem
      # intermediate-certificates:
      #   - file:/path/to/intermediate.der
```

- `xml.max-input-size` và `xml.max-elements` áp dụng cho cả parser và verifier.
- `validation.amount-tolerance` là dung sai tuyệt đối theo đơn vị tiền tệ của hóa đơn. Giá trị âm làm
  ứng dụng dừng khởi động.
- `signature.enabled` điều khiển bước xác minh và các issue chữ ký của `InvoiceInspector`.
- Mỗi phần tử trong hai danh sách chứng thư trỏ tới một chứng thư X.509 dạng PEM hoặc DER. Khi không đọc
  được chứng thư, starter báo tên property và chỉ số phần tử từ 0. Nếu chưa có trust anchor, trạng thái
  tin cậy là `NOT_CHECKED`.

Có thể thay từng bean `InvoiceXmlParser`, `InvoiceSignatureVerifier`, `InvoiceValidator` và
`InvoiceInspector` bằng bean cùng kiểu. Validator mặc định chạy rule có sẵn rồi đến các bean
`InvoiceRule` theo thứ tự Spring. Có thể dùng `@Order`. Validator dùng bean `Clock` được Spring chọn
qua `ObjectProvider.getIfUnique()`, hoặc đồng hồ mặc định theo múi giờ Việt Nam. Inspector thêm các
issue chữ ký sau các rule của validator.

```java
import io.github.hoangluongtran0309.vietnam.einvoice.validation.InvoiceRule;
import io.github.hoangluongtran0309.vietnam.einvoice.validation.Severity;
import io.github.hoangluongtran0309.vietnam.einvoice.validation.ValidationIssue;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;

import java.util.List;

@Configuration(proxyBeanMethods = false)
class InvoiceRulesConfiguration {
    @Bean
    @Order(10)
    InvoiceRule buyerDetailsRule() {
        return document -> document.buyer() == null
                ? List.of(new ValidationIssue("APP-BUYER-001", Severity.WARNING,
                        "HDon/DLHDon/NDHDon/NMua", "Buyer details are required by this application."))
                : List.of();
    }
}
```

## Demo HTTP

Chạy từ thư mục gốc của repo.

```bash
./mvnw -B -Psamples -pl samples/demo-app -am -DskipTests install
./mvnw -B -Psamples -pl samples/demo-app spring-boot:run
```

Dùng fixture tổng hợp có sẵn để thử endpoint.

```bash
curl -sS -F 'file=@samples/demo-app/src/test/resources/einvoice/synthetic/sales-invoice.xml' \
  http://localhost:8080/einvoice/inspect
```

HTTP 200 trả `isValid`, `document` với header rút gọn, số dòng, tổng thanh toán và cờ có mã cơ quan
thuế, cùng `issues` và `signatures`. Mỗi issue có `code`, `severity`, `path`, `message`.
Mỗi chữ ký có `role`, `path`, `status`, `trust`, `algorithm`, `signingTime`. Lỗi XML trả HTTP 400
với `code` và `path`. Demo giữ upload hợp lệ trong bộ nhớ và không ghi nội dung hóa đơn vào log.

## Phạm vi và giới hạn

- Giới hạn mặc định là 10 MiB và 100.000 phần tử, gồm cả gốc. Demo giới hạn mỗi tệp và toàn bộ request
  multipart ở 10 MiB, nên dung lượng XML tối đa qua HTTP còn phụ thuộc phần bao multipart.
- Đầu vào là tài liệu XML có gốc `HDon`. Parser từ chối DOCTYPE và chặn tải tài nguyên XML bên ngoài.
  [Parser và mã lỗi](parser.md) · [Read model và ánh xạ](read-model.md).
- Các rule kiểm tính nhất quán và các trường nằm trong phạm vi đã mô hình hóa. Kiểm XSD và các trường
  có điều kiện của hóa đơn chuyên ngành nằm ngoài phạm vi. [Validator](validation.md) ·
  [Bảng mã kiểm tra](validation-codes.md).
- Verifier kiểm chữ ký và phạm vi dữ liệu được ký. Kiểm tra thu hồi chứng thư, timestamp tin cậy và đầy đủ
  profile XAdES nằm ngoài phạm vi. [Xác minh chữ ký và trạng thái tin cậy](signatures.md).

## Mức độ kiểm chứng

**Preview** theo [ADR-0010](../adr/0010-preview-releases.md). Test dùng XML tổng hợp, chữ ký tạo bằng JDK
và MockMvc để kiểm luồng từ upload qua starter tới core. Đợt kiểm cục bộ với XML thật mới có bằng chứng
đọc và kiểm cấu trúc trên một mẫu. Việc nghiệm thu chữ ký thật từ nhiều nhà cung cấp và đối chiếu XSD
chính thức vẫn còn mở. [Checklist nghiệm thu và các điểm chưa xác minh](acceptance-checklist.md).
