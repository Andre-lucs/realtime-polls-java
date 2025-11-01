package com.andrelucs.realtimepolls.exceptions.service;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class InvalidPollOpinionsException extends InvalidPollCreationException {
    private final int expecteOpinionsCount;
    private final int actualOpinionsCount;

    public InvalidPollOpinionsException(int expecteOpinionsCount, int actualOpinionsCount) {
        super("Poll have only %d options when the minimum required is %d options.".formatted(expecteOpinionsCount, actualOpinionsCount));
        this.expecteOpinionsCount = expecteOpinionsCount;
        this.actualOpinionsCount = actualOpinionsCount;
    }

}
