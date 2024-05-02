package com.eclectics.chamapayments.exception;

import com.eclectics.chamapayments.wrappers.response.UniversalResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.support.WebExchangeBindException;
import reactor.core.publisher.Mono;

import javax.validation.constraints.NotNull;
import java.util.Objects;
import java.util.stream.Collectors;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.MediaType.APPLICATION_JSON;

@Slf4j
@ControllerAdvice
public class ErrorHandler {

    @ExceptionHandler(RuntimeException.class)
    public Mono<ResponseEntity<UniversalResponse>> handleApiError(RuntimeException e) {
        log.error("RuntimeException::: {}", e.getMessage(), e);
        return Mono.just(ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(new UniversalResponse("fail", "Api error")));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public Mono<ResponseEntity<UniversalResponse>> handleApiError(IllegalArgumentException illegalArgumentException) {
        return Mono.just(ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(new UniversalResponse("fail", illegalArgumentException.getMessage())));
    }

    @ExceptionHandler(WebExchangeBindException.class)
    Mono<ResponseEntity<UniversalResponse>> invalidRequestErrorHandler(@NotNull final WebExchangeBindException e) {
        log.error("Invalid request exception occurred::: {}", e.getMessage());
        var errors = e.getBindingResult()
                .getAllErrors()
                .stream()
                .filter(Objects::nonNull)
                .map(this::getValidationErrorMessage)
                .collect(Collectors.joining(","));
        return Mono.just(ResponseEntity.status(BAD_REQUEST)
                .contentType(APPLICATION_JSON)
                .body(UniversalResponse.builder()
                        .status("01")
                        .message(errors)
                        .build()));
    }

    @NotNull
    private String getValidationErrorMessage(@NotNull final ObjectError error) {
        final var errorMessage = new StringBuilder();
        if (error instanceof FieldError) {
            FieldError fe = (FieldError) error;
            errorMessage.append(fe.getField()).append(" - ");
        }
        errorMessage.append(error.getDefaultMessage());
        return errorMessage.toString();
    }
}
