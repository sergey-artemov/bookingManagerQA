package com.sergart.booking_manager_qa;

import com.sergart.booking_manager_qa.repository.BookingRepository;
import io.qameta.allure.Description;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Owner;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import io.qameta.allure.Step;
import io.qameta.allure.Story;
import io.qameta.allure.restassured.AllureRestAssured;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import lombok.extern.log4j.Log4j2;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
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
import java.time.format.DateTimeFormatter;
import java.util.concurrent.atomic.AtomicInteger;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;


@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
@Log4j2
@Epic("Booking management")
@Feature("Bookings API")
class BookingApiTest {

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

    @BeforeAll
    @Step("  Before All")
    static void setUpRestAssured() {
        RestAssured.filters(new AllureRestAssured());
    }

    @BeforeEach
    @Step("  Before Each")
    void setUp() {
        RestAssured.baseURI = "http://localhost";
        RestAssured.port = port;
    }

    // ---------- POS: создание + проверка в БД ----------
    @Test
    @DisplayName("POS: POST валидной брони -> 201, запись есть в БД")
    @Story("Создание бронирования")
    @Description("POST /bookings с корректным JSON должен вернуть 201 и сохранить букинг в БД")
    @Severity(SeverityLevel.CRITICAL)
    @Owner("Sergey")
    void createBooking_success() {
        String room = nextRoom();
        String body = """
                {
                  "roomNumber": %s,
                  "bookedFrom": "2030-06-01",
                  "bookedTo": "2030-06-05",
                  "guestName": "Тестов Тест Тестович",
                  "comment": "автотест POS"
                }
                """.formatted(room);

        Integer id = given()
                .contentType(ContentType.JSON)
                .body(body)
                .when()
                .post("/api/bookings/new")
                .then()
                .statusCode(201)
                .body("id", notNullValue())
                .body("roomNumber", equalTo(Integer.valueOf(room)))
                .extract().path("id");

        // проверка второго слоя: запись реально в базе
        assertThat(bookingRepository.findById(Long.valueOf(id))).isPresent();
    }

    @Test
    @DisplayName("POS: DELETE валидной брони -> 204, нет записи в БД")
    @Story("Удаление бронирования")
    @Description("DELETE /bookings с корректным id должен вернуть 204 и удалить букинг из БД")
    @Severity(SeverityLevel.CRITICAL)
    @Owner("Sergey")
    void deleteBooking_success() {
        int room = Integer.parseInt(nextRoom());
        int id = createBooking(room, "2030-07-01", "2030-07-05");

        given()
                .when()
                .delete("/api/bookings/" + id)
                .then()
                .statusCode(204);

        given()
                .when()
                .get("/api/bookings/" + id)
                .then()
                .statusCode(404);
        assertThat(bookingRepository.findById((long) id)).isEmpty();
    }

    // ---------- NEG: валидация полей ----------

    @Test
    @DisplayName("NEG: POST с roomNumber=999 -> 400 с сообщением о диапазоне")
    @Story("Создание бронирования")
    @Description("POST /bookings с некорректным JSON должен вернуть 400")
    @Severity(SeverityLevel.CRITICAL)
    @Owner("Sergey")
    void createBooking_invalidRoom() {
        String body = """
                {
                  "roomNumber": 999,
                  "bookedFrom": "2030-06-01",
                  "bookedTo": "2030-06-05",
                  "guestName": "Тестов Тест Тестович"
                }
                """;

        given()
                .contentType(ContentType.JSON)
                .body(body)
                .when()
                .post("/api/bookings/new")
                .then()
                .statusCode(400)
                .body("errors", hasItem(containsString("Номер комнаты")));
    }

    @Test
    @DisplayName("NEG: POST с пустым запросом -> 400  с сообщением о незаполненных полях")
    @Story("Создание бронирования")
    @Description("POST /bookings с пустым JSON должен вернуть 400 и несколько сообщений о незаполненных полях")
    @Severity(SeverityLevel.CRITICAL)
    @Owner("Sergey")
    void createBooking_emptyRequest() {
        String body = "{}";

        given()
                .contentType(ContentType.JSON)
                .body(body)
                .when()
                .post("/api/bookings/new")
                .then()
                .statusCode(400)
                .body("status", equalTo(400))
                .body("errors", hasItem(containsString("Номер комнаты обязателен")))
                .body("errors", hasItem(containsString("ФИО гостя обязательно")))
                .body("errors", hasItem(containsString("Дата начала бронирования обязательна")))
                .body("errors", hasItem(containsString("Дата окончания бронирования обязательна")))
        ;
    }

    @Test
    @DisplayName("NEG: POST с одинаковыми датами -> 400 с сообщением о датах")
    @Story("Создание бронирования")
    @Description("POST /bookings с некорректным JSON (одинаковые даты) должен вернуть 400 и сообщение о датах")
    @Severity(SeverityLevel.CRITICAL)
    @Owner("Sergey")
    void createBooking_equalDates() {
        String room = nextRoom();
        String body = """
                {
                  "roomNumber": %s,
                  "bookedFrom": "2030-06-01",
                  "bookedTo": "2030-06-01",
                  "guestName": "Тестов1 Тест1 Тестович1"
                }
                """.formatted(room);

        given()
                .contentType(ContentType.JSON)
                .body(body)
                .when()
                .post("/api/bookings/new")
                .then()
                .statusCode(400)
                .body("errors", hasItem(containsString("Дата окончания должна быть позже даты начала")));
    }

    @Test
    @DisplayName("NEG: POST дата начала больше чем дата окончания -> 400 с сообщением о датах")
    @Story("Создание бронирования")
    @Description("POST /bookings с некорректным JSON (дата начала больше даты окончания) должен вернуть 400 и сообщение о датах")
    @Severity(SeverityLevel.CRITICAL)
    @Owner("Sergey")
    void createBooking_FromDateAfterToDate() {
        String room = nextRoom();
        String body = """
                {
                  "roomNumber": %s,
                  "bookedFrom": "2030-06-01",
                  "bookedTo": "2030-05-10",
                  "guestName": "Тестов Тест Тестович"
                }
                """.formatted(room);

        given()
                .contentType(ContentType.JSON)
                .body(body)
                .when()
                .post("/api/bookings/new")
                .then()
                .statusCode(400)
                .body("errors", hasItem(containsString("Дата окончания должна быть позже даты начала")));
    }

    @Test
    @DisplayName("NEG: POST  дата начала или окончания меньше текущей-> 400 с сообщением о датах")
    @Story("Создание бронирования")
    @Description("POST /bookings с некорректным JSON (дата начала или окончания меньше текущей) должен вернуть 400 и сообщение о датах")
    @Severity(SeverityLevel.CRITICAL)
    @Owner("Sergey")
    void createBooking_DatesLessThatToday() {
        LocalDate from = LocalDate.now().minusDays(10);
        LocalDate to = LocalDate.now().minusDays(5);
        String room = nextRoom();
        String body = """
                {
                  "roomNumber": %s,
                  "bookedFrom": "%s",
                  "bookedTo": "%s",
                  "guestName": "Тестов Тест Тестович"
                }
                """.formatted(room,
                from.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")),
                to.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")));

        given()
                .contentType(ContentType.JSON)
                .body(body)
                .when()
                .post("/api/bookings/new")
                .then()
                .statusCode(400)
                .body("errors", hasItem(containsString("не ранее текущей")));
    }


    // ---------- NEG: BUG-001, пересечение интервалов ----------

    @Test
    @DisplayName("NEG: пересечение дат на один номер -> 409 (регресс BUG-001)")
    @Story("Создание бронирования")
    @Description("POST /bookings с пересечением дат с другим бронированием должен вернуть 400 и сообщение о пересечении")
    @Severity(SeverityLevel.CRITICAL)
    @Owner("Sergey")
    void createBooking_overlap_conflict() {
        int room = Integer.parseInt(nextRoom());
        postBooking(room, "2030-07-01", "2030-07-05", 201);   // базовая бронь
        postBooking(room, "2030-07-04", "2030-07-10", 409);   // пересекается -> конфликт
    }

    @Test
    @DisplayName("POS: стык день в день (выезд 05.07 -> заезд 05.07) -> 201")
    @Story("Создание бронирования")
    @Description("POST /bookings с пересечением дат с другим бронированием стык в стык должен вернуть 201")
    @Severity(SeverityLevel.CRITICAL)
    @Owner("Sergey")
    void createBooking_backToBackAllowed() {
        int room = Integer.parseInt(nextRoom());
        postBooking(room, "2030-07-01", "2030-07-05", 201);
        postBooking(room, "2030-07-05", "2030-07-08", 201);   // стык разрешён
    }

    // ---------- NEG: несуществующий id ----------
    @Test
    @DisplayName("NEG: GET несуществующего id -> 404 с JSON-ошибкой")
    @Story("Получение бронирования")
    @Description("GET /bookings с несуществующим id должен вернуть 404")
    @Severity(SeverityLevel.CRITICAL)
    @Owner("Sergey")
    void getBooking_notFound() {
        given()
                .when()
                .get("/api/bookings/999999")
                .then()
                .statusCode(404)
                .body("status", equalTo(404))
                .body("errors", hasItem(containsString("не найдено")));
    }

    @Test
    @DisplayName("NEG: DELETE отсутствующей брони -> 404, нет записи в БД")
    @Story("Удаление бронирования")
    @Description("DELETE /bookings с несуществующим id должен вернуть 404")
    @Severity(SeverityLevel.CRITICAL)
    @Owner("Sergey")
    void deleteBooking_fail() {
        assertThat(bookingRepository.findById(1234567L)).isEmpty();
        given()
                .when()
                .delete("/api/bookings/1234567")
                .then()
                .statusCode(404)
                .body("status", equalTo(404))
                .body("errors", hasItem(containsString("Бронирование с id=1234567 не найдено")));
    }

    //========== Full Life Cycle Test ============================
    @Test
    @DisplayName("E2E: создать -> прочитать -> обновить -> поймать конфликт -> удалить")
    @Story("Полный цикл")
    @Description("Полный цикл E2E : создание - чтение - обновление - конфликт - удаление")
    @Severity(SeverityLevel.CRITICAL)
    @Owner("Sergey")
    void fullLifecycle_scenario() {
        int roomNumber = Integer.parseInt(nextRoom());
        int bookingSize = 8;  // заполнение из V1_init.sql

        // 1. Создание
        Integer bookingId = createBooking(roomNumber, "2030-09-01", "2030-09-05");
        log.info("1. CREATED: Booking for room {} is {}", roomNumber, bookingId);

        // 2. Чтение: API и БД согласованы
        given()
                .when()
                .get("/api/bookings/" + bookingId)
                .then()
                .statusCode(200)
                .body("roomNumber", equalTo(roomNumber))
                .body("bookedFrom", equalTo("2030-09-01"))
                .body("bookedTo", equalTo("2030-09-05"));
        assertThat(bookingRepository.findById(Long.valueOf(bookingId))).isPresent();
        bookingSize++;
        log.info("2. booking exists in DBA");

        // 3. Обновление: меняем даты и гостя
        given()
                .contentType(ContentType.JSON)
                .body("""
                        {
                          "roomNumber": %s,
                          "bookedFrom": "2030-09-02",
                          "bookedTo": "2030-09-06",
                          "guestName": "Обновлённый Гость",
                          "comment": "изменено e2e-тестом"
                        }
                        """.formatted(roomNumber))
                .when()
                .put("/api/bookings/" + bookingId)
                .then()
                .statusCode(200)
                .body("guestName", equalTo("Обновлённый Гость"));
        log.info("3. PUT new data for room {} ", roomNumber);

        // 4. Конфликт: другая бронь пересекается с обновлённым интервалом
        postBooking(roomNumber, "2030-09-05", "2030-09-08", 409);
        // и стык по-прежнему разрешён
        postBooking(roomNumber, "2030-09-06", "2030-09-08", 201);
        bookingSize++;
        log.info("4. Check conflict");

        // 5. Удаление: API и БД
        given()
                .when()
                .delete("/api/bookings/" + bookingId)
                .then()
                .statusCode(204);
        assertThat(bookingRepository.findById(Long.valueOf(bookingId))).isEmpty();
        bookingSize--;
        log.info("5. DELETE booking {}", bookingId);

        // 6. Остальные брони должны остаться
        Response response = given()
                .when()
                .get("/api/bookings")
                .then().statusCode(200)
                .extract().response();
        int count = response.jsonPath().getList("$").size();
        log.info("6. check other booking, count = {}, expected = {}", count, bookingSize);
        assertEquals(bookingSize, count, "Количество бронирований не совпадает");

    }

    // ---------- хелпер ----------
    @Step("  Создать бронирование: номер {room} с {from} по {to}, ожидаемый статус {expectedStatus}")
    private void postBooking(int room, String from, String to, int expectedStatus) {
        given()
                .contentType(ContentType.JSON)
                .body(bookingJson(room, from, to))
                .when()
                .post("/api/bookings/new")
                .then()
                .statusCode(expectedStatus);
    }

    @Step("  Создать бронирование: номер {room} с {from} по {to}")
    private Integer createBooking(int room, String from, String to) {
        return given()
                .contentType(ContentType.JSON)
                .body(bookingJson(room, from, to))
                .when()
                .post("/api/bookings/new")
                .then()
                .statusCode(201)
                .extract().path("id");
    }


    @Step("  Создание JSON бронирования: номер {room} с {from} по {to}")
    private String bookingJson(int room, String from, String to) {
        return """
                {
                  "roomNumber": %d,
                  "bookedFrom": "%s",
                  "bookedTo": "%s",
                  "guestName": "Тестов Тест Тестович"
                }
                """.formatted(room, from, to);
    }
}
