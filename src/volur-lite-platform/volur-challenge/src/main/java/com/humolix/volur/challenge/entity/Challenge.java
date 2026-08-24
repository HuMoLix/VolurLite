package com.humolix.volur.challenge.entity;

public interface Challenge {

    Long getId();

    String getName();

    default String getDescription() { return null; }

    default Integer getDifficulty() { return null; }

    default String getAttachmentUrl() { return null; }

    default boolean isDynamicContainer() { return false; }

    default Boolean getIsDynamicContainer() { return isDynamicContainer(); }

    default String getImageName() { return null; }

    default Integer getContainerPort() { return null; }

    default String getCategoryName() { return null; }

    default String getFlag() { return null; }

    default String getAuthorName() { return null; }

}
