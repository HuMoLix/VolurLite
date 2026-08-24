package com.humolix.volur.challenge.object.vo.pub;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HintVO implements Serializable {

    private String content;
    private Instant createdAt;
    private Instant updatedAt;

}
