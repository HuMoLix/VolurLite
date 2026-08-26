package com.humolix.volur.container.flag.resolver;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.RandomUtil;
import com.humolix.volur.container.flag.DynamicFlagContext;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.Set;

@Component
@Order(20)
public class RandomPlaceholderResolver implements FlagPlaceholderResolver {

    private static final Set<String> STATIC_KEYS = Set.of(
            "UUID",
            "UUID_UPPER",
            "UUID_SIMPLE",
            "SIMPLE_UUID",
            "UUID_SIMPLE_UPPER",
            "RANDOM_STR",
            "RANDOM_8",
            "RANDOM_16",
            "RANDOM_32",
            "RANDOM_64",
            "RANDOM_NUM_6",
            "TIMESTAMP",
            "TIMESTAMP_SEC",
            "DATE",
            "YYYYMMDD"
    );

    @Override
    public boolean supports(String placeholder) {
        if (placeholder == null) {
            return false;
        }
        String upper = placeholder.toUpperCase();
        return STATIC_KEYS.contains(upper)
                || upper.startsWith("RANDOM:")
                || upper.startsWith("RANDOM_")
                || upper.startsWith("RANDOM_NUM:")
                || upper.startsWith("HEX:")
                || upper.startsWith("HEX_");
    }

    @Override
    public String resolve(String placeholder, DynamicFlagContext context) {
        String upper = placeholder.toUpperCase();

        return switch (upper) {
            case "UUID" -> IdUtil.randomUUID();
            case "UUID_UPPER" -> IdUtil.randomUUID().toUpperCase();
            case "UUID_SIMPLE", "SIMPLE_UUID" -> IdUtil.fastSimpleUUID();
            case "UUID_SIMPLE_UPPER" -> IdUtil.fastSimpleUUID().toUpperCase();
            case "RANDOM_STR", "RANDOM_16" -> RandomUtil.randomString(16);
            case "RANDOM_8" -> RandomUtil.randomString(8);
            case "RANDOM_32" -> RandomUtil.randomString(32);
            case "RANDOM_64" -> RandomUtil.randomString(64);
            case "RANDOM_NUM_6" -> RandomUtil.randomNumbers(6);
            case "TIMESTAMP" -> String.valueOf(System.currentTimeMillis());
            case "TIMESTAMP_SEC" -> String.valueOf(System.currentTimeMillis() / 1000);
            case "DATE", "YYYYMMDD" -> DateUtil.format(new Date(), "yyyyMMdd");
            default -> resolveDynamic(upper);
        };
    }

    private String resolveDynamic(String upper) {
        if (upper.startsWith("RANDOM:") || upper.startsWith("RANDOM_")) {
            String lenStr = upper.substring(7);
            try {
                int len = Integer.parseInt(lenStr);
                return RandomUtil.randomString(Math.clamp(len, 1, 128));
            } catch (NumberFormatException ignored) {}
        }

        if (upper.startsWith("RANDOM_NUM:")) {
            String lenStr = upper.substring(11);
            try {
                int len = Integer.parseInt(lenStr);
                return RandomUtil.randomNumbers(Math.clamp(len, 1, 64));
            } catch (NumberFormatException ignored) {}
        }

        if (upper.startsWith("HEX:") || upper.startsWith("HEX_")) {
            String lenStr = upper.substring(4);
            try {
                int len = Integer.parseInt(lenStr);
                return RandomUtil.randomString("0123456789abcdef", Math.clamp(len, 1, 128));
            } catch (NumberFormatException ignored) {}
        }

        return RandomUtil.randomString(16);
    }

}
