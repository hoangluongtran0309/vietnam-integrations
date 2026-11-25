package io.github.hoangluongtran0309.vietnam.einvoice.document;

import java.math.BigDecimal;

/**
 * One item read from {@code HDon/DLHDon/NDHDon/DSHHDVu/HHDVu}.
 *
 * <p>Discount values retain their XML scale. TODO(v0.3): verify the meaning of TLCKhau, STCKhau and TTCKTMai — official QD 1233/QD-CT technical appendix.</p>
 *
 * @param nature value at {@code HDon/DLHDon/NDHDon/DSHHDVu/HHDVu/TChat}; may be {@code null} when absent;
 *               TODO(v0.3): verify valid values — official Decision 1233/QD-CT and official XSD schema
 * @param itemCode value at {@code HDon/DLHDon/NDHDon/DSHHDVu/HHDVu/MHHDVu}; may be {@code null} when absent
 * @param name value at {@code HDon/DLHDon/NDHDon/DSHHDVu/HHDVu/THHDVu}; may be {@code null} when absent
 * @param unit value at {@code HDon/DLHDon/NDHDon/DSHHDVu/HHDVu/DVTinh}; may be {@code null} when absent
 * @param quantity value at {@code HDon/DLHDon/NDHDon/DSHHDVu/HHDVu/SLuong}, with its XML scale preserved; may be
 *                 {@code null} when absent
 * @param unitPrice value at {@code HDon/DLHDon/NDHDon/DSHHDVu/HHDVu/DGia}, with its XML scale preserved; may be
 *                  {@code null} when absent
 * @param amount value at {@code HDon/DLHDon/NDHDon/DSHHDVu/HHDVu/ThTien}, with its XML scale preserved; may be
 *               {@code null} when absent
 * @param discountRate value at {@code HDon/DLHDon/NDHDon/DSHHDVu/HHDVu/TLCKhau}; may be {@code null} when absent
 * @param discountAmount value at {@code HDon/DLHDon/NDHDon/DSHHDVu/HHDVu/STCKhau}; may be {@code null} when absent
 * @param taxRate value at {@code HDon/DLHDon/NDHDon/DSHHDVu/HHDVu/TSuat}, preserved as written; may be {@code null}
 *                when absent; TODO(v0.3): verify valid values — official Decision 1233/QD-CT and official XSD schema
 */
public record InvoiceItem(
        Integer nature,
        String itemCode,
        String name,
        String unit,
        BigDecimal quantity,
        BigDecimal unitPrice,
        BigDecimal amount,
        BigDecimal discountRate,
        BigDecimal discountAmount,
        String taxRate) {
}
