package io.github.hoangluongtran0309.vietnam.vietqr;

/**
 * Builds a VietQR payload string following the NAPAS EMVCo Merchant-Presented Mode profile.
 */
public final class VietQrEncoder {

    static final String NAPAS_GUID = "A000000727";
    static final String CURRENCY_VND = "704";
    static final String COUNTRY_VN = "VN";

    public String encode(VietQrRequest request) {
        String beneficiary = Tlv.field("00", request.bankBin())
                + Tlv.field("01", request.accountNumber());

        String merchantAccount = Tlv.field("00", NAPAS_GUID)
                + Tlv.field("01", beneficiary)
                + Tlv.field("02", request.serviceCode().code());

        StringBuilder sb = new StringBuilder()
                .append(Tlv.field("00", "01"))
                .append(Tlv.field("01", request.isDynamic() ? "12" : "11"))
                .append(Tlv.field("38", merchantAccount))
                .append(Tlv.field("53", CURRENCY_VND));

        if (request.isDynamic()) {
            sb.append(Tlv.field("54", Long.toString(request.amount())));
        }
        sb.append(Tlv.field("58", COUNTRY_VN));

        String purpose = PurposeNormalizer.normalize(request.purpose());
        if (purpose != null && !purpose.isEmpty()) {
            sb.append(Tlv.field("62", Tlv.field("08", purpose)));
        }

        sb.append("6304");
        return sb.append(Crc16Ccitt.computeHex(sb.toString())).toString();
    }
}
