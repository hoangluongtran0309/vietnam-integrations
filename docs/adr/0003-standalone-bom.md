# ADR-0003: BOM không kế thừa parent

**Trạng thái:** Accepted

## Bối cảnh
Parent POM import `spring-boot-dependencies`. Nếu BOM kế thừa parent, người dùng import BOM sẽ vô tình import luôn
toàn bộ BOM Spring Boot của phiên bản mình build, có thể đè lên phiên bản Spring Boot của họ.

## Quyết định
`vietnam-bom` là POM độc lập, chỉ quản lý version các artifact của dự án.

## Hệ quả
Phải đồng bộ version BOM khi release (workflow đã xử lý).
