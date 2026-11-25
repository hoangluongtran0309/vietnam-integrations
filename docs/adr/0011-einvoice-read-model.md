# ADR-0011: Read model cho hóa đơn điện tử đã phát hành

**Trạng thái:** Accepted — chấp nhận ngày 2026-11-24.

## Bối cảnh

Model phát hành hiện tại tự tính tiền thuế và làm tròn từng dòng theo `HALF_UP`. Khi đọc hóa đơn XML đã phát hành,
thư viện cần giữ nguyên số liệu do người bán ghi để có thể báo chính xác trường thiếu, sai hoặc không khớp ở bước
kiểm tra sau. Các văn bản đã đọc chưa quy định phương pháp làm tròn chung hoặc cấp độ tính thuế theo dòng hay theo
nhóm thuế suất. Danh sách thẻ và đặc tả định dạng hiện mới có trong bản sao thứ cấp của QĐ 1233/QĐ-CT, chưa được
đối chiếu với bản gốc hoặc XSD chính thức.

## Quyết định

Read model nằm trong package `io.github.hoangluongtran0309.vietnam.einvoice.document` và độc lập với model phát
hành. Package mới không dùng các type `Invoice`, `InvoiceLine`, `Party`, `TaxRate`, `IssuedInvoice` hoặc
`InvoiceStatus`.

Read model phản ánh đúng nội dung XML, kể cả khi tài liệu thiếu hoặc sai trường bắt buộc. Constructor không kiểm quy
tắc nghiệp vụ, định dạng mã số thuế hay tính khớp của các tổng tiền. Collection `null` được đổi thành collection rỗng,
được sao chép phòng vệ và không chấp nhận phần tử `null`; các field có thể vắng mặt được giữ là `null`.

Số tiền, số lượng và tỷ giá dùng `BigDecimal`, giữ nguyên scale, không làm tròn, tự tính lại hoặc cung cấp method tính
tổng và thuế. Phiên bản định dạng, ký hiệu mẫu số và thuế suất được giữ dưới dạng `String`; tính chất dòng và các cờ
hóa đơn được giữ dưới dạng `Integer`. Danh sách giá trị hợp lệ của các trường này phải được xác minh với bản gốc QĐ
1233/QĐ-CT và XSD chính thức trước khi cân nhắc kiểu mạnh hơn.

Model chỉ khai báo các thẻ trong bảng trường bắt buộc của
[cơ sở pháp lý](../einvoice/legal-basis.md#trường-bắt-buộc-của-hóa-đơn-gtgt-và-hóa-đơn-bán-hàng). Các thẻ khác được
giữ trong `InvoiceDocument.extensions`. Key là đường dẫn XML đầy đủ bắt đầu từ `HDon`, value là text gốc. Ở mỗi
bước, phần tử có anh em cùng tên mang chỉ số bắt đầu từ 1 theo cú pháp XPath, ví dụ
`HDon/DLHDon/NDHDon/DSHHDVu/HHDVu[2]/STT`. Phần tử duy nhất mang tên đó trong nhóm anh em được ghi
bằng tên thẻ. Javadoc của từng field ghi đường dẫn thẻ XML tương ứng.

Thông tin bên bán và bên mua có thể là dữ liệu cá nhân nên `toString()` che tên, mã số thuế, địa chỉ và họ tên người
mua hàng. `InvoiceDocument.toString()` cũng không làm lộ các giá trị này qua record con.

Thông tin chữ ký chỉ gồm vai trò `SELLER`, `BUYER` hoặc `TAX_AUTHORITY`, thời điểm ký và metadata chứng thư có thể
trích xuất như subject, issuer, serial number và thời hạn hiệu lực. Model không chứa `X509Certificate`, byte chữ ký
hoặc kết quả xác minh; việc xác minh thuộc một work package sau.

## Phương án đã loại

- **Dùng chung model phát hành và model đọc:** phương án này buộc phải đổi public API, tạo breaking change và trộn
  số liệu thư viện tự tính với số liệu người bán đã ghi trong XML.
- **Tạo enum ngay cho thuế suất và ký hiệu mẫu số:** danh sách giá trị hợp lệ chưa được đối chiếu với bản gốc quyết
  định hoặc XSD chính thức, nên enum có thể loại nhầm giá trị hợp lệ và làm mất biểu diễn gốc.

## Hệ quả

- Validator phải tự kiểm các trường bắt buộc, điều kiện áp dụng và tính khớp của tổng tiền; read model không bảo đảm
  hóa đơn hợp lệ.
- Parser phải giữ nguyên scale và giá trị mã hóa, đồng thời đưa các thẻ ngoài phạm vi model vào `extensions`.
- Khi có bản gốc QĐ 1233/QĐ-CT hoặc XSD chính thức, API có thể bổ sung enum hoặc kiểu mạnh hơn bên cạnh giá trị gốc
  mà không xóa giá trị gốc.
