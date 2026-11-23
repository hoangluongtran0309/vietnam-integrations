# Phát hành lên Maven Central

## Thiết lập một lần

1. Đăng nhập https://central.sonatype.com bằng GitHub → namespace `io.github.hoangluongtran0309` được verify tự động.
   - Nếu muốn dùng `dev.hoangluongtran`: thêm namespace đó và verify bằng bản ghi DNS TXT.
   - Đổi namespace là breaking change (groupId + package), nên phải chốt **trước 0.1.0**.
2. Tạo **user token** trong Central Portal (Account → Generate User Token).
3. Tạo GPG key:
   ```bash
   gpg --full-generate-key                  # RSA 4096
   gpg --list-secret-keys --keyid-format=long
   gpg --keyserver keyserver.ubuntu.com --send-keys <KEY_ID>
   gpg --armor --export-secret-keys <KEY_ID>   # dán vào secret GPG_PRIVATE_KEY
   ```
4. Thêm GitHub secrets: `CENTRAL_TOKEN_USERNAME`, `CENTRAL_TOKEN_PASSWORD`, `GPG_PRIVATE_KEY`, `GPG_PASSPHRASE`.

## Mỗi lần release

1. Cập nhật `CHANGELOG.md` qua PR (`main` được bảo vệ, xem [CONTRIBUTING.md](../CONTRIBUTING.md#quy-trình-pr)), merge, rồi `git pull` để tag đúng commit trên `main`.
2. `git tag v0.2.0 && git push origin v0.2.0`
3. Workflow `release.yml` sẽ set version theo tag, ký, và publish.
4. Kiểm tra trên Central Portal; artifact xuất hiện trên search.maven.org sau vài phút đến vài giờ.
5. Tạo GitHub Release từ tag, dán phần CHANGELOG.

## Thử local (không publish)

```bash
./mvnw -Prelease -Dgpg.skip verify
```

Kiểm tra mỗi module có đủ `-sources.jar` và `-javadoc.jar` trong `target/`.

## Smoke test sau khi publish

Sau khi Maven Central đã có đủ artifact của version mới, chạy smoke test từ một local repository
rỗng. Script import BOM, resolve ba starter, compile/runs VietQR, mã tham chiếu và Zalo registry mà không
gọi API ngoài trong bước chạy Java:

```bash
./scripts/smoke-test-central.sh 0.2.0
```

Có thể kiểm một repository khác trước khi artifact xuất hiện trên Central:

```bash
./scripts/smoke-test-central.sh 0.2.0-SNAPSHOT --repo file:///path/to/maven/repository
```

Chỉ coi bước này hoàn tất khi script in `SMOKE TEST OK <version>`.
