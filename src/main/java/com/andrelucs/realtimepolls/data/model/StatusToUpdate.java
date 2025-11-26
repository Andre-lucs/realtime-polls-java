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

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "poll_id", nullable = false)
    @EqualsAndHashCode.Exclude
    @ToString.Exclude
    private Poll poll;
}
//CREATE TABLE status_to_update (
//  id BIGSERIAL PRIMARY KEY,
//  poll_id BIGINT NOT NULL REFERENCES poll (id) ON DELETE CASCADE,
//  current_status VARCHAR(20) NOT NULL,
//  next_status VARCHAR(20) NOT NULL,
//  scheduled_date TIMESTAMP NOT NULL
//);