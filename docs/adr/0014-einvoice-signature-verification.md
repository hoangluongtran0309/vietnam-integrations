# ADR-0014 — Xác minh chữ ký số XML của hóa đơn điện tử

**Trạng thái** — Accepted, ngày 2026-11-26.

## Bối cảnh

Parser đọc metadata chữ ký vào read model và validator kiểm cấu trúc, số liệu. Ứng dụng cần xác minh chữ ký và
xem phạm vi dữ liệu được ký trên XML gốc. Nguồn thuật toán, đường dẫn XMLDSig và cấu trúc XAdES hiện có là bản sao
phụ lục QĐ 1233/QĐ-CT. [Cơ sở định dạng chữ ký](../einvoice/legal-basis.md#chữ-ký-số-trong-xml).

## Quyết định

- Tách `InvoiceSignatureVerifier` khỏi parser và validator, nhận XML gốc dưới dạng byte và trả các chữ ký theo
  thứ tự, vai trò của parser. Kết quả có thể chuyển thành rule ứng dụng cho cùng hóa đơn thông qua `asRule()`.
- Chỉ dùng JDK. Giới hạn mặc định là 10 MiB và 100.000 phần tử, tính cả gốc. Dùng SAX namespace-aware có
  secure processing để đếm phần tử trước khi dựng DOM. Cả hai lượt đều chặn DOCTYPE, thực thể và tài nguyên
  XML bên ngoài. Dùng XMLDSig của JDK với secure validation bật tường minh và danh sách cho phép cho thuật
  toán chữ ký, digest, canonicalization và transform.
- Từ chối `Id` trùng trong toàn tài liệu. Giới hạn resolver ở URI rỗng và fragment Id trong cùng DOM. Chỉ ghi
  nhận phạm vi ký khi tham chiếu đã xác minh trỏ đúng nút nghiệp vụ trực tiếp hoặc ký toàn tài liệu theo dạng
  enveloped.
- Chọn khóa từ chứng thư X.509 đầu tiên trong `KeyInfo/X509Data`. Dựng chuỗi từ chứng thư nhúng và chứng thư ứng
  dụng cấp, rồi dùng `CertPathValidator` PKIX với anchor tường minh và tắt kiểm tra thu hồi. Dựng chuỗi trong bộ
  nhớ tránh việc tải issuer qua AIA khi JVM bật tính năng này. Gộp ứng viên cùng subject và khóa công khai,
  ưu tiên chứng thư ứng dụng cấp. Giới hạn mỗi đường đi ở 10 chứng thư tính cả lá và chưa tính anchor, với
  tối đa 1.000 lượt thử mở rộng đường đi cho mỗi chữ ký.
  [Tài liệu PKI của JDK 17](https://docs.oracle.com/en/java/javase/17/security/java-pki-programmers-guide.html).
- Dùng thời điểm ký khai báo để kiểm hiệu lực chứng thư và xác minh chuỗi. Tách kết quả xác minh chữ ký khỏi kết
  quả tin cậy để ứng dụng có thể phân biệt lỗi dữ liệu ký với lỗi chuỗi chứng thư.

## Hệ quả

- Kiểm tra hoạt động trên byte gốc và chứng thư cấp sẵn. Các lỗi của từng chữ ký có trạng thái riêng, còn lỗi
  đầu vào chung dùng `InvoiceXmlException`.
- `InvoiceValidator` chỉ sinh mã `EINV-SIG-*` khi ứng dụng thêm rule. Thiếu chữ ký cơ quan thuế trên hóa đơn
  có mã sinh cảnh báo. Quy tắc không bắt buộc chữ ký người bán vì NĐ 254 có ngoại lệ.
- Xác minh timestamp, thu hồi qua OCSP/CRL và đầy đủ profile XAdES nằm ngoài phạm vi.
  [Hướng dẫn xác minh chữ ký](../einvoice/signatures.md).
