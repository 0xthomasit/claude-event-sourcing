package com.example.payment.domain.model;

import lombok.Value;

/**
 * Value Object — payment method details.
 * maskedAccount: last 4 digits for CARD, masked phone for MOMO.
 */
@Value
public class PaymentMethod {

    PaymentMethodType type;
    String maskedAccount;

    public static PaymentMethod of(PaymentMethodType type, String maskedAccount) {
        if (type == null) throw new IllegalArgumentException("PaymentMethodType must not be null");
        return new PaymentMethod(type, maskedAccount);
    }

    public static PaymentMethod card(String maskedAccount) {
        return new PaymentMethod(PaymentMethodType.CARD, maskedAccount);
    }

    public static PaymentMethod momo(String maskedPhone) {
        return new PaymentMethod(PaymentMethodType.MOMO, maskedPhone);
    }

    public static PaymentMethod banking(String maskedAccount) {
        return new PaymentMethod(PaymentMethodType.BANKING, maskedAccount);
    }
}
