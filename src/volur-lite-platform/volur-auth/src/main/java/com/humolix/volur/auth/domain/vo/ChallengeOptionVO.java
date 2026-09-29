package com.humolix.volur.auth.domain.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChallengeOptionVO implements Serializable {

    private String type;
    private String title;
    private String subtitle;
    private String icon;
    private String path;

}
