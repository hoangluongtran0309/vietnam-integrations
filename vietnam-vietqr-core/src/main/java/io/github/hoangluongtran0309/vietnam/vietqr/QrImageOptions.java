package io.github.hoangluongtran0309.vietnam.vietqr;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Objects;

/**
 * How {@link QrImageRenderer} draws a QR code.
 *
 * @param size            width and height of the image in pixels, between {@value #MIN_SIZE} and {@value #MAX_SIZE}
 * @param margin          quiet zone around the code, in modules
 * @param errorCorrection error correction level; {@link QrErrorCorrection#Q} or higher when a logo is set
 * @param foreground      color of the dark modules; must be darker than {@code background}
 * @param background      color of the light modules and the quiet zone
 * @param logo            image drawn over the center of the code, or {@code null} for none; copied on the way in
 *                        and out, so later changes to either image do not affect these options
 * @param logoScale       share of the image width the logo area may cover, up to {@value #MAX_LOGO_SCALE}
 */
public record QrImageOptions(
        int size,
        int margin,
        QrErrorCorrection errorCorrection,
        Color foreground,
        Color background,
        BufferedImage logo,
        double logoScale) {

    public static final int MIN_SIZE = 64;
    public static final int MAX_SIZE = 4096;
    public static final int DEFAULT_SIZE = 400;
    public static final int DEFAULT_MARGIN = 2;
    public static final double DEFAULT_LOGO_SCALE = 0.2;
    public static final double MAX_LOGO_SCALE = 0.25;

    public QrImageOptions {
        Objects.requireNonNull(errorCorrection, "errorCorrection");
        Objects.requireNonNull(foreground, "foreground");
        Objects.requireNonNull(background, "background");
        if (size < MIN_SIZE || size > MAX_SIZE) {
            throw new VietQrException("size must be between " + MIN_SIZE + " and " + MAX_SIZE + " pixels: " + size);
        }
        if (margin < 0) {
            throw new VietQrException("margin must not be negative: " + margin);
        }
        if (!(logoScale > 0 && logoScale <= MAX_LOGO_SCALE)) {
            throw new VietQrException("logoScale must be greater than 0 and at most " + MAX_LOGO_SCALE + ": " + logoScale);
        }
        if (luminance(foreground) >= luminance(background)) {
            throw new VietQrException("foreground must be darker than background; many scanners cannot read inverted codes");
        }
        if (logo != null && errorCorrection.compareTo(QrErrorCorrection.Q) < 0) {
            throw new VietQrException("a logo requires errorCorrection Q or H, but was " + errorCorrection);
        }
        logo = logo == null ? null : copy(logo);
    }

    /** A copy of the logo, or {@code null}. */
    @Override
    public BufferedImage logo() {
        return logo == null ? null : copy(logo);
    }

    /** Black on white, no logo, the given size. */
    public static QrImageOptions ofSize(int size) {
        return builder().size(size).build();
    }

    public static Builder builder() {
        return new Builder();
    }

    private static BufferedImage copy(BufferedImage image) {
        BufferedImage copy = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = copy.createGraphics();
        try {
            g.drawImage(image, 0, 0, null);
        } finally {
            g.dispose();
        }
        return copy;
    }

    private static int luminance(Color color) {
        return color.getRed() * 299 + color.getGreen() * 587 + color.getBlue() * 114;
    }

    public static final class Builder {
        private int size = DEFAULT_SIZE;
        private int margin = DEFAULT_MARGIN;
        private QrErrorCorrection errorCorrection;
        private Color foreground = Color.BLACK;
        private Color background = Color.WHITE;
        private BufferedImage logo;
        private double logoScale = DEFAULT_LOGO_SCALE;

        public Builder size(int size) {
            this.size = size;
            return this;
        }

        public Builder margin(int margin) {
            this.margin = margin;
            return this;
        }

        /** Defaults to {@link QrErrorCorrection#M}, or {@link QrErrorCorrection#H} when a logo is set. */
        public Builder errorCorrection(QrErrorCorrection errorCorrection) {
            this.errorCorrection = errorCorrection;
            return this;
        }

        public Builder foreground(Color foreground) {
            this.foreground = foreground;
            return this;
        }

        public Builder background(Color background) {
            this.background = background;
            return this;
        }

        public Builder logo(BufferedImage logo) {
            this.logo = logo;
            return this;
        }

        /** Reads the logo from encoded image bytes in any format {@link ImageIO} supports (PNG, JPEG, GIF, BMP). */
        public Builder logo(byte[] image) {
            Objects.requireNonNull(image, "image");
            try {
                BufferedImage decoded = ImageIO.read(new ByteArrayInputStream(image));
                if (decoded == null) {
                    throw new VietQrException("Unsupported logo image format");
                }
                this.logo = decoded;
                return this;
            } catch (IOException e) {
                throw new VietQrException("Failed to read logo image", e);
            }
        }

        public Builder logoScale(double logoScale) {
            this.logoScale = logoScale;
            return this;
        }

        public QrImageOptions build() {
            QrErrorCorrection level = errorCorrection != null ? errorCorrection
                    : logo != null ? QrErrorCorrection.H : QrErrorCorrection.M;
            return new QrImageOptions(size, margin, level, foreground, background, logo, logoScale);
        }
    }
}
