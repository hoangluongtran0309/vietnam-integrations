# Checklist nghiệm thu hóa đơn điện tử

Dùng checklist này để gỡ nhãn Preview theo [ADR-0010](../adr/0010-preview-releases.md). Với mỗi mẫu, ghi nhà
cung cấp, ngày capture, loại hóa đơn, phiên bản XML và kết quả dự kiến. Với kết quả kiểm dữ liệu thật, ghi mã,
mức độ và đường dẫn issue. [Danh sách test vector theo phiên bản](legal-basis.md#test-vector-cần-có).

Giữ bản gốc ở nơi lưu trữ riêng tư. Fixture chính trong repo phải là capture được phép chia sẻ, đã redact, có
ngày capture và nằm dưới `src/test/resources/**/captures/`. Nếu redact làm thay đổi dữ liệu đã ký, kiểm chữ ký
hợp lệ trên bản gốc trong môi trường riêng. Ghi riêng kỳ vọng của bản đã redact và lưu bằng chứng đối chiếu an toàn.

## Chữ ký người bán và cơ quan thuế

- [ ] Thu hóa đơn đã ký từ ít nhất hai nhà cung cấp, gồm chữ ký người bán và chữ ký cơ quan thuế trên hóa đơn
  có mã. Đối chiếu tên thuộc tính ID (`Id`, `ID`, `id`), URI tham chiếu, phạm vi `DLHDon` và `MCCQT`,
  thuật toán chữ ký, digest và transform. Ghi nhận vị trí chứng thư ký và thứ tự chuỗi trong `X509Data`.
  [TODO ID trong code][signature-id] · [TODO thứ tự chứng thư trong code][certificate-order] ·
  [Hai TODO tương ứng trong tài liệu][signature-id-doc] và [thứ tự chứng thư][certificate-order-doc].
- [ ] Đối chiếu namespace XAdES và đường dẫn `SigningTime` với mẫu thật, phụ lục kỹ thuật và XSD chính thức.
  Dùng một bản đã sửa dữ liệu sau khi ký để kiểm lỗi digest hoặc phạm vi chữ ký.
  [TODO namespace và đường dẫn ký][signing-time].

## Hóa đơn ký trước 2025

- [ ] Thu mẫu thực sự ký trước 2025. Ghi nhận thuật toán chữ ký và digest để xác minh SHA-1 còn xuất hiện trên
  hóa đơn lưu hành hay không. Ghi kết quả trên phiên bản JDK thử nghiệm và đối chiếu mã `EINV-SIG-003` nếu gặp
  thuật toán ngoài danh sách hỗ trợ. [TODO SHA-1 trong code][sha1] · [TODO SHA-1 trong tài liệu][sha1-doc].

## Hóa đơn bán hàng

- [ ] Thu hóa đơn bán hàng có và không có mã, ưu tiên mẫu có cờ `HDDCKPTQuan` và mẫu có điều kiện áp dụng khác
  nhau. Đối chiếu tập giá trị, trường hợp bắt buộc của cờ và các đường dẫn mà `EINV-REQ-003` báo. Đối chiếu
  với nhóm trường bắt buộc cho GTGT, gồm `HDCTTChinh`. [TODO ký hiệu mẫu số][template] · [TODO cờ bán hàng][sales] ·
  [TODO cờ thuê tài chính][leasing] · [TODO bắt buộc và tổng VND][required-doc] ·
  [Các dòng bảng ánh xạ mẫu số][template-doc], [cờ bán hàng][sales-doc] và [cờ thuê tài chính][leasing-doc].

## Ngoại tệ, nhiều thuế suất và làm tròn

- [ ] Thu hóa đơn ngoại tệ và hóa đơn GTGT có nhiều thuế suất. Đối chiếu tỷ giá, scale tiền, giá trị
  `TSuat` trên dòng và nhóm, cùng cách đối chiếu tổng theo từng thuế suất. [TODO thuế suất dòng][line-rate] ·
  [TODO thuế suất nhóm][group-rate] · [Hai dòng bảng ánh xạ][line-rate-doc] và [thuế suất nhóm][group-rate-doc].
- [ ] Thu mẫu VND có phần lẻ trước khi tổng hợp và kết quả tính được nhà cung cấp xác nhận. Tìm hướng dẫn chính
  thức về giới hạn tổng VND, phương pháp làm tròn tiền và thuế, và cấp độ làm tròn theo dòng hay theo nhóm
  thuế suất. [TODO phép tính trong code][rounding] · [TODO cơ sở pháp lý][rounding-doc] ·
  [TODO trong bảng mã][required-doc].

## Tính chất dòng và chiết khấu

- [ ] Thu mẫu có `TChat` khác `1`, gồm dòng khuyến mại, chiết khấu hoặc diễn giải. Đối chiếu danh sách giá trị,
  ảnh hưởng đến tổng và trường hợp sinh `EINV-AMT-900`. [TODO read model][nature] ·
  [TODO validator][nature-rule] · [TODO bảng ánh xạ][nature-doc] · [TODO bảng mã][nature-codes].
- [ ] Thu mẫu có chiết khấu dòng và chiết khấu thương mại. Đối chiếu ý nghĩa và cách tính của `TLCKhau`,
  `STCKhau` và `TTCKTMai` với phụ lục chính thức. [TODO InvoiceItem][item-discount] ·
  [TODO InvoiceTotals][totals-discount] · [TODO tài liệu read model][discount-doc] ·
  [TODO bảng mã][discount-codes].

## Mã số thuế cá nhân

- [ ] Đối chiếu mẫu thật có MST cá nhân 12 chữ số với hướng dẫn của Bộ Tài chính. Xác minh định dạng và thuật
  toán chữ số kiểm tra. Chỉ ghi kết quả đối chiếu dưới dạng mã, mức độ và đường dẫn. [TODO validator][tax-id] ·
  [TODO bảng mã][tax-id-doc].

## Bản gốc quyết định kỹ thuật và XSD

- [ ] Thu bản ký chính thức của QĐ 1233/QĐ-CT và QĐ 1306/QĐ-CT cùng toàn bộ phụ lục, các gói XSD chính thức,
  phiên bản, nguồn tải và điều khoản tái phân phối. Đối chiếu `PBan`, tên thẻ, cardinality, danh sách giá trị và
  trường bắt buộc của từng loại hóa đơn. [TODO phiên bản][version] · [TODO danh sách thẻ trong code][tag-list] ·
  [TODO phiên bản trong bảng][version-doc] · [TODO tên thẻ và cardinality][tag-list-doc] ·
  [TODO trường bắt buộc][required-doc] · [XSD và giấy phép](legal-basis.md#xsd-và-giấy-phép).
- [ ] Đối chiếu lớp bọc thông điệp cơ quan thuế, namespace thẻ nghiệp vụ và định dạng `NLap` giữa XSD và XML
  của các nhà cung cấp. [TODO lớp bọc][wrapper] · [TODO namespace][business-ns] · [TODO định dạng ngày][date].

## Thẻ mở rộng của nhà cung cấp

- [ ] Thu tài liệu định dạng của nhà cung cấp và mẫu có thẻ chưa ánh xạ. Đối chiếu ý nghĩa của từng thẻ mở rộng
  và ghi trường hợp cần đưa vào read model. [TODO các thẻ chưa ánh xạ][extensions-doc].

[signature-id]: ../../vietnam-einvoice-core/src/main/java/io/github/hoangluongtran0309/vietnam/einvoice/signature/SignatureXml.java#L116
[certificate-order]: ../../vietnam-einvoice-core/src/main/java/io/github/hoangluongtran0309/vietnam/einvoice/signature/InvoiceSignatureVerifier.java#L141
[signature-id-doc]: signatures.md?plain=1#L101
[certificate-order-doc]: signatures.md?plain=1#L103
[signing-time]: ../../vietnam-einvoice-core/src/main/java/io/github/hoangluongtran0309/vietnam/einvoice/xml/InvoiceXmlParser.java#L319
[sha1]: ../../vietnam-einvoice-core/src/main/java/io/github/hoangluongtran0309/vietnam/einvoice/signature/InvoiceSignatureVerifier.java#L54
[sha1-doc]: signatures.md?plain=1#L102
[template]: ../../vietnam-einvoice-core/src/main/java/io/github/hoangluongtran0309/vietnam/einvoice/document/InvoiceHeader.java#L13
[sales]: ../../vietnam-einvoice-core/src/main/java/io/github/hoangluongtran0309/vietnam/einvoice/document/InvoiceHeader.java#L20
[leasing]: ../../vietnam-einvoice-core/src/main/java/io/github/hoangluongtran0309/vietnam/einvoice/document/InvoiceHeader.java#L18
[required-doc]: validation-codes.md?plain=1#L62
[template-doc]: read-model.md?plain=1#L42
[sales-doc]: read-model.md?plain=1#L47
[leasing-doc]: read-model.md?plain=1#L46
[line-rate]: ../../vietnam-einvoice-core/src/main/java/io/github/hoangluongtran0309/vietnam/einvoice/document/InvoiceItem.java#L24
[group-rate]: ../../vietnam-einvoice-core/src/main/java/io/github/hoangluongtran0309/vietnam/einvoice/document/TaxRateSummary.java#L9
[line-rate-doc]: read-model.md?plain=1#L68
[group-rate-doc]: read-model.md?plain=1#L69
[rounding]: ../../vietnam-einvoice-core/src/main/java/io/github/hoangluongtran0309/vietnam/einvoice/InvoiceLine.java#L18
[rounding-doc]: legal-basis.md?plain=1#L164
[nature]: ../../vietnam-einvoice-core/src/main/java/io/github/hoangluongtran0309/vietnam/einvoice/document/InvoiceItem.java#L11
[nature-rule]: ../../vietnam-einvoice-core/src/main/java/io/github/hoangluongtran0309/vietnam/einvoice/validation/DefaultInvoiceRules.java#L141
[nature-doc]: read-model.md?plain=1#L59
[nature-codes]: validation-codes.md?plain=1#L60
[item-discount]: ../../vietnam-einvoice-core/src/main/java/io/github/hoangluongtran0309/vietnam/einvoice/document/InvoiceItem.java#L8
[totals-discount]: ../../vietnam-einvoice-core/src/main/java/io/github/hoangluongtran0309/vietnam/einvoice/document/InvoiceTotals.java#L8
[discount-doc]: read-model.md?plain=1#L83
[discount-codes]: validation-codes.md?plain=1#L59
[tax-id]: ../../vietnam-einvoice-core/src/main/java/io/github/hoangluongtran0309/vietnam/einvoice/validation/DefaultInvoiceRules.java#L305
[tax-id-doc]: validation-codes.md?plain=1#L61
[version]: ../../vietnam-einvoice-core/src/main/java/io/github/hoangluongtran0309/vietnam/einvoice/document/InvoiceHeader.java#L10
[tag-list]: ../../vietnam-einvoice-core/src/main/java/io/github/hoangluongtran0309/vietnam/einvoice/document/package-info.java#L5
[version-doc]: read-model.md?plain=1#L40
[tag-list-doc]: read-model.md?plain=1#L189
[wrapper]: ../../vietnam-einvoice-core/src/main/java/io/github/hoangluongtran0309/vietnam/einvoice/xml/InvoiceXmlParser.java#L138
[business-ns]: ../../vietnam-einvoice-core/src/main/java/io/github/hoangluongtran0309/vietnam/einvoice/xml/InvoiceXmlParser.java#L470
[date]: ../../vietnam-einvoice-core/src/main/java/io/github/hoangluongtran0309/vietnam/einvoice/xml/InvoiceXmlParser.java#L260
[extensions-doc]: parser.md?plain=1#L94
