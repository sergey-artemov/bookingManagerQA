package com.sergart.booking_manager_qa.db;

import com.sergart.booking_manager_qa.validation.DatesNotInPast;
import com.sergart.booking_manager_qa.validation.RoomNumber;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Entity
@Table(name = "bookings")
@Data
@NoArgsConstructor
@AllArgsConstructor
@DatesNotInPast
public class Booking {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Номер комнаты: 101-150, 201-250, ..., 501-550
     */
    @NotNull(message = "Номер комнаты обязателен")
    @Column(name = "room_number", nullable = false)
    @RoomNumber  // своя аннотация, см. пункт 2
    private Integer roomNumber;

    /**
     * Забронировано с
     */
    @NotNull(message = "Дата начала бронирования обязательна")
    @Column(name = "booked_from", nullable = false)
    private LocalDate bookedFrom;

    /**
     * Забронировано по
     */
    @NotNull(message = "Дата окончания бронирования обязательна")
    @Column(name = "booked_to", nullable = false)
    private LocalDate bookedTo;

    /**
     * ФИО гостя
     */
    @NotBlank(message = "ФИО гостя обязательно")
    @Size(max = 100, message = "ФИО не должно превышать 50 символов")
    @Column(name = "guest_name", nullable = false, length = 50)
    private String guestName;

    /**
     * Комментарий — может быть пустым
     */
    @Size(max = 200, message = "Комментарий не должен превышать 200 символов")
    @Column(length = 200)
    private String comment;


    /**
     * Дата окончания должна быть не раньше даты начала
     */
    @AssertTrue(message = "Дата окончания должна быть не раньше даты начала")
    @Transient
    public boolean isDateRangeValid() {
        if (bookedFrom == null || bookedTo == null) {
            return true; // пустые даты ловят @NotNull
        }
        return !bookedTo.isBefore(bookedFrom);
    }
}
