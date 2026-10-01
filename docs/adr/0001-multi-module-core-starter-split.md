# ADR-0001: Tách module core và starter

**Trạng thái:** Accepted

## Bối cảnh
Người dùng có thể dùng Quarkus, Micronaut, hoặc Java thuần. Logic VietQR/Zalo/hóa đơn không cần Spring.

## Quyết định
Mỗi tính năng có `vietnam-<feature>-core` (Java thuần) và `vietnam-<feature>-spring-boot-starter` (chỉ auto-configuration).
Theo đúng quy ước đặt tên starter bên thứ ba của Spring Boot: `<name>-spring-boot-starter`, không bắt đầu bằng `spring-boot`.

## Hệ quả
- Core test nhanh, không cần Spring context.
- Starter mỏng, dễ theo kịp các bản Spring Boot mới.
- Nhiều artifact hơn → cần BOM (ADR-0003).
