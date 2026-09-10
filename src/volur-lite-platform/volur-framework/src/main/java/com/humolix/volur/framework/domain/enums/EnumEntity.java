package com.humolix.volur.framework.domain.enums;

public interface EnumEntity {

    int getCode();

    String getDescription();

    default RuntimeException customException(int code) {
        return new IllegalArgumentException("未知的枚举 code: " + code + " [来自 " + this.getClass().getSimpleName() + "]");
    }

    static <E extends Enum<E> & EnumEntity> E fromCode(Class<E> enumClass, int code) {
        for (E enumConstant : enumClass.getEnumConstants()) {
            if (enumConstant.getCode() == code) {
                return enumConstant;
            }
        }

        E[] constants = enumClass.getEnumConstants();
        if (constants != null && constants.length > 0) {
            throw constants[0].customException(code);
        }
        throw new IllegalArgumentException("未知的枚举 code: " + code);
    }

}
