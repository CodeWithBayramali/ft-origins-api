package com.codewithbayramali.ftorigins.player.dto;

import com.codewithbayramali.ftorigins.player.service.BodyModifiers;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class AppearanceRequestTest {

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
    void heightStopsAt190() {
        // The character screen goes up to 190 cm; a taller request is refused, and the clamp agrees.
        assertThat(violations(190)).isEmpty();
        assertThat(violations(191)).containsExactly("heightCm");
        assertThat(BodyModifiers.clampHeight(198)).isEqualTo(190);
    }

    private static List<String> violations(int heightCm) {
        return validator.validate(new AppearanceRequest(null, null, null, null, null, heightCm, null)).stream()
                .map(v -> v.getPropertyPath().toString()).toList();
    }
}
