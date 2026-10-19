package com.humolix.volur.system.service.admin;

public interface AsyncTaskContext {

    Long getTaskId();

    void updateProgress(int progress, String statusMessage);

    boolean isCancelled();

}
