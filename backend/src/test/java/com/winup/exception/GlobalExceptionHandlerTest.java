package com.winup.exception;

import com.winup.exception.ValidationException;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.assertj.core.api.Assertions.assertThat;

class GlobalExceptionHandlerTest {
    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void should_map_validation_exception_to_unprocessable_entity() {
        MockHttpServletRequest request = new MockHttpServletRequest();

        var response = handler.domain(
                new ValidationException("INVALID_STAKE", "Importe fuera de rango"),
                request
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().code()).isEqualTo("INVALID_STAKE");
        assertThat(response.getBody().message()).isEqualTo("Importe fuera de rango");
    }
}
