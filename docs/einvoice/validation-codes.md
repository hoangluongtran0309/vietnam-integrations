# Mã kiểm tra hóa đơn điện tử

Các rule mặc định chạy theo thứ tự bảng. Trong một rule, các trường scalar theo thứ tự liệt kê, các dòng,
nhóm thuế và chữ ký theo thứ tự trong read model. Rule ứng dụng chạy sau cùng.

Đường dẫn trong bảng dùng các tiền tố sau.

| Tên | Đường dẫn |
|---|---|
| Header | `HDon/DLHDon/TTChung` |
| Nội dung | `HDon/DLHDon/NDHDon` |
| Danh sách dòng | `HDon/DLHDon/NDHDon/DSHHDVu` |
| Dòng | `HDon/DLHDon/NDHDon/DSHHDVu/HHDVu` |
| Tổng | `HDon/DLHDon/NDHDon/TToan` |
| Nhóm | `HDon/DLHDon/NDHDon/TToan/THTTLTSuat/LTSuat` |

Rule thêm chỉ số `[n]` từ 1 cho dòng, nhóm hoặc chữ ký nếu có nhiều phần tử cùng tên tại bước đó.
Thời điểm ký dùng đường dẫn metadata XAdES tương ứng vai trò trong read model. Khi thiếu vai trò, dùng `HDon/DSCKS`.

| Mã | Mức độ | Đường dẫn và điều kiện | Nguồn |
|---|---|---|---|
| `EINV-REQ-001` | ERROR | Header `/PBan`, `/THDon`, `/KHMSHDon`, `/KHHDon`, `/NLap`, `/DVTTe`, `/HTTToan`, `/MSTTCGP`. Tiếp theo là Nội dung `/NBan/Ten`, `/NBan/MST`, `/NBan/DChi`, rồi Dòng `/TChat` và `/THHDVu` mỗi dòng, rồi Tổng `/TgTTTBSo`, `/TgTTTBChu`. Mỗi trường thiếu hoặc chuỗi trắng có một issue. | Bảng trường bắt buộc của QĐ 1233, **thứ cấp**. |
| `EINV-REQ-002` | ERROR | Danh sách dòng. Hóa đơn phải có ít nhất một dòng. | Bảng dữ liệu của QĐ 1233, **thứ cấp**. |
| `EINV-REQ-003` | ERROR | Mẫu GTGT `1` cần Header `/HDCTTChinh`, ít nhất một Nhóm có `/ThTien`, Tổng `/TgTCThue` và `/TgTThue`. Báo tại Nhóm nếu chưa có nhóm nào hoặc tại `/ThTien` của nhóm đầu tiên nếu mọi nhóm thiếu thành tiền. Mẫu bán hàng `2` cần Header `/HDDCKPTQuan`. | Phụ lục I mục 1 TT 91, chính thức. Bảng trường của QĐ 1233, **thứ cấp**. |
| `EINV-REQ-004` | ERROR | `HDon/MCCQT` khi ký hiệu hóa đơn bắt đầu bằng `C`. | Phụ lục I mục 2 TT 91 và Điều 10 NĐ 254, chính thức. Tên thẻ theo QĐ 1233, **thứ cấp**. |
| `EINV-CUR-001` | ERROR | Header `/TGia` khi `DVTTe` có giá trị khác `VND`. | Bảng trường bắt buộc của QĐ 1233, **thứ cấp**. |
| `EINV-AMT-001` | INFO hoặc ERROR | Dòng `/ThTien` so với số lượng nhân đơn giá trừ tiền chiết khấu. Tiền chiết khấu vắng được tính bằng 0. | Kiểm tra nhất quán theo quan sát mẫu và bảng thẻ QĐ 1233, **thứ cấp**. |
| `EINV-AMT-002` | INFO hoặc ERROR | Nhóm `/ThTien` so với tổng thành tiền các dòng có cùng `TSuat`. Bỏ qua khi thiếu thuế suất dòng hoặc thiếu thành tiền của dòng thuộc nhóm. | Bảng tổng hợp của QĐ 1233, **thứ cấp**, và chính sách kiểm nhất quán. |
| `EINV-AMT-003` | INFO hoặc ERROR | Nhóm `/TThue` so với thành tiền nhóm nhân thuế suất nếu `TSuat` khớp `^\d{1,3}(\.\d+)?%$`. | Điểm 5 Phụ lục NĐ 254 yêu cầu tổng hợp theo thuế suất. Công thức kiểm và dung sai là chính sách ứng dụng. |
| `EINV-AMT-004` | INFO hoặc ERROR | Tổng `/TgTCThue` so với tổng thành tiền của các nhóm, khi có nhóm và có đủ số liệu. | Bảng tổng hợp của QĐ 1233, **thứ cấp**, và chính sách kiểm nhất quán. |
| `EINV-AMT-005` | INFO hoặc ERROR | Tổng `/TgTThue` so với tổng tiền thuế của các nhóm, khi có nhóm và có đủ số liệu. | Điểm 5 Phụ lục NĐ 254 và bảng tổng hợp của QĐ 1233, **thứ cấp**. |
| `EINV-AMT-006` | INFO hoặc ERROR | Tổng `/TgTTTBSo` so với tổng trước thuế cộng tổng thuế, khi đủ cả ba trường và không có khoản thu có thể chưa được mô hình hóa. | Kiểm tra nhất quán theo quan sát mẫu và bảng tổng hợp QĐ 1233, **thứ cấp**. |
| `EINV-AMT-007` | ERROR | Dòng `/TSuat` có giá trị nhưng không khớp nguyên văn với nhóm nào khi có nhóm. | Bảng tổng hợp của QĐ 1233, **thứ cấp**, và chính sách kiểm nhất quán. |
| `EINV-AMT-008` | WARNING | Với `VND`, báo từng trường có phần lẻ khác 0 theo thứ tự Tổng `/TgTCThue`, `/TgTThue`, `/TgTTTBSo`, `/TTCKTMai`. | Quy tắc tổng tiền VND của QĐ 1233, **thứ cấp**. |
| `EINV-AMT-900` | INFO | Danh sách dòng khi có `TChat` vắng hoặc khác `1`, bỏ qua AMT-001 đến AMT-007. Nếu không có lý do trên, báo tại Tổng khi AMT-006 có đủ toán hạng nhưng gặp key mở rộng dưới `TToan/` nằm ngoài cây `TTKhac/`. | Chính sách ứng dụng khi ngữ nghĩa dòng hoặc khoản thu chưa xác minh. |
| `EINV-DATE-001` | ERROR | Header `/NLap` sau ngày hiện tại theo múi giờ của `Clock`. | Chính sách kiểm nhất quán. |
| `EINV-DATE-002` | WARNING | `HDon/DSCKS/{NBan,NMua,CQT}/Signature/Object/QualifyingProperties/SignedProperties/SignedSignatureProperties/SigningTime` khi ngày ký ở Việt Nam trước `NLap`. | Chính sách kiểm nhất quán. Cấu trúc metadata dựa trên QĐ 1233, **thứ cấp**. |
| `EINV-MST-001` | WARNING | Nội dung `/NBan/MST`, `/NMua/MST`, rồi Header `/MSTTCGP` có giá trị sai mẫu 10 chữ số hoặc 10 chữ số, dấu gạch nối, 3 chữ số. | Kiểm tra định dạng theo chính sách của validator. Còn chờ hướng dẫn MST cá nhân và chữ số kiểm tra. |

AMT-001 đến AMT-006 dùng [cùng một quy tắc dung sai](validation.md#dung-sai-và-điều-kiện-kiểm-tra).
Sau đây là [các nguồn chính thức và thứ cấp](legal-basis.md).

## Cần đối chiếu

- TODO(v0.3): verify the meaning of TLCKhau, STCKhau and TTCKTMai — official QD 1233/QD-CT technical appendix.
- TODO(v0.3): verify TChat values and how promotion, discount and note lines affect totals — official QD 1233/QD-CT.
- TODO(v0.3): verify 12-digit personal tax codes and the check-digit algorithm — official Ministry of Finance guidance.
- TODO(v0.3): verify required XML fields, VND total constraints and amount and VAT rounding — official QD 1233/QD-CT technical appendix and XSD.
