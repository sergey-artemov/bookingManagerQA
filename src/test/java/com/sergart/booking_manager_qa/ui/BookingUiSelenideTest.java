package com.sergart.booking_manager_qa.ui;

import com.codeborne.selenide.Configuration;
import com.codeborne.selenide.Selenide;
import com.codeborne.selenide.SelenideElement;
import com.codeborne.selenide.logevents.SelenideLogger;
import com.sergart.booking_manager_qa.db.Booking;
import com.sergart.booking_manager_qa.repository.BookingRepository;
import io.qameta.allure.Description;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Owner;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import io.qameta.allure.Step;
import io.qameta.allure.Story;
import io.qameta.allure.selenide.AllureSelenide;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.LocalDate;
import java.util.concurrent.atomic.AtomicInteger;

import static com.codeborne.selenide.Condition.text;
import static com.codeborne.selenide.Condition.visible;
import static com.codeborne.selenide.Selenide.$;
import static com.codeborne.selenide.Selenide.$x;
import static com.codeborne.selenide.Selenide.open;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
@Epic("Booking management")
@Feature("Bookings UI")
class BookingUiSelenideTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine");

    @LocalServerPort
    int port;

    @Autowired
    BookingRepository bookingRepository;

    private static final AtomicInteger ROOM_SEQ = new AtomicInteger(401);

    @Step("  Получить следущую комнату")
    private String nextRoom() {
        return String.valueOf(ROOM_SEQ.getAndIncrement());
    }

    @Step("  Before All")
    @BeforeAll
    static void setUp() {
        Configuration.headless = true;       // на CI окно не нужно; false — чтобы смотреть глазами
        Configuration.browser = "chrome";
        Configuration.browserSize = "1920x1080";
        Configuration.timeout = 8000;        // дефолт 4s
        System.setProperty("chromeoptions.args", "--lang=ru");

        SelenideLogger.addListener("allure", new AllureSelenide()
                 //    .screenshots(true)            // скриншот при падении шага (по умолчанию true)
                // .savePageSource(true)         // HTML страницы при падении шага (по умолчанию true)
                // .includeSelenideSteps(true)   // каждое действие Selenide как отдельный шаг
        );
    }

    @Test
    @Story("Создание бронирования")
    @DisplayName("Полный цикл бронирования Е2Е  Selenide")
    @Description("Полный цикл бронирования e2e : заполнение, сохранение, удаление с подтверждением")
    @Severity(SeverityLevel.NORMAL)
    @Owner("Sergey")
    void selenide_fullLifecycle_createCheckDeleteWithConfirm() {
        String room = nextRoom();
        String guest = "E2E Guest";
        LocalDate from = LocalDate.now().plusDays(1);
        LocalDate to = LocalDate.now().plusDays(2);

        // 1. Список -> форма -> заполнение -> сохранение
        open("http://localhost:" + port + "/bookings");
        $x("//a[contains(text(),'Новая бронь')]").click();
        $("#roomNumber").setValue(room);
        $("#guestName").setValue(guest);
        //$("#bookedFrom").setValue(from);
        //$("#bookedTo").setValue(to);
        setDate("#bookedFrom".substring(1), from);  // "bookedFrom"
        setDate("bookedTo", to);

        $x("//button[text()='Сохранить']").click();

        // 2. Вернулись в список, бронь видна (автоожидание, не sleep)
        $("#bookings-table").shouldHave(text(room)).shouldHave(text(guest));

        // 3. Проверка второго слоя: данные реально в БД
        assertThat(bookingRepository.findAll())
                .anyMatch(b -> String.valueOf(b.getRoomNumber()).equals(room)
                        && guest.equals(b.getGuestName()));

        // 4а. «Отмена»: dismiss() — диалог отменён, бронь на месте
        SelenideElement row = $x("//tr[td[text()='" + room + "']]");
        row.$("button.btn-danger").click();
        String dialogTextDismiss = Selenide.dismiss();
        assertThat(dialogTextDismiss).contains(room).contains(guest);   // текст брони попал в dismiss
        row.shouldBe(visible);                                   // бронь всё ещё в списке

        // 4б. Удаление: confirm() — принимает диалог
        row.$("button.btn-danger").click();
        String dialogTextConfirm = Selenide.confirm();

        // 5. Строка исчезла из списка (ожидание после перезагрузки страницы)
        assertThat(dialogTextConfirm).contains(room).contains(guest);   // текст брони попал в confirm
        $("#bookings-table").shouldNotHave(text(room));

        // 6. И из БД
        assertThat(bookingRepository.findAll())
                .noneMatch(b -> String.valueOf(b.getRoomNumber()).equals(room));
    }

    @Test
    @Story("Создание бронирования")
    @DisplayName("Бронирование с перекрытием")
    @Description("Создаём  бронирование с перекрытием другого брониования - остаёмся на форме с ошибкой")
    @Severity(SeverityLevel.NORMAL)
    @Owner("Sergey")
    void selenide_createOverlappingBooking_staysOnFormWithError() {
        String room = nextRoom();
        LocalDate from = LocalDate.now().plusDays(1);
        LocalDate to = LocalDate.now().plusDays(2);

        // Предусловие: занятая бронь создаётся напрямую через репозиторий (без UI)
        Booking existing = new Booking();
        existing.setRoomNumber(Integer.parseInt(room));
        existing.setGuestName("Занятая комната");
        existing.setBookedFrom(from);
        existing.setBookedTo(to);
        existing.setComment("предсоздана тестом");
        bookingRepository.save(existing);

        open("http://localhost:" + port + "/bookings");
        $x("//a[text()='Новая бронь']").click();
        $("#roomNumber").setValue(room);
        $("#guestName").setValue("Overlap Guest");
        setDate("bookedFrom", from);  // "bookedFrom"
        setDate("bookedTo", to);

        $x("//button[text()='Сохранить']").click();

        // Остались на форме, блок ошибок виден, и в нём что-то про пересечение
        //Номер 501 уже забронирован
        $(".errors").shouldBe(visible);
        $(".errors").shouldHave(text("Номер " + room + " уже забронирован"));
    }

    @Step("  Форматирование даты : inputId = {inputId}  date = {date}")
    private void setDate(String inputId, LocalDate date) {
        Selenide.executeJavaScript(
                "document.getElementById(arguments[0]).value = arguments[1]",
                inputId, date.toString());   // date.toString() даёт ISO: 2026-11-25
    }
}
