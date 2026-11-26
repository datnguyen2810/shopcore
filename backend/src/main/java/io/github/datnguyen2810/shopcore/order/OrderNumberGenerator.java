package io.github.datnguyen2810.shopcore.order;

import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

// tạo mã đơn dễ đọc khi checkout tạo đơn mới (vd: ORD-20261120-000123) -> dùng mã này để tra cứu
@Component
public class OrderNumberGenerator {

    private static final String ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
    private static final int SUFFIX_LENGTH = 6;

    private final SecureRandom random = new SecureRandom();

    public String generate(Instant now) {
        String date = DateTimeFormatter.BASIC_ISO_DATE.format(now.atOffset(ZoneOffset.UTC).toLocalDate());
        StringBuilder suffix = new StringBuilder(SUFFIX_LENGTH);
        for (int i = 0; i < SUFFIX_LENGTH; i++) {
            suffix.append(ALPHABET.charAt(random.nextInt(ALPHABET.length())));
        }
        return "SC-" + date + "-" + suffix;
    }
}
