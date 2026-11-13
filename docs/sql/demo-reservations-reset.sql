-- 반복 시연 전에 확정 예매만 정리(삭제)하는 수동 실행 SQL (MySQL 8).

SET NAMES utf8mb4;
SET @demo_database = 'lottecinema_demo';

START TRANSACTION;

-- 외래 키의 자식인 확정 좌석을 먼저 정리한다.
DELETE rs
FROM reservation_seat rs
JOIN reservation r ON r.id = rs.reservation_id
JOIN showtime st ON st.id = r.showtime_id AND st.id = rs.showtime_id
JOIN movie m ON m.id = st.movie_id
JOIN screen s ON s.id = st.screen_id
JOIN cinema c ON c.id = s.cinema_id
WHERE DATABASE() = @demo_database
  AND st.id IN (1, 2, 3, 4, 5, 6, 7, 8, 9)
  AND m.id = 1 AND m.title = '극장판 치이카와: 인어 섬의 비밀'
  AND c.id = 16 AND c.name = '용산'
  AND s.name IN ('1관', '4관', '5관')
  AND st.start_at >= '2026-10-27 00:00:00'
  AND st.start_at < '2026-10-28 00:00:00';
SET @deleted_seat_count = ROW_COUNT();

-- 좌석과 같은 조건으로 예매 행을 지운다. 다른 회차의 예매는 유지한다.
DELETE r
FROM reservation r
JOIN showtime st ON st.id = r.showtime_id
JOIN movie m ON m.id = st.movie_id
JOIN screen s ON s.id = st.screen_id
JOIN cinema c ON c.id = s.cinema_id
WHERE DATABASE() = @demo_database
  AND st.id IN (1, 2, 3, 4, 5, 6, 7, 8, 9)
  AND m.id = 1 AND m.title = '극장판 치이카와: 인어 섬의 비밀'
  AND c.id = 16 AND c.name = '용산'
  AND s.name IN ('1관', '4관', '5관')
  AND st.start_at >= '2026-10-27 00:00:00'
  AND st.start_at < '2026-10-28 00:00:00';
SET @deleted_reservation_count = ROW_COUNT();

COMMIT;

SELECT DATABASE() AS selected_database, @demo_database AS expected_database,
       IF(DATABASE() = @demo_database, 'RESET_COMPLETED', 'SKIPPED_WRONG_DATABASE') AS reset_status,
       @deleted_reservation_count AS deleted_reservation_count,
       @deleted_seat_count AS deleted_seat_count;

-- 정상 초기화 후에는 원본 회차 9개가 각각 총77/잔여77이어야 한다.
SELECT st.id AS showtime_id, s.name AS screen_name,
       (SELECT COUNT(*) FROM seat WHERE screen_id = s.id) AS total_seat_count,
       (SELECT COUNT(*) FROM seat WHERE screen_id = s.id)
           - (SELECT COUNT(*) FROM reservation_seat WHERE showtime_id = st.id) AS available_seat_count
FROM showtime st
JOIN screen s ON s.id = st.screen_id
JOIN movie m ON m.id = st.movie_id
JOIN cinema c ON c.id = s.cinema_id
WHERE DATABASE() = @demo_database
  AND st.id IN (1, 2, 3, 4, 5, 6, 7, 8, 9)
  AND m.id = 1 AND m.title = '극장판 치이카와: 인어 섬의 비밀'
  AND c.id = 16 AND c.name = '용산'
  AND s.name IN ('1관', '4관', '5관')
  AND st.start_at >= '2026-10-27 00:00:00'
  AND st.start_at < '2026-10-28 00:00:00'
ORDER BY st.start_at, st.id;
