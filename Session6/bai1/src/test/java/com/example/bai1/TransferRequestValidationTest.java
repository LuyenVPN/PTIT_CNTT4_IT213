package com.example.bai1;

import com.example.bai1.dto.TransferRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;

class TransferRequestValidationTest {

    private static ValidatorFactory validatorFactory;
    private static Validator validator;

    @BeforeAll
    static void setUp() {
        validatorFactory = Validation.buildDefaultValidatorFactory();
        validator = validatorFactory.getValidator();
    }

    @AfterAll
    static void tearDown() {
        validatorFactory.close();
    }

    @Test
    void shouldRejectInvalidTransferRequest() {

        TransferRequest request = new TransferRequest(
                null,              // senderAccountId sai
                "",                // receiverAccountNumber sai
                "XYZ",             // bankCode sai
                new BigDecimal("5000"), // amount sai
                "Test transfer"
        );

        Set<ConstraintViolation<TransferRequest>> violations =
                validator.validate(request);

        assertFalse(violations.isEmpty());

        System.out.println("===== VALIDATION ERRORS =====");

        violations.forEach(error ->
                System.out.println(
                        error.getPropertyPath()
                                + " : "
                                + error.getMessage()
                )
        );
    }
}
