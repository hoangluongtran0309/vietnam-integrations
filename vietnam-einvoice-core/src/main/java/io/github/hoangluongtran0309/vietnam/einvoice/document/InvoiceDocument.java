package io.github.hoangluongtran0309.vietnam.einvoice.document;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Data read from an issued e-invoice XML document without business validation or recalculation.
 *
 * @param header content at {@code HDon/DLHDon/TTChung}; may be {@code null} when absent
 * @param seller content at {@code HDon/DLHDon/NDHDon/NBan}; may be {@code null} when absent
 * @param buyer content at {@code HDon/DLHDon/NDHDon/NMua}; may be {@code null} when absent
 * @param items entries at {@code HDon/DLHDon/NDHDon/DSHHDVu/HHDVu}; never {@code null}
 * @param taxRateSummaries entries at {@code HDon/DLHDon/NDHDon/TToan/THTTLTSuat/LTSuat}; never {@code null}
 * @param totals content at {@code HDon/DLHDon/NDHDon/TToan}; may be {@code null} when absent
 * @param taxAuthorityCode value at {@code HDon/MCCQT}; may be {@code null} when absent
 * @param signatures metadata from {@code HDon/DSCKS/NBan/ds:Signature},
 *                   {@code HDon/DSCKS/NMua/ds:Signature}, or {@code HDon/DSCKS/CQT/ds:Signature}; never
 *                   {@code null}
 * @param extensions original text of other XML elements, keyed by their full XML path starting at {@code HDon}.
 *                   Each path step for an element with same-name siblings includes a 1-based XPath-style index,
 *                   as in {@code HDon/DLHDon/NDHDon/DSHHDVu/HHDVu[2]/STCKhau}. A step with no same-name siblings
 *                   has no index. The map is never {@code null} and rejects {@code null} keys and values.
 */
public record InvoiceDocument(
        InvoiceHeader header,
        InvoiceParty seller,
        InvoiceParty buyer,
        List<InvoiceItem> items,
        List<TaxRateSummary> taxRateSummaries,
        InvoiceTotals totals,
        String taxAuthorityCode,
        List<InvoiceSignature> signatures,
        Map<String, String> extensions) {

    public InvoiceDocument {
        items = immutableCopy(items, "items");
        taxRateSummaries = immutableCopy(taxRateSummaries, "taxRateSummaries");
        signatures = immutableCopy(signatures, "signatures");
        extensions = extensions == null ? Map.of() : extensions;
        extensions.forEach((key, value) -> {
            Objects.requireNonNull(key, "extensions");
            Objects.requireNonNull(value, "extensions");
        });
        extensions = Map.copyOf(extensions);
    }

    @Override
    public String toString() {
        return "InvoiceDocument[header=" + header
                + ", seller=" + seller
                + ", buyer=" + buyer
                + ", items=" + items
                + ", taxRateSummaries=" + taxRateSummaries
                + ", totals=" + totals
                + ", taxAuthorityCode=" + taxAuthorityCode
                + ", signatures=" + signatures
                + ", extensions=" + extensions.keySet() + "]";
    }

    private static <T> List<T> immutableCopy(List<T> values, String parameterName) {
        List<T> source = values == null ? List.of() : values;
        source.forEach(value -> Objects.requireNonNull(value, parameterName));
        return List.copyOf(source);
    }
}
