package com.sergart.booking_manager_qa.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Documented
@Constraint(validatedBy = RoomNumberValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface RoomNumber {
    String message() default "Номер комнаты должен быть в диапазоне 101-150, 201-250, ..., 501-550";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
