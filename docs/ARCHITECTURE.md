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

## VietQR payload

Cấu trúc payload (bảng tag, ví dụ phân tích từng trường) nằm ở
[vietqr/README.md#cấu-trúc-payload](vietqr/README.md#cấu-trúc-payload).
Vẫn còn `TODO(v0.1): verify` với đặc tả chính thức của NAPAS (chưa được công bố công khai).
