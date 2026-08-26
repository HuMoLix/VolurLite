package com.humolix.volur.competition.domain.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum InvitationStatus {

    PENDING("待确认"),
    ACCEPTED("已接受"),
    REJECTED("已拒绝"),
    EXPIRED("已过期"),
    CANCELLED("已撤销");

    private final String description;

}
