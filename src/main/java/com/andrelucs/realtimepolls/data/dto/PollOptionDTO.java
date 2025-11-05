package com.andrelucs.realtimepolls.data.dto;

import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

import java.util.Objects;

@NoArgsConstructor
@AllArgsConstructor
public final class PollOptionDTO {
    private Long id;
    private Long pollId;
    private String description;
    private int votes;

    public Long id() {
        return id;
    }

    public Long pollId() {
        return pollId;
    }

    public String description() {
        return description;
    }

    public int votes() {
        return votes;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;
        var that = (PollOptionDTO) obj;
        return Objects.equals(this.id, that.id) &&
                Objects.equals(this.pollId, that.pollId) &&
                Objects.equals(this.description, that.description) &&
                this.votes == that.votes;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, pollId, description, votes);
    }

    @Override
    public String toString() {
        return "PollOptionDTO[" +
                "id=" + id + ", " +
                "pollId=" + pollId + ", " +
                "description=" + description + ", " +
                "votes=" + votes + ']';
    }

}
