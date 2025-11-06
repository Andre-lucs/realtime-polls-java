package com.andrelucs.realtimepolls.data.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "poll")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Poll {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "The poll should have a question.")
    @Column(nullable = false)
    private String question;

    @NotNull(message = "The start date is mandatory for the poll.")
    @Column(name = "start_date", nullable = false)
    private LocalDateTime startDate;

    @NotNull(message = "The end date is mandatory for the poll.")
    @Column(name = "end_date", nullable = false)
    private LocalDateTime endDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PollStatus status;

    // Lombok exclude to avoid unecessary fetchs
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @Size(min = 3, message = "Minimum options count is 3")
    @OneToMany(mappedBy = "poll", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<PollOption> options;

    public void addOption(PollOption option){
        if (options == null) options = new ArrayList<>();
        option.setPoll(this);
        this.options.add(option);
    }

    @PrePersist
    @PreUpdate
    public void updateStatusBasedOnDates() {
        LocalDateTime now = LocalDateTime.now();

        if (now.isBefore(startDate)) {
            this.status = PollStatus.NOT_STARTED;
        } else if (now.isAfter(endDate)) {
            this.status = PollStatus.FINISHED;
        } else {
            this.status = PollStatus.STARTED;
        }
    }

    public String toString(boolean includeOpinios){
        return toString().concat(options.toString());
    }

}
