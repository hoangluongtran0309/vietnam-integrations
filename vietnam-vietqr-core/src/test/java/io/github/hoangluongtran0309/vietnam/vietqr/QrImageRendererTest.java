package io.github.hoangluongtran0309.vietnam.vietqr;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.BinaryBitmap;
import com.google.zxing.EncodeHintType;
import com.google.zxing.client.j2se.BufferedImageLuminanceSource;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.HybridBinarizer;
import com.google.zxing.qrcode.QRCodeReader;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class QrImageRendererTest {

    private static final Color LOGO_COLOR = new Color(0xD7, 0x1E, 0x28);

    private final QrImageRenderer renderer = new QrImageRenderer();

    private final String typicalPayload = new VietQrEncoder().encode(VietQrRequest.builder()
            .bank(VietQrBank.VIETCOMBANK)
            .accountNumber("0123456789")
            .amount(150_000)
            .purpose("DH12345 Thanh toan")
            .build());

    /** Longest payload the encoder can produce for a dynamic QR: maximum purpose and a 19-character account. */
    private final String longPayload = new VietQrEncoder().encode(VietQrRequest.builder()
            .bank(VietQrBank.VIETCOMBANK)
            .accountNumber("1234567890123456789")
            .amount(999_999_999)
            .purpose("DH12345 " + "x".repeat(VietQrEncoder.MAX_PURPOSE_LENGTH - 8))
            .build());

    @Test
    void sizeOnlyOverloadKeepsThePreviousOutput() throws Exception {
        var matrix = new QRCodeWriter().encode(longPayload, BarcodeFormat.QR_CODE, 400, 400,
                Map.of(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.M, EncodeHintType.MARGIN, 2));
        ByteArrayOutputStream previous = new ByteArrayOutputStream();
        MatrixToImageWriter.writeToStream(matrix, "PNG", previous);

        assertThat(renderer.renderPng(longPayload, 400)).isEqualTo(previous.toByteArray());
    }

    @Test
    void defaultImageDecodesBackToThePayload() throws Exception {
        BufferedImage image = read(renderer.renderPng(longPayload, 400));

        assertThat(image.getWidth()).isEqualTo(400);
        assertThat(image.getHeight()).isEqualTo(400);
        assertThat(decode(image)).isEqualTo(longPayload);
    }

    @Test
    void customColorsAreAppliedAndStillDecode() throws Exception {
        Color navy = new Color(0x0B, 0x2A, 0x5B);
        Color cream = new Color(0xFF, 0xF8, 0xE7);

        BufferedImage image = read(renderer.renderPng(longPayload,
                QrImageOptions.builder().foreground(navy).background(cream).build()));

        assertThat(image.getRGB(0, 0)).isEqualTo(cream.getRGB());
        assertThat(containsColor(image, navy)).isTrue();
        assertThat(decode(image)).isEqualTo(longPayload);
    }

    // The long payload is not used here: at level H it needs QR version 15, and ZXing's reader fails to locate some
    // version 15-H codes even without a logo, although other decoders (OpenCV) read them. See docs/vietqr/rendering.md.
    @ParameterizedTest
    @EnumSource(value = QrErrorCorrection.class, names = {"Q", "H"})
    void logoAtMaximumScaleStillDecodes(QrErrorCorrection level) throws Exception {
        BufferedImage image = read(renderer.renderPng(typicalPayload, QrImageOptions.builder()
                .logo(solidLogo(120, 80))
                .logoScale(QrImageOptions.MAX_LOGO_SCALE)
                .errorCorrection(level)
                .build()));

        assertThat(image.getRGB(200, 200)).isEqualTo(LOGO_COLOR.getRGB());
        assertThat(decode(image)).isEqualTo(typicalPayload);
    }

    @Test
    void logoDefaultsToHighErrorCorrection() {
        assertThat(QrImageOptions.builder().logo(solidLogo(10, 10)).build().errorCorrection())
                .isEqualTo(QrErrorCorrection.H);
        assertThat(QrImageOptions.builder().build().errorCorrection()).isEqualTo(QrErrorCorrection.M);
    }

    @Test
    void logoCanBeReadFromImageBytes() throws Exception {
        ByteArrayOutputStream png = new ByteArrayOutputStream();
        ImageIO.write(solidLogo(30, 20), "PNG", png);

        QrImageOptions options = QrImageOptions.builder().logo(png.toByteArray()).build();

        assertThat(options.logo().getWidth()).isEqualTo(30);
        assertThat(options.logo().getHeight()).isEqualTo(20);
    }

    @Test
    void logoCannotBeChangedThroughTheSourceOrTheAccessor() {
        BufferedImage source = solidLogo(10, 10);
        QrImageOptions options = QrImageOptions.builder().logo(source).build();

        source.setRGB(0, 0, Color.BLUE.getRGB());
        options.logo().setRGB(1, 1, Color.BLUE.getRGB());

        assertThat(options.logo().getRGB(0, 0)).isEqualTo(LOGO_COLOR.getRGB());
        assertThat(options.logo().getRGB(1, 1)).isEqualTo(LOGO_COLOR.getRGB());
    }

    @Test
    void rejectsLogoBytesThatAreNotAnImage() {
        assertThatThrownBy(() -> QrImageOptions.builder().logo(new byte[] {1, 2, 3}))
                .isInstanceOf(VietQrException.class)
                .hasMessage("Unsupported logo image format");
    }

    @ParameterizedTest
    @EnumSource(value = QrErrorCorrection.class, names = {"L", "M"})
    void rejectsLogoWithLowErrorCorrection(QrErrorCorrection level) {
        assertThatThrownBy(() -> QrImageOptions.builder().logo(solidLogo(10, 10)).errorCorrection(level).build())
                .isInstanceOf(VietQrException.class)
                .hasMessage("a logo requires errorCorrection Q or H, but was " + level);
    }

    @Test
    void rejectsOutOfRangeValues() {
        assertThatThrownBy(() -> QrImageOptions.ofSize(QrImageOptions.MIN_SIZE - 1))
                .isInstanceOf(VietQrException.class).hasMessageContaining("size must be between");
        assertThatThrownBy(() -> QrImageOptions.ofSize(QrImageOptions.MAX_SIZE + 1))
                .isInstanceOf(VietQrException.class).hasMessageContaining("size must be between");
        assertThatThrownBy(() -> QrImageOptions.builder().margin(-1).build())
                .isInstanceOf(VietQrException.class).hasMessageContaining("margin");
        assertThatThrownBy(() -> QrImageOptions.builder().logoScale(0).build())
                .isInstanceOf(VietQrException.class).hasMessageContaining("logoScale");
        assertThatThrownBy(() -> QrImageOptions.builder().logoScale(QrImageOptions.MAX_LOGO_SCALE + 0.01).build())
                .isInstanceOf(VietQrException.class).hasMessageContaining("logoScale");
    }

    @Test
    void rejectsInvertedColors() {
        assertThatThrownBy(() -> QrImageOptions.builder().foreground(Color.WHITE).background(Color.BLACK).build())
                .isInstanceOf(VietQrException.class)
                .hasMessageContaining("foreground must be darker than background");
        assertThatThrownBy(() -> QrImageOptions.builder().foreground(Color.GRAY).background(Color.GRAY).build())
                .isInstanceOf(VietQrException.class);
    }

    private static BufferedImage solidLogo(int width, int height) {
        BufferedImage logo = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = logo.createGraphics();
        g.setColor(LOGO_COLOR);
        g.fillRect(0, 0, width, height);
        g.dispose();
        return logo;
    }

    private static BufferedImage read(byte[] png) throws IOException {
        return ImageIO.read(new ByteArrayInputStream(png));
    }

    private static String decode(BufferedImage image) throws Exception {
        BinaryBitmap bitmap = new BinaryBitmap(new HybridBinarizer(new BufferedImageLuminanceSource(image)));
        return new QRCodeReader().decode(bitmap).getText();
    }

    private static boolean containsColor(BufferedImage image, Color color) {
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                if (image.getRGB(x, y) == color.getRGB()) {
                    return true;
                }
            }
        }
        return false;
    }
}
