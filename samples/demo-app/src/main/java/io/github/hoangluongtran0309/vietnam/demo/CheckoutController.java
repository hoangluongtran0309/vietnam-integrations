package io.github.hoangluongtran0309.vietnam.demo;

import io.github.hoangluongtran0309.vietnam.vietqr.QrImageRenderer;
import io.github.hoangluongtran0309.vietnam.vietqr.autoconfigure.VietQrGenerator;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
class CheckoutController {

    private final VietQrGenerator vietQr;
    private final QrImageRenderer renderer;

    CheckoutController(VietQrGenerator vietQr, QrImageRenderer renderer) {
        this.vietQr = vietQr;
        this.renderer = renderer;
    }

    /** GET /qr?amount=150000&orderId=DH12345 → payload string */
    @GetMapping("/qr")
    String payload(@RequestParam long amount, @RequestParam String orderId) {
        return vietQr.forAmount(amount, "Thanh toan " + orderId);
    }

    /** GET /qr.png?amount=150000&orderId=DH12345 → scan with any banking app */
    @GetMapping(value = "/qr.png", produces = MediaType.IMAGE_PNG_VALUE)
    byte[] image(@RequestParam long amount, @RequestParam String orderId) {
        return renderer.renderPng(vietQr.forAmount(amount, "Thanh toan " + orderId), 400);
    }
}
