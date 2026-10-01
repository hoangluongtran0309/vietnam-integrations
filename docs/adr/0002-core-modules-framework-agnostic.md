# ADR-0002: Baseline Java 17, core không phụ thuộc framework

**Trạng thái:** Accepted (phần JSON: Proposed)

## Bối cảnh
Dự án cá nhân dùng Java 21, nhưng thư viện cần chạy được ở nhiều dự án nhất có thể. Spring Boot 4 yêu cầu tối thiểu Java 17.

## Quyết định
- `maven.compiler.release=17`. CI chạy cả 17 và 21.
- Core chỉ dùng JDK: `java.net.http.HttpClient`, `java.security`, records.
- **JSON (đề xuất, chốt ở Phase 2):** core định nghĩa interface `JsonCodec` nhỏ; starter cung cấp implementation
  bằng Jackson do Spring Boot quản lý. Tránh để core kéo Jackson 2 vs Jackson 3 vào classpath người dùng.

## Hệ quả
Không dùng được tính năng Java 21 (pattern matching cho switch đầy đủ, virtual threads API) trong code thư viện.
