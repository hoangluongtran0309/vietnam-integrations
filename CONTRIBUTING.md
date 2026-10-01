# Đóng góp

Cảm ơn bạn! Issue và PR bằng tiếng Việt hoặc tiếng Anh đều được.

## Build

```bash
./mvnw verify
```

## Quy tắc

- Định danh trong code bằng tiếng Anh; docs ưu tiên tiếng Việt.
- Core module không import `org.springframework.*`.
- Mọi `@Bean` trong starter phải `@ConditionalOnMissingBean` và có test bằng `ApplicationContextRunner`.
- Test gọi HTTP dùng WireMock, không gọi API thật trong CI.
- Không commit credential, kể cả sandbox.

## Đóng góp được hoan nghênh nhất

- **Test vector VietQR** từ app ngân hàng thật (che số tài khoản nếu cần — có thể dùng tài khoản test).
- **Adapter hóa đơn điện tử**: tạo module `vietnam-einvoice-provider-<tên>`, implement `EInvoiceProvider`.
