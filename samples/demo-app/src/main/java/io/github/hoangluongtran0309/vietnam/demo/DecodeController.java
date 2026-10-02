package io.github.hoangluongtran0309.vietnam.demo;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.BinaryBitmap;
import com.google.zxing.DecodeHintType;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.NotFoundException;
import com.google.zxing.client.j2se.BufferedImageLuminanceSource;
import com.google.zxing.common.HybridBinarizer;
import io.github.hoangluongtran0309.vietnam.vietqr.ServiceCode;
import io.github.hoangluongtran0309.vietnam.vietqr.VietQrBank;
import io.github.hoangluongtran0309.vietnam.vietqr.VietQrDecoder;
import io.github.hoangluongtran0309.vietnam.vietqr.VietQrException;
import io.github.hoangluongtran0309.vietnam.vietqr.VietQrPayload;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;

/**
 * Turns a payload or a screenshot of a QR code into decoded fields, to collect test vectors from real banking apps
 * (docs/vietqr/test-vectors.md). The raw payload is returned even when the library cannot decode it.
 */
@RestController
class DecodeController {

    private final VietQrDecoder decoder;

    DecodeController(VietQrDecoder decoder) {
        this.decoder = decoder;
    }

    /** {@code curl -H 'Content-Type: text/plain' --data '000201...' localhost:8080/decode} */
    @PostMapping(value = "/decode", consumes = MediaType.TEXT_PLAIN_VALUE)
    DecodeResult decode(@RequestBody String payload) {
        return decodePayload(payload.strip());
    }

    /** {@code curl -F file=@screenshot.png localhost:8080/decode-image} */
    @PostMapping(value = "/decode-image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    ResponseEntity<DecodeResult> decodeImage(@RequestParam MultipartFile file) throws IOException {
        BufferedImage image;
        try (InputStream in = file.getInputStream()) {
            image = ImageIO.read(in);
        }
        if (image == null) {
            return ResponseEntity.badRequest().body(DecodeResult.failed(null, "Unsupported image format"));
        }
        try {
            String text = new MultiFormatReader().decode(
                    new BinaryBitmap(new HybridBinarizer(new BufferedImageLuminanceSource(image))),
                    Map.of(DecodeHintType.POSSIBLE_FORMATS, List.of(BarcodeFormat.QR_CODE),
                            DecodeHintType.TRY_HARDER, Boolean.TRUE)).getText();
            return ResponseEntity.ok(decodePayload(text));
        } catch (NotFoundException e) {
            return ResponseEntity.badRequest().body(DecodeResult.failed(null, "No QR code found in image"));
        }
    }

    private DecodeResult decodePayload(String payload) {
        try {
            VietQrPayload decoded = decoder.decode(payload);
            return new DecodeResult(payload, new Fields(
                    decoded.bankBin(),
                    decoded.bank().map(VietQrBank::displayName).orElse(null),
                    decoded.accountNumber(),
                    decoded.serviceCode(),
                    decoded.amount(),
                    decoded.purpose(),
                    decoded.dynamic()), null);
        } catch (VietQrException e) {
            return DecodeResult.failed(payload, e.getMessage());
        }
    }

    /** {@code payload} is the raw QR text; {@code decoded} is null and {@code error} set when decoding failed. */
    record DecodeResult(String payload, Fields decoded, String error) {
        static DecodeResult failed(String payload, String error) {
            return new DecodeResult(payload, null, error);
        }
    }

    record Fields(String bankBin, String bankName, String accountNumber, ServiceCode serviceCode, Long amount,
                  String purpose, boolean dynamic) {
    }
}
