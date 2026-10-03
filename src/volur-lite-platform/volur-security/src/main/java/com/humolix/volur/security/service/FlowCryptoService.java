package com.humolix.volur.security.service;

public interface FlowCryptoService {

    String decrypt(String envelope, String flowName, String flowEntry, String dsh);

    String encrypt(String plainText, String flowName, String flowEntry, String dsh);

}
