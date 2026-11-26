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
2. `git tag v0.3.0 && git push origin v0.3.0`
3. Workflow `release.yml` sẽ set version theo tag, ký, và publish.
4. Kiểm tra trên Central Portal; artifact xuất hiện trên search.maven.org sau vài phút đến vài giờ.

## Thử local (không publish)

```bash
./mvnw -Prelease -Dgpg.skip verify
```

Kiểm tra mỗi module có đủ `-sources.jar` và `-javadoc.jar` trong `target/`.

## Smoke test sau khi publish

Sau khi Maven Central đã có đủ artifact của version mới, chạy smoke test từ một local repository
rỗng. Script import BOM, resolve ba starter, rồi biên dịch và chạy các phép thử VietQR, mã tham chiếu,
Zalo registry và `InvoiceInspector` trên XML hóa đơn tổng hợp. Bước chạy Java chỉ dùng dữ liệu cục bộ và
in kết quả `isValid` của hóa đơn.

```bash
./scripts/smoke-test-central.sh 0.3.0
```

Có thể kiểm một repository đã có artifact 0.3.0 trước khi phiên bản này xuất hiện trên Central.

```bash
./scripts/smoke-test-central.sh 0.3.0 --repo file:///path/to/maven/repository
```

Chỉ coi bước này hoàn tất khi script in `SMOKE TEST OK <version>`.

## Sau khi phát hành

1. Tạo GitHub Release từ tag. Lấy mục tương ứng trong `CHANGELOG.md`. Đổi các liên kết tương đối
   sang `https://github.com/hoangluongtran0309/vietnam-integrations/blob/v<version>/...` vì chúng bị hỏng
   trên trang Release.
2. Cập nhật badge, bảng Trạng thái và ví dụ cài đặt trong `README.md`. Cập nhật dependency mẫu trong
   `docs/*/README.md`.
3. Đánh dấu dòng Release tương ứng trong `docs/ROADMAP.md`.
4. Nâng version phát triển lên `<version kế tiếp>-SNAPSHOT`. Ví dụ sau khi phát hành 0.3.0:

   ```bash
   NEXT_VERSION=0.4.0-SNAPSHOT
   ./mvnw -B versions:set -DnewVersion="$NEXT_VERSION" -DprocessAllModules -DgenerateBackupPoms=false
   ./mvnw -B -f vietnam-bom/pom.xml versions:set -DnewVersion="$NEXT_VERSION" -DgenerateBackupPoms=false
   ```

   Nếu `samples/demo-app/pom.xml` chưa đổi vì nằm trong profile `samples`, sửa version của parent trong file đó.
5. Chạy lại `git grep` để kiểm tra badge và ví dụ cài đặt còn dùng version cũ. Điều chỉnh mẫu theo các
   version cũ của từng lần phát hành. Ví dụ sau khi phát hành 0.3.0, lệnh sau phải trả về rỗng:

   ```bash
   git grep -nE '<version>0\.[12]\.0</version>|status-0\.2\.0' -- '*.md' ':!CHANGELOG.md' ':!docs/adr/'
   ```
