# Test vector VietQR thật

Mỗi file `*.properties` trong thư mục này là **một payload thu thập từ app ngân hàng thật** và được
`RealPayloadVectorsTest` decode, so với giá trị mong đợi.

- Hướng dẫn thu thập, định dạng, cách che số tài khoản: [docs/vietqr/test-vectors.md](../../../../../docs/vietqr/test-vectors.md)
- Bắt đầu bằng cách sao chép `example.properties.template` thành `<ngan-hang>-<truong-hop>.properties`
  (ví dụ `mbbank-static.properties`). File `.template` không được test đọc.
- Không bao giờ tự tạo payload bằng encoder của thư viện rồi đặt vào đây: vector phải đến từ hệ thống khác.
