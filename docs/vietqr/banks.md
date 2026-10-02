# Danh sách ngân hàng (BIN NAPAS)

Mỗi mã VietQR chứa **BIN** (Bank Identification Number) 6 chữ số của ngân hàng thụ hưởng — tag `38` → `01` → `00`
trong payload. Enum `VietQrBank` (module `vietnam-vietqr-core`) gom sẵn BIN của các thành viên NAPAS để bạn
không phải tra tay.

## Tóm tắt nhanh

| Câu hỏi | Trả lời |
|---|---|
| Có bao nhiêu mục? | **65** (ngân hàng thương mại, ngân hàng số, chi nhánh ngân hàng nước ngoài, ví điện tử, công ty tài chính) |
| Lấy từ đâu? | Danh sách công khai `https://api.vietqr.io/v2/banks`, chụp ngày **2026-10-02** |
| Đã đối chiếu với NAPAS chưa? | **Chưa.** NAPAS không công bố danh sách BIN dạng máy đọc được. Xem [Độ tin cậy](#độ-tin-cậy-của-dữ-liệu) |
| Ngân hàng không có trong enum? | Truyền BIN thô: `VietQrRequest.builder().bankBin("970xxx")` |
| Có nên lưu `ordinal()` vào DB? | **Không.** Hằng mới được thêm dần; hãy lưu `bin()` |

## Cách dùng

### Java thuần

```java
// Theo hằng số
VietQrRequest request = VietQrRequest.builder()
        .bank(VietQrBank.VIETCOMBANK)          // tương đương .bankBin("970436")
        .accountNumber("0123456789")
        .amount(150_000)
        .purpose("DH12345")
        .build();

// Theo BIN thô (ngân hàng chưa có trong enum, hoặc BIN đọc từ DB)
VietQrRequest raw = VietQrRequest.builder()
        .bankBin("970436")
        .accountNumber("0123456789")
        .build();
```

### Tra cứu

```java
VietQrBank.fromBin("970436");          // Optional[VIETCOMBANK]
VietQrBank.fromShortCode("vcb");       // Optional[VIETCOMBANK] — không phân biệt hoa thường
VietQrBank.fromBin("000000");          // Optional.empty

VietQrBank.MB_BANK.bin();              // "970422"
VietQrBank.MB_BANK.shortCode();        // "MB"
VietQrBank.MB_BANK.displayName();      // "MBBank" — dùng để hiển thị cho người dùng
```

### Từ payload đã decode

```java
VietQrPayload payload = new VietQrDecoder().decode(qrText);
String bankName = payload.bank()
        .map(VietQrBank::displayName)
        .orElse("Ngân hàng BIN " + payload.bankBin());
```

`payload.bank()` trả về `Optional.empty()` khi BIN không có trong enum — payload vẫn hợp lệ, chỉ là thư viện
chưa biết tên ngân hàng.

### Spring Boot

Property `vietnam.vietqr.bank-bin` nhận **BIN**, không nhận tên hằng:

```yaml
vietnam:
  vietqr:
    bank-bin: "970436"            # Vietcombank
    account-number: "0123456789"  # luôn để trong ngoặc kép: YAML có thể hiểu nhầm số bắt đầu bằng 0
```

## Độ tin cậy của dữ liệu

- Nguồn `api.vietqr.io` do bên thứ ba (VietQR.io) vận hành, được cộng đồng dùng rộng rãi nhưng **không phải
  danh sách chính thức của NAPAS**. Trong code vẫn để `TODO(v0.1): verify every BIN against the official NAPAS member list`.
- Có mặt trong enum **không đảm bảo** thành viên đó nhận chuyển khoản bằng VietQR. Cột *Chuyển khoản qua VietQR\**
  dưới đây lấy nguyên trường `transferSupported` của nguồn, chưa được kiểm chứng độc lập — khi tích hợp với một
  ngân hàng cụ thể, hãy quét thử bằng app thật (xem [manual-verification.md](manual-verification.md)).
- Với ngân hàng đã đổi tên, BIN không đổi. Một số trường hợp hay gặp:

| BIN | Hằng số | Tên cũ |
|---|---|---|
| `970414` | `MBV` | OceanBank |
| `970449` | `LPBANK` | LienVietPostBank |
| `970406` | `VIKKI` | DongA Bank |

## Danh sách đầy đủ

Sắp xếp theo BIN. Hằng số đặt tên theo `shortName` của nguồn (UPPER_SNAKE_CASE); 10 hằng có từ bản đầu
(`VIETCOMBANK`, `VIETINBANK`, `BIDV`, `AGRIBANK`, `TECHCOMBANK`, `MB_BANK`, `ACB`, `VPBANK`, `TPBANK`, `SACOMBANK`)
giữ nguyên tên và giá trị.

| BIN | Hằng số | Mã (`shortCode`) | Tên hiển thị (`displayName`) | Tên đầy đủ | Chuyển khoản qua VietQR* |
|---|---|---|---|---|---|
| `422589` | `CIMB` | `CIMB` | CIMB | Ngân hàng TNHH MTV CIMB Việt Nam | ✅ |
| `458761` | `HSBC` | `HSBC` | HSBC | Ngân hàng TNHH MTV HSBC (Việt Nam) | — |
| `533948` | `CITIBANK` | `CITIBANK` | Citibank | Ngân hàng Citibank, N.A. - Chi nhánh Hà Nội | — |
| `546034` | `CAKE` | `CAKE` | CAKE | TMCP Việt Nam Thịnh Vượng - Ngân hàng số CAKE by VPBank | ✅ |
| `546035` | `UBANK` | `Ubank` | Ubank | TMCP Việt Nam Thịnh Vượng - Ngân hàng số Ubank by VPBank | ✅ |
| `668888` | `KBANK` | `KBank` | KBank | Ngân hàng Đại chúng TNHH Kasikornbank | ✅ |
| `796500` | `DBS_BANK` | `DBS` | DBSBank | DBS Bank Ltd - Chi nhánh Thành phố Hồ Chí Minh | — |
| `801011` | `NONGHYUP_HANOI` | `NHB HN` | Nonghyup | Ngân hàng Nonghyup - Chi nhánh Hà Nội | — |
| `963388` | `TIMO` | `TIMO` | Timo | Ngân hàng số Timo by Ban Viet Bank (Timo by Ban Viet Bank) | ✅ |
| `970400` | `SAIGONBANK` | `SGICB` | SaigonBank | Ngân hàng TMCP Sài Gòn Công Thương | ✅ |
| `970403` | `SACOMBANK` | `STB` | Sacombank | Ngân hàng TMCP Sài Gòn Thương Tín | ✅ |
| `970405` | `AGRIBANK` | `VBA` | Agribank | Ngân hàng Nông nghiệp và Phát triển Nông thôn Việt Nam | ✅ |
| `970406` | `VIKKI` | `Vikki` | Vikki | Ngân hàng TNHH MTV Số Vikki | — |
| `970407` | `TECHCOMBANK` | `TCB` | Techcombank | Ngân hàng TMCP Kỹ thương Việt Nam | ✅ |
| `970408` | `GPBANK` | `GPB` | GPBank | Ngân hàng Thương mại TNHH MTV Dầu Khí Toàn Cầu | — |
| `970409` | `BAC_A_BANK` | `BAB` | BacABank | Ngân hàng TMCP Bắc Á | ✅ |
| `970410` | `STANDARD_CHARTERED` | `SCVN` | StandardChartered | Ngân hàng TNHH MTV Standard Chartered Bank Việt Nam | — |
| `970412` | `PVCOMBANK` | `PVCB` | PVcomBank | Ngân hàng TMCP Đại Chúng Việt Nam | ✅ |
| `970414` | `MBV` | `MBV` | MBV | Ngân hàng TNHH MTV Việt Nam Hiện Đại | ✅ |
| `970415` | `VIETINBANK` | `ICB` | VietinBank | Ngân hàng TMCP Công thương Việt Nam | ✅ |
| `970416` | `ACB` | `ACB` | ACB | Ngân hàng TMCP Á Châu | ✅ |
| `970418` | `BIDV` | `BIDV` | BIDV | Ngân hàng TMCP Đầu tư và Phát triển Việt Nam | ✅ |
| `970419` | `NCB` | `NCB` | NCB | Ngân hàng TMCP Quốc Dân | ✅ |
| `970421` | `VRB` | `VRB` | VRB | Ngân hàng Liên doanh Việt - Nga | — |
| `970422` | `MB_BANK` | `MB` | MBBank | Ngân hàng TMCP Quân đội | ✅ |
| `970423` | `TPBANK` | `TPB` | TPBank | Ngân hàng TMCP Tiên Phong | ✅ |
| `970424` | `SHINHAN_BANK` | `SHBVN` | ShinhanBank | Ngân hàng TNHH MTV Shinhan Việt Nam | ✅ |
| `970425` | `ABBANK` | `ABB` | ABBANK | Ngân hàng TMCP An Bình | ✅ |
| `970426` | `MSB` | `MSB` | MSB | Ngân hàng TMCP Hàng Hải Việt Nam | ✅ |
| `970427` | `VIET_A_BANK` | `VAB` | VietABank | Ngân hàng TMCP Việt Á | ✅ |
| `970428` | `NAM_A_BANK` | `NAB` | NamABank | Ngân hàng TMCP Nam Á | ✅ |
| `970429` | `SCB` | `SCB` | SCB | Ngân hàng TMCP Sài Gòn | ✅ |
| `970430` | `PGBANK` | `PGB` | PGBank | Ngân hàng TMCP Thịnh vượng và Phát triển | ✅ |
| `970431` | `EXIMBANK` | `EIB` | Eximbank | Ngân hàng TMCP Xuất Nhập khẩu Việt Nam | ✅ |
| `970432` | `VPBANK` | `VPB` | VPBank | Ngân hàng TMCP Việt Nam Thịnh Vượng | ✅ |
| `970433` | `VIETBANK` | `VIETBANK` | VietBank | Ngân hàng TMCP Việt Nam Thương Tín | ✅ |
| `970434` | `INDOVINA_BANK` | `IVB` | IndovinaBank | Ngân hàng TNHH Indovina | — |
| `970436` | `VIETCOMBANK` | `VCB` | Vietcombank | Ngân hàng TMCP Ngoại Thương Việt Nam | ✅ |
| `970437` | `HDBANK` | `HDB` | HDBank | Ngân hàng TMCP Phát triển Thành phố Hồ Chí Minh | ✅ |
| `970438` | `BAOVIET_BANK` | `BVB` | BaoVietBank | Ngân hàng TMCP Bảo Việt | ✅ |
| `970439` | `PUBLIC_BANK` | `PBVN` | PublicBank | Ngân hàng TNHH MTV Public Việt Nam | — |
| `970440` | `SEABANK` | `SEAB` | SeABank | Ngân hàng TMCP Đông Nam Á | ✅ |
| `970441` | `VIB` | `VIB` | VIB | Ngân hàng TMCP Quốc tế Việt Nam | ✅ |
| `970442` | `HONG_LEONG` | `HLBVN` | HongLeong | Ngân hàng TNHH MTV Hong Leong Việt Nam | — |
| `970443` | `SHB` | `SHB` | SHB | Ngân hàng TMCP Sài Gòn - Hà Nội | ✅ |
| `970444` | `CBBANK` | `CBB` | CBBank | Ngân hàng Thương mại TNHH MTV Xây dựng Việt Nam | — |
| `970446` | `COOPBANK` | `COOPBANK` | COOPBANK | Ngân hàng Hợp tác xã Việt Nam | ✅ |
| `970448` | `OCB` | `OCB` | OCB | Ngân hàng TMCP Phương Đông | ✅ |
| `970449` | `LPBANK` | `LPB` | LPBank | Ngân hàng TMCP Lộc Phát Việt Nam | ✅ |
| `970452` | `KIENLONGBANK` | `KLB` | KienLongBank | Ngân hàng TMCP Kiên Long | ✅ |
| `970454` | `VIET_CAPITAL_BANK` | `VCCB` | VietCapitalBank | Ngân hàng TMCP Bản Việt | ✅ |
| `970455` | `IBK_HANOI` | `IBK - HN` | IBKHN | Ngân hàng Công nghiệp Hàn Quốc - Chi nhánh Hà Nội | — |
| `970456` | `IBK_HCM` | `IBK - HCM` | IBKHCM | Ngân hàng Công nghiệp Hàn Quốc - Chi nhánh TP. Hồ Chí Minh | — |
| `970457` | `WOORI` | `WVN` | Woori | Ngân hàng TNHH MTV Woori Việt Nam | ✅ |
| `970458` | `UOB` | `UOB` | UnitedOverseas | Ngân hàng United Overseas - Chi nhánh TP. Hồ Chí Minh | — |
| `970462` | `KOOKMIN_HANOI` | `KBHN` | KookminHN | Ngân hàng Kookmin - Chi nhánh Hà Nội | — |
| `970463` | `KOOKMIN_HCM` | `KBHCM` | KookminHCM | Ngân hàng Kookmin - Chi nhánh Thành phố Hồ Chí Minh | — |
| `970466` | `KEB_HANA_HCM` | `KEBHANAHCM` | KEBHanaHCM | Ngân hàng KEB Hana – Chi nhánh Thành phố Hồ Chí Minh | — |
| `970467` | `KEB_HANA_HANOI` | `KEBHANAHN` | KEBHANAHN | Ngân hàng KEB Hana – Chi nhánh Hà Nội | — |
| `971005` | `VIETTEL_MONEY` | `VTLMONEY` | ViettelMoney | Tổng Công ty Dịch vụ số Viettel - Chi nhánh tập đoàn công nghiệp viễn thông Quân Đội | — |
| `971011` | `VNPT_MONEY` | `VNPTMONEY` | VNPTMoney | VNPT Money | — |
| `971025` | `MOMO` | `momo` | MoMo | CTCP Dịch Vụ Di Động Trực Tuyến | ✅ |
| `971133` | `PVCOMBANK_PAY` | `PVDB` | PVcomBank Pay | Ngân hàng TMCP Đại Chúng Việt Nam Ngân hàng số | ✅ |
| `977777` | `MAFC` | `MAFC` | MAFC | Công ty Tài chính TNHH MTV Mirae Asset (Việt Nam) | — |
| `999888` | `VBSP` | `VBSP` | VBSP | Ngân hàng Chính sách Xã hội | — |

\* Theo trường `transferSupported` của `api.vietqr.io` tại ngày chụp, chưa kiểm chứng độc lập.

## Cập nhật danh sách (cho maintainer)

1. So sánh BIN trong code với nguồn:

   ```bash
   curl -s https://api.vietqr.io/v2/banks | jq -r '.data[].bin' | sort > remote-bins.txt
   grep -oE '"[0-9]{6}"' vietnam-vietqr-core/src/main/java/io/github/hoangluongtran0309/vietnam/vietqr/VietQrBank.java \
     | tr -d '"' | sort > local-bins.txt
   diff local-bins.txt remote-bins.txt   # dòng ">" = BIN mới cần thêm, dòng "<" = BIN đã bị nguồn gỡ
   ```

2. BIN mới: thêm hằng **vào cuối** enum (không chèn giữa), kèm Javadoc `shortName — tên đầy đủ`.
3. BIN bị gỡ hoặc đổi tên ngân hàng: **không xóa / đổi tên hằng** trước 1.0 nếu chưa ghi breaking change vào
   `CHANGELOG.md`. Ưu tiên đánh dấu `@Deprecated` kèm ghi chú.
4. Cập nhật bảng ở trang này và ngày chụp ở Javadoc của `VietQrBank`.
5. `./mvnw -pl vietnam-vietqr-core -am verify` — `VietQrBankTest` kiểm tra BIN 6 chữ số, không trùng BIN/mã,
   tra cứu hai chiều và giữ nguyên giá trị 10 hằng cũ.
