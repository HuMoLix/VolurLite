package com.humolix.volur.challenge.object.dto.stage.ctf;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CtfChallengeCategoryUpdateRequestDTO {

    private String name;
    private String description;
    private String iconUrl;

}
