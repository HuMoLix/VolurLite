package com.humolix.volur.verification.domain.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminEmailStatisticsVO {

    private long totalEmails;

    private long successEmails;

    private long failedEmails;

    private long todayEmails;

}
