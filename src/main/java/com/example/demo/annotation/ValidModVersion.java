package com.example.demo.annotation;

import com.example.demo.validator.ModVersionValidator;
import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.*;

@Constraint(validatedBy = ModVersionValidator.class)
@Target({ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidModVersion {
    String message() default "Версія має бути у форматі чисел, наприклад '1.0' або 'v1.0'";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}