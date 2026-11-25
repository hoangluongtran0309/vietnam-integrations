# Fixture XML hóa đơn tổng hợp

Các XML được tự dựng theo bảng thẻ trong
[cơ sở pháp lý](../../../../../../docs/einvoice/legal-basis.md) và bảng ánh xạ chiết khấu trong
[read model](../../../../../../docs/einvoice/read-model.md). Bảng thẻ dựa trên bản sao thứ cấp của QĐ 1233/QĐ-CT.
Toàn bộ tên, địa chỉ, số hóa đơn và mã cơ quan thuế là dữ liệu mẫu. Các MST có tiền tố `MST-MAU-` là chuỗi giả
không thuộc tổ chức nào. Các fixture không phải hóa đơn thật và không có chữ ký XML hợp lệ. Test chỉ kiểm cách
repo hiểu tài liệu, chưa cho phép kết luận về định dạng hóa đơn thật.

| File | Mục đích | Giá trị test đối chiếu |
|---|---|---|
| [vat-two-rates.xml](vat-two-rates.xml) | GTGT, hai mức thuế suất, tiếng Việt và scale | Số `00000001`, mẫu `1`, ký hiệu `C26MAU`, ngày 2026-11-25, người bán Công ty Mẫu A, bên mua Công ty Mẫu B, tỷ giá `25000.00`, MCCQT `MA-CQT-MAU`. Các dòng Sổ mẫu `2.00 × 10.50 → 21.000` (5%), Bút mẫu `3 × 4.00 → 12.00` (10%), Giấy mẫu `1.0 × 9.000 → 9.00` (5%). Hai nhóm `5% / 30.000 / 1.50` và `10% / 12.00 / 1.200`. Tổng trước thuế, thuế và thanh toán lần lượt `42.000`, `2.700`, `44.7000`. |
| [vat-with-extensions.xml](vat-with-extensions.xml) | Đếm anh em theo local name, giữ text và bỏ qua cây chữ ký | Số `00000002`, ký hiệu `C26MAU`, ngày 2026-11-25, người bán Công ty Mẫu C. Hai dòng thành tiền `1000.00` và `2000.00`, nhóm `10% / 3000.00 / 300`, tổng `3000 / 300 / 3300`, MCCQT vắng. Hai khoản chiết khấu dòng `100.00` và `200.00` được ánh xạ. Hai key `MDDKDoanh[1]/TDDKDoanh` và `MDDKDoanh[2]/TDDKDoanh` giữ các giá trị `  Điểm mẫu 1  ` và `Điểm mẫu 2`. Không có key dưới `HDon/DSCKS`. |
| [vat-line-discount.xml](vat-line-discount.xml) | GTGT có chiết khấu dòng | Hai dòng `2.00 × 250.00 − 50.00 = 450.000` và `1 × 550 = 550`, cùng thuế suất 10%. Tổng chiết khấu `50.000`, tổng trước thuế `1000`, thuế `100`, thanh toán `1100`. |
| [sales-no-buyer.xml](sales-no-buyer.xml) | Hóa đơn bán hàng thiếu người mua và số hóa đơn | Buyer và số hóa đơn là `null`. Mẫu `2`, ký hiệu `K26MAU`, ngày 2026-11-25, người bán Cửa hàng Mẫu D, cờ khu phi thuế quan `0`, dòng Hàng mẫu và tổng thanh toán `90`, không có nhóm thuế suất hoặc MCCQT. |
| [signed-metadata.xml](signed-metadata.xml) | Metadata của hai chữ ký mẫu | Vai trò `SELLER` và `TAX_AUTHORITY`, giờ ký `2026-11-25T02:30:00Z` và `2026-11-25T02:45:00Z`. CN người bán `Synthetic Test Seller`, serial `8da396a3a0c0264b`. CN cơ quan thuế `Synthetic Test Tax Authority`, serial `155f060354d02830`. Cả hai có hạn từ `2026-11-25T00:00:00Z` đến `2036-11-25T00:00:00Z`. |
| [utf16-encoded.xml](utf16-encoded.xml) | Đọc UTF-16 có BOM và khai báo encoding | Số `00000004`, ký hiệu `C26MAU`, ngày 2026-11-25, người bán Cửa hàng Mẫu Đ, dòng Ấm trà mẫu và tổng thanh toán `70.00`, không có nhóm thuế suất hoặc MCCQT. |

Cấu trúc lặp `MDDKDoanh/TDDKDoanh`, các namespace `urn:synthetic:…` và lá ngoài model dưới `DSCKS`
chỉ tạo tình huống kiểm thử.

## Tạo chứng thư mẫu

`SignatureValue` chứa Base64 của chuỗi giả `FAKE`. Hai chứng thư X.509 tự ký dùng RSA 2048 và có hạn
10 năm. Các lệnh dưới đây tạo keystore tạm và xuất chứng thư công khai. Chỉ phần Base64 giữa hai dòng
`BEGIN CERTIFICATE` và `END CERTIFICATE` được chép vào XML. Serial do `keytool` sinh nên sẽ khác khi chạy lại.

```bash
cert_dir=$(mktemp -d)
trap 'rm -rf "$cert_dir"' EXIT
for entry in seller tax-authority
do
    case "$entry" in
        seller) test_cn='Synthetic Test Seller' ;;
        tax-authority) test_cn='Synthetic Test Tax Authority' ;;
    esac
    TZ=UTC keytool -genkeypair -alias "$entry" -keyalg RSA -keysize 2048 \
        -dname "CN=$test_cn" -startdate '2026/11/25 00:00:00' -validity 3653 \
        -storetype PKCS12 -keystore "$cert_dir/$entry.p12" -storepass synthetic-only
    keytool -exportcert -rfc -alias "$entry" -keystore "$cert_dir/$entry.p12" \
        -storepass synthetic-only -file "$cert_dir/$entry.pem"
done
```
