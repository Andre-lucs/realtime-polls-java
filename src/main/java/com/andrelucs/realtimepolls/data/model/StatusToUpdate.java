package com.andrelucs.realtimepolls.data.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "status_to_update")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StatusToUpdate{
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @NotNull
    @Enumerated(EnumType.STRING)
    private PollStatus currentStatus;
    @NotNull
    @Enumerated(EnumType.STRING)
    private PollStatus nextStatus;
    private LocalDateTime scheduledDate;

    private LocalDateTime processedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "poll_id", nullable = false)
    @EqualsAndHashCode.Exclude
    @ToString.Exclude
    private Poll poll;
}