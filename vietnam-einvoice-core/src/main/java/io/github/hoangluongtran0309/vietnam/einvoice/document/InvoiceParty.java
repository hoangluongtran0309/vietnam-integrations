package io.github.hoangluongtran0309.vietnam.einvoice.document;

/**
 * Seller or buyer information read from an issued invoice.
 *
 * @param name value at {@code HDon/DLHDon/NDHDon/NBan/Ten} or {@code HDon/DLHDon/NDHDon/NMua/Ten}; may be
 *             {@code null} when absent
 * @param taxCode value at {@code HDon/DLHDon/NDHDon/NBan/MST} or {@code HDon/DLHDon/NDHDon/NMua/MST}; may be
 *                {@code null} when absent
 * @param address value at {@code HDon/DLHDon/NDHDon/NBan/DChi} or {@code HDon/DLHDon/NDHDon/NMua/DChi}; may be
 *                {@code null} when absent
 * @param buyerFullName value at {@code HDon/DLHDon/NDHDon/NMua/HVTNMHang}; may be {@code null} when absent or for
 *                      a seller
 */
public record InvoiceParty(String name, String taxCode, String address, String buyerFullName) {

    @Override
    public String toString() {
        return "InvoiceParty[name=" + redact(name)
                + ", taxCode=" + redact(taxCode)
                + ", address=" + redact(address)
                + ", buyerFullName=" + redact(buyerFullName) + "]";
    }

    private static String redact(String value) {
        return "<redacted:" + (value == null ? 0 : value.length()) + ">";
    }
}
