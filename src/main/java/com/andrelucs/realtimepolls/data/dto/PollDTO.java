package com.andrelucs.realtimepolls.data.dto;

import com.andrelucs.realtimepolls.data.model.PollStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public final class PollDTO {
    private Long id;
    private String question;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private PollStatus status;
    private List<PollOptionDTO> options;
}
