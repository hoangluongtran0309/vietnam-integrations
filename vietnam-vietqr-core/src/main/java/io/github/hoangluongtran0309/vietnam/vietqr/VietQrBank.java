package io.github.hoangluongtran0309.vietnam.vietqr;

import java.util.Arrays;
import java.util.Optional;

/**
 * Vietnamese banks and other NAPAS members (digital banks, e-wallets, finance companies) with their NAPAS BINs.
 *
 * <p>Source: the public VietQR bank list at {@code https://api.vietqr.io/v2/banks}, snapshot taken 2026-10-02.
 * Being listed here does not guarantee that the member accepts VietQR transfers; see {@code docs/vietqr/banks.md}.
 * New constants are appended over time, so never persist {@link #ordinal()}; store {@link #bin()} instead.
 *
 * <p>TODO(v0.1): verify every BIN against the official NAPAS member list (the current source is a third party).
 * Users can always pass a raw BIN via {@link VietQrRequest.Builder#bankBin(String)}.
 */
public enum VietQrBank {

    /** Vietcombank — Ngân hàng TMCP Ngoại Thương Việt Nam. */
    VIETCOMBANK("VCB", "970436", "Vietcombank"),

    /** VietinBank — Ngân hàng TMCP Công thương Việt Nam. */
    VIETINBANK("ICB", "970415", "VietinBank"),

    /** BIDV — Ngân hàng TMCP Đầu tư và Phát triển Việt Nam. */
    BIDV("BIDV", "970418", "BIDV"),

    /** Agribank — Ngân hàng Nông nghiệp và Phát triển Nông thôn Việt Nam. */
    AGRIBANK("VBA", "970405", "Agribank"),

    /** Techcombank — Ngân hàng TMCP Kỹ thương Việt Nam. */
    TECHCOMBANK("TCB", "970407", "Techcombank"),

    /** MBBank — Ngân hàng TMCP Quân đội. */
    MB_BANK("MB", "970422", "MBBank"),

    /** ACB — Ngân hàng TMCP Á Châu. */
    ACB("ACB", "970416", "ACB"),

    /** VPBank — Ngân hàng TMCP Việt Nam Thịnh Vượng. */
    VPBANK("VPB", "970432", "VPBank"),

    /** TPBank — Ngân hàng TMCP Tiên Phong. */
    TPBANK("TPB", "970423", "TPBank"),

    /** Sacombank — Ngân hàng TMCP Sài Gòn Thương Tín. */
    SACOMBANK("STB", "970403", "Sacombank"),

    /** CIMB — Ngân hàng TNHH MTV CIMB Việt Nam. */
    CIMB("CIMB", "422589", "CIMB"),

    /** HSBC — Ngân hàng TNHH MTV HSBC (Việt Nam). */
    HSBC("HSBC", "458761", "HSBC"),

    /** Citibank — Ngân hàng Citibank, N.A. - Chi nhánh Hà Nội. */
    CITIBANK("CITIBANK", "533948", "Citibank"),

    /** CAKE — TMCP Việt Nam Thịnh Vượng - Ngân hàng số CAKE by VPBank. */
    CAKE("CAKE", "546034", "CAKE"),

    /** Ubank — TMCP Việt Nam Thịnh Vượng - Ngân hàng số Ubank by VPBank. */
    UBANK("Ubank", "546035", "Ubank"),

    /** KBank — Ngân hàng Đại chúng TNHH Kasikornbank. */
    KBANK("KBank", "668888", "KBank"),

    /** DBSBank — DBS Bank Ltd - Chi nhánh Thành phố Hồ Chí Minh. */
    DBS_BANK("DBS", "796500", "DBSBank"),

    /** Nonghyup — Ngân hàng Nonghyup - Chi nhánh Hà Nội. */
    NONGHYUP_HANOI("NHB HN", "801011", "Nonghyup"),

    /** Timo — Ngân hàng số Timo by Ban Viet Bank (Timo by Ban Viet Bank). */
    TIMO("TIMO", "963388", "Timo"),

    /** SaigonBank — Ngân hàng TMCP Sài Gòn Công Thương. */
    SAIGONBANK("SGICB", "970400", "SaigonBank"),

    /** Vikki — Ngân hàng TNHH MTV Số Vikki. */
    VIKKI("Vikki", "970406", "Vikki"),

    /** GPBank — Ngân hàng Thương mại TNHH MTV Dầu Khí Toàn Cầu. */
    GPBANK("GPB", "970408", "GPBank"),

    /** BacABank — Ngân hàng TMCP Bắc Á. */
    BAC_A_BANK("BAB", "970409", "BacABank"),

    /** StandardChartered — Ngân hàng TNHH MTV Standard Chartered Bank Việt Nam. */
    STANDARD_CHARTERED("SCVN", "970410", "StandardChartered"),

    /** PVcomBank — Ngân hàng TMCP Đại Chúng Việt Nam. */
    PVCOMBANK("PVCB", "970412", "PVcomBank"),

    /** MBV — Ngân hàng TNHH MTV Việt Nam Hiện Đại. */
    MBV("MBV", "970414", "MBV"),

    /** NCB — Ngân hàng TMCP Quốc Dân. */
    NCB("NCB", "970419", "NCB"),

    /** VRB — Ngân hàng Liên doanh Việt - Nga. */
    VRB("VRB", "970421", "VRB"),

    /** ShinhanBank — Ngân hàng TNHH MTV Shinhan Việt Nam. */
    SHINHAN_BANK("SHBVN", "970424", "ShinhanBank"),

    /** ABBANK — Ngân hàng TMCP An Bình. */
    ABBANK("ABB", "970425", "ABBANK"),

    /** MSB — Ngân hàng TMCP Hàng Hải Việt Nam. */
    MSB("MSB", "970426", "MSB"),

    /** VietABank — Ngân hàng TMCP Việt Á. */
    VIET_A_BANK("VAB", "970427", "VietABank"),

    /** NamABank — Ngân hàng TMCP Nam Á. */
    NAM_A_BANK("NAB", "970428", "NamABank"),

    /** SCB — Ngân hàng TMCP Sài Gòn. */
    SCB("SCB", "970429", "SCB"),

    /** PGBank — Ngân hàng TMCP Thịnh vượng và Phát triển. */
    PGBANK("PGB", "970430", "PGBank"),

    /** Eximbank — Ngân hàng TMCP Xuất Nhập khẩu Việt Nam. */
    EXIMBANK("EIB", "970431", "Eximbank"),

    /** VietBank — Ngân hàng TMCP Việt Nam Thương Tín. */
    VIETBANK("VIETBANK", "970433", "VietBank"),

    /** IndovinaBank — Ngân hàng TNHH Indovina. */
    INDOVINA_BANK("IVB", "970434", "IndovinaBank"),

    /** HDBank — Ngân hàng TMCP Phát triển Thành phố Hồ Chí Minh. */
    HDBANK("HDB", "970437", "HDBank"),

    /** BaoVietBank — Ngân hàng TMCP Bảo Việt. */
    BAOVIET_BANK("BVB", "970438", "BaoVietBank"),

    /** PublicBank — Ngân hàng TNHH MTV Public Việt Nam. */
    PUBLIC_BANK("PBVN", "970439", "PublicBank"),

    /** SeABank — Ngân hàng TMCP Đông Nam Á. */
    SEABANK("SEAB", "970440", "SeABank"),

    /** VIB — Ngân hàng TMCP Quốc tế Việt Nam. */
    VIB("VIB", "970441", "VIB"),

    /** HongLeong — Ngân hàng TNHH MTV Hong Leong Việt Nam. */
    HONG_LEONG("HLBVN", "970442", "HongLeong"),

    /** SHB — Ngân hàng TMCP Sài Gòn - Hà Nội. */
    SHB("SHB", "970443", "SHB"),

    /** CBBank — Ngân hàng Thương mại TNHH MTV Xây dựng Việt Nam. */
    CBBANK("CBB", "970444", "CBBank"),

    /** COOPBANK — Ngân hàng Hợp tác xã Việt Nam. */
    COOPBANK("COOPBANK", "970446", "COOPBANK"),

    /** OCB — Ngân hàng TMCP Phương Đông. */
    OCB("OCB", "970448", "OCB"),

    /** LPBank — Ngân hàng TMCP Lộc Phát Việt Nam. */
    LPBANK("LPB", "970449", "LPBank"),

    /** KienLongBank — Ngân hàng TMCP Kiên Long. */
    KIENLONGBANK("KLB", "970452", "KienLongBank"),

    /** VietCapitalBank — Ngân hàng TMCP Bản Việt. */
    VIET_CAPITAL_BANK("VCCB", "970454", "VietCapitalBank"),

    /** IBKHN — Ngân hàng Công nghiệp Hàn Quốc - Chi nhánh Hà Nội. */
    IBK_HANOI("IBK - HN", "970455", "IBKHN"),

    /** IBKHCM — Ngân hàng Công nghiệp Hàn Quốc - Chi nhánh TP. Hồ Chí Minh. */
    IBK_HCM("IBK - HCM", "970456", "IBKHCM"),

    /** Woori — Ngân hàng TNHH MTV Woori Việt Nam. */
    WOORI("WVN", "970457", "Woori"),

    /** UnitedOverseas — Ngân hàng United Overseas - Chi nhánh TP. Hồ Chí Minh. */
    UOB("UOB", "970458", "UnitedOverseas"),

    /** KookminHN — Ngân hàng Kookmin - Chi nhánh Hà Nội. */
    KOOKMIN_HANOI("KBHN", "970462", "KookminHN"),

    /** KookminHCM — Ngân hàng Kookmin - Chi nhánh Thành phố Hồ Chí Minh. */
    KOOKMIN_HCM("KBHCM", "970463", "KookminHCM"),

    /** KEBHanaHCM — Ngân hàng KEB Hana – Chi nhánh Thành phố Hồ Chí Minh. */
    KEB_HANA_HCM("KEBHANAHCM", "970466", "KEBHanaHCM"),

    /** KEBHANAHN — Ngân hàng KEB Hana – Chi nhánh Hà Nội. */
    KEB_HANA_HANOI("KEBHANAHN", "970467", "KEBHANAHN"),

    /** ViettelMoney — Tổng Công ty Dịch vụ số Viettel - Chi nhánh tập đoàn công nghiệp viễn thông Quân Đội. */
    VIETTEL_MONEY("VTLMONEY", "971005", "ViettelMoney"),

    /** VNPTMoney — VNPT Money. */
    VNPT_MONEY("VNPTMONEY", "971011", "VNPTMoney"),

    /** MoMo — CTCP Dịch Vụ Di Động Trực Tuyến. */
    MOMO("momo", "971025", "MoMo"),

    /** PVcomBank Pay — Ngân hàng TMCP Đại Chúng Việt Nam Ngân hàng số. */
    PVCOMBANK_PAY("PVDB", "971133", "PVcomBank Pay"),

    /** MAFC — Công ty Tài chính TNHH MTV Mirae Asset (Việt Nam). */
    MAFC("MAFC", "977777", "MAFC"),

    /** VBSP — Ngân hàng Chính sách Xã hội. */
    VBSP("VBSP", "999888", "VBSP");

    private final String shortCode;
    private final String bin;
    private final String displayName;

    VietQrBank(String shortCode, String bin, String displayName) {
        this.shortCode = shortCode;
        this.bin = bin;
        this.displayName = displayName;
    }

    /** Short code used by the VietQR bank list, e.g. {@code VCB}. */
    public String shortCode() {
        return shortCode;
    }

    /** 6-digit NAPAS BIN, e.g. {@code 970436}. */
    public String bin() {
        return bin;
    }

    /** Brand name suitable for display, e.g. {@code Vietcombank}. */
    public String displayName() {
        return displayName;
    }

    public static Optional<VietQrBank> fromBin(String bin) {
        return Arrays.stream(values()).filter(b -> b.bin.equals(bin)).findFirst();
    }

    /** Case-insensitive lookup by {@link #shortCode()}. */
    public static Optional<VietQrBank> fromShortCode(String shortCode) {
        return Arrays.stream(values()).filter(b -> b.shortCode.equalsIgnoreCase(shortCode)).findFirst();
    }
}
