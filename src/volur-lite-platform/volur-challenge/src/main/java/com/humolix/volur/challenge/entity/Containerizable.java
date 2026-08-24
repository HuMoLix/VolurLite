package com.humolix.volur.challenge.entity;

public interface Containerizable extends Challenge {

    String getImageName();

    Integer getContainerPort();

    boolean isDynamicContainer();

}
