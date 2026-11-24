# Cơ sở pháp lý và định dạng XML hóa đơn điện tử

Tài liệu này ghi nhận kết quả đối chiếu tại ngày **24/11/2026**. Mọi URL trong tài liệu đã được truy cập trong
ngày này. “Chính thức” nghĩa là bản đăng trên Công báo hoặc cổng của cơ quan nhà nước; “thứ cấp” nghĩa là bản sao
trên cơ sở dữ liệu tư nhân và chưa thay thế việc đối chiếu bản gốc.

## Kết luận ngắn

- Khung đang có hiệu lực là Luật Quản lý thuế 108/2025/QH15, Nghị định 254/2026/NĐ-CP và Thông tư
  91/2026/TT-BTC. Luật có hiệu lực chung từ 01/07/2026, riêng Điều 13 và việc dùng hóa đơn điện tử của hộ/cá
  nhân kinh doanh tại Điều 26 có hiệu lực từ 01/01/2026; Nghị định và Thông tư có hiệu lực từ 01/07/2026.
- Nghị định 123/2020/NĐ-CP, Điều 1 Nghị định 41/2022/NĐ-CP và Nghị định 70/2025/NĐ-CP hết hiệu lực từ
  01/07/2026 theo khoản 2 Điều 43 Nghị định 254. Thông tư 32/2025/TT-BTC hết hiệu lực cùng ngày theo khoản 2
  Điều 25 Thông tư 91.
- Điều 3 Thông tư 91 xác nhận hóa đơn điện tử dùng XML và giao Cục Thuế xây dựng, công bố thành phần dữ liệu
  nghiệp vụ. Công văn 7813/CT-QLNT của Cục Thuế xác nhận QĐ 1233/QĐ-CT ngày 27/08/2026 đang được dùng, nhưng
  bản quyết định và phụ lục kỹ thuật chính thức chưa được tìm thấy trên các cổng ưu tiên.
- Bản sao thứ cấp của QĐ 1233 ghi phiên bản thông điệp 2.1.1, cho phép tiếp nhận 2.1.0 đến hết 31/12/2026 và
  DSig đến hết 09/04/2027. Các thông tin này **chưa đối chiếu được bản gốc**.
- Chưa tìm thấy gói XSD chính thức hoặc điều khoản cho phép tái phân phối. Không đóng gói XSD lấy từ nguồn thứ
  cấp vào JAR Apache-2.0.
- Nguồn đã đọc không quy định phương pháp làm tròn chung (`HALF_UP`, v.v.) hoặc tính thuế theo dòng hay theo
  tổng. Bản sao phụ lục chỉ yêu cầu một số tổng tiền VND không có phần thập phân và tổng hợp thuế theo thuế suất.

Read model tương ứng được mô tả tại [read-model.md](read-model.md).

## Văn bản pháp lý

| Văn bản | Trích yếu và điều khoản dùng | Ban hành | Hiệu lực | Trạng thái tại 24/11/2026 | Nguồn | Ngày kiểm tra |
|---|---|---:|---:|---|---|---:|
| Luật 108/2025/QH15 | Luật Quản lý thuế; khoản 5 Điều 26 đặt nguyên tắc lập hóa đơn điện tử theo định dạng chuẩn | 10/12/2025 | 01/07/2026; ngoại lệ nêu tại khoản 2 Điều 52 áp dụng từ 01/01/2026 | Còn hiệu lực đối với nội dung được rà soát | [Công báo][law-108] — chính thức | 24/11/2026 |
| Nghị định 254/2026/NĐ-CP | Quy định về hóa đơn điện tử, chứng từ điện tử; Điều 5, Điều 10, Điều 43, Điều 44 và Phụ lục | 30/06/2026 | 01/07/2026 | Còn hiệu lực; là nghị định trực tiếp đang áp dụng | [Công báo][decree-254] — chính thức | 24/11/2026 |
| Thông tư 91/2026/TT-BTC | Điều 3 quy định XML; Điều 24 chuyển tiếp; Điều 25 hiệu lực | 30/06/2026 | 01/07/2026 | Còn hiệu lực; Phụ lục I–IV bắt buộc, Phụ lục V tham khảo | [Công báo][circular-91] — chính thức | 24/11/2026 |
| Nghị định 123/2020/NĐ-CP | Khung hóa đơn, chứng từ trước 01/07/2026 | 19/10/2020 | 01/07/2022 | Hết hiệu lực từ 01/07/2026 theo điểm a khoản 2 Điều 43 Nghị định 254 | [Công báo][decree-123] — chính thức | 24/11/2026 |
| Điều 1 Nghị định 41/2022/NĐ-CP | Sửa Nghị định 123 và Nghị định 15/2022/NĐ-CP | 20/06/2022 | 20/06/2022 | Hết hiệu lực từ 01/07/2026 theo điểm b khoản 2 Điều 43 Nghị định 254 | [Công báo][decree-41] — chính thức | 24/11/2026 |
| Nghị định 70/2025/NĐ-CP | Sửa Nghị định 123 | 20/03/2025 | 01/06/2025 | Hết hiệu lực từ 01/07/2026 theo điểm c khoản 2 Điều 43 Nghị định 254 | [Công báo][decree-70] — chính thức | 24/11/2026 |
| Thông tư 78/2021/TT-BTC | Hướng dẫn Nghị định 123 | 17/09/2021 | 01/07/2022 | Đã được Thông tư 32 thay thế từ 01/06/2025 | [Công báo][circular-78] — chính thức | 24/11/2026 |
| Thông tư 32/2025/TT-BTC | Hướng dẫn Nghị định 123 và Nghị định 70 | 31/05/2025 | 01/06/2025 | Hết hiệu lực từ 01/07/2026 theo khoản 2 Điều 25 Thông tư 91 | [bản PDF Công báo, Điều 12][circular-32-pdf] — chính thức | 24/11/2026 |
| 41/VBHN-VPQH | Bản hợp nhất Luật Kế toán; Điều 41 quy định thời hạn lưu tài liệu kế toán | 16/03/2026 | Bản hợp nhất | Dùng để tra cứu nội dung hiện hành | [Công báo][accounting-law] — chính thức | 24/11/2026 |

Lưu ý: trang giới thiệu Thông tư 32 trên Công báo hiển thị sai ngày hiệu lực. Bảng trên lấy ngày **01/06/2025**
trực tiếp từ khoản 1 Điều 12 của bản PDF ký số.

### Chuyển tiếp và hóa đơn đã lập

- Điều 44 Nghị định 254 chỉ quy định chuyển tiếp cho hóa đơn bán hàng dự trữ quốc gia bằng giấy, biên lai giấy
  và hóa đơn do cơ quan thuế đặt in; điều này không cho phép suy ra XML cũ hết giá trị.
- Khoản 2 Điều 24 Thông tư 91 vẫn quy định cách điều chỉnh hoặc thay thế hóa đơn đã lập theo Nghị định 123,
  Nghị định 70 và Thông tư 32. Đây là bằng chứng chính thức rằng hóa đơn của khung cũ vẫn phải được xử lý sau
  01/07/2026.
- Khoản 1, 2 Điều 5 Nghị định 254 yêu cầu hóa đơn được lưu an toàn, toàn vẹn, đủ thời hạn theo pháp luật kế toán,
  dưới dạng thông điệp dữ liệu, và phải sẵn sàng in hoặc tra cứu. Khoản 5 Điều 41 bản hợp nhất Luật Kế toán quy
  định ít nhất 05 năm, ít nhất 10 năm hoặc vĩnh viễn tùy loại tài liệu; không có căn cứ để gán một thời hạn duy nhất
  cho mọi hóa đơn.
- Vì vậy, việc parser tiếp tục đọc các phiên bản cũ là **kết luận thiết kế** để đáp ứng nhu cầu lưu/tra cứu và xử lý
  hóa đơn cũ, không phải câu chữ pháp luật bắt buộc một thư viện phải hỗ trợ mọi phiên bản.

## Quyết định kỹ thuật và phiên bản XML

Quyết định của Cục Thuế không được tìm thấy dưới dạng bản gốc đầy đủ trên các cổng ưu tiên. Chuỗi dưới đây được
lập từ bản sao thứ cấp. Nguồn chính thức duy nhất tìm thấy cho QĐ 1233 là [Công văn 7813/CT-QLNT][letter-7813]:
công văn dẫn QĐ 1233 và dùng giá trị `KTPVDC` tại STT 9 Phụ lục V, nhưng không xác nhận toàn bộ Điều 2 hoặc phụ
lục kỹ thuật.

| Phiên bản | Văn bản và mốc áp dụng tìm thấy | File XSD | Khác biệt chính so với trước | Mức xác minh | Ngày kiểm tra |
|---|---|---|---|---|---:|
| 2.0.0 | QĐ 1450/QĐ-TCT ngày 07/10/2021 | Chưa tìm thấy từ nguồn chính thức | Phiên bản nền của QĐ 1450; chưa có bộ schema để diff | [Bản sao QĐ 1450][decision-1450] — thứ cấp | 24/11/2026 |
| 2.0.1 | QĐ 1510/QĐ-TCT ngày 21/09/2022 sửa QĐ 1450; Thông báo 547/TB-TCT đính chính giá trị phiên bản | Chưa tìm thấy từ nguồn chính thức | Chưa đối chiếu được danh sách thay đổi bằng hai gói schema; không dùng mô tả của nhà cung cấp làm kết luận | [Thông báo 547][notice-547] — thứ cấp | 24/11/2026 |
| 2.1.0 | QĐ 1271/QĐ-CT ngày 30/05/2025 rồi QĐ 1306/QĐ-CT ngày 13/06/2025; bản sao QĐ 1306 nói thay QĐ 1450, QĐ 1510 và QĐ 1271 | Chưa tìm thấy từ nguồn chính thức | Việc gắn 2.1.0 với bộ quyết định này là suy luận từ điều khoản chuyển tiếp của bản sao QĐ 1233; chưa có phụ lục gốc để diff | [Bản sao QĐ 1306][decision-1306] — thứ cấp; [Bộ Tài chính nhắc QĐ 1306][mof-1306] — chính thức nhưng không nêu chuỗi thay thế | 24/11/2026 |
| 2.1.1 | Bản sao QĐ 1233/QĐ-CT ngày 27/08/2026 nói có hiệu lực từ ngày ký, thay QĐ 1306; hệ thống nhận song song 2.1.0 đến hết 31/12/2026 | Chưa tìm thấy từ nguồn chính thức; bản sao đính kèm chỉ là DOCX mô tả bảng dữ liệu | Phụ lục thứ cấp mô tả XAdES và Điều 2 cho nhận DSig đến hết 09/04/2027; danh sách thẻ đổi so với 2.1.0 chưa xác minh | [Bản sao QĐ 1233 và DOCX đính kèm][decision-1233] — thứ cấp; [Công văn 7813][letter-7813] — chính thức, chỉ xác nhận một phần | 24/11/2026 |

Không được diễn đạt 2.1.1 là “bắt buộc duy nhất từ 27/08/2026”: chính bản sao Điều 2 mô tả giai đoạn nhận song
song. Mốc 31/12/2026 là mốc hệ thống ngừng **tiếp nhận** 2.1.0, không phải mốc làm mất giá trị hóa đơn 2.1.0 đã
lập. Tương tự, mốc 09/04/2027 không phải căn cứ để kết luận chữ ký DSig đã lập mất giá trị.

## Trường bắt buộc của hóa đơn GTGT và hóa đơn bán hàng

Bảng này chỉ mô tả hóa đơn thông thường. Tên thẻ và ràng buộc được chép từ bảng mô tả ở Phần II, Mục II, khoản
2.a và 2.b của [DOCX đính kèm bản sao QĐ 1233][decision-1233]. Đây là **nguồn thứ cấp, chưa kiểm tra bằng XSD
chính thức**. Đường dẫn đầy đủ được ghi để tránh nhầm các thẻ trùng tên; “có điều kiện” giữ nguyên ý nghĩa “nếu
có” hoặc ngoại lệ trong bảng nguồn.

| Đường dẫn XML | Ý nghĩa | Kiểu/độ dài tối đa | Hóa đơn GTGT | Hóa đơn bán hàng | Nguồn, kiểm tra |
|---|---|---|---|---|---|
| `HDon/DLHDon/TTChung/PBan` | Phiên bản thông điệp | Chuỗi, 6 | Bắt buộc; bản sao ghi `2.1.1` | Bắt buộc; bản sao ghi `2.1.1` | QĐ 1233 II.II.2.a–b, thứ cấp, 24/11/2026 |
| `HDon/DLHDon/TTChung/THDon` | Tên hóa đơn | Chuỗi, 100 | Bắt buộc | Bắt buộc | Như trên; Điều 10 NĐ 254 |
| `HDon/DLHDon/TTChung/KHMSHDon` | Ký hiệu mẫu số | Chuỗi, 1 | Bắt buộc | Bắt buộc | Như trên; Phụ lục I TT 91 |
| `HDon/DLHDon/TTChung/KHHDon` | Ký hiệu hóa đơn | Chuỗi, 6 | Bắt buộc | Bắt buộc | Như trên; Phụ lục I TT 91 |
| `HDon/DLHDon/TTChung/SHDon` | Số hóa đơn | Số, 8 | Có điều kiện: “bắt buộc nếu có” | Có điều kiện: “bắt buộc nếu có” | QĐ 1233 II.II.2.a–b, thứ cấp, 24/11/2026 |
| `HDon/DLHDon/TTChung/NLap` | Ngày lập | Ngày | Bắt buộc | Bắt buộc | Như trên |
| `HDon/DLHDon/TTChung/HDCTTChinh` | Cờ hóa đơn cho thuê tài chính | Số, 1 | Bắt buộc | Không áp dụng | QĐ 1233 II.II.2.a, thứ cấp, 24/11/2026 |
| `HDon/DLHDon/TTChung/HDDCKPTQuan` | Cờ hóa đơn dành cho khu phi thuế quan | Số, 1 | Không áp dụng | Bắt buộc | QĐ 1233 II.II.2.b, thứ cấp, 24/11/2026 |
| `HDon/DLHDon/TTChung/DVTTe` | Đơn vị tiền tệ | Chuỗi, 3 | Bắt buộc | Bắt buộc | QĐ 1233 II.II.2.a–b, thứ cấp, 24/11/2026 |
| `HDon/DLHDon/TTChung/TGia` | Tỷ giá | Số, 7,2 | Bắt buộc trừ VND | Bắt buộc trừ VND | Như trên |
| `HDon/DLHDon/TTChung/HTTToan` | Hình thức thanh toán | Chuỗi, 50 | Bắt buộc | Bắt buộc | Như trên |
| `HDon/DLHDon/TTChung/MSTTCGP` | MST tổ chức cung cấp giải pháp HĐĐT | Chuỗi, 14 | Bắt buộc | Bắt buộc | Như trên |
| `HDon/DLHDon/NDHDon/NBan/Ten` | Tên người bán | Chuỗi, 400 | Bắt buộc | Bắt buộc | Như trên; Điều 10 NĐ 254 |
| `HDon/DLHDon/NDHDon/NBan/MST` | MST người bán | Chuỗi, 14 | Bắt buộc | Bắt buộc | Như trên; Điều 10 NĐ 254 |
| `HDon/DLHDon/NDHDon/NBan/DChi` | Địa chỉ người bán | Chuỗi, 400 | Bắt buộc | Bắt buộc | Như trên; Điều 10 NĐ 254 |
| `HDon/DLHDon/NDHDon/NMua/Ten` | Tên người mua | Chuỗi, 400 | Có điều kiện theo thông tin người mua/hàng hóa đặc trưng | Có điều kiện tương tự | QĐ 1233 II.II.2.a–b, thứ cấp; điểm 4 Phụ lục NĐ 254, 24/11/2026 |
| `HDon/DLHDon/NDHDon/NMua/MST` | MST người mua | Chuỗi, 14 | Có điều kiện | Có điều kiện | Như trên |
| `HDon/DLHDon/NDHDon/NMua/DChi` | Địa chỉ người mua | Chuỗi, 400 | Có điều kiện | Có điều kiện | Như trên |
| `HDon/DLHDon/NDHDon/NMua/HVTNMHang` | Họ tên người mua hàng | Chuỗi, 100 | Có điều kiện; bản sao yêu cầu giá trị “Bán cho người tiêu dùng” khi không có thông tin người mua | Có điều kiện tương tự | QĐ 1233 II.II.2.a–b, thứ cấp; điểm 4 Phụ lục NĐ 254, 24/11/2026 |
| `HDon/DLHDon/NDHDon/DSHHDVu/HHDVu/TChat` | Tính chất dòng | Số, 1 | Bắt buộc | Bắt buộc | QĐ 1233 II.II.2.a–b, thứ cấp, 24/11/2026 |
| `HDon/DLHDon/NDHDon/DSHHDVu/HHDVu/MHHDVu` | Mã hàng hóa, dịch vụ | Chuỗi, 50 | Có điều kiện: nếu có | Có điều kiện: nếu có | Như trên |
| `HDon/DLHDon/NDHDon/DSHHDVu/HHDVu/THHDVu` | Tên hàng hóa, dịch vụ | Chuỗi, 500 | Bắt buộc | Bắt buộc | Như trên; điểm 5 Phụ lục NĐ 254 |
| `HDon/DLHDon/NDHDon/DSHHDVu/HHDVu/DVTinh` | Đơn vị tính | Chuỗi, 50 | Có điều kiện: nếu có | Có điều kiện: nếu có | Như trên |
| `HDon/DLHDon/NDHDon/DSHHDVu/HHDVu/SLuong` | Số lượng | Số, 21,6 | Có điều kiện: nếu có | Có điều kiện: nếu có | Như trên |
| `HDon/DLHDon/NDHDon/DSHHDVu/HHDVu/DGia` | Đơn giá | Số, 21,6 | Có điều kiện: nếu có | Có điều kiện: nếu có | Như trên |
| `HDon/DLHDon/NDHDon/DSHHDVu/HHDVu/ThTien` | Thành tiền chưa thuế/thành tiền | Số, 21,6 | Bắt buộc, trừ dòng diễn giải | Bắt buộc, trừ dòng diễn giải | Như trên |
| `HDon/DLHDon/NDHDon/DSHHDVu/HHDVu/TSuat` | Thuế suất GTGT của dòng | Chuỗi, 11 | Có điều kiện: nếu có | Không áp dụng | QĐ 1233 II.II.2.a, thứ cấp; điểm 5 Phụ lục NĐ 254, 24/11/2026 |
| `HDon/DLHDon/NDHDon/TToan/THTTLTSuat/LTSuat/TSuat` | Thuế suất của nhóm tổng hợp | Chuỗi, 11 | Có điều kiện: nếu có | Không áp dụng | Như trên |
| `HDon/DLHDon/NDHDon/TToan/THTTLTSuat/LTSuat/ThTien` | Thành tiền chưa thuế theo thuế suất | Số, 21,6 | Bắt buộc | Không áp dụng | Như trên |
| `HDon/DLHDon/NDHDon/TToan/THTTLTSuat/LTSuat/TThue` | Tiền thuế theo thuế suất | Số, 21,6 | Có điều kiện: nếu có | Không áp dụng | Như trên |
| `HDon/DLHDon/NDHDon/TToan/TgTCThue` | Tổng tiền chưa thuế | Số, 21,6 | Bắt buộc | Không áp dụng | QĐ 1233 II.II.2.a, thứ cấp, 24/11/2026 |
| `HDon/DLHDon/NDHDon/TToan/TgTThue` | Tổng tiền thuế | Số, 21,6 | Bắt buộc | Không áp dụng | Như trên |
| `HDon/DLHDon/NDHDon/TToan/TgTTTBSo` | Tổng thanh toán bằng số | Số, 21,6 | Bắt buộc | Bắt buộc | QĐ 1233 II.II.2.a–b, thứ cấp, 24/11/2026 |
| `HDon/DLHDon/NDHDon/TToan/TgTTTBChu` | Tổng thanh toán bằng chữ | Chuỗi, 255 | Bắt buộc | Bắt buộc | Như trên |
| `HDon/MCCQT` | Mã cơ quan thuế | Chuỗi, 34 | Bắt buộc với hóa đơn có mã đủ điều kiện cấp mã | Tương tự | QĐ 1233 II.II.2.a–b, thứ cấp; Điều 10 NĐ 254, 24/11/2026 |
| `HDon/DSCKS/NBan/ds:Signature` | Chữ ký số người bán/đơn vị nhận ủy nhiệm | XML Signature | Có điều kiện theo ngoại lệ pháp lý | Có điều kiện tương tự | QĐ 1233 II.II.2.a–b, thứ cấp; điểm 6, 9 Phụ lục NĐ 254, 24/11/2026 |
| `HDon/DSCKS/CQT/ds:Signature` | Chữ ký số cơ quan thuế | XML Signature | Bắt buộc với hóa đơn có mã đủ điều kiện cấp mã | Tương tự | QĐ 1233 II.II.2.a–b, thứ cấp, 24/11/2026 |

Hóa đơn điều chỉnh, thay thế, ủy nhiệm, hàng hóa đặc trưng, hóa đơn từ máy tính tiền và các loại chuyên ngành có
thêm trường bắt buộc có điều kiện. Hóa đơn bán hàng của hộ/cá nhân kinh doanh còn có nhóm
`NBan/MDDKDoanh`, `NBan/TDDKDoanh`, `NBan/DCDDKDoanh`. Không nên dùng bảng rút gọn này làm schema xác
thực cho các trường hợp đó.

## Làm tròn và tính thuế

### Điều đã xác minh

- Điểm 5 Phụ lục Nghị định 254 yêu cầu thể hiện tổng tiền thuế GTGT theo từng loại thuế suất và tổng cộng tiền
  thuế. Văn bản không quy định thứ tự hay cấp độ thực hiện phép tính.
- Bản sao phụ lục QĐ 1233 cho phép số có tối đa 21 chữ số, tối đa 6 chữ số thập phân. Các tổng tiền chưa thuế,
  tổng giảm trừ không chịu thuế, tổng doanh thu quy đổi, tổng tiền thuế, tổng chiết khấu thương mại, tổng giảm trừ
  khác và tổng thanh toán bằng VND phải làm tròn đến đồng, không ghi phần thập phân.
- Ngoại lệ làm tròn duy nhất tìm thấy trong Nghị định 254 là điểm 5.e Phụ lục: doanh nghiệp vận tải hàng không có
  thể làm tròn đến hàng nghìn đối với một số khoản thu trên chứng từ vận tải theo quy định IATA. Không được khái
  quát ngoại lệ này cho hóa đơn thông thường.

### Điều văn bản không quy định

Các nguồn đã đọc **không quy định** phương pháp làm tròn chung (`HALF_UP`, `HALF_EVEN`, v.v.) và **không quy
định** tiền thuế phải được tính/làm tròn theo từng dòng hay từ tổng tiền theo từng thuế suất. Cấu trúc
`THTTLTSuat/LTSuat` chỉ chứng minh dữ liệu được tổng hợp theo thuế suất, không chứng minh thuật toán tính.

[InvoiceLine.java](../../vietnam-einvoice-core/src/main/java/io/github/hoangluongtran0309/vietnam/einvoice/InvoiceLine.java)
hiện làm tròn `amountBeforeTax()` và `taxAmount()` từng dòng về 0 chữ số bằng `HALF_UP`. Không dùng kết quả này để
xác thực pháp lý cho tới khi có bản kỹ thuật chính thức và test vector của nhà cung cấp. TODO đề xuất cho lần sửa
code sau:

```java
// TODO(v0.3): verify amount and VAT rounding (mode and line-level versus
// tax-rate aggregate) against the official Decision 1233/QD-CT schema package
// and redacted provider captures before using these calculations for validation.
```

## Chữ ký số trong XML

Điều 3 Thông tư 91 chỉ quy định thành phần chữ ký số tuân theo pháp luật giao dịch điện tử; Nghị định 254 quy định
chủ thể phải ký và các ngoại lệ. Hai văn bản chính thức này không nêu profile XMLDSig/XAdES, thuật toán hay đường
dẫn thẻ.

Bản sao phụ lục QĐ 1233, chưa đối chiếu bản gốc, mô tả:

- RSA; hàm băm SHA-256, SHA-384 hoặc SHA-512; chữ ký XML theo ETSI EN 319 132-1 V1.2.1 (XAdES).
- `SigningTime` tại
  `ds:Signature/ds:Object/xades:QualifyingProperties/xades:SignedProperties/xades:SignedSignatureProperties/xades:SigningTime`.
- Mỗi chữ ký tham chiếu dữ liệu nghiệp vụ và `xades:SignedProperties`; chứng thư X.509 nằm tại
  `ds:Signature/ds:KeyInfo/ds:X509Data/ds:X509Certificate`.
- Người bán ký `HDon/DLHDon` và `HDon/DSCKS/NBan/ds:Signature/ds:Object/xades:SignedProperties`; người mua
  tương tự dưới `NMua`. Với hóa đơn có mã, cơ quan thuế ký `HDon/DLHDon`, `HDon/MCCQT` và `SignedProperties`
  của chữ ký dưới `CQT`.
- Bản sao Điều 2 QĐ 1233 nói hệ thống tiếp nhận song song DSig và XAdES, DSig đến hết 09/04/2027. Đây là mốc
  tiếp nhận, không phải căn cứ vô hiệu hóa chữ ký DSig đã lập.

Parser chỉ nên bật kiểm tra profile XAdES nêu trên sau khi đối chiếu bản gốc, namespace và test vector thật.

## XSD và giấy phép

Chưa tìm thấy gói `.xsd` cho 2.0.0, 2.0.1, 2.1.0 hoặc 2.1.1 trên Công báo, Cổng Bộ Tài chính, Cổng Cục Thuế hay
cổng hóa đơn điện tử. Điều này chỉ có nghĩa là **chưa tìm thấy**, không chứng minh XSD không tồn tại. DOCX từ nguồn
thứ cấp mô tả bảng trường và hình khối, không phải gói XSD.

Không tìm thấy thông báo bản quyền hoặc giấy phép cho phép phân phối lại XSD. Yêu cầu “ghi rõ nguồn” trên cổng
thông tin không phải giấy phép phần mềm và không đủ để kết luận có thể đưa XSD vào JAR Apache-2.0.

Đề xuất:

1. Không commit hoặc đóng gói XSD từ nguồn thứ cấp.
2. Yêu cầu người dùng cung cấp gói XSD chính thức tại runtime cho tới khi Cục Thuế công bố URL ổn định và điều
   khoản tái phân phối rõ ràng.
3. Nếu nhận được văn bản cho phép tái phân phối, lưu nguyên file, checksum, phiên bản, URL và ngày tải; không dựng
   schema từ bảng mô tả.
4. Parser phải nhận biết phiên bản và không dùng schema 2.1.1 để từ chối hóa đơn cũ trước khi có chính sách tương
   thích đã được kiểm chứng.

## Chưa xác minh

| Câu hỏi còn mở | Lý do | Cần tải từ nguồn chính thức |
|---|---|---|
| QĐ 1450 còn hiệu lực hay bị thay thế theo chuỗi nào | Bản sao QĐ 1306 nói thay QĐ 1450, QĐ 1510 và QĐ 1271, nhưng chưa tìm thấy bản QĐ 1306 chính thức | QĐ 1306/QĐ-CT ngày 13/06/2025 có chữ ký và toàn bộ phụ lục |
| Phiên bản hiện hành và chuyển tiếp | QĐ 1233 được Công văn 7813 xác nhận một phần; Điều 2 và phiên bản 2.1.1 mới có ở bản sao thứ cấp | QĐ 1233/QĐ-CT ngày 27/08/2026 có chữ ký và toàn bộ phụ lục |
| File XSD chính thức | Không tìm thấy URL/gói schema từ cổng ưu tiên | Gói XSD và tài liệu phát hành cho 2.0.0, 2.0.1, 2.1.0, 2.1.1; checksum nếu cơ quan thuế công bố |
| Khác biệt thẻ giữa định dạng cũ và mới | Không thể diff bảng mô tả với độ tin cậy của XSD | Hai gói schema 2.1.0 và 2.1.1, cùng release note chính thức; tốt nhất thêm 2.0.0 và 2.0.1 |
| Quyền đóng gói XSD | Không có điều khoản tái phân phối rõ ràng | Giấy phép hoặc văn bản chấp thuận của Cục Thuế/Bộ Tài chính cho việc phân phối trong JAR mã nguồn mở |
| Trường bắt buộc và profile chữ ký | Hiện chép từ phụ lục thứ cấp, chưa kiểm tra namespace/cardinality bằng schema | Phụ lục kỹ thuật và XSD chính thức của QĐ 1233 |
| Làm tròn và cấp độ tính thuế | NĐ 254, TT 91 và bản sao QĐ 1233 không nêu rounding mode hay line-vs-aggregate | Hướng dẫn chính thức hoặc tài liệu nhà cung cấp kèm XML thật đã redact; nếu không có thì API phải coi đây là policy cấu hình, không phải quy tắc pháp lý |

Nơi nên tìm hoặc đề nghị cung cấp: mục văn bản/tải xuống của Cục Thuế, cổng hóa đơn điện tử của Cục Thuế, Cổng
Bộ Tài chính, hoặc đầu mối Ban Công nghệ, chuyển đổi số và tự động hóa thuộc Cục Thuế.

## Test vector cần có

Mọi capture thật phải được redact, ghi ngày capture và lưu dưới `src/test/resources/**/captures/` khi bắt đầu
implementation.

Mapping phiên bản–giai đoạn dưới đây là kế hoạch thu thập dựa trên nguồn thứ cấp; phải xác nhận lại bằng bản gốc
quyết định, phụ lục kỹ thuật và gói XSD trước khi dùng làm tiêu chí tương thích.

| Loại hóa đơn | Phiên bản/chữ ký cần có | Giai đoạn | Mục đích |
|---|---|---|---|
| Hóa đơn GTGT có mã | 2.0.0 hoặc 2.0.1, DSig | Trước 01/06/2025 | Tương thích dữ liệu theo QĐ 1450/QĐ 1510 |
| Hóa đơn bán hàng có mã | 2.0.1, DSig | Trước 01/06/2025 | Nhánh không có tổng hợp thuế GTGT |
| Hóa đơn GTGT có và không mã | 2.1.0, DSig | Trước 27/08/2026; mốc bắt đầu chưa xác minh | Định dạng QĐ 1271/QĐ 1306 chưa có schema chính thức |
| Hóa đơn GTGT nhiều thuế suất | 2.1.0, DSig | 27/08/2026–31/12/2026 | Giai đoạn nhận song song, kiểm tổng hợp theo thuế suất |
| Hóa đơn GTGT có mã | 2.1.1, XAdES và chữ ký CQT | Từ 27/08/2026 | Vị trí tham chiếu, `SigningTime`, chứng thư X.509 |
| Hóa đơn bán hàng không mã | 2.1.1, XAdES | Từ 27/08/2026 | Người mua không cung cấp đủ thông tin; các trường có điều kiện |
| Hóa đơn điều chỉnh/thay thế hóa đơn cũ | Phiên bản cũ và hóa đơn xử lý 2.1.1 | Sau 01/07/2026 | Điều 24 TT 91 và liên kết hóa đơn liên quan |
| Hóa đơn ngoại tệ | 2.1.0 và 2.1.1 | Trước và sau 27/08/2026 | Tỷ giá, tối đa 6 chữ số thập phân và tổng tiền |
| Hóa đơn VND có giá trị lẻ trước khi tổng hợp | 2.1.1 | Từ 27/08/2026 | Quan sát cách nhà cung cấp làm tròn; không mã hóa `HALF_UP` trước khi có kết luận |
| Một bản XML đã sửa dữ liệu sau ký | Cùng một capture hợp lệ, tạo bản tampered trong test | Mọi phiên bản chữ ký được hỗ trợ | Kết quả xác minh chữ ký phải thất bại ổn định |

## Nhận định ban đầu và kết luận

| Nhận định ban đầu | Kết luận theo văn bản gốc |
|---|---|
| Nghị định 254/2026/NĐ-CP và Thông tư 91/2026/TT-BTC có hiệu lực từ 01/07/2026 | **Đúng.** Khoản 1 Điều 43 Nghị định 254 và khoản 1 Điều 25 Thông tư 91 |
| Nghị định 254 thay khung Nghị định 123/2020/NĐ-CP đã được Nghị định 70/2025/NĐ-CP sửa | **Đúng.** Khoản 2 Điều 43 Nghị định 254 làm Nghị định 123, Điều 1 Nghị định 41 và Nghị định 70 hết hiệu lực từ 01/07/2026 |
| Thông tư 91 thay Thông tư 32/2025/TT-BTC | **Đúng về kết quả hiệu lực.** Khoản 2 Điều 25 Thông tư 91 dùng cách diễn đạt “Thông tư 32 ... hết hiệu lực”, không dùng từ “thay thế” |
| Thông tư 78/2021/TT-BTC vẫn là hướng dẫn hiện hành | **Sai.** Khoản 1 Điều 12 Thông tư 32 đã thay Thông tư 78 từ 01/06/2025; sau đó Thông tư 32 hết hiệu lực từ 01/07/2026 |

[law-108]: https://congbao.chinhphu.vn/van-ban/luat-so-108-2025-qh15-468670/61635.htm
[decree-254]: https://congbao.chinhphu.vn/van-ban/nghi-dinh-so-254-2026-nd-cp-469957/66826.htm
[circular-91]: https://congbao.chinhphu.vn/van-ban/thong-tu-so-91-2026-tt-btc-470171.htm
[decree-123]: https://congbao.chinhphu.vn/tai-ve-van-ban-so-123-2020-nd-cp-32290-33002?format=pdf
[decree-41]: https://congbao.chinhphu.vn/van-ban-dang-cong-bao/chinh-phu-c1/trang-140.htm
[decree-70]: https://congbao.chinhphu.vn/van-ban/nghi-dinh-so-70-2025-nd-cp-44623/55799.htm
[circular-78]: https://congbao.chinhphu.vn/van-ban/thong-tu-so-78-2021-tt-btc-34547.htm
[circular-32-pdf]: https://congbao.chinhphu.vn/tai-ve-van-ban-so-32-2025-tt-btc-45039-56688?format=pdf
[accounting-law]: https://congbao.chinhphu.vn/van-ban/van-ban-hop-nhat-so-41-vbhn-vpqh-469126/63459.htm
[letter-7813]: https://gdt.gov.vn/wps/wcm/connect/83ec90bf-459e-4407-9ab6-1a5ca5572013/7813.pdf?MOD=AJPERES
[decision-1450]: https://thuvienphapluat.vn/van-ban/Thue-Phi-Le-Phi/Quyet-dinh-1450-QD-TCT-2021-thanh-phan-chua-du-lieu-nghiep-vu-hoa-don-dien-tu-490526.aspx
[notice-547]: https://luatvietnam.vn/thue/thong-bao-547-tb-tct-2023-dinh-chinh-quyet-dinh-1510-qd-tct-267040-d6.html
[decision-1306]: https://thuvienphapluat.vn/van-ban/Thue-Phi-Le-Phi/Quyet-dinh-1306-QD-CT-2025-thanh-phan-chua-du-lieu-nghiep-vu-hoa-don-chung-tu-dien-tu-664897.aspx
[mof-1306]: https://mof.gov.vn/cong-nghe-thong-tin-va-chuyen-doi-so/ung-dung-cntt/tang-cuong-giam-sat-va-mo-rong-hoa-don-dien-tu-khoi-tao-tu-may-tinh-tien
[decision-1233]: https://luatvietnam.vn/thue/quyet-dinh-1233-qd-ct-2026-quy-dinh-thanh-phan-du-lieu-hoa-don-chung-tu-dien-tu-446016-d1.html
