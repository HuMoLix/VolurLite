package com.humolix.volur.notification.domain.dto;

import com.humolix.volur.notification.domain.enums.BroadcastDisplayType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AdminBroadcastPageRequestDTO {

    private Integer page;
    private Integer size;
    private String keyword;
    private BroadcastDisplayType displayType;
    private String category;
    private String sortBy;
    private String sortOrder;

}
