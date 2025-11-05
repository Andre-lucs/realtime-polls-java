package com.andrelucs.realtimepolls.exceptions;

import com.andrelucs.realtimepolls.data.dto.ExceptionDTO;
import com.andrelucs.realtimepolls.exceptions.controller.BadRequestException;
import com.andrelucs.realtimepolls.exceptions.controller.FailPollOptionsUpdateException;
import com.andrelucs.realtimepolls.exceptions.controller.InvalidPollException;
import com.andrelucs.realtimepolls.exceptions.controller.PollNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ExceptionDTO> handleInvalidArgumetException(MethodArgumentNotValidException e,  HttpServletRequest request){
        StringBuilder message = new StringBuilder();
        for (var err : e.getBindingResult().getAllErrors()){
            message.append(" ");
            message.append(err.getDefaultMessage());
        }
        return defaultResponseGenerator(HttpStatus.BAD_REQUEST, message.toString(), request);
    }

    @ExceptionHandler(PollNotFoundException.class)
    public ResponseEntity<ExceptionDTO> handlePollNotFoundException(PollNotFoundException e, HttpServletRequest request) {
        return defaultResponseGenerator(HttpStatus.NOT_FOUND, e.getMessage(), request);
    }

    @ExceptionHandler(InvalidPollException.class)
    public ResponseEntity<ExceptionDTO> handleInvalidPollException( InvalidPollException e, HttpServletRequest request) {
        return defaultResponseGenerator(HttpStatus.FORBIDDEN, e.getMessage(), request);
    }

    @ExceptionHandler(FailPollOptionsUpdateException.class)
    public ResponseEntity<ExceptionDTO> handleFailPollOptionsUpdateException(FailPollOptionsUpdateException e, HttpServletRequest request) {
        return defaultResponseGenerator(HttpStatus.FORBIDDEN, e.getMessage(), request);
    }

    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<ExceptionDTO> handleBadRequestException(BadRequestException e, HttpServletRequest request) {
        return defaultResponseGenerator(HttpStatus.BAD_REQUEST, e.getMessage(), request);
    }

    private ResponseEntity<ExceptionDTO> defaultResponseGenerator(HttpStatus status, String message, HttpServletRequest request){
        return new ResponseEntity<>(
                new ExceptionDTO(
                        message,
                        status.value(),
                        request.getRequestURI(),
                        LocalDateTime.now())
                , status);
    }

}
