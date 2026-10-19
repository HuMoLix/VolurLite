package com.humolix.volur.challenge.domain.dto.ctf;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CtfAttachmentDTO implements Serializable {

    private String name;
    private String url;
    private Long size;

}
