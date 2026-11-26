# Kiểm tra hóa đơn điện tử đã parse

`InvoiceValidator` trong `vietnam-einvoice-core` kiểm tra trường bắt buộc, số liệu, tỷ giá, ngày và định dạng
mã số thuế. `ValidationReport.isValid()` trả về `true` khi không có issue `ERROR`. Issue `WARNING` và `INFO`
vẫn có mặt trong báo cáo. Xem [bảng mã và thứ tự issue](validation-codes.md).

## Sử dụng từ Java

```java
import io.github.hoangluongtran0309.vietnam.einvoice.validation.InvoiceValidator;
import io.github.hoangluongtran0309.vietnam.einvoice.validation.ValidationReport;
import io.github.hoangluongtran0309.vietnam.einvoice.xml.InvoiceXmlParser;

import java.io.InputStream;

final class InvoiceChecks {
    static ValidationReport check(InputStream xml) {
        var document = InvoiceXmlParser.defaults().parse(xml);
        return InvoiceValidator.defaults().validate(document);
    }
}
```

`errors()`, `warnings()` và `withCode(String)` giữ thứ tự trong báo cáo. Message của rule mặc định viết
bằng tiếng Anh và chỉ mô tả lỗi. `path` dùng local name và chỉ số 1-based cho phần tử có anh em cùng tên,
ví dụ `HDon/DLHDon/NDHDon/DSHHDVu/HHDVu[2]/ThTien`.

```java
import io.github.hoangluongtran0309.vietnam.einvoice.validation.InvoiceValidator;
import io.github.hoangluongtran0309.vietnam.einvoice.validation.Severity;
import io.github.hoangluongtran0309.vietnam.einvoice.validation.ValidationIssue;

import java.math.BigDecimal;
import java.time.Clock;
import java.util.List;

final class InvoicePolicy {
    static InvoiceValidator validator(Clock clock) {
        return InvoiceValidator.builder()
                .clock(clock)
                .amountTolerance(new BigDecimal("0.50"))
                .addRule(document -> document.buyer() == null
                        ? List.of(new ValidationIssue("APP-BUYER-001", Severity.WARNING,
                                "HDon/DLHDon/NDHDon/NMua", "Buyer details are required by this application."))
                        : List.of())
                .build();
    }
}
```

Rule ứng dụng chạy theo thứ tự đăng ký sau các rule mặc định. Exception từ rule ứng dụng được truyền tới
bên gọi. Message do ứng dụng cung cấp cần tránh ghi giá trị hóa đơn. `build()` từ chối clock, dung sai hoặc
rule null và dung sai âm. `validate(null)` ném `NullPointerException`.

## Dung sai và điều kiện kiểm tra

Mặc định dùng dung sai `1` theo đơn vị tiền tệ của hóa đơn và đồng hồ ở `Asia/Ho_Chi_Minh`. Ngày hiện tại được
tính trong múi giờ của `Clock` đã cấu hình. Ngày ký luôn được quy về `Asia/Ho_Chi_Minh`.

Các phép so sánh dùng `BigDecimal`. Với `diff` bằng trị tuyệt đối của số kỳ vọng trừ số đã ghi, `diff = 0`
không sinh issue, `0 < diff ≤ dung sai` sinh `INFO`, và `diff > dung sai` sinh `ERROR`. Dung sai là chính sách
ứng dụng vì các văn bản đã đối chiếu chưa quy định cách làm tròn chung. Đặt dung sai `0` để yêu cầu khớp chính xác.
[Xem cơ sở về làm tròn và tính thuế](legal-basis.md#làm-tròn-và-tính-thuế).

`TSuat` được so khớp nguyên văn khi ghép dòng và nhóm. Chỉ dạng phần trăm từ một đến ba chữ số nguyên,
có thể kèm phần thập phân, được dùng để tính thuế. Khi thiếu toán hạng, phép so sánh tương ứng được bỏ qua.
Validator ghi `EINV-AMT-900` khi tính chất dòng nằm ngoài phạm vi số học hoặc có khoản thu có thể chưa được mô hình hóa.

## Phạm vi

Validator kiểm tra tính nhất quán của read model cho hóa đơn thông thường. Dùng `addRule(verification.asRule())`
để đưa kết quả [xác minh chữ ký](signatures.md) từ XML gốc vào báo cáo. Kiểm XSD, độ dài trường và các trường có
điều kiện của người mua nằm ngoài phạm vi. `EINV-DATE-002` chỉ kiểm
quan hệ ngày dựa trên metadata thời điểm ký đã có. Định dạng MST được kiểm ở mức mẫu ký tự, với chữ số kiểm tra
và MST cá nhân 12 chữ số còn chờ đối chiếu.
