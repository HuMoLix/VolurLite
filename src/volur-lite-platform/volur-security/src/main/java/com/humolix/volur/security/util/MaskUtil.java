package com.humolix.volur.security.util;

public final class MaskUtil {

    private static final String DEFAULT_MASK = "***";

    private MaskUtil() {
    }

    public static String identifierCardMask(String identifierCard) {
        if (identifierCard == null) {
            return null;
        }
        String trimmed = identifierCard.trim();
        if (trimmed.isEmpty()) {
            return "";
        }
        int len = trimmed.length();
        if (len == 18) {
            return trimmed.substring(0, 6) + "********" + trimmed.substring(14);
        }
        if (len == 15) {
            return trimmed.substring(0, 6) + "******" + trimmed.substring(12);
        }
        if (len >= 8) {
            return trimmed.substring(0, 4) + "****" + trimmed.substring(len - 4);
        }
        return DEFAULT_MASK;
    }

    public static String IdentifierCardMask(String identifierCard) {
        return identifierCardMask(identifierCard);
    }

    public static String phoneMask(String phone) {
        if (phone == null) {
            return null;
        }
        String trimmed = phone.trim();
        if (trimmed.isEmpty()) {
            return "";
        }
        int len = trimmed.length();
        if (len == 11) {
            return trimmed.substring(0, 3) + "****" + trimmed.substring(7);
        }
        if (len > 7) {
            return trimmed.substring(0, 3) + "****" + trimmed.substring(len - 4);
        }
        return DEFAULT_MASK;
    }

    public static String PhoneMask(String phone) {
        return phoneMask(phone);
    }

    public static String emailMask(String email) {
        if (email == null) {
            return null;
        }
        String trimmed = email.trim();
        if (trimmed.isEmpty()) {
            return "";
        }
        int atIndex = trimmed.indexOf('@');
        if (atIndex <= 0) {
            return DEFAULT_MASK;
        }
        String prefix = trimmed.substring(0, atIndex);
        String domain = trimmed.substring(atIndex);
        if (prefix.length() <= 2) {
            return prefix.charAt(0) + "***" + domain;
        }
        return prefix.charAt(0) + "***" + prefix.charAt(prefix.length() - 1) + domain;
    }

    public static String EmailMask(String email) {
        return emailMask(email);
    }

    public static String nameMask(String name) {
        if (name == null) {
            return null;
        }
        String trimmed = name.trim();
        if (trimmed.isEmpty()) {
            return "";
        }
        int len = trimmed.length();
        if (len == 1) {
            return "*";
        }
        if (len == 2) {
            return trimmed.charAt(0) + "*";
        }
        if (len == 3) {
            return trimmed.charAt(0) + "*" + trimmed.charAt(2);
        }
        return trimmed.charAt(0) + "*".repeat(len - 2) + trimmed.charAt(len - 1);
    }

    public static String NameMask(String name) {
        return nameMask(name);
    }

    public static String bankCardMask(String bankCard) {
        if (bankCard == null) {
            return null;
        }
        String trimmed = bankCard.replaceAll("\\s+", "");
        if (trimmed.isEmpty()) {
            return "";
        }
        int len = trimmed.length();
        if (len < 10) {
            return DEFAULT_MASK;
        }
        return trimmed.substring(0, 6) + "*".repeat(len - 10) + trimmed.substring(len - 4);
    }

    public static String BankCardMask(String bankCard) {
        return bankCardMask(bankCard);
    }

    public static String keyMask(String key) {
        if (key == null) {
            return null;
        }
        String trimmed = key.trim();
        if (trimmed.isEmpty()) {
            return "";
        }
        if (trimmed.length() <= 8) {
            return "********";
        }
        return trimmed.substring(0, 4) + "*".repeat(trimmed.length() - 8) + trimmed.substring(trimmed.length() - 4);
    }

    public static String KeyMask(String key) {
        return keyMask(key);
    }

    public static String genericMask(String str, int prefixKeep, int suffixKeep, char maskChar) {
        if (str == null) {
            return null;
        }
        int len = str.length();
        if (prefixKeep < 0 || suffixKeep < 0 || prefixKeep + suffixKeep >= len) {
            return str;
        }
        return str.substring(0, prefixKeep)
                + String.valueOf(maskChar).repeat(len - prefixKeep - suffixKeep)
                + str.substring(len - suffixKeep);
    }

    public static String GenericMask(String str, int prefixKeep, int suffixKeep, char maskChar) {
        return genericMask(str, prefixKeep, suffixKeep, maskChar);
    }

}
