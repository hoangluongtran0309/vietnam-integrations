# Kiến trúc

## Bố cục module

```
vietnam-bom                              BOM cho người dùng (không kế thừa parent)
vietnam-<feature>-core                   Java thuần: domain, encoder, port interfaces
vietnam-<feature>-spring-boot-starter    @AutoConfiguration + @ConfigurationProperties
vietnam-einvoice-provider-<name>         Adapter cho từng nhà cung cấp (thêm dần)
samples/demo-app                         Ứng dụng mẫu, không publish
```

Mô hình ports & adapters:

- **Domain/core**: `VietQrEncoder`, `Invoice`, `ZaloToken`… không biết tới Spring.
- **Ports**: `ZaloTokenStore`, `ZaloOAuthClient`, `ZaloOaClient`, `EInvoiceProvider`,
  `PaymentConfirmationListener` (xác nhận thanh toán VietQR, [ADR-0006](adr/0006-payment-confirmation-spi.md)).
- **Adapters**: `InMemoryZaloTokenStore`, (sắp có) `JdbcZaloTokenStore`, `vietnam-einvoice-provider-*`.
- **Starter**: chỉ nối dây — đọc properties, tạo bean, mọi bean đều `@ConditionalOnMissingBean`.

## Quy ước

- Package gốc: `io.github.hoangluongtran0309.vietnam.<feature>`; auto-config ở `.<feature>.autoconfigure`.
- Prefix properties: `vietnam.vietqr`, `vietnam.zalo-oa`, `vietnam.einvoice`.
- Mọi field trong `@ConfigurationProperties` có Javadoc (sinh metadata cho IDE).
- Dependency nặng (ZXing, JDBC…) là `optional` và được bật bằng `@ConditionalOnClass`.
- Exception riêng cho từng module, là unchecked: `VietQrException`, `ZaloException`, `EInvoiceException`.

## VietQR payload (tham khảo nhanh)

| Tag | Ý nghĩa | Giá trị |
|---|---|---|
| 00 | Payload format | `01` |
| 01 | Point of initiation | `11` tĩnh / `12` động (có số tiền) |
| 38 | Merchant account | `00`=`A000000727`, `01`=[`00` BIN, `01` số TK], `02` service code |
| 53 | Tiền tệ | `704` |
| 54 | Số tiền | chỉ khi QR động |
| 58 | Quốc gia | `VN` |
| 62 | Additional data | `08` = nội dung chuyển khoản |
| 63 | CRC | CRC-16/CCITT-FALSE trên toàn bộ chuỗi tính cả `6304` |

Cần đối chiếu lại với đặc tả NAPAS chính thức trước khi release 0.1.0.
