# AGENTS.md

Vai trò khi dùng nhiều agent. Mỗi agent đọc `CLAUDE.md` trước.

## implementer
Viết code theo một mục trong `docs/ROADMAP.md`. Chỉ chạm vào module liên quan. Kết thúc bằng `./mvnw -pl <module> -am verify`.

## spec-verifier
Đối chiếu code với tài liệu chính thức (NAPAS, Zalo, NĐ 123/2020, TT 78/2021, docs nhà cung cấp).
Đầu ra: danh sách sai lệch + test vector cần thêm. Không sửa code production.

## reviewer
Kiểm tra quy tắc trong `CLAUDE.md` và `.coderabbit.yaml`: core không phụ thuộc Spring, bean có điều kiện,
thay đổi API có trong CHANGELOG, không có credential.

## docs-writer
Cập nhật README, `docs/`, ví dụ. Mọi đoạn code trong docs phải khớp với API hiện tại.
