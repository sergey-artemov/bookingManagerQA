package com.sergart.booking_manager_qa.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Documented
@Constraint(validatedBy = DatesNotInPastValidator.class)
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface DatesNotInPast {
    String message() default "Даты должны быть не ранее текущей";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
