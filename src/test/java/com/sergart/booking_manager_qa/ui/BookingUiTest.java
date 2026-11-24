package com.sergart.booking_manager_qa.ui;

import com.sergart.booking_manager_qa.db.Booking;
import com.sergart.booking_manager_qa.repository.BookingRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class BookingUiTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine");

    @Autowired
    MockMvc mockMvc;

    @Autowired
    BookingRepository bookingRepository;

    // Уникальные номера из диапазона 501–550, чтобы тесты не мешали друг другу
    private static final AtomicInteger ROOM_SEQ = new AtomicInteger(501);

    private String nextRoom() {
        return String.valueOf(ROOM_SEQ.getAndIncrement());
    }

    private Booking saveBooking(String room, String guestName, LocalDate from, LocalDate to) {
        Booking b = new Booking();
        b.setRoomNumber(Integer.valueOf(room));
        b.setGuestName(guestName);
        b.setBookedFrom(from);
        b.setBookedTo(to);
        return bookingRepository.save(b);
    }

    private String postForm(String action, String room, String guest,
                            String from, String to) throws Exception {
        var result = mockMvc.perform(post(action)
                        .param("roomNumber", room)
                        .param("guestName", guest)
                        .param("bookedFrom", from)
                        .param("bookedTo", to))
                .andDo(print())   // полная картина: статус, заголовки, тело
                .andReturn();
        return result.getResponse().getContentAsString(StandardCharsets.UTF_8);
    }

    @Test
    void ui_list_showsCreatedBookings() throws Exception {
        String room = nextRoom();
        saveBooking(room, "Alice", LocalDate.now().plusDays(1), LocalDate.now().plusDays(3));

        mockMvc.perform(get("/bookings"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Alice")))
                .andExpect(content().string(containsString(room)));
    }

    @Test
    void ui_createForm_rendersWithFields() throws Exception {
        mockMvc.perform(get("/bookings"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Номер")))
                .andExpect(content().string(containsString("Гость")))
                .andExpect(content().string(containsString("С")))
                .andExpect(content().string(containsString("По")));
    }

    @Test
    void ui_create_validForm_redirectsToListAndPersists() throws Exception {
        String room = nextRoom();

        mockMvc.perform(post("/bookings")
                        .param("roomNumber", room)
                        .param("guestName", "Sergey")
                        .param("bookedFrom", LocalDate.now().plusDays(1).toString())
                        .param("bookedTo", LocalDate.now().plusDays(2).toString()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/bookings"));

        assertThat(bookingRepository.findAll())
                .anyMatch(b -> String.valueOf(b.getRoomNumber()).equals(room)
                        && "Sergey".equals(b.getGuestName()));
    }

    @Test
    void ui_create_invalidRoom_staysOnFormWithError() throws Exception {
        long before = bookingRepository.count();

        String html = postForm("/bookings", "999", "Guest", // 999 вне диапазонов R1
                LocalDate.now().plusDays(1).toString(),
                LocalDate.now().plusDays(2).toString());

        assertThat(html).contains("class=\"errors\"");  // форма вернулась с блоком ошибок
        assertThat(bookingRepository.count()).isEqualTo(before); // в БД ничего не попало
    }

    @Test
    void ui_create_overlap_showsConflictError() throws Exception {
        String room = nextRoom();
        LocalDate from = LocalDate.now().plusDays(5);
        LocalDate to = LocalDate.now().plusDays(7);
        saveBooking(room, "First", from, to);

        String html = postForm("/bookings", room, "Second",
                from.plusDays(1).toString(), to.plusDays(1).toString());

        assertThat(html).contains("class=\"errors\""); // R8 отработало через UI
        assertThat(html).contains("Номер %s уже забронирован".formatted(room));
        assertThat(bookingRepository.findAll()
                .stream().filter(b -> String.valueOf(b.getRoomNumber()).equals(room)).count())
                .isEqualTo(1); // вторая бронь не создалась
    }

    @Test
    void ui_edit_prefilledFormAndSaves() throws Exception {
        String room = nextRoom();
        Booking saved = saveBooking(room, "Old Name",
                LocalDate.now().plusDays(10), LocalDate.now().plusDays(12));

        // Форма предзаполнена
        mockMvc.perform(get("/bookings/{id}/edit", saved.getId()))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Old Name")));

        // Сохраняем с новым гостем
        mockMvc.perform(post("/bookings/{id}/edit", saved.getId())
                        .param("roomNumber", room)
                        .param("guestName", "New Name")
                        .param("bookedFrom", LocalDate.now().plusDays(10).toString())
                        .param("bookedTo", LocalDate.now().plusDays(12).toString()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/bookings"));

        assertThat(bookingRepository.findById(saved.getId()).orElseThrow().getGuestName())
                .isEqualTo("New Name");
    }

    @Test
    void ui_delete_removesBooking() throws Exception {
        String room = nextRoom();
        Booking saved = saveBooking(room, "To Delete",
                LocalDate.now().plusDays(15), LocalDate.now().plusDays(17));

        mockMvc.perform(post("/bookings/{id}/delete", saved.getId()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/bookings"));

        assertThat(bookingRepository.findById(saved.getId())).isEmpty();
    }

    @Test
    void ui_edit_missingBooking_redirectsToList() throws Exception {
        mockMvc.perform(get("/bookings/{id}/edit", 999999L))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/bookings"));
    }
}
