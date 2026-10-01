# CLAUDE.md

Hướng dẫn cho Claude Code khi làm việc trong repo này.

## Dự án
Vietnam Integrations — multi-module Maven: thư viện + Spring Boot 4 starters cho VietQR, Zalo OA, hóa đơn điện tử. Java 17 baseline.
Kế hoạch: `docs/ROADMAP.md`. Kiến trúc: `docs/ARCHITECTURE.md`. Quyết định: `docs/adr/`.

## Lệnh
- Build + test toàn bộ: `./mvnw verify`
- Một module: `./mvnw -pl vietnam-vietqr-core -am test`
- Sample: `./mvnw -Psamples -pl samples/demo-app spring-boot:run`

## Quy tắc bắt buộc
1. `vietnam-*-core` KHÔNG được import `org.springframework.*` hay Jackson. Kiểm tra trước khi kết thúc.
2. Mọi `@Bean` trong starter: `@ConditionalOnMissingBean`. Auto-config mới phải được thêm vào
   `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports` và có test `ApplicationContextRunner`.
3. Không đổi public API của core mà không cập nhật `CHANGELOG.md`.
4. Không đoán đặc tả bên ngoài (NAPAS, Zalo, nhà cung cấp hóa đơn). Nếu chưa xác minh được, để `TODO(vX.Y): verify ...`
   và nói rõ trong tóm tắt.
5. Test HTTP dùng WireMock; không gọi API thật.
6. Định danh tiếng Anh; Javadoc tiếng Anh; docs trong `docs/` tiếng Việt.

## Definition of Done cho một task
- `./mvnw verify` xanh
- Có test cho hành vi mới (ưu tiên test vector thật hơn mock)
- Docs/README cập nhật nếu thay đổi cách dùng
- ADR mới nếu là quyết định kiến trúc
