package io.github.datnguyen2810.shopcore.order;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

import java.time.Duration;

@ConfigurationProperties("app.order")
public record OrderProperties(@DefaultValue("15m") Duration paymentTimeout) {

    public OrderProperties {
        if (paymentTimeout.isNegative() || paymentTimeout.isZero()) {
            throw new IllegalArgumentException("Order payment timeout must be positive");
        }
    }
}
