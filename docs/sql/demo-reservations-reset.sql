-- 반복 시연 전에 확정 예매만 정리하는 수동 실행 SQL (MySQL 8).
-- 실행 전 API 서버를 중지하고 시연 전용 DB를 선택한다.
-- 아래 DB 이름을 실제 시연 전용 DB 이름과 맞춘 뒤 파일 전체를 실행한다.
-- 선택한 DB가 이 이름과 다르면 DELETE는 실행되어도 대상이 0건이다.
-- 마지막 결과의 reset_status가 SKIPPED_WRONG_DATABASE이면 DB 선택부터 다시 확인한다.
--
-- demo-data-seed.sql이 만든 movieId=1, cinemaId=16, showtimeId=1~9만 정리한다.
-- ID 외에 영화명·영화관명·날짜·상영관명도 확인하여 다른 예매는 남긴다.
-- 지역·영화·영화관·회차·좌석 및 다른 회차의 예매는 삭제하지 않는다.
-- 기존 requestId도 함께 사라지므로 새 시연에서는 새로운 UUID를 만든다.
-- 앱 시작이나 API 요청마다 자동 실행하지 않는다.
-- SQL 도구는 오류 발생 시 중단하도록 설정하고 오류가 나면 ROLLBACK한다.

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
  AND s.name IN ('1관', '4관')
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
  AND s.name IN ('1관', '4관')
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
  AND s.name IN ('1관', '4관')
  AND st.start_at >= '2026-10-27 00:00:00'
  AND st.start_at < '2026-10-28 00:00:00'
ORDER BY st.start_at, st.id;
