package com.humolix.volur.challenge.domain.entity;

public interface Containerizable {

    String getImageName();

    Integer getContainerPort();

    default boolean isDynamicContainer() {
        return getImageName() != null && !getImageName().isBlank();
    }

}
