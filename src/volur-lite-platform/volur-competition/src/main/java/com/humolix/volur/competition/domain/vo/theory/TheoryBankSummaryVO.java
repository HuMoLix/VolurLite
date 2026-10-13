package com.humolix.volur.competition.domain.vo.theory;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TheoryBankSummaryVO implements Serializable {

    private Long id;
    private String name;
    @Builder.Default
    private Long questionCount = 0L;

}
