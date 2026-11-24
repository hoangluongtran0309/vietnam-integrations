# Read model cho hóa đơn điện tử đã phát hành

## Mục đích

Package `io.github.hoangluongtran0309.vietnam.einvoice.document` biểu diễn dữ liệu đã đọc từ XML hóa đơn do một
đơn vị khác phát hành. Model này giữ lại cả tài liệu thiếu hoặc sai trường để parser và validator ở các work package
sau có thể báo lỗi đúng vị trí.

Read model độc lập với model phát hành trong package `io.github.hoangluongtran0309.vietnam.einvoice`. Model phát
hành nhận dữ liệu đầu vào để tự tính thành tiền, tiền thuế và làm tròn; read model chỉ lưu số liệu người bán đã ghi,
không import hoặc chuyển đổi sang `Invoice`, `InvoiceLine`, `Party`, `TaxRate`, `IssuedInvoice` hay `InvoiceStatus`.

## Nguyên tắc

- Không làm tròn, tự tính tổng hoặc tính lại thuế. `BigDecimal` giữ nguyên scale của giá trị XML.
- Mọi scalar và nhóm dữ liệu đều có thể `null`, kể cả khi bảng nguồn gọi thẻ đó là bắt buộc. Ba list và
  `extensions` luôn không-null, bất biến và được sao chép phòng vệ. List từ chối phần tử `null` và map từ chối
  key hoặc value `null`.
- Phiên bản, ký hiệu mẫu số và thuế suất được giữ dưới dạng `String`; tính chất dòng và hai cờ hóa đơn dùng
  `Integer`. Read model không dùng enum khi danh sách giá trị hợp lệ chưa được xác minh.
- `InvoiceDocument.extensions` giữ text gốc của thẻ chưa được mô hình hóa. Key là đường dẫn XML đầy đủ bắt đầu
  từ `HDon`. Ở mỗi bước, phần tử có anh em cùng tên mang chỉ số bắt đầu từ 1 theo cú pháp XPath, ví dụ
  `HDon/DLHDon/NDHDon/DSHHDVu/HHDVu[2]/STCKhau`. Phần tử duy nhất mang tên đó trong nhóm anh em được ghi
  bằng tên thẻ. Parser sau này chịu trách nhiệm điền map.
- `InvoiceParty.toString()` che tên, mã số thuế, địa chỉ và họ tên người mua. `InvoiceDocument.toString()` chỉ in
  key của `extensions`, không in value chưa biết có chứa dữ liệu cá nhân hay không.
- Chữ ký chỉ lưu vai trò, thời điểm ký và metadata chứng thư. Model không giữ `X509Certificate`, byte chữ ký hoặc
  kết quả xác minh.

## Ánh xạ XML sang Java

Tên thẻ trong bảng dưới đây lấy từ bảng trường bắt buộc của
[cơ sở pháp lý và định dạng XML](legal-basis.md#trường-bắt-buộc-của-hóa-đơn-gtgt-và-hóa-đơn-bán-hàng). Đây là bản
sao thứ cấp của QĐ 1233/QĐ-CT, chưa được đối chiếu với bản gốc hoặc XSD chính thức. Cột “Có thể null” mô tả Java
API, không phải tính bắt buộc pháp lý của XML.

| Đường dẫn thẻ XML | Field Java | Kiểu | Có thể null | Ghi chú |
|---|---|---|---|---|
| `HDon/DLHDon/TTChung/PBan` | `InvoiceHeader.formatVersion` | `String` | Có | Nguồn thứ cấp; TODO(v0.3): verify danh sách giá trị — bản gốc QĐ 1233/QĐ-CT và XSD chính thức |
| `HDon/DLHDon/TTChung/THDon` | `InvoiceHeader.invoiceName` | `String` | Có | Nguồn thẻ thứ cấp |
| `HDon/DLHDon/TTChung/KHMSHDon` | `InvoiceHeader.invoiceTemplateSymbol` | `String` | Có | Giữ giá trị gốc; TODO(v0.3): verify danh sách giá trị — bản gốc QĐ 1233/QĐ-CT và XSD chính thức |
| `HDon/DLHDon/TTChung/KHHDon` | `InvoiceHeader.invoiceSymbol` | `String` | Có | Nguồn thẻ thứ cấp |
| `HDon/DLHDon/TTChung/SHDon` | `InvoiceHeader.invoiceNumber` | `String` | Có | Dùng `String` để giữ biểu diễn gốc, kể cả số 0 đầu |
| `HDon/DLHDon/TTChung/NLap` | `InvoiceHeader.issueDate` | `LocalDate` | Có | Nguồn thẻ thứ cấp |
| `HDon/DLHDon/TTChung/HDCTTChinh` | `InvoiceHeader.financialLeasingFlag` | `Integer` | Có | Giữ giá trị gốc; TODO(v0.3): verify danh sách giá trị — bản gốc QĐ 1233/QĐ-CT và XSD chính thức |
| `HDon/DLHDon/TTChung/HDDCKPTQuan` | `InvoiceHeader.nonTariffAreaFlag` | `Integer` | Có | Giữ giá trị gốc; TODO(v0.3): verify danh sách giá trị — bản gốc QĐ 1233/QĐ-CT và XSD chính thức |
| `HDon/DLHDon/TTChung/DVTTe` | `InvoiceHeader.currencyCode` | `String` | Có | Nguồn thẻ thứ cấp |
| `HDon/DLHDon/TTChung/TGia` | `InvoiceHeader.exchangeRate` | `BigDecimal` | Có | Giữ nguyên scale |
| `HDon/DLHDon/TTChung/HTTToan` | `InvoiceHeader.paymentMethod` | `String` | Có | Nguồn thẻ thứ cấp |
| `HDon/DLHDon/TTChung/MSTTCGP` | `InvoiceHeader.solutionProviderTaxCode` | `String` | Có | Nguồn thẻ thứ cấp |
| `HDon/DLHDon/NDHDon/NBan/Ten` | `InvoiceParty.name` của `seller` | `String` | Có | Được che trong `toString()` |
| `HDon/DLHDon/NDHDon/NBan/MST` | `InvoiceParty.taxCode` của `seller` | `String` | Có | Được che trong `toString()` |
| `HDon/DLHDon/NDHDon/NBan/DChi` | `InvoiceParty.address` của `seller` | `String` | Có | Được che trong `toString()` |
| `HDon/DLHDon/NDHDon/NMua/Ten` | `InvoiceParty.name` của `buyer` | `String` | Có | Có điều kiện; được che trong `toString()` |
| `HDon/DLHDon/NDHDon/NMua/MST` | `InvoiceParty.taxCode` của `buyer` | `String` | Có | Có điều kiện; được che trong `toString()` |
| `HDon/DLHDon/NDHDon/NMua/DChi` | `InvoiceParty.address` của `buyer` | `String` | Có | Có điều kiện; được che trong `toString()` |
| `HDon/DLHDon/NDHDon/NMua/HVTNMHang` | `InvoiceParty.buyerFullName` | `String` | Có | Có điều kiện; được che trong `toString()` |
| `HDon/DLHDon/NDHDon/DSHHDVu/HHDVu/TChat` | `InvoiceItem.nature` | `Integer` | Có | Giữ giá trị gốc; TODO(v0.3): verify danh sách giá trị — bản gốc QĐ 1233/QĐ-CT và XSD chính thức |
| `HDon/DLHDon/NDHDon/DSHHDVu/HHDVu/MHHDVu` | `InvoiceItem.itemCode` | `String` | Có | Có điều kiện |
| `HDon/DLHDon/NDHDon/DSHHDVu/HHDVu/THHDVu` | `InvoiceItem.name` | `String` | Có | Nguồn thẻ thứ cấp |
| `HDon/DLHDon/NDHDon/DSHHDVu/HHDVu/DVTinh` | `InvoiceItem.unit` | `String` | Có | Có điều kiện |
| `HDon/DLHDon/NDHDon/DSHHDVu/HHDVu/SLuong` | `InvoiceItem.quantity` | `BigDecimal` | Có | Có điều kiện; giữ nguyên scale |
| `HDon/DLHDon/NDHDon/DSHHDVu/HHDVu/DGia` | `InvoiceItem.unitPrice` | `BigDecimal` | Có | Có điều kiện; giữ nguyên scale |
| `HDon/DLHDon/NDHDon/DSHHDVu/HHDVu/ThTien` | `InvoiceItem.amount` | `BigDecimal` | Có | Không tự tính lại; giữ nguyên scale |
| `HDon/DLHDon/NDHDon/DSHHDVu/HHDVu/TSuat` | `InvoiceItem.taxRate` | `String` | Có | Giữ nguyên text; TODO(v0.3): verify danh sách giá trị — bản gốc QĐ 1233/QĐ-CT và XSD chính thức |
| `HDon/DLHDon/NDHDon/TToan/THTTLTSuat/LTSuat/TSuat` | `TaxRateSummary.taxRate` | `String` | Có | Giữ nguyên text; TODO(v0.3): verify danh sách giá trị — bản gốc QĐ 1233/QĐ-CT và XSD chính thức |
| `HDon/DLHDon/NDHDon/TToan/THTTLTSuat/LTSuat/ThTien` | `TaxRateSummary.amount` | `BigDecimal` | Có | Không tự cộng lại; giữ nguyên scale |
| `HDon/DLHDon/NDHDon/TToan/THTTLTSuat/LTSuat/TThue` | `TaxRateSummary.taxAmount` | `BigDecimal` | Có | Có điều kiện; giữ nguyên scale |
| `HDon/DLHDon/NDHDon/TToan/TgTCThue` | `InvoiceTotals.totalAmountBeforeTax` | `BigDecimal` | Có | Không tự cộng lại; giữ nguyên scale |
| `HDon/DLHDon/NDHDon/TToan/TgTThue` | `InvoiceTotals.totalTaxAmount` | `BigDecimal` | Có | Không tự cộng lại; giữ nguyên scale |
| `HDon/DLHDon/NDHDon/TToan/TgTTTBSo` | `InvoiceTotals.totalPaymentAmount` | `BigDecimal` | Có | Không tự cộng lại; giữ nguyên scale |
| `HDon/DLHDon/NDHDon/TToan/TgTTTBChu` | `InvoiceTotals.totalPaymentAmountInWords` | `String` | Có | Nguồn thẻ thứ cấp |
| `HDon/MCCQT` | `InvoiceDocument.taxAuthorityCode` | `String` | Có | Có điều kiện |
| `HDon/DSCKS/NBan/ds:Signature` | phần tử `InvoiceDocument.signatures`, role `SELLER` | `InvoiceSignature` | Không | List không-null, có thể rỗng; không giữ byte chữ ký |
| `HDon/DSCKS/CQT/ds:Signature` | phần tử `InvoiceDocument.signatures`, role `TAX_AUTHORITY` | `InvoiceSignature` | Không | List không-null, có thể rỗng; không giữ byte chữ ký |

Phần mô tả chữ ký trong `legal-basis.md` còn nêu chữ ký người mua và metadata XAdES. Chúng được ánh xạ riêng như
sau; tên thẻ, namespace và cardinality vẫn là dữ liệu từ nguồn thứ cấp:

| Đường dẫn thẻ XML | Field Java | Kiểu | Có thể null | Ghi chú |
|---|---|---|---|---|
| `HDon/DSCKS/NMua/ds:Signature` | phần tử `InvoiceDocument.signatures`, role `BUYER` | `InvoiceSignature` | Không | List không-null, có thể rỗng |
| `HDon/DSCKS/NBan/ds:Signature/ds:Object/xades:QualifyingProperties/xades:SignedProperties/xades:SignedSignatureProperties/xades:SigningTime` | `InvoiceSignature.signingTime` | `Instant` | Có | Chỉ lưu giá trị trích xuất |
| `HDon/DSCKS/NMua/ds:Signature/ds:Object/xades:QualifyingProperties/xades:SignedProperties/xades:SignedSignatureProperties/xades:SigningTime` | `InvoiceSignature.signingTime` | `Instant` | Có | Chỉ lưu giá trị trích xuất |
| `HDon/DSCKS/CQT/ds:Signature/ds:Object/xades:QualifyingProperties/xades:SignedProperties/xades:SignedSignatureProperties/xades:SigningTime` | `InvoiceSignature.signingTime` | `Instant` | Có | Chỉ lưu giá trị trích xuất |
| `HDon/DSCKS/NBan/ds:Signature/ds:KeyInfo/ds:X509Data/ds:X509Certificate` | `InvoiceSignature.certificate` → `CertificateInfo.*` | `CertificateInfo` | Có | Subject, issuer, serial, `notBefore`, `notAfter` đều có thể null |
| `HDon/DSCKS/NMua/ds:Signature/ds:KeyInfo/ds:X509Data/ds:X509Certificate` | `InvoiceSignature.certificate` → `CertificateInfo.*` | `CertificateInfo` | Có | Không giữ certificate hoặc byte mã hóa |
| `HDon/DSCKS/CQT/ds:Signature/ds:KeyInfo/ds:X509Data/ds:X509Certificate` | `InvoiceSignature.certificate` → `CertificateInfo.*` | `CertificateInfo` | Có | Không lưu kết quả xác minh |
| Đường dẫn XML đầy đủ bắt đầu từ `HDon` cho các thẻ ngoài model | `InvoiceDocument.extensions` | `Map<String, String>` | Không | Ở mỗi bước, dùng chỉ số XPath từ 1 cho phần tử có anh em cùng tên. Phần tử duy nhất mang tên đó chỉ cần tên thẻ. Value là text gốc. |

## Ví dụ tạo read model bằng Java

Ví dụ dùng dữ liệu giả cho hai dòng hàng. Hai giá trị `STCKhau` có key riêng nhờ chỉ số `HHDVu[1]` và `HHDVu[2]`.

```java
import io.github.hoangluongtran0309.vietnam.einvoice.document.InvoiceDocument;
import io.github.hoangluongtran0309.vietnam.einvoice.document.InvoiceHeader;
import io.github.hoangluongtran0309.vietnam.einvoice.document.InvoiceItem;
import io.github.hoangluongtran0309.vietnam.einvoice.document.InvoiceParty;
import io.github.hoangluongtran0309.vietnam.einvoice.document.InvoiceTotals;
import io.github.hoangluongtran0309.vietnam.einvoice.document.TaxRateSummary;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

InvoiceHeader header = new InvoiceHeader(
        "2.1.1",
        "Hóa đơn giá trị gia tăng",
        "1",
        "C26TAA",
        "00000001",
        LocalDate.of(2026, 11, 24),
        null,
        null,
        "VND",
        null,
        "Chuyển khoản",
        "SOLUTION_PROVIDER_TAX_CODE");

InvoiceParty seller = new InvoiceParty(
        "SELLER_NAME", "SELLER_TAX_CODE", "SELLER_ADDRESS", null);

InvoiceItem firstItem = new InvoiceItem(
        1,
        "ITEM_1",
        "Dịch vụ mẫu thứ nhất",
        "lần",
        new BigDecimal("1.00"),
        new BigDecimal("1100"),
        new BigDecimal("1000"),
        "10%");

InvoiceItem secondItem = new InvoiceItem(
        1,
        "ITEM_2",
        "Dịch vụ mẫu thứ hai",
        "lần",
        new BigDecimal("1.00"),
        new BigDecimal("2200"),
        new BigDecimal("2000"),
        "10%");

TaxRateSummary taxSummary = new TaxRateSummary(
        "10%", new BigDecimal("3000"), new BigDecimal("300"));

InvoiceTotals totals = new InvoiceTotals(
        new BigDecimal("3000"),
        new BigDecimal("300"),
        new BigDecimal("3300"),
        "Ba nghìn ba trăm đồng");

InvoiceDocument document = new InvoiceDocument(
        header,
        seller,
        null,
        List.of(firstItem, secondItem),
        List.of(taxSummary),
        totals,
        null,
        List.of(),
        Map.of(
                "HDon/DLHDon/NDHDon/DSHHDVu/HHDVu[1]/STCKhau", "100.00",
                "HDon/DLHDon/NDHDon/DSHHDVu/HHDVu[2]/STCKhau", "200.00"));
```

Đối tượng trên chỉ là dữ liệu đã đọc. Việc trường nào bắt buộc, tổng tiền có khớp hay chữ ký có hợp lệ không thuộc
validator và signature verifier ở các work package sau.

## Chưa xác minh

Danh sách thẻ, namespace, cardinality, tập giá trị mã hóa, profile chữ ký và quy tắc làm tròn vẫn còn các điểm chưa
xác minh. Xem mục [Chưa xác minh](legal-basis.md#chưa-xác-minh) và danh sách test vector cần thu trong cùng tài liệu.

TODO(v0.3): verify tên thẻ, cardinality và danh sách giá trị — bản gốc QĐ 1233/QĐ-CT và gói XSD chính thức.

Quyết định tách model đọc khỏi model phát hành được ghi tại
[ADR-0011](../adr/0011-einvoice-read-model.md).
