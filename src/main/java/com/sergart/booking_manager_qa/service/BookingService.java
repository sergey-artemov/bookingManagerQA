package com.sergart.booking_manager_qa.service;

import com.sergart.booking_manager_qa.db.Booking;
import com.sergart.booking_manager_qa.exception.BookingConflictException;
import com.sergart.booking_manager_qa.exception.NotFoundException;
import com.sergart.booking_manager_qa.repository.BookingRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class BookingService {

    private final BookingRepository repository;

    public BookingService(BookingRepository repository) {
        this.repository = repository;
    }

    public List<Booking> findAll() {
        return repository.findAll();
    }

    public Booking findById(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new NotFoundException("Бронирование с id=" + id + " не найдено"));
    }

    public void delete(Long id) {
        repository.deleteById(id);
    }

    public Booking create(Booking booking) {
        checkOverlap(booking.getRoomNumber(), booking, null);
        return repository.save(booking);
    }

    public Booking update(Long id, Booking booking) {
        if (!repository.existsById(id)) {
            throw new NotFoundException("Бронирование с id=" + id + " не найдено");
        }
        booking.setId(id);
        checkOverlap(booking.getRoomNumber(), booking, id);
        return repository.save(booking);
    }

    /**
     * R8: один номер не может быть забронирован дважды на пересекающиеся даты.
     * Интервалы [from, to) — выезд в день заезда новой брони разрешён.
     */
    private void checkOverlap(Integer roomNumber, Booking candidate, Long excludeId) {
        List<Booking> sameRoom = repository.findByRoomNumber(roomNumber);
        for (Booking existing : sameRoom) {
            if (existing.getId().equals(excludeId)) {
                continue; // при обновлении не сравниваем бронь с самой собой
            }
            boolean overlaps = existing.getBookedFrom().isBefore(candidate.getBookedTo())
                    && existing.getBookedTo().isAfter(candidate.getBookedFrom());
            if (overlaps) {
                throw new BookingConflictException(String.format(
                        "Номер %d уже забронирован с %s по %s (конфликт с бронью id=%d %s)",
                        roomNumber, existing.getBookedFrom(), existing.getBookedTo(), existing.getId(), existing.getGuestName()));
            }
        }
    }
}
