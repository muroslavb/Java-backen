package com.example.demo.validator;

import com.example.demo.annotation.ValidModVersion;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class ModVersionValidator implements ConstraintValidator<ValidModVersion, String> {
    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null || value.isBlank()) {
            return true; // Перевірку на null залишаємо для @NotBlank
        }
        return value.matches("^v?\\d+\\.\\d+(\\.\\d+)?$");
    }
}