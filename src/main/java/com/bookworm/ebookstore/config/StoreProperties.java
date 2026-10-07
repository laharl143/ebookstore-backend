package com.bookworm.ebookstore.config;

import java.math.BigDecimal;
import java.time.ZoneId;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** Store rules bound from the {@code app.*} properties (spec 0002): currency, VAT, delivery, points, cancel window. */
@ConfigurationProperties(prefix = "app")
public record StoreProperties(
        String currency,
        ZoneId zone,
        BigDecimal vatRate,
        BigDecimal deliveryCharge,
        int deliveryDays,
        Points points,
        int cancelWindowHours) {

    /** Gift points: one point earned per {@code pesosPerPoint} paid, each point worth {@code pesoValue} when redeemed. */
    public record Points(int pesosPerPoint, BigDecimal pesoValue) {
    }
}
