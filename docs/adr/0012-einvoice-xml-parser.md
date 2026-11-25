# ADR-0012 — Parser XML hóa đơn điện tử

**Trạng thái** — Accepted, ngày 2026-11-25.

## Bối cảnh

Ứng dụng cần đọc XML hóa đơn đã phát hành vào read model của
[ADR-0011](0011-einvoice-read-model.md). Bảng thẻ dựa trên bản sao thứ cấp của QĐ 1233/QĐ-CT. Repo hiện có
fixture XML tổng hợp. Khả năng tương thích với hóa đơn thực và XML từ sandbox vẫn cần xác minh.
Xem [cơ sở pháp lý và định dạng XML](../einvoice/legal-basis.md).

## Quyết định

- Đọc tài liệu có phần tử gốc mang local name `HDon`. Việc đọc lớp bọc thông điệp cơ quan thuế chờ đối chiếu
  bản gốc QĐ 1233/QĐ-CT.
- Dùng local name cho các thẻ nghiệp vụ vì namespace chính thức chưa được xác minh. Đối với cấu trúc chữ ký,
  nhận diện phần tử DSig theo namespace XMLDSig và tìm `SigningTime` theo local name trong `ds:Object`
  vì namespace XAdES chưa được xác minh.
- Giữ `PBan` dưới dạng chuỗi và chấp nhận mọi giá trị của thẻ này. Nhờ vậy ứng dụng có thể đọc dữ liệu lưu trữ,
  kể cả tài liệu mang phiên bản cũ hoặc chưa biết. Khả năng đọc đúng từng phiên bản phụ thuộc vào cấu trúc thẻ
  thực tế. Việc kiểm tra phiên bản và quy tắc nghiệp vụ thuộc bước kiểm tra riêng.
- Trích vai trò, thời điểm ký và metadata chứng thư X.509. Việc xác minh chữ ký, tài liệu được tham chiếu,
  chuỗi tin cậy và trạng thái chứng thư thuộc bước xác minh riêng.
- Dùng SAX của JDK với secure processing. Từ chối mọi DOCTYPE ngay khi trình đọc báo khai báo. Tắt thực thể
  ngoài, tải DTD ngoài và XInclude. Đặt quyền truy cập DTD và schema ngoài thành chuỗi rỗng.
- Áp dụng giới hạn 10 MiB mặc định. Kiểm kích thước mảng byte trước khi đọc và đếm byte trong khi đọc stream.
  Trình đọc lấy encoding từ khai báo XML hoặc BOM.
- Giới hạn mặc định 100.000 phần tử XML trên mỗi tài liệu, tính cả gốc và các phần tử ngoài model.
  Đếm phần tử khi SAX báo thẻ mở và dừng trước khi thêm phần tử vượt giới hạn vào cây.

## Giới hạn

Parser bỏ qua thuộc tính XML khi tạo model và `extensions`.

## Hệ quả

Parser cung cấp mã lỗi ổn định và đường dẫn theo local name khi xác định được thẻ liên quan. Dữ liệu nghiệp vụ
giữ biểu diễn gốc theo read model. Các test chỉ kiểm ánh xạ và cách parser xử lý đầu vào theo tài liệu hiện có.
Cần tài liệu gốc và XML thật đã redact để kiểm tra tương thích định dạng. Xem [hướng dẫn parser](../einvoice/parser.md).
