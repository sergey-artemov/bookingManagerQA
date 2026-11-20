package com.sergart.booking_manager_qa.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class RoomNumberValidator implements ConstraintValidator<RoomNumber, Integer> {

    @Override
    public boolean isValid(Integer roomNumber, ConstraintValidatorContext context) {
        if (roomNumber == null) {
            return true; // null ловит @NotNull, дублировать сообщение не нужно
        }
        int floor = roomNumber / 100;      // 1..5
        int room  = roomNumber % 100;      // 01..50
        return floor >= 1 && floor <= 5
                && room >= 1 && room <= 50;
    }
}
