# Xác minh chữ ký số XML của hóa đơn điện tử

`InvoiceSignatureVerifier` trong `vietnam-einvoice-core` xác minh chữ ký trên mảng byte XML gốc. Nó kiểm các
phần tử `ds:Signature` dưới `HDon/DSCKS/NBan`, `HDon/DSCKS/NMua` và `HDon/DSCKS/CQT` theo thứ tự tài liệu,
giống `InvoiceXmlParser`. Các bước lặp cùng local name mang chỉ số từ 1, ví dụ `HDon/DSCKS/NBan/Signature[2]`.
Giới hạn mặc định là 10 MiB và 100.000 phần tử, gồm cả gốc và các phần tử ngoài read model. Đặt giới hạn dương
bằng `maxInputBytes(long)` và `maxElements(int)`. Verifier kiểm số byte trước khi đọc và dùng SAX đếm phần tử
trước khi dựng DOM. Vượt giới hạn phần tử trả lỗi `TOO_MANY_ELEMENTS`.

## Dùng cùng parser và validator

Truyền cùng mảng byte cho parser và verifier. Đăng ký `verification.asRule()` cho hóa đơn tương ứng nếu muốn
đưa kết quả chữ ký vào `ValidationReport`. Có thể dùng độc lập `verification.checks()` để đọc kết quả từng chữ
ký. `InvoiceValidator.defaults()` chỉ chạy các quy tắc mặc định. [Mã kiểm tra chữ ký](validation-codes.md#chữ-ký-số).

```java
import io.github.hoangluongtran0309.vietnam.einvoice.signature.InvoiceSignatureVerifier;
import io.github.hoangluongtran0309.vietnam.einvoice.validation.InvoiceValidator;
import io.github.hoangluongtran0309.vietnam.einvoice.validation.ValidationReport;
import io.github.hoangluongtran0309.vietnam.einvoice.xml.InvoiceXmlParser;

import java.security.cert.TrustAnchor;
import java.security.cert.X509Certificate;
import java.util.Collection;
import java.util.Set;

final class InvoiceSignatureExample {
    static ValidationReport validate(byte[] xml, Set<TrustAnchor> anchors,
                                     Collection<X509Certificate> intermediates) {
        var invoice = InvoiceXmlParser.defaults().parse(xml);
        var verification = InvoiceSignatureVerifier.builder()
                .trustAnchors(anchors)
                .intermediateCertificates(intermediates)
                .build()
                .verify(xml);
        return InvoiceValidator.builder()
                .addRule(verification.asRule())
                .build()
                .validate(invoice);
    }
}
```

Dùng `InvoiceSignatureVerifier.defaults()` hoặc truyền tập anchor rỗng nếu chỉ cần kiểm chữ ký và dữ liệu
được ký. Khi ghi log kết quả, chỉ dùng vai trò, trạng thái, mã issue và đường dẫn chuẩn hóa để tránh ghi
metadata chứng thư hay giá trị trên hóa đơn.

## Trạng thái chữ ký

| `SignatureStatus` | Ý nghĩa |
|---|---|
| `VALID` | Giá trị chữ ký và digest của các tham chiếu trong `SignedInfo` hợp lệ. Chữ ký phủ dữ liệu hóa đơn và chứng thư còn hiệu lực tại `SigningTime` nếu có. |
| `INVALID_SIGNATURE` | Giá trị chữ ký không xác minh được bằng khóa công khai của chứng thư nhúng. |
| `INVALID_REFERENCE` | URI bị từ chối, Id không tồn tại, hoặc không xác minh được digest của tham chiếu. |
| `REFERENCE_NOT_COVERING_INVOICE` | Chữ ký và digest hợp lệ nhưng chữ ký không phủ phần tử `HDon/DLHDon` trực tiếp. |
| `UNSUPPORTED_ALGORITHM` | Thuật toán chữ ký, digest, chuẩn hóa XML hoặc transform nằm ngoài danh sách cho phép. |
| `MISSING_CERTIFICATE` | `ds:KeyInfo/ds:X509Data` không có chứng thư X.509. |
| `CERTIFICATE_NOT_VALID_AT_SIGNING_TIME` | `SigningTime` nằm ngoài khoảng hiệu lực của chứng thư người ký. |
| `MALFORMED` | Cấu trúc chữ ký hoặc metadata không đọc được, ví dụ thiếu `SignedInfo`. Các chữ ký còn lại vẫn được kiểm. |

| `TrustStatus` | Ý nghĩa |
|---|---|
| `NOT_CHECKED` | Ứng dụng chưa cấp anchor hoặc trạng thái chữ ký khác `VALID`. |
| `TRUSTED` | Xác minh được chuỗi PKIX tại thời điểm ký khai báo, tới anchor do ứng dụng cấp. |
| `UNTRUSTED` | Đã cấu hình anchor nhưng không xác minh được chuỗi PKIX trong giới hạn tìm kiếm, hoặc thiếu thời điểm ký. |

## Thuật toán, tham chiếu và dữ liệu được ký

Verifier dùng XMLDSig của JDK với chế độ secure validation. `SignatureMethod` chấp nhận RSA với SHA-256,
SHA-384 hoặc SHA-512. `DigestMethod` chấp nhận SHA-256, SHA-384 hoặc SHA-512. Các transform gồm
`enveloped-signature` và canonicalization C14N 1.0, C14N 1.1, exclusive C14N, mỗi dạng canonicalization
đều hỗ trợ tùy chọn comments. SHA-1 trả `UNSUPPORTED_ALGORITHM`. Chứng thư X.509 đầu tiên trong
`KeyInfo/X509Data` theo thứ tự XML được coi là chứng thư ký. Verifier dùng khóa công khai của chứng thư này
để kiểm chữ ký. [Cơ sở định dạng và vị trí ký, nguồn thứ cấp](legal-basis.md#chữ-ký-số-trong-xml).

Verifier đăng ký thuộc tính `Id` không namespace trên mọi phần tử và dừng với `DUPLICATE_ID` nếu giá trị lặp.
Resolver chỉ nhận URI rỗng hoặc `#<id>` trỏ đến phần tử đã đăng ký trong cùng tài liệu. HTTP, URI tệp,
đường dẫn tương đối và XPointer bị từ chối. DOCTYPE và tải thực thể, DTD, schema ngoài đều bị chặn.

Để chống signature wrapping, chữ ký chỉ được tính là phủ dữ liệu hóa đơn khi tham chiếu đã xác minh trỏ đúng
nút `DLHDon` là con trực tiếp duy nhất của `HDon`, hoặc tham chiếu cả tài liệu qua URI rỗng có transform
`enveloped-signature`. Quy tắc tương tự áp dụng cho `MCCQT`. `coversTaxAuthorityCode` cung cấp thông tin về
phạm vi ký. Khi bật `asRule()`, chữ ký cơ quan thuế `VALID` nhưng thiếu phạm vi này sinh cảnh báo.

## Phạm vi tin cậy

Verifier dựng chuỗi PKIX trong bộ nhớ từ chứng thư nhúng và chứng thư trung gian do ứng dụng cấp. Với các ứng viên
có cùng subject và khóa công khai, verifier giữ một chứng thư và ưu tiên bản do ứng dụng cấp. Mỗi đường đi
có tối đa 10 chứng thư, gồm chứng thư lá và chưa tính anchor. Mỗi chữ ký có tối đa 1.000 lượt thử mở rộng đường
đi. Nếu hết giới hạn mà chưa xác minh được chuỗi, verifier trả `UNTRUSTED`.

Kiểm tra thu hồi qua OCSP và CRL, tải chứng thư qua AIA, xác minh timestamp và xác minh đầy đủ profile XAdES
nằm ngoài phạm vi. Vai trò trong kết quả được suy ra từ vị trí phần tử chữ ký.

`SigningTime` là giá trị khai báo trong XML. Kiểm tra digest của `SignedProperties` diễn ra nếu khối này được
`SignedInfo` tham chiếu. Khi không có thời điểm ký, verifier bỏ qua thời hạn chứng thư lá và trả `UNTRUSTED`
nếu đã cấp anchor. [ADR-0014](../adr/0014-einvoice-signature-verification.md).

## Cần đối chiếu

- TODO(v0.3): verify whether providers use other ID attribute names (ID, id) — real signed invoices.
- TODO(v0.3): verify whether invoices still in circulation use SHA-1 — real signed invoices before 2025.
- TODO(v0.3): verify certificate order in X509Data — real signed invoices.
