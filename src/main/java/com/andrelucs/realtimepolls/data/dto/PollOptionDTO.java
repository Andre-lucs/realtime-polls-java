package com.andrelucs.realtimepolls.data.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public final class PollOptionDTO {
    private Long id;
    private String description;
    private int votes;
}
