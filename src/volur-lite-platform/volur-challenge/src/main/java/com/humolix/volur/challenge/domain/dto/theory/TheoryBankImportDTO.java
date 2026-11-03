package com.humolix.volur.challenge.domain.dto.theory;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.humolix.volur.framework.common.annotation.Trimmed;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class TheoryBankImportDTO {

    @Trimmed
    @JsonAlias({"bankName", "title"})
    private String name;

    @Trimmed
    private String description;

    @Trimmed
    private String category;

    @JsonAlias({"challenges", "items"})
    private List<TheoryChallengeImportDTO> questions;

}
