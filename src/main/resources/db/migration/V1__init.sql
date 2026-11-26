-- V1: схема bookings + начальные тестовые данные.
-- Для чистого старта миграция удаляет её и пересоздаёт.

DROP TABLE IF EXISTS bookings;

CREATE TABLE bookings (
    id           BIGSERIAL PRIMARY KEY,
    room_number  INTEGER      NOT NULL,
    booked_from  DATE         NOT NULL,
    booked_to    DATE         NOT NULL,
    guest_name   VARCHAR(100) NOT NULL,
    comment      VARCHAR(200)
);

CREATE INDEX idx_bookings_room_dates ON bookings (room_number, booked_from, booked_to);

-- 8 тестовых записей (на 2026-11-17 все даты в будущем/сегодня)
INSERT INTO bookings (room_number, booked_from, booked_to, guest_name, comment) VALUES
(101, '2026-12-01', '2026-12-05', 'Иванов Иван Иванович',       'Поздний заезд'),
(101, '2026-12-10', '2026-12-12', 'Петрова Анна Сергеевна',     NULL),
(215, '2026-12-03', '2026-12-20', 'Сидоров Пётр Николаевич',    'С детьми'),
(305, '2026-11-20', '2026-11-25', 'Кузнецова Ольга Ивановна',   NULL),
(540, '2026-12-31', '2027-01-03', 'Смирнов Алексей Викторович', 'Новогодний заезд, переход через год'),
(550, '2027-02-14', '2027-02-16', 'Морозова Елена Дмитриевна',  NULL),
(150, '2026-12-15', '2026-12-15', 'Тестов Тест Тестович',       'Однодневная бронь'),
(201, '2027-01-10', '2027-01-15', 'Волков Дмитрий Андреевич',   'Корпоратив');
