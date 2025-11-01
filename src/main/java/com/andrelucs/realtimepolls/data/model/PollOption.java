package com.andrelucs.realtimepolls.data.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Entity
@Table(name = "poll_option")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PollOption {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "The option should have a description text.")
    @Column(nullable = false)
    private String description;

    @Column(nullable = false)
    private Integer votes = 0;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "poll_id", nullable = false)
    @EqualsAndHashCode.Exclude
    @ToString.Exclude
    private Poll poll;


}
