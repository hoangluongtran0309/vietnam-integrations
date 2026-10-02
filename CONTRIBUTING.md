# Đóng góp

Cảm ơn bạn! Issue và PR bằng tiếng Việt hoặc tiếng Anh đều được.

## Build

```bash
./mvnw verify
```

## Quy trình PR

Nhánh `main` được bảo vệ bằng ruleset `protect-main`, nên không push thẳng lên `main` được:

1. Tạo branch từ `main`, commit, push, mở PR.
2. CI phải xanh: check `build (17)` và `build (21)` là bắt buộc.
   CodeRabbit sẽ tự review PR; nên xử lý hoặc trả lời các comment của nó trước khi merge.
3. Merge bằng **Squash and merge** hoặc **Rebase and merge**. `main` yêu cầu lịch sử tuyến tính nên không dùng được merge commit.

Ruleset cũng chặn force push và xóa `main`. Repository admin được bypass để xử lý trường hợp khẩn cấp.

> Maintainer: nếu đổi tên job hoặc matrix Java trong `.github/workflows/ci.yml`, phải cập nhật danh sách
> required status checks của ruleset, nếu không mọi PR sẽ bị treo chờ check không bao giờ chạy.

## Quy tắc

- Định danh trong code bằng tiếng Anh; docs ưu tiên tiếng Việt.
- Core module không import `org.springframework.*`.
- Mọi `@Bean` trong starter phải `@ConditionalOnMissingBean` và có test bằng `ApplicationContextRunner`.
- Test gọi HTTP dùng WireMock, không gọi API thật trong CI.
- Không commit credential, kể cả sandbox.

## Đóng góp được hoan nghênh nhất

- **Test vector VietQR** từ app ngân hàng thật (che số tài khoản nếu cần — có thể dùng tài khoản test).
- **Adapter hóa đơn điện tử**: tạo module `vietnam-einvoice-provider-<tên>`, implement `EInvoiceProvider`.
