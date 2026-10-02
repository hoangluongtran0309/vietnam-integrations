package io.github.hoangluongtran0309.vietnam.demo;

import io.github.hoangluongtran0309.vietnam.vietqr.QrImageOptions;
import io.github.hoangluongtran0309.vietnam.vietqr.QrImageRenderer;
import io.github.hoangluongtran0309.vietnam.vietqr.VietQrBank;
import io.github.hoangluongtran0309.vietnam.vietqr.VietQrDecoder;
import io.github.hoangluongtran0309.vietnam.vietqr.VietQrPayload;
import io.github.hoangluongtran0309.vietnam.vietqr.autoconfigure.VietQrGenerator;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Manual verification page: a set of QR codes to scan with real banking apps (docs/vietqr/manual-verification.md).
 */
@RestController
class VerificationController {

    private static final long AMOUNT = 10_000;
    private static final int SIZE = 480;

    private final QrImageRenderer renderer;
    private final VietQrDecoder decoder;
    private final Map<String, VerificationCase> cases = new LinkedHashMap<>();

    VerificationController(VietQrGenerator vietQr, QrImageRenderer renderer, VietQrDecoder decoder) {
        this.renderer = renderer;
        this.decoder = decoder;

        QrImageOptions plain = QrImageOptions.ofSize(SIZE);
        QrImageOptions colors = QrImageOptions.builder()
                .size(SIZE)
                .foreground(new Color(0x0B, 0x2A, 0x5B))
                .background(new Color(0xFF, 0xF8, 0xE7))
                .build();
        QrImageOptions logo = QrImageOptions.builder().size(SIZE).logo(demoLogo()).build();
        QrImageOptions largestLogo = QrImageOptions.builder()
                .size(SIZE)
                .logo(demoLogo())
                .logoScale(QrImageOptions.MAX_LOGO_SCALE)
                .build();

        List<VerificationCase> all = List.of(
                new VerificationCase("static", "QR tĩnh — người trả tự nhập số tiền", vietQr.staticQr(), plain),
                new VerificationCase("amount", "QR động 10.000 đ, nội dung ngắn", vietQr.forAmount(AMOUNT, "DH001"), plain),
                new VerificationCase("diacritics", "Nội dung có dấu tiếng Việt",
                        vietQr.forAmount(AMOUNT, "Thanh toán đơn hàng DH002"), plain),
                new VerificationCase("special", "Ký tự đặc biệt # - / : . ,",
                        vietQr.forAmount(AMOUNT, "DH003 #A-B/C:D.E,F"), plain),
                purposeOfLength(vietQr, 25, plain),
                purposeOfLength(vietQr, 26, plain),
                purposeOfLength(vietQr, 50, plain),
                purposeOfLength(vietQr, 70, plain),
                purposeOfLength(vietQr, 95, plain),
                new VerificationCase("colors", "Màu tùy chỉnh (navy trên kem)", vietQr.forAmount(AMOUNT, "DH004"), colors),
                new VerificationCase("logo", "Logo, scale mặc định 0.2", vietQr.forAmount(AMOUNT, "DH005"), logo),
                new VerificationCase("logo-max", "Logo, scale tối đa 0.25", vietQr.forAmount(AMOUNT, "DH006"), largestLogo));
        all.forEach(c -> cases.put(c.id(), c));
    }

    @GetMapping(value = "/verify", produces = MediaType.TEXT_HTML_VALUE)
    String page() {
        StringBuilder html = new StringBuilder("""
                <!doctype html>
                <html lang="vi"><head><meta charset="utf-8">
                <meta name="viewport" content="width=device-width, initial-scale=1">
                <title>VietQR — kiểm tra thủ công</title>
                <style>
                  body { font-family: system-ui, sans-serif; max-width: 980px; margin: 0 auto; padding: 16px; }
                  section { border-top: 1px solid #ccc; padding: 16px 0; display: flex; gap: 24px; flex-wrap: wrap; }
                  img { width: 320px; height: 320px; }
                  table { border-collapse: collapse; } td { padding: 2px 8px; vertical-align: top; }
                  code { word-break: break-all; }
                </style></head><body>
                <h1>VietQR — kiểm tra thủ công</h1>
                <p>Quét từng mã bằng app ngân hàng và so với cột "Mong đợi". Ghi kết quả vào
                <code>docs/vietqr/manual-verification.md</code>. Muốn quét ra người nhận thật, chạy app với
                <code>--vietnam.vietqr.bank-bin=...</code> và <code>--vietnam.vietqr.account-number=...</code>.</p>
                <p>Lấy payload từ ảnh chụp QR của app ngân hàng (để làm test vector):</p>
                <form action="/decode-image" method="post" enctype="multipart/form-data">
                  <input type="file" name="file" accept="image/*" required> <button>Decode ảnh</button>
                </form>
                """);
        for (VerificationCase c : cases.values()) {
            VietQrPayload expected = decoder.decode(c.payload());
            String bank = expected.bank().map(VietQrBank::displayName).orElse("BIN " + expected.bankBin());
            String purpose = expected.purpose() == null ? "(không có)" : expected.purpose();
            html.append("<section id=\"").append(c.id()).append("\">")
                    .append("<img src=\"/verify/").append(c.id()).append(".png\" alt=\"").append(c.id()).append("\">")
                    .append("<div><h2>").append(escape(c.id())).append(" — ").append(escape(c.title())).append("</h2>")
                    .append("<table>")
                    .append(row("Ngân hàng", bank + " (" + expected.bankBin() + ")"))
                    .append(row("Số tài khoản", expected.accountNumber()))
                    .append(row("Số tiền", expected.amount() == null ? "(người trả nhập)" : expected.amount() + " đ"))
                    .append(row("Nội dung", purpose))
                    .append(row("Độ dài nội dung", expected.purpose() == null ? "0" : String.valueOf(expected.purpose().length())))
                    .append(row("Độ dài payload", String.valueOf(c.payload().length())))
                    .append("</table><p>Payload: <code>").append(escape(c.payload())).append("</code></p></div></section>");
        }
        return html.append("</body></html>").toString();
    }

    @GetMapping(value = "/verify/{id}.png", produces = MediaType.IMAGE_PNG_VALUE)
    byte[] image(@PathVariable String id) {
        VerificationCase c = cases.get(id);
        if (c == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        return renderer.renderPng(c.payload(), c.options());
    }

    /**
     * A purpose of exactly {@code length} characters that shows where a banking app cut it: every tenth character is
     * the tens digit of its position, the rest are {@code x}. E.g. length 25: {@code L25-xxxxx1xxxxxxxxx2xxxxx}.
     */
    private static VerificationCase purposeOfLength(VietQrGenerator vietQr, int length, QrImageOptions options) {
        String prefix = "L" + length + "-";
        StringBuilder purpose = new StringBuilder(prefix);
        for (int position = prefix.length() + 1; position <= length; position++) {
            purpose.append(position % 10 == 0 ? Character.forDigit(position / 10 % 10, 10) : 'x');
        }
        return new VerificationCase("purpose-" + length, "Nội dung dài đúng " + length + " ký tự",
                vietQr.forAmount(AMOUNT, purpose.toString()), options);
    }

    /** Red rounded square with a yellow star, drawn in code so the sample has no binary assets. */
    private static BufferedImage demoLogo() {
        BufferedImage logo = new BufferedImage(200, 200, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = logo.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setColor(new Color(0xDA, 0x25, 0x1D));
        g.fillRoundRect(0, 0, 200, 200, 48, 48);
        Polygon star = new Polygon();
        for (int i = 0; i < 10; i++) {
            double radius = i % 2 == 0 ? 75 : 30;
            double angle = Math.toRadians(-90 + i * 36);
            star.addPoint((int) Math.round(100 + radius * Math.cos(angle)), (int) Math.round(108 + radius * Math.sin(angle)));
        }
        g.setColor(new Color(0xFF, 0xCD, 0x00));
        g.fillPolygon(star);
        g.dispose();
        return logo;
    }

    private static String row(String label, String value) {
        return "<tr><td>" + escape(label) + "</td><td><b>" + escape(value) + "</b></td></tr>";
    }

    private static String escape(String text) {
        return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
    }

    private record VerificationCase(String id, String title, String payload, QrImageOptions options) {
    }
}
