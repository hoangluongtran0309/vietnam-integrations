# Đọc XML hóa đơn điện tử

`InvoiceXmlParser` trong `vietnam-einvoice-core` đọc XML có gốc `HDon` thành `InvoiceDocument`. Instance
parser bất biến, có thể dùng lại giữa các luồng. Kết quả giữ nguyên text của thẻ chuỗi và scale của
`BigDecimal`. Thẻ vắng mặt cho giá trị `null` theo read model. Thẻ số hoặc ngày rỗng hay chỉ chứa khoảng
trắng cũng cho `null`. Xem [bảng ánh xạ và quy tắc extensions](read-model.md#ánh-xạ-xml-sang-java).

## Cách dùng

Truyền `InputStream` hoặc `byte[]` để trình đọc xác định encoding theo khai báo XML hoặc BOM. Bên gọi
chịu trách nhiệm đóng stream. Mặc định giới hạn là 10 MiB (10.485.760 byte), tính cả BOM. Dùng
`maxInputBytes(long)` để đặt giới hạn dương khác. Mảng byte được kiểm trước khi đọc. Với stream, parser
dừng ngay khi đọc được byte đầu tiên vượt giới hạn.

Parser mặc định cho phép tối đa 100.000 phần tử, tính cả gốc `HDon` và các phần tử ngoài model. Dùng
`maxElements(int)` để đặt giới hạn dương khác. Parser dừng trước khi thêm phần tử vượt giới hạn vào cây.

```java
import io.github.hoangluongtran0309.vietnam.einvoice.document.InvoiceDocument;
import io.github.hoangluongtran0309.vietnam.einvoice.xml.InvoiceXmlParser;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

final class InvoiceXmlExample {
    private static final InvoiceXmlParser DEFAULT_PARSER = InvoiceXmlParser.defaults();
    private static final InvoiceXmlParser LIMITED_PARSER =
            InvoiceXmlParser.builder()
                    .maxInputBytes(2L * 1024 * 1024)
                    .maxElements(25_000)
                    .build();

    static InvoiceDocument readFile(Path file) throws IOException {
        try (InputStream input = Files.newInputStream(file)) {
            return LIMITED_PARSER.parse(input);
        }
    }

    static InvoiceDocument readBytes(byte[] xml) {
        return DEFAULT_PARSER.parse(xml);
    }
}
```

## Mã lỗi

`InvoiceXmlException` kế thừa `EInvoiceException`. Dùng `code()` để phân loại lỗi. `path()` có thể là
`null` nếu lỗi chưa gắn với thẻ cụ thể. Đường dẫn bắt đầu từ `HDon` và dùng local name, kể cả trong cây
chữ ký. Các thẻ có anh em cùng tên được đánh chỉ số từ 1. Lỗi trùng thẻ trỏ tới lần xuất hiện thứ hai.
Thông báo chỉ gồm mã lỗi, đường dẫn nếu có và mô tả ngắn.

| Mã | Trường hợp |
|---|---|
| `INPUT_TOO_LARGE` | Mảng byte hoặc stream vượt giới hạn. |
| `TOO_MANY_ELEMENTS` | Tài liệu vượt giới hạn số phần tử. |
| `MALFORMED_XML` | Đầu vào không phải XML hợp thức. |
| `DOCTYPE_NOT_ALLOWED` | Có khai báo DOCTYPE, kể cả khai báo nội bộ không có thực thể. |
| `UNEXPECTED_ROOT` | Local name của phần tử gốc khác `HDon`. |
| `DUPLICATE_ID` | Giá trị thuộc tính `Id` không namespace bị trùng. Mã này chỉ dùng trong [verifier](signatures.md), vì parser bỏ qua thuộc tính. |
| `DUPLICATE_ELEMENT` | Thẻ chỉ nhận một giá trị xuất hiện nhiều lần dưới cùng phần tử cha. |
| `INVALID_NUMBER` | Không chuyển được text của thẻ thành `BigDecimal`. |
| `INVALID_INTEGER` | Không chuyển được text của thẻ thành `Integer` 32 bit có dấu. |
| `INVALID_DATE` | `NLap` sai định dạng `yyyy-MM-dd` hoặc `SigningTime` sai định dạng ngày giờ ISO-8601 có offset. |
| `INVALID_CERTIFICATE` | Không đọc được dữ liệu Base64 thành chứng thư X.509. |
| `IO_ERROR` | Lỗi trong khi đọc stream. |

## Phạm vi xử lý

Sau khi parse, dùng [validator](validation.md) để kiểm tra trường bắt buộc và tính khớp của số tiền.
Dùng [verifier](signatures.md) trên byte XML gốc để xác minh chữ ký.
Parser giữ nguyên `PBan` ở mọi phiên bản. Chữ ký được đọc dưới dạng metadata, gồm vai trò, thời điểm ký đã
đổi sang `Instant` và thông tin từ chứng thư đầu tiên nếu có. Đầu vào là tài liệu `HDon` độc lập.
Cây `HDon/DSCKS` nằm ngoài phạm vi thu thập `extensions`. Parser bỏ qua thuộc tính XML khi tạo model
và `extensions`. Xem
[ADR-0012](../adr/0012-einvoice-xml-parser.md).

## Mức độ kiểm chứng

Bộ test hiện dùng XML tổng hợp tự dựng theo bảng thẻ từ bản sao thứ cấp của QĐ 1233/QĐ-CT. Các chữ ký
trong fixture chỉ phục vụ kiểm tra việc trích metadata và có `SignatureValue` giả. Kết quả test xác nhận
parser khớp với cách repo hiểu tài liệu. Khả năng tương thích với XML từ sandbox còn chờ kiểm chứng.
Xem [danh mục fixture](../../vietnam-einvoice-core/src/test/resources/einvoice/synthetic/README.md)
và [các điểm chưa xác minh](legal-basis.md#chưa-xác-minh).

Parser đã được kiểm với một hóa đơn thật phiên bản `2.1.0` trên máy của chủ dự án. Tệp XML được lưu
ngoài repo và chưa từng được commit. Trong mẫu này, thẻ nghiệp vụ không có namespace, `NLap` có dạng
`yyyy-MM-dd` và số tiền có tối đa 6 chữ số thập phân. `HVTNMHang` là trường người mua duy nhất được ánh
xạ. Mẫu có `MCCQT` và `DSCKS` không chứa chữ ký số. Nhà cung cấp bổ sung nhiều khối `TTKhac/TTin`.
Các thẻ `STT`, `DLQRCode`, `KBHang`, `TGHang` và `MDHang` được giữ
trong `extensions`. Ba thẻ chiết khấu được ánh xạ vào [read model](read-model.md). Ý nghĩa của các thẻ này chưa được xác minh.

<!-- TODO(v0.3): verify the meaning of the unmapped tags — provider XML format documentation. -->

Để chạy lại, đặt các tệp XML trong một thư mục riêng ngoài repo và trỏ `EINVOICE_REAL_SAMPLES_DIR`
đến thư mục đó. Test báo số lần theo trạng thái chữ ký, trạng thái tin cậy và vai trò. Các issue của validator
được tổng hợp theo mã, mức độ và đường dẫn chuẩn hóa. Khi không đặt biến môi trường,
JUnit bỏ qua `RealInvoiceSamplesTest`.

```bash
EINVOICE_REAL_SAMPLES_DIR=/path/to/private/invoice-xml \
  ./mvnw -B -pl vietnam-einvoice-core -am test \
  -Dtest=RealInvoiceSamplesTest -Dsurefire.failIfNoSpecifiedTests=false
```
