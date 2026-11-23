# ADR-0010: Phát hành tích hợp ở mức Preview

**Trạng thái:** Accepted — chấp nhận ngày 2026-11-23.

## Bối cảnh

ROADMAP đặt nguyên tắc chỉ làm tích hợp có thể kiểm tra với hệ thống thật. Tuy nhiên, chủ dự án hiện không có
Zalo Official Account để nghiệm thu các module Zalo OA/ZBS với dịch vụ thật. Các module trong dự án lại dùng chung
một phiên bản qua BOM, nên trì hoãn Zalo cũng trì hoãn phần VietQR đối soát đã hoàn tất.

Bản Zalo 0.1.0 đang có trên Maven Central yếu hơn code hiện tại: công thức chữ ký webhook chưa được đối chiếu và
refresh token chưa được xoay atomically giữa nhiều instance. Giữ người dùng ở bản đó trong lúc chờ OA thật làm tăng
rủi ro vận hành, dù code mới đã có test đơn vị, test end-to-end với Zalo giả bằng WireMock và PostgreSQL thật, cùng
bằng chứng đối chiếu với SDK khác.

## Quyết định

Một tích hợp chưa được nghiệm thu với hệ thống thật vẫn có thể phát hành ở mức **Preview** khi đồng thời thỏa mãn
bốn điều kiện:

1. README, CHANGELOG và trang tài liệu của module ghi rõ nhãn Preview và không ngụ ý đã kiểm với hệ thống thật.
2. Có danh sách các điểm chưa xác minh, trỏ được tới `TODO` tương ứng trong code.
3. Có test end-to-end với hệ thống giả để kiểm tra việc nối dây giữa các lớp tích hợp.
4. Có checklist nghiệm thu thật nêu rõ bằng chứng cần thu để gỡ nhãn.

Nhãn Preview chỉ được gỡ sau khi tích hợp đã được nghiệm thu với hệ thống thật và fixture chính đã được thay bằng
capture thật, đã redact và ghi ngày capture. Đối với Zalo OA/ZBS, checklist nghiệm thu nằm tại
[checklist capture Zalo thật](../zalo/capture-checklist.md).

## Phương án đã cân nhắc

- **Chờ tới khi có OA thật mới phát hành 0.2.0:** giữ đúng nguyên tắc ROADMAP theo nghĩa chặt nhất, nhưng chặn cả
  VietQR đối soát đã ổn định và để người dùng Zalo ở bản 0.1.0 có cơ chế token/chữ ký yếu hơn.
- **Không publish các module Zalo trong 0.2.0:** tránh phát hành phần chưa nghiệm thu thật, nhưng phải thay đổi build
  và BOM dùng chung, đồng thời Maven Central vẫn chỉ hiển thị Zalo 0.1.0 yếu hơn code hiện tại. Cách này cũng làm các
  module cùng phiên bản khó hiểu hơn cho người dùng.

## Hệ quả

- Bản 0.2.0 phát hành đầy đủ các module; VietQR là phần ổn định, còn Zalo OA/ZBS mang nhãn Preview.
- API Zalo có thể thay đổi trong 0.2.x nếu capture thật cho thấy giả định hiện tại không đúng. Mọi thay đổi phá vỡ
  sẽ có tiền tố **Breaking:** trong CHANGELOG cùng hướng dẫn nâng cấp.
- Test với Zalo giả và đối chiếu SDK là bằng chứng hữu ích nhưng không thay thế nghiệm thu App/OA, ZBS template,
  webhook, chính sách gửi tin, hạn mức hoặc giao nhận thực tế.
- Việc gỡ nhãn Preview cần một thay đổi tài liệu riêng kèm bằng chứng nghiệm thu và fixture capture đã redact.
