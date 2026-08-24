package com.humolix.volur.scoreboard.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserRankResponseVO implements Serializable {

    private Long userId;
    private String username;
    private String avatarUrl;
    private String org;
    private Double totalPoints;
    private Long solves;

}
