package io.github.hoangluongtran0309.vietnam.vietqr;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageConfig;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;

import javax.imageio.ImageIO;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Map;
import java.util.Objects;

/**
 * Renders a payload to a PNG image. Requires the optional {@code com.google.zxing:javase} dependency.
 */
public final class QrImageRenderer {

    /** Black on white with {@link QrImageOptions#DEFAULT_MARGIN} and error correction M. */
    public byte[] renderPng(String payload, int sizePx) {
        return renderPng(payload, QrImageOptions.ofSize(sizePx));
    }

    public byte[] renderPng(String payload, QrImageOptions options) {
        Objects.requireNonNull(payload, "payload");
        Objects.requireNonNull(options, "options");
        try {
            BitMatrix matrix = new QRCodeWriter().encode(payload, BarcodeFormat.QR_CODE, options.size(), options.size(),
                    Map.of(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.valueOf(options.errorCorrection().name()),
                            EncodeHintType.MARGIN, options.margin()));
            BufferedImage image = MatrixToImageWriter.toBufferedImage(matrix,
                    new MatrixToImageConfig(options.foreground().getRGB(), options.background().getRGB()));
            if (options.logo() != null) {
                image = withLogo(image, options);
            }
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            if (!ImageIO.write(image, "PNG", out)) {
                throw new VietQrException("No PNG writer available");
            }
            return out.toByteArray();
        } catch (WriterException | IOException e) {
            throw new VietQrException("Failed to render QR image", e);
        }
    }

    /**
     * Clears a square of {@code logoScale} times the image width in the background color and fits the logo inside it.
     * The modules hidden under that square are recovered through error correction.
     */
    private static BufferedImage withLogo(BufferedImage code, QrImageOptions options) {
        int width = code.getWidth();
        int height = code.getHeight();
        // The code alone may be a 1-bit image; draw onto a full-color canvas so the logo keeps its colors.
        BufferedImage canvas = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = canvas.createGraphics();
        try {
            g.drawImage(code, 0, 0, null);
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

            int area = (int) Math.round(Math.min(width, height) * options.logoScale());
            int padding = Math.max(1, area / 10);
            BufferedImage logo = options.logo();
            double fit = Math.min((double) (area - 2 * padding) / logo.getWidth(),
                    (double) (area - 2 * padding) / logo.getHeight());
            int logoWidth = Math.max(1, (int) Math.round(logo.getWidth() * fit));
            int logoHeight = Math.max(1, (int) Math.round(logo.getHeight() * fit));
            int x = (width - logoWidth) / 2;
            int y = (height - logoHeight) / 2;

            g.setColor(options.background());
            g.fillRect(x - padding, y - padding, logoWidth + 2 * padding, logoHeight + 2 * padding);
            g.drawImage(logo, x, y, logoWidth, logoHeight, null);
        } finally {
            g.dispose();
        }
        return canvas;
    }
}
