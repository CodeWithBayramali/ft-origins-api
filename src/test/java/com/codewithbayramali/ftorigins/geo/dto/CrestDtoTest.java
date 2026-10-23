package com.codewithbayramali.ftorigins.geo.dto;

import com.codewithbayramali.ftorigins.auth.dto.RegisterRequest;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class CrestDtoTest {

    private static ValidatorFactory factory;
    private static Validator validator;

    @BeforeAll
    static void setUp() {
        factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @AfterAll
    static void tearDown() {
        factory.close();
    }

    @Test
    void theLastIdOfEachCatalogPassesAndOnePastItDoesNot() {
        // The client catalogs: 12 shapes, 64 symbols, 16 colours.
        assertThat(violations(new CrestDto(11, 63, 15, 0))).isEmpty();
        assertThat(violations(new CrestDto(12, 0, 1, 0))).containsExactly("shape");
        assertThat(violations(new CrestDto(0, 64, 1, 0))).containsExactly("symbol");
        assertThat(violations(new CrestDto(0, 0, 16, 0))).containsExactly("base");
        assertThat(violations(new CrestDto(0, 0, 1, 16))).containsExactly("detail");
    }

    @Test
    void negativeOrMissingIdsAreRejected() {
        assertThat(violations(new CrestDto(-1, 0, 1, 0))).containsExactly("shape");
        assertThat(violations(new CrestDto(null, null, null, null)))
                .containsExactlyInAnyOrder("shape", "symbol", "base", "detail");
    }

    @Test
    void aRegistrationsCrestIsValidatedToo() {
        RegisterRequest request = new RegisterRequest("crest_fan", "fan@example.com", "secret123",
                LocalDate.of(2000, 1, 1), 36.9, 30.6, null, null, null, null, null, null,
                new CrestDto(12, 0, 1, 0), false, null);

        assertThat(validator.validate(request).stream().map(v -> v.getPropertyPath().toString()).toList())
                .containsExactly("crest.shape");
    }

    private static List<String> violations(CrestDto crest) {
        return validator.validate(crest).stream().map(v -> v.getPropertyPath().toString()).toList();
    }
}
