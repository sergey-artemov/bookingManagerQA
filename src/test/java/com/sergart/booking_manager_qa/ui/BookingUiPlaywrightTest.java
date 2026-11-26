package com.sergart.booking_manager_qa.ui;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import com.microsoft.playwright.assertions.PlaywrightAssertions;
import com.microsoft.playwright.options.AriaRole;
import com.sergart.booking_manager_qa.repository.BookingRepository;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class BookingUiPlaywrightTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine");

    @LocalServerPort
    int port;

    @Autowired
    BookingRepository bookingRepository;

    Playwright playwright;
    Browser browser;
    Page page;

    // Перехват confirm-диалогов: Selenide тут был однострочником, здесь — обработчик
    final List<String> dialogMessages = new ArrayList<>();
    final AtomicBoolean acceptDialogs = new AtomicBoolean(false);

    private static final AtomicInteger ROOM_SEQ = new AtomicInteger(401);

    private String nextRoom() {
        return String.valueOf(ROOM_SEQ.getAndIncrement());
    }

    @BeforeAll
    void launchBrowser() {
        playwright = Playwright.create();
        browser = playwright.chromium().launch();   // headless по умолчанию
    }

    @BeforeEach
    void createPage() {
        acceptDialogs.set(false);
        dialogMessages.clear();
        page = browser.newContext().newPage();      // свежий контекст — состояние изолировано
        page.onDialog(dialog -> {
            dialogMessages.add(dialog.message());
            if (acceptDialogs.get()) {
                dialog.accept();
            } else {
                dialog.dismiss();
            }
        });
    }

    @AfterAll
    void close() {
        browser.close();
        playwright.close();
    }

    @Test
    void playwright_fullLifecycle_createCheckDeleteWithConfirm() {
        String room = nextRoom();
        String guest = "PW Guest";
        String from = LocalDate.now().plusDays(1).toString();
        String to = LocalDate.now().plusDays(2).toString();

        // 1. Список -> форма
        page.navigate("http://localhost:" + port + "/bookings");
        page.getByRole(AriaRole.LINK, new Page.GetByRoleOptions().setName("Новая бронь")).click();

        // 2. Заполнение: fill() на date-инпут принимает ISO напрямую — без JS-костылей
        page.getByLabel("Номер").fill(room);
        page.getByLabel("Гость").fill(guest);
        page.getByLabel("Дата заезда").fill(from);
        page.getByLabel("Дата выезда").fill(to);
        page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Сохранить")).click();

        // 3. Строка появилась в списке
        Locator row = page.locator("#bookings-table tbody tr")
                .filter(new Locator.FilterOptions().setHasText(room));
        PlaywrightAssertions.assertThat(row).isVisible();

        // 4. Данные в БД
        assertThat(bookingRepository.findAll())
                .anyMatch(b -> String.valueOf(b.getRoomNumber()).equals(room)
                        && guest.equals(b.getGuestName()));

        // 5. «Отмена» в confirm: handler по умолчанию dismiss
        row.getByRole(AriaRole.BUTTON, new Locator.GetByRoleOptions().setName("Удалить")).click();
        assertThat(dialogMessages).anySatisfy(msg -> {
            assertThat(msg).contains(room).contains(guest);
        });
        PlaywrightAssertions.assertThat(row).isVisible();   // бронь на месте

        // 6. «OK»: переключаем флаг — следующий диалог принимается
        acceptDialogs.set(true);
        row.getByRole(AriaRole.BUTTON, new Locator.GetByRoleOptions().setName("Удалить")).click();

        // 7. Строка исчезла из списка и из БД
        PlaywrightAssertions.assertThat(row).hasCount(0);
        assertThat(bookingRepository.findAll())
                .noneMatch(b -> String.valueOf(b.getRoomNumber()).equals(room));
    }
}
