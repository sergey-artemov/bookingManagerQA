package com.sergart.booking_manager_qa.validation;

import com.sergart.booking_manager_qa.db.Booking;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class DatesNotInPastValidator implements ConstraintValidator<DatesNotInPast, Booking> {

    private static final DateTimeFormatter RU_DATE =
            DateTimeFormatter.ofPattern("dd.MM.yyyy");

    @Override
    public boolean isValid(Booking booking, ConstraintValidatorContext context) {
        if (booking.getBookedFrom() == null || booking.getBookedTo() == null) {
            return true; // null ловит @NotNull
        }
        LocalDate today = LocalDate.now();
        boolean ok = !booking.getBookedFrom().isBefore(today)
                && !booking.getBookedTo().isBefore(today);
        if (!ok) {
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate(
                            "Даты должны быть не ранее текущей (" + today.format(RU_DATE) + ")")
                    .addConstraintViolation();
        }
        return ok;
    }
}
