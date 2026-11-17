# Mã tham chiếu và đối soát thanh toán

VietQR chỉ mang số tiền và nội dung chuyển khoản tới app ngân hàng. Khi giao dịch về tài khoản, ngân hàng
có thể đổi chữ hoa/thường, bỏ dấu hoặc thêm tiền tố/hậu tố. Vì vậy, so sánh toàn bộ nội dung với mã đơn
hàng không đủ tin cậy.

Core VietQR giải quyết phần thuần, không giữ trạng thái:

1. `PaymentReferenceFormat` sinh mã tham chiếu có ký tự kiểm tra và trích mã từ nội dung ngân hàng ghi nhận.
2. `PaymentReconciler` tra khoản cần thu qua `PayableLookup` của ứng dụng rồi trả về kết quả có kiểu.
3. Ứng dụng vẫn sở hữu đơn hàng, database, quy tắc cập nhật trạng thái và việc chống xử lý trùng.

Quyết định thiết kế và thuật toán ký tự kiểm tra nằm trong
[ADR-0009](../adr/0009-payment-reference-and-reconciliation.md).

## Chọn prefix

Prefix nhận diện không gian mã của ứng dụng, ví dụ `DH` cho đơn hàng. Prefix phải có 2–6 chữ cái
ASCII hoa `A–Z`, nên ngắn, ổn định và không chứa dữ liệu nghiệp vụ thay đổi theo từng đơn.

Phần body do ứng dụng cung cấp có 1–20 chữ hoặc số ASCII; `encode` tự viết hoa. Mã hoàn chỉnh gồm
`prefix + body + checkCharacter` và không dài quá 25 ký tự. Với prefix dài 6 ký tự, body vì thế chỉ còn tối
đa 18 ký tự.

```java
PaymentReferenceFormat format = PaymentReferenceFormat.of("DH");
String reference = format.encode("12345"); // DH123455
```

Không truyền lại prefix trong body: dùng `encode("12345")`, không dùng `encode("DH12345")`.

## Cấu hình Spring Boot và sinh QR

Khai báo prefix cùng tài khoản nhận mặc định:

```yaml
vietnam:
  vietqr:
    bank-bin: "970436"
    account-number: "0123456789"
    reference:
      prefix: "DH"
```

Khi có `vietnam.vietqr.reference.prefix`, starter tạo `PaymentReferenceFormat`. Nếu ứng dụng khai báo thêm một
bean `PayableLookup`, starter tạo `PaymentReconciler`. Thiếu lookup là hợp lệ: ứng dụng vẫn sinh được mã tham
chiếu và QR mà không có reconciler.

```java
@Service
class CheckoutService {

    private final PaymentReferenceFormat references;
    private final VietQrGenerator vietQr;

    CheckoutService(PaymentReferenceFormat references, VietQrGenerator vietQr) {
        this.references = references;
        this.vietQr = vietQr;
    }

    String qrPayload(String orderNumber, long total) {
        String reference = references.encode(orderNumber);
        return vietQr.forReference(total, reference);
    }
}
```

`forReference` chỉ chấp nhận mã hợp lệ theo format đã cấu hình rồi dùng chính mã đó làm `purpose`.
Mã sai prefix, sai ký tự kiểm tra hoặc chưa cấu hình prefix sẽ làm method ném `VietQrException`.

## Trích mã từ nội dung chuyển khoản

Giả sử QR mang mã `DH123455`, nhưng nội dung phía ngân hàng là:

```text
MBVCB.1234.DH123455.CT tu 0123
```

`PaymentReferenceFormat.extract(...)` vẫn trả về `DH123455`. Method bỏ dấu, không phân biệt chữ hoa/thường,
xem dấu câu là dấu tách, tìm được cả mã bị dính vào chữ xung quanh và loại mã lặp. Chỉ ứng viên có
ký tự kiểm tra đúng mới được giữ lại.

Một chuỗi dính liền có thể vô tình tạo ra nhiều độ dài đều hợp lệ. Core không tự đoán; reconciler trả
`Unmatched(MULTIPLE_REFERENCES, candidates)` để ứng dụng chuyển sang xử lý tay.

## Cung cấp khoản cần thu

`PayableLookup` là port do ứng dụng implement. Implementation phải an toàn khi được gọi đồng thời từ nhiều thread.

```java
@Bean
PayableLookup payableLookup(OrderRepository orders) {
    return reference -> orders.findByPaymentReference(reference)
            .map(order -> new Payable(
                    order.paymentReference(),
                    order.total(),
                    order.paymentStatus(),
                    order.receivingAccountNumber(),
                    order.receivingBankBin()));
}
```

`PayableStatus` có ba giá trị: `OPEN`, `SETTLED` và `CANCELLED`. `Payable.bankBin` có thể là `null` nếu ứng dụng
không lưu BIN; số tài khoản vẫn được kiểm tra. BIN chỉ được so sánh khi cả payable và confirmation đều có giá trị.

## Xử lý kết quả đối soát

Adapter thông báo thanh toán sẽ tạo `PaymentConfirmation`. Ứng dụng truyền confirmation vào reconciler rồi
phân nhánh trên `ReconciliationResult`. Ví dụ sau dùng cú pháp Java 17:

```java
void applyResult(ReconciliationResult result, PaymentConfirmation confirmation) {
    if (result instanceof Matched matched) {
        orders.markPaid(matched.payable().reference(), confirmation.idempotencyKey());
    } else if (result instanceof Underpaid underpaid) {
        orders.flagUnderpaid(underpaid.payable().reference(), underpaid.missingAmount());
    } else if (result instanceof Overpaid overpaid) {
        orders.markPaidWithExcess(overpaid.payable().reference(), overpaid.excessAmount());
    } else if (result instanceof AlreadySettled) {
        audit.recordLateOrDuplicate(confirmation.idempotencyKey());
    } else if (result instanceof Cancelled cancelled) {
        manualReview.addCancelled(cancelled.payable(), confirmation);
    } else if (result instanceof WrongAccount wrongAccount) {
        alerts.raise(wrongAccount, confirmation);
    } else if (result instanceof Unmatched unmatched) {
        manualReview.addUnmatched(confirmation, unmatched.reason(), unmatched.candidates());
    } else {
        throw new IllegalStateException("Unsupported reconciliation result: " + result.getClass().getName());
    }
}
```

Chính sách nghiệp vụ thuộc về ứng dụng. Gợi ý ban đầu:

| Kết quả | Xử lý gợi ý |
|---|---|
| `Matched` | Đánh dấu đã thanh toán. |
| `Underpaid` | Giữ trạng thái chờ, ghi số tiền thiếu; nếu cho phép cộng dồn nhiều giao dịch thì thực hiện trong database của ứng dụng. |
| `Overpaid` | Đánh dấu đã thanh toán kèm số tiền thừa; đưa vào quy trình hoàn/khấu trừ có phê duyệt. |
| `AlreadySettled` | Ghi audit; không cộng tiền hoặc hoàn tất đơn lần nữa. |
| `Cancelled` | Chuyển xử lý tay; không tự mở lại đơn. |
| `WrongAccount` | Cảnh báo vì mã tham chiếu đúng nhưng tài khoản nhận không khớp. |
| `Unmatched` | Lưu hàng đợi xử lý tay cùng `reason` và danh sách `candidates`. |

`Unmatched.reason()` cho biết bước không khớp: `NO_REFERENCE` khi không trích được mã hợp lệ,
`MULTIPLE_REFERENCES` khi có nhiều mã khác nhau, và `UNKNOWN_REFERENCE` khi trích được đúng một mã nhưng lookup
không tìm thấy khoản cần thu.

Reconciler không ném exception cho các tình huống nghiệp vụ trên. Exception từ `PayableLookup` được để nổi
lên, nhờ đó adapter có thể báo xử lý thất bại và cho phép nhà cung cấp gửi lại.

## Chống xử lý trùng

Thông báo thanh toán có ngữ nghĩa at-least-once: cùng một giao dịch có thể đến nhiều lần.
`PaymentReconciler` cố ý không giữ danh sách đã xử lý. Hãy lưu `PaymentConfirmation.idempotencyKey()` trong
cùng transaction cập nhật đơn hàng.

Một schema tối thiểu có thể là:

```sql
CREATE TABLE processed_payment (
    idempotency_key VARCHAR(255) PRIMARY KEY,
    payment_reference VARCHAR(25) NOT NULL,
    processed_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);
```

`PRIMARY KEY` ở đây chính là ràng buộc `UNIQUE` chống hai worker cùng xử lý một giao dịch. Luồng xử lý nên:

1. Bắt đầu transaction.
2. `INSERT` idempotency key; nếu vi phạm unique thì coi là đã xử lý và kết thúc an toàn.
3. Gọi reconciler, cập nhật khoản cần thu hoặc lưu bản ghi xử lý tay.
4. Commit cả hai thay đổi cùng lúc. Nếu lookup hay database lỗi, rollback cả idempotency key để lần gửi lại
   còn có thể xử lý.

Không dùng `AlreadySettled` thay cho idempotency key: một khoản cần thu đã tất toán vẫn có thể nhận một giao
dịch khác thật sự, cần được audit riêng.

## Demo offline

Demo app cung cấp `/checkout` để tạo khoản cần thu giả lập và `POST /demo/payments/simulate` để tạo
`PaymentConfirmation` giả lập từ nội dung + số tiền. Hai endpoint này chỉ minh họa luồng đối soát offline;
chúng không kết nối ngân hàng hay nhà cung cấp thông báo thanh toán thật.
