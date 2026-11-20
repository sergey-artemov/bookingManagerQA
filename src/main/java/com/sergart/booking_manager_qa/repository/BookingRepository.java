package com.sergart.booking_manager_qa.repository;

import com.sergart.booking_manager_qa.db.Booking;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface BookingRepository extends JpaRepository<Booking, Long> {

    /**
     * Все брони конкретного номера
     */
    List<Booking> findByRoomNumber(Integer roomNumber);

    /**
     * Брони номера, пересекающиеся с периодом (найдём коллизии)
     */
    List<Booking> findByRoomNumberAndBookedFromLessThanEqualAndBookedToGreaterThanEqual(
            Integer roomNumber, LocalDate to, LocalDate from);
}
